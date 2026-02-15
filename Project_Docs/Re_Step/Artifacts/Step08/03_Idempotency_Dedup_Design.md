# 03 Idempotency Dedup Design

## 元数据
| 属性 | 值 |
|---|---|
| Step | Step-08 D1 |
| RUN_ID | 20260215T201100Z |
| 依据 | FAILURE_MODEL.md + CRITIQUE §4.4 + RE_STEP_08 §3 |

## 1. 适用范围

### 1.1 变更型请求（需幂等保护）
| 操作 | 当前 action | 副作用 | 幂等需求 |
|---|---|---|---|
| 白名单添加 | whitelist_add | 游戏内添加白名单 | 重复添加无害但需去重 |
| 白名单移除 | whitelist_remove | 游戏内移除白名单 | 重复移除无害 |
| 绑定 | bind | 写 UUID + 白名单 | 需去重 |
| 解绑 | unbind | 移除绑定 + 白名单 | 需去重 |
| 物品发放 | give_item | 发放物品 | **高风险**—重复=重复发放 |
| 执行命令 | execute_command | 任意服务端命令 | **高风险** |
| 停服 | stop_server | 关闭服务器 | 重复无害（已关闭） |

### 1.2 查询型请求（无需幂等保护）
| 操作 | 说明 |
|---|---|
| get_*, has_*, resolve_* | 幂等天然满足 |
| heartbeat, register | 幂等天然满足 |

## 2. Key 结构

### 2.1 格式
```
idempotencyKey = "{action}:{target}:{requestId}"
```
- `action`: 操作类型 (whitelist_add, give_item, etc.)
- `target`: 操作目标 (playerName, serverId, etc.)
- `requestId`: UUID (已有)

### 2.2 示例
```
whitelist_add:PlayerA:550e8400-e29b-41d4-a716-446655440000
give_item:PlayerB:6ba7b810-9dad-11d1-80b4-00c04fd430c8
```

## 3. 缓存策略

### 3.1 存储
| 项 | 设计 |
|---|---|
| 存储位置 | Alpha: ConcurrentHashMap (内存) |
| Key | idempotencyKey |
| Value | 结果 (成功/失败/进行中) + 时间戳 |
| 容量 | 最大 10000 条 |

### 3.2 TTL
| 请求类型 | TTL |
|---|---|
| 变更型 | 300s (5 分钟) |
| 高风险 (give_item, execute_command) | 600s (10 分钟) |

### 3.3 淘汰
- 过期条目定时清理（60s 间隔）
- 超容量 LRU 淘汰

## 4. 冲突语义

### 4.1 重复请求判定
| 场景 | 缓存状态 | 响应 |
|---|---|---|
| 首次请求 | 不存在 → 写入 PENDING | 正常处理 |
| 重复请求 (相同 key) | 存在 + PENDING | 返回 `BRG_VALIDATION_207` (DEDUP_CONFLICT) |
| 重复请求 (已完成) | 存在 + SUCCESS | 返回缓存结果 (幂等) |
| 重复请求 (已失败) | 存在 + FAILED | 允许重试 (清除旧缓存) |

### 4.2 新增错误码
| 字段 | 值 |
|---|---|
| 错误码 | BRG_VALIDATION_207 |
| 含义 | 幂等键冲突 (DEDUP_CONFLICT) |
| retryable | true (等待后重试) |

## 5. 代码改造点
| 文件 | 改造 |
|---|---|
| BridgeProxy (Alpha) | 发送前检查去重缓存 + 写入 PENDING |
| BridgeProxy (Alpha) | 收到回执后更新缓存状态 |
| BridgeErrorMapper (Alpha+Reforged) | 新增 BRG_VALIDATION_207 |
| 新增: IdempotencyCache.java (Alpha) | 去重缓存实现 (CHM + TTL + 定时清理) |

## 6. 不实施项（D1 范围外）
- 持久化去重窗口（D3 数据一致性阶段）
- 跨重启缓存恢复（D3）
- 补偿逻辑（D3）
