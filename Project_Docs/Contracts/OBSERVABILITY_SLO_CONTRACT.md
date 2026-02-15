# OBSERVABILITY_SLO_CONTRACT

## 1. 统一元数据（强制）
| 字段 | 值 |
| --- | --- |
| Contract-ID | A2-OBS-SLO |
| 版本 | 1.0.0 |
| 状态 | Active |
| 生效日期 | 2026-02-14 |
| 最后更新 | 2026-02-14 |
| 责任角色 | owner |
| 适用系统 | MapBot2NeoForge |
| 依赖合同 | COMMAND_AUTHORIZATION_CONTRACT, CONFIG_SCHEMA_CONTRACT, DATA_CONSISTENCY_CONTRACT |

## 2. 强制章节合规声明（强制）
本文件包含统一强制章节：`统一元数据`、`适用范围`、`术语与角色`、`规范条款`、`审计与证据`、`迁移与兼容`、`验证步骤`、`可机检规则`、`反向测试`、`脆弱假设与缓释`。

## 3. 适用范围（强制）
本合同定义权限、配置、一致性链路的可观测指标、服务目标、告警与故障响应标准。

## 4. 术语与角色（强制）
### 4.1 角色集合
权限角色严格限制为：`user`、`admin`、`owner`。

### 4.2 可观测术语
- `SLI`：服务指标（可度量）。
- `SLO`：服务目标（可承诺）。
- `Alert Threshold`：触发告警的阈值条件。
- `Incident Severity`：故障等级。

## 5. 规范条款（强制）
### 5.1 核心指标
| 指标名 | 类型 | 关键标签 | 说明 |
| --- | --- | --- | --- |
| `auth_decision_total` | Counter | `decision`, `role`, `command_category` | 鉴权允许/拒绝总量 |
| `auth_decision_latency_ms` | Histogram | `command_category` | 鉴权延迟分布 |
| `config_reload_total` | Counter | `result` | 配置热重载成功/失败总量 |
| `consistency_conflict_total` | Counter | `resource_type` | 版本冲突事件总量 |
| `replay_lag_seconds` | Gauge | `pipeline` | 回放滞后秒数 |
| `audit_log_write_total` | Counter | `result` | 审计落盘成功/失败总量 |

### 5.2 SLI/SLO
| SLI | 目标 SLO | 统计窗口 |
| --- | --- | --- |
| 鉴权成功可用率 `allow+deny` 响应完成比 | `>= 99.95%` | 30 天滚动 |
| 鉴权延迟 `p95(auth_decision_latency_ms)` | `<= 50ms` | 7 天滚动 |
| 配置热重载成功率 | `>= 99.90%` | 30 天滚动 |
| 一致性恢复 RTO 达标率 | `>= 99.00%` 且单次 `<= 10 分钟` | 30 天滚动 |
| 审计落盘成功率 | `>= 99.99%` | 30 天滚动 |

### 5.3 告警阈值
| 监控项 | Warning | Critical |
| --- | --- | --- |
| `auth_decision_latency_ms p95` | `> 40ms` 持续 10 分钟 | `> 50ms` 持续 5 分钟 |
| 鉴权错误率（非业务拒绝） | `> 0.3%` 持续 10 分钟 | `> 1.0%` 持续 5 分钟 |
| 配置热重载失败率 | `> 0.5%` 持续 15 分钟 | `> 2.0%` 持续 5 分钟 |
| `replay_lag_seconds` | `> 30s` 持续 10 分钟 | `> 60s` 持续 5 分钟 |
| 审计落盘失败率 | `> 0.1%` 持续 10 分钟 | `> 0.5%` 持续 3 分钟 |

### 5.4 故障分级
| 等级 | 判定标准 | 响应要求 |
| --- | --- | --- |
| `S1` | 鉴权不可用或 owner 级命令错误放行/拒绝 | 5 分钟内响应，30 分钟内缓解 |
| `S2` | 管理命令大面积失败或一致性恢复超时 | 15 分钟内响应，2 小时内缓解 |
| `S3` | 局部指标退化但核心路径可用 | 1 小时内响应，1 个工作日内修复 |
| `S4` | 文档/告警噪音/低风险偏差 | 排期修复 |

### 5.5 排障最小证据集
每次告警排障必须收集以下最小证据：
- `incident_id`、开始/结束时间、影响范围
- 至少 15 分钟窗口的指标截图或导出（含基线对比）
- 一条失败样本的 `request_id` 全链路日志
- 当前 `policy_version`、`schema.version`、`snapshot_version`
- 最近一次配置变更与部署记录

## 6. 审计与证据（强制）
- 所有 `Critical` 告警必须形成事后记录（RCA），含根因、遏制、长期修复项。
- RCA 至少保留 180 天，并可按 `incident_id` 检索。
- SLO 违约必须触发改进工单并指定 `owner` 级责任人。

## 7. 迁移与兼容（强制）
- 指标重命名必须提供至少 1 个发布周期的双写（旧名 + 新名）。
- 告警规则变更必须先在影子规则运行 24 小时再切换。
- SLO 目标收紧前需提供最近 30 天基线数据与容量评估。

## 8. 验证步骤（强制）
1. 在测试环境注入鉴权延迟，验证 Warning/Critical 阈值和触发时长。
2. 人工制造配置热重载失败，确认告警、日志与指标三方一致。
3. 触发一次回放滞后场景，验证 `replay_lag_seconds` 与分级判定。
4. 抽查一次故障工单，核验最小证据集字段齐全且可回放。

## 9. 可机检规则（强制）
1. 告警等级列必须存在：`rg -n "^\\| 监控项 \\| Warning \\| Critical \\|$" Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
2. 鉴权延迟阈值必须存在：`rg -n "auth_decision_latency_ms p95.*40ms.*50ms" Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
3. Critical RCA 义务必须存在：`rg -n "Critical.*事后记录" Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
4. 排障版本证据必须存在：`rg -n "policy_version.*schema.version.*snapshot_version" Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`

## 10. 反向测试（强制）
- 前置条件：`auth_decision_latency_ms` 连续 15 分钟保持 `p95<=35ms`，鉴权错误率 `<0.3%`。
- 执行步骤：持续发送正常鉴权请求并采集指标窗口。
- 通过判据：不触发 Warning/Critical 告警，SLI 仍计入可用样本且无误报工单。

## 11. 脆弱假设与缓释（强制）
| 假设ID | 失效条件 | 后果 | 缓释条款（MUST） |
| --- | --- | --- | --- |
| ASSUMP-05 | 仅单一告警通道可用且通道不可达 | Critical 事件无人响应，RCA 断链 | Critical 告警必须配置主备两条独立通道；任一通道心跳失败超过 5 分钟必须升级告警。 |
