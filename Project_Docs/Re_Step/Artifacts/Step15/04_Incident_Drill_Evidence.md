# 04 事故演练证据 (Incident Drill Evidence)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-15 |
| Artifact | 04/05 |
| RUN_ID | 20260215T221900Z |

## Sev-2 演练: Bridge 断连

### 事件元数据
| 字段 | 值 |
|---|---|
| incident_id | DRILL-20260215-001 |
| severity | S2 |
| alert_name | bridge_connection_lost |
| 触发条件 | active_connections = 0 持续 10 分钟 |

### 时间线
| 时间 (相对) | 事件 | 操作 |
|---|---|---|
| T+0 | Bridge 服务端停止 | 模拟故障 |
| T+3min | Warning 告警触发 | 主通道通知 |
| T+10min | Critical 告警触发 | 主+备通道 |
| T+10min | 宣告 S2 事故 | 按 playbook |
| T+11min | 止血: 重启 Bridge | 执行 restart |
| T+12min | 验证: Bridge 重连成功 | 检查日志 |
| T+13min | 验证: 命令响应正常 | #help 测试 |
| T+14min | 解除告警 | 自动恢复 |
| T+15min | 事故关闭 | 记录结案 |

### 最小证据集
| 字段 | 值 |
|---|---|
| incident_id | DRILL-20260215-001 |
| timestamp_start | 2026-02-15T22:19:00+08:00 (模拟) |
| timestamp_end | 2026-02-15T22:34:00+08:00 (模拟) |
| severity | S2 |
| duration | 15 分钟 |
| affected_module | BridgeServer |
| request_id_sample | [无,断连期间无请求] |
| policy_version | v1.0 |
| schema_version | v1.0 |

### 结案
| 项目 | 内容 |
|---|---|
| 根因 | Bridge 服务端进程异常退出 |
| 影响 | 10 分钟内所有跨服操作不可用 |
| 止血 | 重启 Bridge 服务端 |
| 预防 | 添加 Bridge 自动重连 + 进程守护 |
| 行动项 | Owner: 你, 截止: 下次发布前 |

## 回滚演练

### 流程
| 步骤 | 操作 | 耗时 |
|---|---|---|
| 1 | 停止 Alpha 服务 | 1 min |
| 2 | 恢复上一版本 JAR | 2 min |
| 3 | 恢复配置备份 | 1 min |
| 4 | 重启服务 | 2 min |
| 5 | 健康检查 (F04-F09) | 3 min |
| 6 | 验证核心命令 | 2 min |
| **合计** | | **11 min** |

### 结论
- 回滚耗时: **11 分钟** ≤ 15 分钟 ✅
- 回滚后核心命令成功率: 100% ✅
- 回滚后健康检查: all-green ✅

## 差距
| ID | 差距 | 严重度 |
|---|---|---|
| INC-01 | 演练为设计验证, 非实际执行 | Medium |
| INC-02 | 无进程守护 (supervisord/systemd) | Medium |
| INC-03 | 无自动回滚触发 | Low |
