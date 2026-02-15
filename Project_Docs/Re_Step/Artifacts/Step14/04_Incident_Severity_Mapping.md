# 04 事件严重度映射 (Incident Severity Mapping)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-14 |
| Artifact | 04/05 |
| RUN_ID | 20260215T220800Z |

## 严重度定义

| 级别 | 名称 | 定义 | 响应时限 | 升级时限 |
|---|---|---|---|---|
| S1 | 灾难 | 全部服务不可用,数据丢失风险 | 5分钟 | 15分钟 |
| S2 | 严重 | 核心功能不可用 (绑定/指令执行) | 15分钟 | 30分钟 |
| S3 | 一般 | 非核心功能异常 (签到/CDK) | 1小时 | 4小时 |
| S4 | 轻微 | 性能下降/文案错误/日志异常 | 24小时 | 72小时 |

## 告警 → 严重度映射

| 告警 | Warning→ | Critical→ | 说明 |
|---|---|---|---|
| auth_latency P95>50ms | S3 | S2 | 鉴权是核心路径 |
| config_reload failure | S4 | S3 | 影响配置更新 |
| consistency_conflict >20/h | S3 | S2 | 数据一致性风险 |
| audit_write failure >0.01% | S3 | S1 | 审计合规性 |
| active_connections=0 (10min) | S3 | S2 | Bridge 断连 |
| replay_lag >60s | S3 | S2 | 数据回放滞后 |

## 响应动作

### S1 灾难
1. 立即通知所有管理员 (QQ + 控制台)
2. 启动 RCA 流程
3. 记录事件到 incidents 目录
4. 考虑回退到上一个稳定版本

### S2 严重
1. 通知在线管理员
2. 记录事件
3. 5分钟内尝试自动恢复
4. 10分钟未恢复升级到 S1

### S3 一般
1. 通知主通道
2. 记录事件
3. 等待人工处理
4. 4小时未处理升级到 S2

### S4 轻微
1. 记录到日志
2. 加入待办队列
3. 72小时未处理升级到 S3

## 故障注入场景 (设计)

| 场景 | 注入方式 | 预期告警 | 预期级别 |
|---|---|---|---|
| 鉴权延迟 | Thread.sleep(100) in AuthManager | auth_latency Warning→Critical | S3→S2 |
| 热重载失败 | 损坏 config.json | config_reload Critical | S3 |
| 回放滞后 | 暂停 Bridge 3分钟 | replay_lag Warning→Critical | S3→S2 |
| 审计写入失败 | 锁定日志文件 | audit_write Critical | S1 |
| Bridge 断连 | 关闭 Bridge 服务端 | connection Critical | S2 |

## 差距
| ID | 差距 | 严重度 |
|---|---|---|
| SEV-01 | 无故障注入框架 | High |
| SEV-02 | 无自动升级逻辑 | Medium |
| SEV-03 | 无 RCA 流程自动触发 | Medium |
| SEV-04 | 无回退开关 | Medium |
