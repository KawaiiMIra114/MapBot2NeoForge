# 04 切服强回执设计 (SwitchServer StrongAck Design)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-12 |
| Artifact | 04/06 |
| RUN_ID | 20260215T214100Z |

## 当前实现分析

### BridgeHandlers.handleSwitchServer (Reforged 端)
1. 解析 `arg1=玩家名`, `arg2=目标地址(host:port)`
2. 验证玩家在线 + 目标有效
3. `tryExecuteFromPlayer(server, player, "transfer " + host + " " + port)`
4. CompletableFuture 等待结果 (5秒超时)
5. 返回 SUCCESS/FAIL/TIMEOUT

### Alpha 端 (指令触发)
- `BridgeProxy.executeProxyCommand()` 发送 switch_server 到目标子服
- 等待子服 proxy_response

## 强回执语义设计

### 禁止规则
1. **禁止"发送即成功"**: 必须等待执行回执
2. **禁止吞超时**: 超时必须返回 FLOW-TIMEOUT 而非成功
3. **禁止假成功**: 断连/重连不得被误报为成功

### 回执状态
| 状态 | 含义 | 用户体验 |
|---|---|---|
| ACK-SUCCESS | 转移命令执行成功 | "✅ 已将 {player} 转移到 {server}" |
| ACK-PENDING | 等待执行结果 | "⏳ 转移命令已发送,等待确认..." |
| ACK-TIMEOUT | 等待超时 | "⚠️ 转移命令已发送但未收到确认" |
| ACK-FAIL | 执行失败 | "❌ 转移失败: {reason}" |

### 当前差距
| ID | 差距 | 严重度 | 修复方向 |
|---|---|---|---|
| SWITCH-01 | 超时返回字符串而非结构化码 | Medium | 统一为 FLOW-TIMEOUT |
| SWITCH-02 | 无 pending 状态管理 | Medium | 增加 PendingTransferStore |
| SWITCH-03 | 断连后无补偿检查 | Low | 重连时检查 pending 转移 |
| SWITCH-04 | 无 requestId 级追踪 | Low | 增加 requestId 打点 |

### Pending 管理
```
transfer 命令发出 → 记录 PENDING_TRANSFER(requestId, player, target, timestamp)
收到回执 → 更新 COMPLETED/FAILED → 通知用户
超时 (5s) → 标记 TIMEOUT → 返回 ACK-TIMEOUT
```
