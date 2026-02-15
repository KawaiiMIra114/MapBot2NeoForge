# 03_Replay_Recovery_Flow

## 合同引用
- `A2-DATA-CONSISTENCY` §5.4 回放策略, §5.5 失败恢复
- `ARCH-A4-FAILURE-MODEL` §3.5 恢复策略, §3.6 pending处置

## 回放链路映射

### 当前状态: MISSING
当前系统无 event sourcing / event_log 实现。状态基于 DataManager 内存 + Redis/文件持久化。

### 合同要求回放流程
```
1. 加载最新有效快照 (snapshot)
2. 校验 snapshot_checksum (SHA-256)
3. 按 event_sequence 严格递增回放 event_log
4. 每条事件携带 idempotency_key，重複事件仅执行一次
5. 非法事件进入隔离队列
```

### 当前恢复流程 (As-Is)
```
启动 → DataManager.init()
  → loadBindings() (properties 文件)
  → loadMutes() (properties 文件)
  → loadPermissions() (properties 文件)
  → loadAdmins() (properties 文件)
  → syncFromRedis() (如 Redis 可用：全量覆盖内存)
```

### 差距分析

| 合同要求 | 当前实现 | 差距 |
|---|---|---|
| 加载有效快照 | loadBindings from file | PARTIAL: 有加载但无 checksum |
| checksum 校验 | 无 | MISSING |
| 快照失败回退 | 无 | MISSING |
| event_log 增量回放 | 无 event_log | MISSING |
| 幂等回放 | 无 idempotency_key | MISSING |
| 非法事件隔离 | 无 | MISSING |

## 恢复路径映射

### FAILURE_MODEL §3.5 冷启动恢复
| 步骤 | 合同要求 | 当前实现 | 差距 |
|---|---|---|---|
| 1. 连接恢复 | 连接恢复 | BridgeClient 自动重连 (3s 固定退避) | PARTIAL: 无指数退避+抖动 |
| 2. 权威状态拉取 | 权威快照同步 | syncFromRedis() 全量覆盖 | PARTIAL: 无版本对比 |
| 3. 待确认队列对账 | pending 队列清理 | pendingRequests 重启后清空 | MISSING: 无持久化/对账 |
| 4. 恢复写操作 | 恢复后放行 | 无门禁控制 | MISSING: 无恢复完成判据 |

### FAILURE_MODEL §3.6 Pending 处置
| 请求类型 | 合同最大滞留 | 当前行为 | 差距 |
|---|---|---|---|
| 查询类 | 15s | 10s 超时返回 null | PARTIAL: 超时值不一致 |
| 变更类 | 120s | 10s 超时返回 null | MISSING: 无补偿任务 |
| 文件类 | 30s | 10s 超时 | PARTIAL: 超时值不一致 |
| 控制类 | 20s | 10s 超时 | PARTIAL: 超时值不一致 |

### FAILURE_MODEL §3.7 防雪崩控制
| 控制项 | 合同要求 | 当前实现 | 差距 |
|---|---|---|---|
| inflight 限额 ≤200 | 无限制 | pendingRequests 无上限 | MISSING |
| 指数退避 | base=500ms, factor=2, max=10s | BridgeClient 固定 3s | PARTIAL |
| 重试上限 3 次 | 无限制 | 无上限 | MISSING |
| 熔断 | 5s 失败率>50% 则熔断 30s | 无熔断 | MISSING |
| 优先级队列 | 写>查询 | 无优先级 | MISSING |

## 综合判定
- **回放覆盖率**: 0% (无 event sourcing)
- **恢复覆盖率**: ~20% (有基础加载，缺高级恢复)
- **Pending 处置覆盖率**: ~30% (有超时但不分类)
- **防雪崩覆盖率**: ~10% (仅有基础重连)
- **B3 判定**: 回放和高级恢复属于 FAILURE_MODEL 差距表中的中期整改项（2026-03-05~2026-03-12），映射审计完成即可通过 B3。
