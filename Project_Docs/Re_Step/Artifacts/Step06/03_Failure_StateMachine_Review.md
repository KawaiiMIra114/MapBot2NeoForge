# 03 Failure StateMachine Review

## 元数据
| 属性 | 值 |
|---|---|
| Step | Step-06 C1 |
| RUN_ID | 20260215T193300Z |
| 评审日期 | 2026-02-15 |
| 评审者 | AI Agent (主代理) |
| 依据 | FAILURE_MODEL.md §3.1-3.6 + 代码静态追踪 |

## 1. 状态机实现评审

### 1.1 定义 vs 实现对照
| 状态 | FAILURE_MODEL 定义 | 代码实现 | 判定 |
|---|---|---|---|
| `PENDING` | 请求已发出，等待响应 | ✅ pendingRequests (ConcurrentHashMap) + SignManager KEY_PENDING | 部分实现 |
| `ACKED` | 请求已被接收方确认 | ❌ 未实现 | 缺失 |
| `COMMITTED` | 权威侧确认完成 | ❌ 未实现（返回字符串 SUCCESS） | 缺失 |
| `PREPARED` | 意图已记录但未确认 | ❌ 未实现 | 缺失 |
| `COMPENSATED` | 超时/失败后补偿完成 | ❌ 未实现 | 缺失 |
| `COMPENSATING` | 补偿任务执行中 | ❌ 未实现 | 缺失 |
| `DEGRADED` | 断连后降级模式 | ❌ 未实现（仅返回错误字符串） | 缺失 |
| `FAILED_TIMEOUT` | 超时失败 | ❌ 未实现（返回 "FAIL:xxx" 字符串） | 缺失 |
| `FAILED_DISCONNECT` | 断连失败 | ❌ 未实现 | 缺失 |
| `FAILED_RETRY_EXHAUSTED` | 重试耗尽 | ❌ 未实现 | 缺失 |
| `RETRY_SCHEDULED` | 重试已安排 | ❌ 未实现（重连有指数退避） | 缺失 |

### 1.2 实际错误处理模式
当前系统使用**字符串前缀模式**而非状态机：
```
"SUCCESS:xxx"  → 成功
"FAIL:xxx"     → 失败（无分类）
"INVALID:xxx"  → 输入错误
"TIMEOUT"      → 超时（BridgeClient 10s 统一超时）
```

### 1.3 状态迁移一致性
| 迁移路径 (定义) | 触发条件 | 日志字段 | 对外回执 | 判定 |
|---|---|---|---|---|
| PENDING → ACKED → COMMITTED | 正常响应 | ❌ 无 | "SUCCESS:..." | ❌ 无状态感知 |
| PENDING → FAILED_TIMEOUT | 10s 超时 | ⚠ 仅 LOGGER.warn | "TIMEOUT" | ❌ 无结构化 |
| PENDING → FAILED_DISCONNECT | 连接断开 | ⚠ 仅 LOGGER.warn | "FAIL:Bridge 未连接" | ❌ 无分类 |
| FAILED → COMPENSATING → COMPENSATED | 需要补偿 | ❌ 未实现 | ❌ 未实现 | ❌ 完全缺失 |

## 2. 超时策略评审

### 2.1 定义 vs 实现
| 链路 | 定义超时 | 实现超时 | 判定 |
|---|---|---|---|
| 查询类请求 | 2s | 10s (统一) | ⚠ 超出定义 |
| 变更类请求 | 3s | 10s (统一) | ⚠ 超出定义 |
| 管理 API (HTTP) | 5s | 无显式超时 | ❌ 缺失 |
| 心跳检测 | 连续3个周期 | 30s 间隔 (BridgeClient L39) | ✅ 有实现 |

### 2.2 超时后行为
| 链路 | 定义行为 | 实际行为 | 判定 |
|---|---|---|---|
| 查询类 | 失败快返 + stale cache | 返回 "TIMEOUT" 字符串 | ⚠ 无 stale cache |
| 变更类 | 待确认队列 | 完成 Future 并返回失败 | ❌ 无待确认队列 |
| HTTP | 返回错误码 | N/A | ❌ 无超时设置 |

## 3. 断连策略评审

### 3.1 重连机制
| 定义要求 | 实现情况 | 判定 |
|---|---|---|
| 进入 DEGRADED 模式 | ❌ 无 DEGRADED 标记 | 缺失 |
| 指数退避 + 抖动 | ⚠ 固定 5s 延迟 (RECONNECT_DELAY_MS) | 不符合 |
| 保留请求上下文 | ⚠ pendingRequests 保留但无 request_id | 部分 |

### 3.2 断连后 pending 请求
| 定义 | 实现 | 判定 |
|---|---|---|
| 查询类 15s 滞留上限 | 10s 统一超时（近似） | ⚠ 可接受 |
| 变更类 120s 滞留上限 | 10s 统一超时 | ❌ 低于定义 |
| 超限失败上报 | 仅 LOGGER.warn | ❌ 无结构化上报 |
| 补偿任务触发 | 未实现 | ❌ 缺失 |

## 4. 半成功处理评审

### 4.1 多服发放链路
| 定义阶段 | 实现方式 | 判定 |
|---|---|---|
| PREPARED（意图记录） | ❌ 直接执行 | 缺失 |
| COMMITTED（确认完成） | 字符串 "SUCCESS" 返回 | ❌ 无持久化 |
| COMPENSATED（超时补偿） | ❌ 未实现 | 缺失 |
| 区分"已完成"与"待确认" | ❌ 统一返回成功/失败 | 缺失 |

## 5. 幂等与去重评审

### 5.1 request_id 机制
| 定义要求 | 实现 | 判定 |
|---|---|---|
| 全局唯一 request_id | ✅ UUID 作为 requestId (BridgeClient L52) | 实现 |
| 去重窗口 | ❌ 仅 pendingRequests 内存级 | 缺失 |
| 重启后去重上下文 | ❌ 进程重启丢失 | 缺失 |
| 统一重试器 | ❌ 无统一重试 | 缺失 |

## 6. 差距汇总

| ID | 描述 | 风险 | 依据 |
|---|---|---|---|
| FM-01 | 故障状态机未实现（8/11 状态缺失） | **High** | FAILURE_MODEL §3.6.4 |
| FM-02 | 字符串错误码替代结构化失败分类 | **High** | FAILURE_MODEL §3.6.2 |
| FM-03 | 统一 10s 超时未按链路类型区分 | **Medium** | FAILURE_MODEL §3.1 |
| FM-04 | 重连采用固定延迟而非指数退避 | **Medium** | FAILURE_MODEL §3.2 |
| FM-05 | 半成功处理无两阶段语义 | **High** | FAILURE_MODEL §3.3 |
| FM-06 | 去重窗口无持久化 | **High** | FAILURE_MODEL §3.4 |
| FM-07 | 补偿机制完全缺失 | **High** | FAILURE_MODEL §3.6.3 |
| FM-08 | DEGRADED 模式未实现 | **Medium** | FAILURE_MODEL §3.2 |

## 7. 结论
- **High**: 5 项 (FM-01, FM-02, FM-05, FM-06, FM-07)
- **Medium**: 3 项 (FM-03, FM-04, FM-08)
- 状态机已在架构文档中完整定义，但代码层面仅实现了最基础的 pending→response 路径。
- 这是已知的"设计先行、实现后补"模式，属于 D 阶段整改范围。
