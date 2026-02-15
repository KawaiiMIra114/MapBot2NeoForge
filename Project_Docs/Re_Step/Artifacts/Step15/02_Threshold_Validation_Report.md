# 02 阈值验证报告 (Threshold Validation Report)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-15 |
| Artifact | 02/05 |
| RUN_ID | 20260215T221900Z |

## F01-F18 阈值验证

### 部署阈值 (F01-F09)
| ID | 阈值 | 来源 | 验证结果 | 动态校准 |
|---|---|---|---|---|
| F01 | JDK ≥17 | DEPLOYMENT_RUNBOOK | ✅ 固定 | N/A |
| F02 | Build exit=0 | DEPLOYMENT_RUNBOOK | ✅ 固定 | N/A |
| F03 | Config valid JSON | DEPLOYMENT_RUNBOOK | ✅ 固定 | N/A |
| F04 | 启动 ≤30s | DEPLOYMENT_RUNBOOK | ✅ 设计 | 可配置 |
| F05 | Bridge 握手 ≤5s | DEPLOYMENT_RUNBOOK | ✅ 设计 | 超时可调 |
| F06 | OneBot 连接 ≤10s | DEPLOYMENT_RUNBOOK | ✅ 设计 | 重试3次 |
| F07 | 命令响应 ≤1s | DEPLOYMENT_RUNBOOK | ✅ 设计 | N/A |
| F08 | 指标采集启动 | DEPLOYMENT_RUNBOOK | ✅ 设计 | N/A |
| F09 | 健康检查 all-green | DEPLOYMENT_RUNBOOK | ✅ 设计 | N/A |

### 运维阈值 (F10-F14)
| ID | 阈值 | 来源 | 验证结果 | 动态校准 |
|---|---|---|---|---|
| F10 | auth_latency P95 ≤50ms | OBSERVABILITY_SLO | ✅ 对齐 F1 | 窗口7天 |
| F11 | config_reload 成功率 ≥99.90% | OBSERVABILITY_SLO | ✅ 对齐 F1 | 窗口30天 |
| F12 | consistency 恢复 ≤10min | OBSERVABILITY_SLO | ✅ 对齐 F1 | 每次 |
| F13 | audit 落盘 ≥99.99% | OBSERVABILITY_SLO | ✅ 对齐 F1 | 窗口30天 |
| F14 | Bridge 断连 ≤10min | OPERATIONS_RUNBOOK | ✅ 设计 | 自动重连 |

### 事故阈值 (F15-F18)
| ID | 阈值 | 来源 | 验证结果 | 动态校准 |
|---|---|---|---|---|
| F15 | S1 响应 ≤5min | INCIDENT_PLAYBOOK | ✅ 对齐 F1 | N/A |
| F16 | S2 响应 ≤15min | INCIDENT_PLAYBOOK | ✅ 对齐 F1 | N/A |
| F17 | 回滚 ≤15min | UPGRADE_GUIDE | ✅ 设计 | N/A |
| F18 | RCA 完成 ≤72h | INCIDENT_PLAYBOOK | ✅ 设计 | N/A |

## 动态校准规则
1. P95 延迟阈值随流量自适应: 低流量期间 Warning 阈值放宽 20%
2. 连接超时随网络质量调整: 跨区部署时阈值翻倍
3. 窗口长度与数据量挂钩: 数据不足 7 天时使用全量

## 差距
| ID | 差距 | 严重度 |
|---|---|---|
| THR-01 | 动态校准未实现 (仅设计) | Medium |
| THR-02 | F04-F09 未实际执行验证 | Medium |
