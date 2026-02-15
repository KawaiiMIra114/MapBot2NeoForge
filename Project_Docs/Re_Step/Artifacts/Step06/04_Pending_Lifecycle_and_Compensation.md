# 04 Pending Lifecycle and Compensation

## 元数据
| 属性 | 值 |
|---|---|
| Step | Step-06 C1 |
| RUN_ID | 20260215T193300Z |
| 评审日期 | 2026-02-15 |
| 评审者 | AI Agent (主代理) |
| 依据 | FAILURE_MODEL.md §3.6 + 代码静态追踪 |

## 1. Pending 请求类型分析

### 1.1 当前 pending 请求机制
| 组件 | 实现方式 | 位置 |
|---|---|---|
| BridgeClient | `ConcurrentHashMap<String, CompletableFuture<String>>` pendingRequests | BridgeClient L52 |
| SignManager | `KEY_PENDING` Redis key (签到待领取) | SignManager L22 |

### 1.2 滞留上限对照
| 请求类型 | FAILURE_MODEL 定义 | 实际实现 | 判定 |
|---|---|---|---|
| 查询类 (get_*, has_*, resolve_*) | 15s | 10s 统一超时 | ⚠ 可接受但不精确 |
| 变更类 (bind_*, whitelist_*, give_item) | 120s | 10s 统一超时 | ❌ 远低于定义 |
| 文件类 (file_*) | 30s | 10s 统一超时 | ❌ 低于定义 |
| 控制类 (stop_server, execute_command) | 20s | 10s 统一超时 | ❌ 低于定义 |
| 签到领取 (SignManager) | 86400s (1天) | TTL 86400 Redis | ✅ 有 TTL |

### 1.3 统一超时问题
当前 BridgeProxy 使用 `CompletableFuture.get(10, TimeUnit.SECONDS)` 统一 10s 超时。
- ✅ 优点: 简单一致，不会无限挂起。
- ❌ 问题: 变更类请求需要更长的滞留窗口（120s）以等待跨服回执。
- ❌ 问题: 超时后无"待确认队列"保留请求上下文。

## 2. 到期动作对照

### 2.1 查询类
| 定义动作 | 实际动作 | 判定 |
|---|---|---|
| 标记 FAILED_TIMEOUT | 返回 "TIMEOUT" 字符串 | ⚠ 无状态标记 |
| 返回降级结果 | 返回错误字符串 | ⚠ 无 stale cache |

### 2.2 变更类
| 定义动作 | 实际动作 | 判定 |
|---|---|---|
| 进入 COMPENSATING | ❌ 直接返回失败 | 缺失 |
| 触发补偿任务 | ❌ 未实现 | 缺失 |

### 2.3 控制类
| 定义动作 | 实际动作 | 判定 |
|---|---|---|
| 标记失败并上报警报 | 返回错误字符串到 QQ | ⚠ 无告警 |
| 不自动重复执行 | ✅ 不重试 | 安全 |

## 3. 失败上报评审

### 3.1 结构化事件
| 定义字段 | 实现 | 判定 |
|---|---|---|
| request_id | ✅ UUID (pendingRequests key) | 有 |
| action | ❌ 未记录操作类型 | 缺失 |
| server_id | ❌ 未记录目标服务器 | 缺失 |
| state_from / state_to | ❌ 无状态机 | 缺失 |
| elapsed_ms | ❌ 未记录耗时 | 缺失 |
| error_code | ❌ 字符串前缀替代 | 缺失 |

### 3.2 失败上报目标
| 定义目标 | 实现 | 判定 |
|---|---|---|
| 业务日志 (error 级) | ⚠ LOGGER.warn (非 error) | 部分 |
| 告警通道 (5分钟5次) | ❌ 未实现 | 缺失 |
| 审计记录 (可追溯) | ❌ 未实现 | 缺失 |

## 4. 补偿动作评审

### 4.1 定义 vs 实现
| 操作类型 | 定义补偿 | 实际实现 | 判定 |
|---|---|---|---|
| whitelist_add/remove | 权威快照重放 | ❌ 无自动补偿 | 缺失 |
| give_item 多服发放 | 补偿任务/离线CDK | ❌ 无补偿 | 缺失 |
| bind/unbind | 权威表回写+白名单同步 | ⚠ 手动重试 | 部分 |
| playtime_add | request_id 去重补写 | ❌ 无补偿 | 缺失 |

## 5. 签到 Pending (SignManager) 专项

### 5.1 生命周期
```
签到 → Redis SET KEY_PENDING+qq (TTL 86400s) → 玩家上线领取 → DEL KEY_PENDING
```

### 5.2 评审
| 项目 | 判定 |
|---|---|
| 有 TTL 限制 (86400s) | ✅ |
| 到期后自动清理 | ✅ Redis TTL |
| 重复签到防护 | ✅ 检查已签到标记 |
| 领取后清理 | ✅ DEL |
| 补偿：过期未领取 | ⚠ 静默过期，无通知 |

## 6. 差距汇总

| ID | 描述 | 风险 | 修复建议 |
|---|---|---|---|
| PL-01 | 变更类请求无待确认队列 | **High** | 引入持久化待确认队列 |
| PL-02 | 失败上报无结构化事件 | **High** | 落地 request_id+action+elapsed_ms+error_code 日志格式 |
| PL-03 | 补偿机制完全缺失 | **High** | 按 FAILURE_MODEL §3.6.3 补实现 |
| PL-04 | 告警阈值未配置 | **Medium** | 5分钟5次失败触发告警 |
| PL-05 | 超时未按请求类型区分 | **Medium** | 查询2s/变更120s/文件30s/控制20s |
| PL-06 | 签到过期静默丢弃 | **Low** | 过期前通知或续期机制 |

## 7. 结论
- **High**: 3 项 (PL-01, PL-02, PL-03)
- **Medium**: 2 项 (PL-04, PL-05)
- **Low**: 1 项 (PL-06)
- pending 基本生命周期已有（超时/清理），但缺乏结构化的失败上报和补偿闭环。
