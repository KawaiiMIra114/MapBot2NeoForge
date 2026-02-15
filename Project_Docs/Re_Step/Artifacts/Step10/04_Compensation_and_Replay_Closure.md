# 04 Compensation and Replay Closure

## 元数据
| 属性 | 值 |
|---|---|
| Step | Step-10 D3 |
| RUN_ID | 20260215T205400Z |
| 依据 | RE_STEP_10 §详细步骤 4 + DATA_CONSISTENCY_CONTRACT |

## 1. 当前半成功场景

### 1.1 半成功操作清单
| 操作 | 步骤 A | 步骤 B | 半成功后果 |
|---|---|---|---|
| 签到领奖 | Alpha 生成奖品 + PENDING | Reforged give_item | 奖品生成但未发放 (KEY_PENDING 留存) |
| 白名单 + 绑定 | add_whitelist | bind_player | 白名单加了但绑定失败 |
| CDK 兑换 | 标记 CDK 已用 | give_item | CDK 用了但物品未发 |
| 服务器切换 | 发送 switch_request | 目标服务器接受 | 请求发出但超时未响应 |
| 跨服消息 | Alpha 转发 | Reforged 处理 | 消息丢失无感知 |

### 1.2 当前补偿机制
| 操作 | 补偿方式 | 充分性 |
|---|---|---|
| 签到 PENDING | TTL 24h 过期 → 可重签 | ⚠ 被动等待，无主动补偿 |
| 白名单半成功 | 无补偿 | ❌ |
| CDK 半成功 | 无补偿 | ❌ |
| 切服超时 | BRG_TIMEOUT_501 → 用户可重试 | ⚠ 用户手动重试 |

## 2. 补偿状态机设计

### 2.1 事务状态
```
PENDING → COMMITTED (成功)
PENDING → FAILED → COMPENSATING → COMPENSATED (补偿成功)
PENDING → FAILED → COMPENSATING → STUCK (补偿也失败)
```

### 2.2 补偿记录
```json
{
  "txId": "tx_20260215_001",
  "type": "SIGN_REWARD",
  "state": "PENDING",
  "createdAt": "2026-02-15T20:00:00Z",
  "steps": [
    { "action": "GENERATE_REWARD", "state": "COMMITTED" },
    { "action": "GIVE_ITEM", "state": "PENDING" }
  ],
  "retryCount": 0,
  "maxRetries": 3,
  "ttlSeconds": 86400
}
```

### 2.3 终态收敛规则
| 条件 | 动作 |
|---|---|
| 所有步骤 COMMITTED | 整体 → COMMITTED |
| 任一步骤 FAILED + 补偿成功 | 整体 → COMPENSATED |
| 补偿重试耗尽 | 整体 → STUCK + 告警 |
| TTL 过期 | 整体 → EXPIRED + 清理 |

## 3. 回放去重

### 3.1 幂等性保证
- 每个事务有唯一 `txId`
- 回放时检查 `txId` 是否已 COMMITTED
- 若已 COMMITTED → 跳过
- 若 PENDING → 重新执行

### 3.2 去重存储
```java
// Alpha 侧
ConcurrentHashMap<String, TxState> txLog; // txId → state
// 定期清理 COMMITTED/COMPENSATED 超过 TTL 的条目
```

## 4. 改造优先级
| # | 操作 | 优先级 | 理由 |
|---|---|---|---|
| C1 | 签到领奖补偿 | **P0** | 已有 PENDING 机制, 改造成本低 |
| C2 | CDK 兑换补偿 | **P0** | CDK 不可重复使用, 补偿关键 |
| C3 | 白名单 + 绑定原子化 | **P1** | 可合并为事务 |
| C4 | 切服超时重试 | **P2** | 用户可手动重试 |
| C5 | 跨服消息确认 | **P2** | 非关键路径 |
