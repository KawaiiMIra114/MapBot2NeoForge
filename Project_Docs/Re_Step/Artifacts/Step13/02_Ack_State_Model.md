# 02 回执状态模型 (Ack State Model)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-13 |
| Artifact | 02/05 |
| RUN_ID | 20260215T215300Z |

## 统一状态机

```
REQUEST → PENDING → SUCCESS
                  → FAILED
                  → TIMEOUT → FAILED_TIMEOUT
                            → COMMITTED (延迟确认)
```

### 状态定义
| 状态 | 含义 | API 返回 | QQ 回执 |
|---|---|---|---|
| SUCCESS | 执行成功 | `200 {state:"SUCCESS"}` | "[成功] ..." |
| FAILED | 执行失败 | `409 {state:"FAILED", errorCode:"BRG_*"}` | "[错误] ..." |
| PENDING | 等待回执 | `202 {state:"PENDING", requestId:"..."}` | "[等待] 命令已发送..." |
| FAILED_TIMEOUT | 超时失败 | `504 {state:"FAILED_TIMEOUT"}` | "[超时] ..." |
| COMMITTED | 延迟确认 | `200 {state:"COMMITTED"}` | "[完成] 延迟确认成功" |

## 同步等待窗口

### 规则
1. **同步等待**: 所有动作默认等待 5 秒
2. **未收到回执**: 必须返回 PENDING (禁止返回 SUCCESS)
3. **超时处理**: 5秒后标记 FAILED_TIMEOUT
4. **延迟确认**: PENDING → COMMITTED 需轮询机制

### 按动作等待策略
| 动作 | 等待方式 | 超时 | 超时后行为 |
|---|---|---|---|
| bind | CompletableFuture | 5s | FAILED_TIMEOUT |
| unbind | 同步 | 3s | FAILED_TIMEOUT |
| execute_command | CompletableFuture | 5s | FAILED_TIMEOUT |
| switch_server | CompletableFuture | 5s | PENDING (可查询) |
| whitelist_add/remove | CompletableFuture | 5s | FAILED_TIMEOUT |
| reload | 同步 | 即时 | N/A |

## Pending 归并策略

### 当前实现
- CompletableFuture.get(5, TimeUnit.SECONDS) → 超时抛异常
- 超时后直接返回错误文本 (无 PENDING 状态)

### 目标实现
1. 超时后返回 PENDING + requestId
2. 提供 `GET /api/pending/{requestId}` 查询最终状态
3. PENDING 超过 30s 自动归并为 FAILED_TIMEOUT
4. 归并时写审计事件

### 差距
| ID | 差距 | 严重度 |
|---|---|---|
| ACK-01 | 无 PENDING 状态 (超时直接失败) | High |
| ACK-02 | 无 pending 查询接口 | High |
| ACK-03 | 超时未写审计事件 | Medium |
| ACK-04 | 无延迟确认 (COMMITTED) 机制 | Medium |
