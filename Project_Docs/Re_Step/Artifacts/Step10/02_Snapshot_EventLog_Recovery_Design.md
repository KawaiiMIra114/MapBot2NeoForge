# 02 Snapshot EventLog Recovery Design

## 元数据
| 属性 | 值 |
|---|---|
| Step | Step-10 D3 |
| RUN_ID | 20260215T205400Z |
| 依据 | FAILURE_MODEL + DATA_CONSISTENCY_CONTRACT + RE_STEP_10 §详细步骤 2 |

## 1. 当前恢复能力

### 1.1 现状审计
| 数据 | 持久化 | 恢复方式 | 完整性校验 |
|---|---|---|---|
| bindings | JSON 文件 | 启动时全量读取 | ❌ 无校验 |
| permissions | JSON 文件 | 启动时全量读取 | ❌ 无校验 |
| sign cache | JSON + ATOMIC_MOVE | 启动时全量读取 | ❌ 无校验 |
| metrics | JSON 文件 | MetricsStorage.load() + restore | ❌ 无校验 |
| playtime (Alpha) | Redis | Redis 持久化 | ✅ Redis 自身 |
| playtime (Reforged) | JSON | 启动时读取 + 定期保存 | ❌ 无校验 |
| config | .properties | AlphaConfig.reload() | ⚠ 部分 validate |

### 1.2 恢复链路缺失
- 无 snapshot 版本号
- 无 event_log 增量日志
- 无回放机制
- 无 RTO/RPO 指标

## 2. 设计方案

### 2.1 快照策略
```
snapshot_v{version}_{timestamp}.json
├── header: { version, timestamp, checksum_sha256 }
├── data: { ... 完整状态 ... }
└── footer: { record_count, data_size_bytes }
```

### 2.2 事件日志 (append-only)
```
event_log_{date}.jsonl
每行: { "seq": 12345, "ts": "...", "type": "BIND", "entity": "bindings", 
         "key": "qq:123", "old": "uuid_a", "new": "uuid_b", "version": 7 }
```

### 2.3 恢复流程
```
1. 启动 → 读取最新 snapshot
2. 校验 checksum → 失败则尝试前一个 snapshot
3. 从 snapshot.version 开始回放 event_log
4. 回放到最新事件 → 达到一致终态
5. 生成新 snapshot 并清理旧日志
```

### 2.4 RTO/RPO 目标
| 指标 | 目标 | 依据 |
|---|---|---|
| RTO | ≤ 30s | snapshot 恢复 + 增量回放 |
| RPO | ≤ 1 min | event_log 实时追加 |
| Snapshot 频率 | 每 5 min 或每 100 次变更 | 平衡 I/O 与恢复时间 |

## 3. 改造点
| # | 文件 | 改造 |
|---|---|---|
| R1 | DataManager (Alpha) | 增加 snapshot 头 + checksum |
| R2 | DataManager (Alpha) | 增加 event_log 写入 |
| R3 | DataManager (Alpha) | 启动时校验 + 回放 |
| R4 | PlaytimeManager (Reforged) | snapshot + 校验 |
| R5 | SignManager (Reforged) | 已有 ATOMIC_MOVE, 增加 checksum |
| R6 | MetricsStorage (Alpha) | 增加 snapshot 头 |
