# 01 SLI/SLO 看板规格 (SLI SLO Dashboard Spec)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-14 |
| Artifact | 01/05 |
| RUN_ID | 20260215T220800Z |

## 核心指标字典

### 合同指标 (6 个)
| 指标名 | 类型 | 标签 | 单位 | 采样周期 | 缺失值策略 |
|---|---|---|---|---|---|
| auth_decision_total | Counter | decision={allow/deny}, role={user/admin/owner}, command_category | count | 实时 | 归零 |
| auth_decision_latency_ms | Histogram | decision, role | ms | 实时 | 忽略 (无请求) |
| config_reload_total | Counter | result={success/failure}, trigger={manual/auto} | count | 事件驱动 | 归零 |
| consistency_conflict_total | Counter | type={cas_fail/version_mismatch/schema_drift} | count | 事件驱动 | 归零 |
| replay_lag_seconds | Gauge | source={bridge/onebot} | seconds | 10s | 保持最后值 |
| audit_log_write_total | Counter | target={file/db}, result={success/failure} | count | 事件驱动 | 归零 |

### 业务补充指标
| 指标名 | 类型 | 说明 |
|---|---|---|
| bridge_request_total | Counter | Bridge 请求总量 (按 action 标签) |
| bridge_response_latency_ms | Histogram | Bridge 响应延迟 |
| command_execution_total | Counter | 命令执行总量 (按 command, result) |
| active_connections | Gauge | 当前 Bridge 活跃连接数 |

## SLO 定义

| SLO | 目标 | 窗口 | 数据源 | 公式 |
|---|---|---|---|---|
| 可用率 | ≥99.95% | 7天滚动 | auth_decision_total | 1 - (deny_count - expected_deny) / total |
| 鉴权延迟 P95 | ≤50ms | 7天滚动 | auth_decision_latency_ms | histogram_quantile(0.95) |
| 热重载成功率 | ≥99.90% | 30天滚动 | config_reload_total | success / total |
| 一致性恢复达标率 | ≥99.00% | 30天滚动 | consistency_conflict_total | resolved_in_10min / total |
| 一致性单次恢复 | ≤10分钟 | 每次 | consistency timestamps | end - start |
| 审计落盘成功率 | ≥99.99% | 30天滚动 | audit_log_write_total | success / total |

## 看板分层

### L1 概览看板
- 系统可用率 (大字)
- 活跃连接数
- 当前告警数

### L2 指标看板
- auth_decision 趋势图 (按 decision 分色)
- auth_latency P50/P95/P99
- config_reload 成功/失败柱状图
- consistency_conflict 趋势

### L3 诊断看板
- 每连接延迟热力图
- 错误码分布饼图
- 审计写入延迟长尾

## 当前实现状况
- **MetricsStorage**: 已有基础 init/save 实现
- **MetricsCollector**: 已有 start 方法
- **LOGGER**: 449+ 日志点分布在 Alpha/Reforged

## 差距
| ID | 差距 | 严重度 |
|---|---|---|
| OBS-01 | MetricsStorage 仅存储,无指标定义 | High |
| OBS-02 | 无 histogram 类型支持 | High |
| OBS-03 | 无 SLO 计算自动任务 | Medium |
| OBS-04 | 无看板定义 (无 Grafana/Prometheus) | Medium |
| OBS-05 | 日志点无结构化标签 | Medium |
