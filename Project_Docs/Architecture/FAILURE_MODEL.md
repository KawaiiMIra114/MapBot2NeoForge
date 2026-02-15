# FAILURE_MODEL

## 0. 元数据
| 字段 | 值 |
|---|---|
| Document-ID | ARCH-A4-FAILURE-MODEL |
| Status | Active |
| Owner | KnowledgeBase A4 |
| Last-Updated | 2026-02-14 |
| Applicable-Version | v5.7.x（参考 `Project_Docs/CURRENT_STATUS.md`） |
| Scope | `MapBot_Reforged/` + `Mapbot-Alpha-V1/` |
| Depends-On | Bridge 事件上报、Alpha 权威数据、多服发放链路 |
| Review-Cadence | 每月一次，或故障根因变更后立即复审 |

## 1. 目标与边界
- 目标: 对超时、断连、半成功、重试污染建立统一判定与恢复策略，保证可恢复与可观测。
- 边界: 本文定义故障处理原则与执行清单，不替代具体类/方法实现细节。

## 2. 术语与版本口径
- `Timeout`: 在约定时间窗内未收到成功响应。
- `Disconnect`: 长连接主动或被动中断，或心跳失联。
- `Half-Success`: 请求在部分环节生效，整体状态未一致（例如多服发放只成功一部分）。
- `Retry Pollution`: 无幂等控制的重复重试导致重复扣减、重复发放或状态覆盖。
- `Recovery Point`: 可确认“恢复完成”的一致性边界点。

## 3. 强制规则
### 3.1 超时策略
| 链路 | 默认超时 | 超时后行为 |
|---|---|---|
| Bridge 请求（查询类） | 2s | 失败快返 + 标记 stale cache，不阻塞主线程 |
| Bridge 请求（变更类） | 3s | 进入待确认队列，禁止直接判定成功 |
| 管理 API（HTTP） | 5s | 返回可诊断错误码，避免无限等待 |
| 心跳检测 | 连续 3 个周期丢失 | 判定断连并触发重连状态机 |

### 3.2 断连策略
- 断连后立即进入 `DEGRADED` 模式: 仅允许本地可安全执行的只读/弱一致功能。
- 重连采用指数退避 + 抖动（jitter），并设置上限，避免雪崩重连。
- 未完成的“变更类请求”必须保留请求上下文（`request_id`、时间戳、意图）以便恢复。

### 3.3 半成功处理
- 对“多服发放、绑定变更、禁言变更”等操作采用两阶段语义:
1. `PREPARED`: 意图已记录但未确认全局成功。
2. `COMMITTED`: 权威侧确认完成。
3. `COMPENSATED`: 超时/失败后执行补偿并留痕。
- 用户可见反馈必须区分“已完成”与“待确认”，禁止在 `PREPARED` 态宣告最终成功。

### 3.4 重试污染防护
- 所有变更类请求必须携带全局唯一 `request_id`，并在去重窗口内拒绝重复执行。
- 重试只允许由统一重试器触发，禁止业务层手写 `while(retry)`。
- 对已超过去重窗口的重放请求，必须要求人工确认或二次校验。
- 外部重复消息（网络抖动/重复投递）必须以幂等日志判定是否已处理。

### 3.5 恢复策略
- 冷启动恢复顺序: 连接恢复 -> 权威状态拉取 -> 待确认队列对账 -> 对外恢复写操作。
- 恢复完成判据:
1. 心跳稳定。
2. 去重索引可用。
3. 待确认队列清零或全部转 `COMPENSATED`。
- 恢复期间所有自动补偿必须输出结构化审计日志。

### 3.6 断连后 pending 请求处理规范（强制）
#### 3.6.1 最大滞留时间（Max Retention）
| 请求类型 | 状态 | 最大滞留时间 | 到期动作 |
|---|---|---|---|
| 查询类（`get_*`, `has_*`, `resolve_*`） | `PENDING` | 15 秒 | 标记 `FAILED_TIMEOUT`，立即返回降级结果 |
| 变更类（`bind_*`, `whitelist_*`, `give_item`, `playtime_add`） | `PENDING` | 120 秒 | 进入 `COMPENSATING`，触发补偿任务 |
| 文件类（`file_*`） | `PENDING` | 30 秒 | 标记失败并要求调用方重试（带新 `request_id`） |
| 关键控制类（`stop_server`, `execute_command`） | `PENDING` | 20 秒 | 标记失败并上报警报，不自动重复执行 |

#### 3.6.2 失败上报（Failure Reporting）
- 每个超时/断连失败必须产生结构化事件：`request_id`, `action`, `server_id`, `state_from`, `state_to`, `elapsed_ms`, `error_code`。
- 失败上报目标至少包含:
1. 业务日志（error 级）。
2. 告警通道（连续 5 分钟内同类失败 >= 5 次触发告警）。
3. 审计记录（可追溯到调用人/来源服务）。
- 对外返回错误码统一化: `FAILED_TIMEOUT`, `FAILED_DISCONNECT`, `FAILED_RETRY_EXHAUSTED`, `FAILED_COMPENSATION`.

#### 3.6.3 补偿动作（Compensation）
| 操作类型 | 补偿动作 | 成功判据 |
|---|---|---|
| `whitelist_add/remove` | 以 Alpha 权威绑定快照重放覆盖目标服状态 | 目标服白名单与权威快照一致 |
| `give_item` 多服发放 | 若部分成功：生成可追溯补偿任务；若全部失败：回退为离线 CDK 流程 | 不重复发放，且用户最终可领取一次 |
| `bind/unbind` | 以 Alpha 权威绑定表回写并重试白名单同步 | QQ->UUID 映射与白名单一致 |
| `playtime_add` | 按 `request_id` 去重后补写缺失增量 | 总时长无重复累加、无负漂移 |

#### 3.6.4 状态机
- `PENDING -> ACKED -> COMMITTED`
- `PENDING -> FAILED_TIMEOUT -> COMPENSATING -> COMPENSATED`
- `PENDING -> FAILED_DISCONNECT -> RETRY_SCHEDULED -> (COMMITTED | FAILED_RETRY_EXHAUSTED)`
- 任何状态迁移必须写审计日志，禁止静默丢弃 pending 记录。

### 3.7 极端场景演练：Alpha 重启 + 子服断连 + 重试风暴（防雪崩）
#### 目标
- 在三重故障叠加时，验证系统不会因无界重试导致队列爆炸、CPU 飙升或级联超时。

#### 演练步骤
1. 在高并发请求阶段（建议 200+ 并发）触发 Alpha 重启。
2. 同时下线 1~2 台子服，制造部分断连与半成功。
3. 对同一业务动作持续重放（模拟上游风暴重试）。
4. 观察 10 分钟并记录恢复窗口内指标。

#### 防雪崩控制（必须同时启用）
- 请求预算: 每实例 `inflight_write_requests <= 200`，超过即拒绝并返回 `FAILED_RETRY_EXHAUSTED`。
- 退避重试: 指数退避 `base=500ms, factor=2, max=10s` + 20% 抖动。
- 重试上限: 同一 `request_id` 最大重试次数 3 次。
- 熔断策略: 5 秒内失败率 > 50% 则熔断 30 秒，仅保留健康探测流量。
- 优先级队列: 写操作 > 查询操作，低优先级超限时丢弃并记录审计。

#### 通过阈值
- 重试风暴期间实例 CPU 使用率峰值 < 80%（持续 3 分钟窗口）。
- 待处理写请求队列长度峰值 < 1000，且 5 分钟内回落到 < 100。
- 同一 `request_id` 重复副作用数 = 0。
- 故障解除后 120 秒内恢复到 `COMMITTED/COMPENSATED` 终态占比 >= 99%。

## 4. 可操作检查点
- [ ] 配置检查: 所有关键链路都存在显式超时值，且无“无限等待”配置。
- [ ] 演练检查: 人工断开 Bridge 连接后，系统进入 `DEGRADED` 且可自动重连。
- [ ] 幂等检查: 重放同一 `request_id` 不会产生重复副作用。
- [ ] 半成功检查: 多服发放任意节点失败时，状态可见为“待确认/补偿中”，非“成功”。
- [ ] 恢复检查: 进程重启后可对账未决请求并给出最终状态。
- [ ] 观测检查: 日志与指标可区分 timeout/disconnect/retry/compensate 四类事件。

## 5. 可执行实验计划
### 实验 F-1: 超时与断连混沌实验
- 目标: 验证 timeout/disconnect 分类准确，且系统能进入并退出 `DEGRADED` 模式。
- 步骤:
1. 运行基线流量 5 分钟，记录正常错误率与延迟。
2. 注入网络抖动（延迟、丢包、短时断连）并持续 10 分钟。
3. 观察系统是否按规则触发重连、降级、恢复。
4. 对日志中 `FAILED_TIMEOUT` 与 `FAILED_DISCONNECT` 分类做抽样核验。
- 采样窗口: 总计 15 分钟；故障段 10 分钟；按 5 秒粒度采样。
- 通过阈值:
1. 分类误判率 < 1%。
2. 断连后 30 秒内进入 `DEGRADED`，恢复后 120 秒内退出。
3. 故障期间无无限等待请求（超时命中率 100%）。
- 失败处置:
1. 立即收紧超时与重连参数（降低上限，增加退避）。
2. 启动故障保护开关，暂停高风险写操作。
3. 24 小时内提交分类规则修复与回归报告。

### 实验 F-2: pending 滞留与补偿闭环实验
- 目标: 验证 pending 请求不会超时滞留，超限后会失败上报并触发补偿。
- 步骤:
1. 构造查询类、变更类、文件类、控制类请求各 200 条。
2. 在执行中断开 Alpha 与子服连接，制造大量 pending。
3. 恢复连接后观察状态迁移与补偿任务执行。
4. 核对 `PENDING -> ... -> COMMITTED/COMPENSATED` 终态覆盖率。
- 采样窗口: 故障注入 10 分钟 + 恢复观察 10 分钟。
- 通过阈值:
1. 各类请求滞留不超过 3.6.1 上限（15s/120s/30s/20s）。
2. 超限请求失败上报完整率 = 100%（字段齐全）。
3. 补偿完成后终态收敛率 >= 99%。
- 失败处置:
1. 立即清理超期 pending 并切换到保护模式（仅只读）。
2. 人工触发补偿重放并出具差异清单。
3. 若 2 小时内未收敛，升级 Sev-2 并冻结相关发布。

### 实验 F-3: 重试风暴防雪崩实验
- 目标: 验证重试预算、熔断、优先级队列在极端流量下有效，避免级联崩溃。
- 步骤:
1. 生成同类请求重放流（同 `request_id` + 不同 `request_id` 混合）。
2. 注入 Alpha 重启与子服断连，触发重试风暴。
3. 验证 `inflight` 限额、熔断开启、低优先级丢弃是否生效。
4. 统计 CPU、队列长度、重复副作用、恢复时延。
- 采样窗口: 20 分钟（风暴段 8 分钟，恢复段 12 分钟）。
- 通过阈值:
1. CPU 峰值 < 80%（3 分钟窗口）。
2. 写队列峰值 < 1000，5 分钟内回落 < 100。
3. 同一 `request_id` 重复副作用数 = 0。
- 失败处置:
1. 立即提高拒绝策略优先级（更低 inflight 上限、更长熔断）。
2. 关闭非关键写路径，保留心跳与健康探测。
3. 回滚最近重试策略变更并触发专项复盘。

## 6. 现状差距表（As-Is vs To-Be）
| 主题 | As-Is（当前实现） | To-Be（目标） | 证据路径 | 风险等级 | 整改期限 | Owner |
|---|---|---|---|---|---|---|
| 去重窗口与幂等索引 | 仅有 `pendingRequests`，无显式“已处理请求去重窗口” | 增加去重索引（TTL）并对重复 `request_id` 拒绝副作用重放 | `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/BridgeProxy.java:34`, `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/BridgeProxy.java:767` | High | 2026-03-05 | Alpha Core Maintainer |
| 半成功状态机 | 多服发放仅返回字符串 `SUCCESS/FAIL`，无 `PREPARED/COMMITTED/COMPENSATED` 持久状态 | 引入持久化状态机并可对账恢复 | `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/BridgeProxy.java:533`, `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/BridgeProxy.java:575` | High | 2026-03-12 | Alpha Core Maintainer |
| 断连后 pending 统一处置 | 超时后主要返回错误字符串，缺少统一失败码、补偿任务和告警阈值 | 落地 3.6 规范：滞留上限 + 失败上报 + 补偿闭环 | `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/BridgeProxy.java:732`, `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/BridgeProxy.java:779` | High | 2026-03-12 | SRE + Alpha Core Maintainer |

## 7. 短期阈值与逾期处理
| 项 | 临时可接受阈值（到期前） | 到期日 | 逾期处理 |
|---|---|---|---|
| 去重窗口缺失 | 允许 `request_id` 去重仅覆盖“单进程内 pending”；重启后可能丢失去重上下文 | 2026-03-05 | 逾期后冻结涉及跨服发奖/绑定的高风险变更，并升级 Sev-2 跟踪 |
| 半成功状态机未持久化 | 允许以字符串结果临时回报，但必须保留失败审计日志并人工复核补偿 | 2026-03-12 | 逾期后关闭自动多服发放，统一降级为离线 CDK 补偿路径 |
