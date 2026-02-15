# 02 告警规则 Warning/Critical (Alert Rules)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-14 |
| Artifact | 02/05 |
| RUN_ID | 20260215T220800Z |

## 双级告警规则

### 鉴权延迟
| 级别 | 条件 | 持续 | 动作 |
|---|---|---|---|
| Warning | auth_decision_latency_ms P95 > 40ms | 10分钟 | 通知主通道 |
| Critical | auth_decision_latency_ms P95 > 50ms | 5分钟 | 通知主+备通道, 触发 RCA |

### 热重载失败
| 级别 | 条件 | 持续 | 动作 |
|---|---|---|---|
| Warning | config_reload_total{result=failure} > 0 | 即时 | 通知主通道 |
| Critical | 连续 2 次 failure | 即时 | 通知主+备通道, 锁定配置 |

### 一致性冲突
| 级别 | 条件 | 持续 | 动作 |
|---|---|---|---|
| Warning | consistency_conflict_total > 5/h | 15分钟 | 通知主通道 |
| Critical | consistency_conflict_total > 20/h 或恢复 >10min | 5分钟 | 通知主+备, 触发 RCA |

### 审计落盘
| 级别 | 条件 | 持续 | 动作 |
|---|---|---|---|
| Warning | audit_log_write_total{result=failure} > 0 | 即时 | 通知主通道 |
| Critical | failure_rate > 0.01% | 5分钟 | 通知主+备, 暂停写入切换备份 |

### Bridge 连接
| 级别 | 条件 | 持续 | 动作 |
|---|---|---|---|
| Warning | active_connections = 0 | 3分钟 | 通知主通道 |
| Critical | active_connections = 0 | 10分钟 | 通知主+备, 触发重连 |

### 回放滞后
| 级别 | 条件 | 持续 | 动作 |
|---|---|---|---|
| Warning | replay_lag_seconds > 30 | 5分钟 | 通知主通道 |
| Critical | replay_lag_seconds > 60 | 5分钟 | 通知主+备, 触发 RCA |

## 告警通道

### 主通道
- QQ 管理群消息 (OneBotClient.sendGroupMessage)
- 日志文件 WARNING/ERROR 级别

### 备通道
- 控制台标准输出 (LOGGER.error)
- 独立告警日志文件 (alerts.log)

### 通道健康检查
- 每 5 分钟验证主通道可达性
- 主通道失效 5 分钟: 自动升级到备通道 + Critical 告警

## 去抖与抑制

### 去抖策略
- 同一告警 5 分钟内不重复发送
- 状态变更 (Warning→Critical, Critical→Resolved) 立即发送

### 抑制策略
- 下游告警被上游告警抑制 (如 Bridge 断连 抑制 所有 Bridge 相关告警)
- 维护窗口期间静默 (可配置)

## 差距
| ID | 差距 | 严重度 |
|---|---|---|
| ALT-01 | 无告警框架 (需自建或接入) | High |
| ALT-02 | 无备通道独立告警日志 | Medium |
| ALT-03 | 无去抖/抑制逻辑 | Medium |
| ALT-04 | 无通道健康检查 | Medium |
| ALT-05 | 无维护窗口静默 | Low |
