# 01 稳定化工作台 (Stabilization Backlog)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-20 |
| Artifact | 01/05 |
| RUN_ID | 20260216T172900Z |

## 灰度遗留问题 (H2 RCA 行动项)
本次灰度四阶段均顺利通过，未触发回滚，无 RCA 事件。
以下为 H2 自审识别的差距转化为 I1 工作项：

| ID | 来源 | 描述 | P级 | 修复动作 | 截止 | Owner |
|---|---|---|---|---|---|---|
| SB-01 | GTH-01 | 自动回滚脚本未编码 | P0 | 编写 rollback.sh 自动检测→回滚→验证 | I1 闭环 | Solo |
| SB-02 | GRP-01 | 无自动化流量切换脚本 | P1 | 编写 traffic_shift.sh 按阶段切流 | I2 | Solo |
| SB-03 | GRP-02 | 暗发布特性开关未编码 | P1 | 在 config 增加 feature_flag 字段 | I2 | Solo |
| SB-04 | GTH-02 | 阈值告警未接入自动化流水线 | P1 | 接入告警→自动回滚触发链 | I2 | Solo |
| SB-05 | PDL-01 | 阶段决策日志缺少自动化生成 | P2 | 编写 phase_log_gen.py | I2 | Solo |
| SB-06 | RCA-01 | RCA 工具链未自动化 | P2 | 编写 rca_template_gen.py | I2 | Solo |

## 全局技术债 (高优先级精选)
| ID | 描述 | P级 | 来源阶段 | 状态 |
|---|---|---|---|---|
| TD-01 | PlaytimeStore 多线程访问缺保护 | P0 | C1 | 设计已完成(D2)，实现挂起 |
| TD-02 | CDK 兑换缺少幂等校验 | P0 | E2 | 设计已完成，实现挂起 |
| TD-03 | Bridge 重连缺指数退避 | P1 | D1 | 设计已完成，实现挂起 |
| TD-04 | 签到奖池缺概率总和校验 | P1 | E1 | 设计已完成 |
| TD-05 | 告警发送无 dedup 窗口 | P1 | F1 | 设计已完成 |

## P0 闭环计划
| P0 项 | 降级方案 | 监控补强 |
|---|---|---|
| SB-01 (自动回滚未编码) | 人工回滚 SOP 已在 INCIDENT_RESPONSE_PLAYBOOK | 回滚触发后增加 Bridge 告警 |
| TD-01 (多线程) | 单线程降级模式，性能下降可接受 | OOM 监控已在 SLO |
| TD-02 (CDK 幂等) | Redis SETNX 防重复，可容忍误告警 | CDK 兑换日志审计 |

## 差距
| ID | 差距 | 严重度 |
|---|---|---|
| SBK-01 | P0 技术债实现均为"设计完成/实现挂起" | High |
