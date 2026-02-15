# 04_SLO_Metrics_Alert_Profile

## 合同引用
- Contract-ID: `A2-OBS-SLO`
- Architecture: `ARCH-A4-FAILURE-MODEL` §3.3

## SLO 指标映射

### 核心指标清单 (合同 §5.1)

| 指标名 | 类型 | 合同描述 | 当前实现位点 | 实现状态 | 差距详情 |
|---|---|---|---|---|---|
| `auth_decision_total` | Counter | 按 role/decision/path 计数 | AuthManager.hasContractPermission() | MISSING | 无 Counter 机制 |
| `auth_decision_latency_ms` | Histogram | P50/P95/P99 | AuthManager | MISSING | 无耗时统计 |
| `config_reload_total` | Counter | 按 result(success/fail/rollback) | AlphaConfig.reload() → 有 configVersion | PARTIAL | 有版本号但无 Counter |
| `config_reload_latency_ms` | Histogram | 端到端重载耗时 | AlphaConfig.reload() | MISSING | 无耗时统计 |
| `consistency_conflict_total` | Counter | 按 entity_type/resolution 计数 | DataManager.bind() | MISSING | 有冲突检测但无计数 |
| `replay_lag_seconds` | Gauge | 回放延迟 | 无回放机制 | MISSING | 依赖 event sourcing |
| `audit_log_write_total` | Counter | 按 operation/outcome 计数 | LOGGER.info 分散审计 | PARTIAL | 有日志但无结构化计数 |
| `bridge_request_total` | Counter | 按 action/result 计数 | BridgeProxy 各方法 | MISSING | 有 LOGGER 但无 Counter |
| `bridge_request_latency_ms` | Histogram | 请求耗时 | BridgeProxy 超时10s | MISSING | 无耗时统计 |
| `tps` | Gauge | 每服务器 TPS | MetricsCollector | IMPLEMENTED | ✅ |
| `memory_usage_mb` | Gauge | 每服务器内存 | MetricsCollector | IMPLEMENTED | ✅ |
| `online_players` | Gauge | 每服务器玩家数 | MetricsCollector | IMPLEMENTED | ✅ |

### SLI/SLO 目标 (合同 §5.2)

| SLI | SLO 目标 | 当前可度量 | 差距 |
|---|---|---|---|
| 鉴权决策成功率 | ≥99.9% | 不可度量 | 缺 Counter |
| 鉴权决策延迟P99 | ≤5ms | 不可度量 | 缺 Histogram |
| 配置重载成功率 | ≥99.5% | 不可度量 | 缺 Counter |
| Bridge 请求成功率 | ≥95% (跨网络) | 不可度量 | 缺 Counter |
| Bridge 请求延迟P95 | ≤3000ms | 不可度量 | 缺 Histogram |
| 审计日志写入率 | 100% (零丢失) | 不可度量 | 缺 Counter |

## 告警配置 (合同 §5.3)

### 告警分级
| 级别 | 合同定义 | 当前实现 | 差距 |
|---|---|---|---|
| S1-Critical | 系统不可用，需立即人工介入 | 无 | MISSING |
| S2-High | 核心功能降级 | 无 | MISSING |
| S3-Medium | 非核心功能异常 | 无 | MISSING |
| S4-Low | 观察性告警 | 无 | MISSING |

### 告警规则映射
| 规则 | 触发条件 (合同) | 级别 | 当前状态 | 实现优先级 |
|---|---|---|---|---|
| AUTH_FAIL_BURST | auth_decision_total{decision=deny} 5min内>50 | S2 | MISSING | P1 |
| CONFIG_RELOAD_FAIL | config_reload_total{result=fail} 任意一次 | S2 | MISSING | P1 |
| BRIDGE_TIMEOUT_BURST | bridge_request_total{result=timeout} 5min内>20 | S2 | MISSING | P1 |
| TPS_LOW | tps < 15 持续 60s | S3 | MISSING | P2 |
| MEMORY_HIGH | memory_usage > 80% 持续 120s | S3 | MISSING | P2 |
| CONFLICT_BURST | consistency_conflict_total 5min内>10 | S3 | MISSING | P2 |

### RCA 链路 (合同 §5.4)

| 故障信号 | RCA 追踪路径 | 当前可追踪 | 差距 |
|---|---|---|---|
| 鉴权拒绝突增 | auth_decision_total → LOGGER → DataManager 权限查反查 | PARTIAL: 有日志但无结构化链路 | 缺 traceId/correlationId |
| Bridge 超时突增 | bridge_request_total → pendingRequests → 网络诊断 | PARTIAL: 有日志 | 缺结构化 trace |
| 数据冲突突增 | consistency_conflict_total → DataManager 变更历史 → event_log | MISSING | 缺 event_log |

## 综合判定
- **SLO 指标覆盖率**: 3/12 (25%) — 仅 TPS/内存/玩家数
- **SLO 目标可度量率**: 0/6 (0%)
- **告警覆盖率**: 0/6 (0%)
- **RCA 可追踪率**: ~20%
- **B3 判定**: SLO 指标体系属于 OBS-SLO 合同的中期整改项（合同状态 Active，整改窗口 2026-03-05），映射审计完成即可标注为 B3 已识别风险。
