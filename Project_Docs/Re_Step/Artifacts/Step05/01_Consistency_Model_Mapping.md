# 01_Consistency_Model_Mapping

## 合同引用
- Contract-ID: `A2-DATA-CONSISTENCY`
- Architecture: `ARCH-A4-FAILURE-MODEL`

## 一致性模型映射矩阵

### 事实源定义 (§5.1)
| 数据域 | 合同要求事实源 | 当前实现 | 差距 | 修复动作 | 验收方式 | 优先级 |
|---|---|---|---|---|---|---|
| 命令授权策略 | `authorization_policy_store` | AuthManager 内存 + ContractRole 枚举 | 无持久化 policy_store，重启丢失运行时权限调整 | 引入 Redis/文件持久化的 policy_store | 重启后策略恢复验证 | P1 |
| 运行时配置 | `config_store` | AlphaConfig + alpha.properties 文件 | 已实现 ConfigSchema 校验 + 事务 reload | — | — (B2 已闭环) | DONE |
| 状态变更历史 | `event_log` | 无实现 | 无 append-only event_log | 引入轻量 event_log（JSON 文件或 Redis Stream） | 验证事件写入和按序读取 | P1 |
| 读优化缓存 | `materialized_cache` | ServerRegistry (在线态) | 已明确为缓存非事实源 | — | — (已明确) | DONE |

### 版本号规则 (§5.2)
| 要求 | 当前实现 | 差距 | 修复动作 | 优先级 |
|---|---|---|---|---|
| entity_version:uint64 每次写入+1 | DataManager 无版本字段 | MISSING | 为 bindings/mutes/permissions 各自维护单调递增版本号 | P1 |
| snapshot_version + checksum | 无快照版本号 | MISSING | 在 DataManager 持久化时增加 snapshot_version 和 SHA-256 checksum | P2 |
| 响应携带 version_hint | 无实现 | MISSING | API 响应和 Bridge 响应增加 version_hint 字段 | P2 |

### 冲突解决策略 (§5.3)
| 要求 | 当前实现 | 差距 | 修复动作 | 优先级 |
|---|---|---|---|---|
| CAS 写入 + expected_version | bind() 用 isUUIDBound 前检查 | PARTIAL: 检查后写入非原子，存在 TOCTOU | 引入 CAS 原子操作（compareAndSet on version） | P1 |
| CONSISTENCY-409 冲突返回 | 返回 false 或 FAIL:OCCUPIED | PARTIAL: 有冲突检测但无标准错误码 | 统一返回 CONSISTENCY-409 错误码 | P1 |
| 角色优先级决策 | 无实现 | MISSING | 并发写入裁决按 owner>admin>user 排序 | P2 |

### 回放策略 (§5.4)
| 要求 | 当前实现 | 差距 | 修复动作 | 优先级 |
|---|---|---|---|---|
| snapshot + event_log 模型 | 无实现 | MISSING | 引入 event sourcing 基础框架 | P1 |
| idempotency_key 幂等去重 | BridgeProxy 无去重缓存 | MISSING | 引入 TTL 去重索引 | P1 |
| 非法事件隔离队列 | 无实现 | MISSING | 引入 dead-letter 队列 | P2 |

### 失败恢复 (§5.5)
| 要求 | 当前实现 | 差距 | 修复动作 | 优先级 |
|---|---|---|---|---|
| 最新快照加载+checksum | DataManager.init → loadBindings | PARTIAL: 有加载但无 checksum 校验 | 增加 checksum 校验和回退 | P1 |
| RTO ≤ 10min / RPO ≤ 60s | 无度量 | MISSING | 引入恢复计时和告警 | P2 |

### 一致性级别 (§5.6)
| 场景 | 合同要求 | 当前实现 | 差距 |
|---|---|---|---|
| 命令鉴权 | 强一致 | AuthManager 内存判定 | OK（单节点强一致） |
| 会话读己之写 | 写入后立即可读 | DataManager ConcurrentHashMap | OK（单节点内） |
| 可观测聚合 | 最终一致 ≤60s | MetricsCollector 5s间隔 | OK |

## 综合判定
- **当前一致性覆盖率**: ~30%
- **DONE 项**: 2 (配置事实源、缓存定义)
- **P1 缺口**: 7 项 (entity_version/CAS/conflict-code/event_log/idempotency/snapshot-checksum/recovery)
- **P2 缺口**: 5 项 (snapshot_version/version_hint/角色优先级/dead-letter/RTO度量)
- **B3 判定**: P1 缺口的存在为 **已识别风险**，但属于中长期架构演进项（合同整改期限 2026-03-05~2026-03-12），不阻塞 B3→C1 推进的前提是完成映射审计和差距标注。
