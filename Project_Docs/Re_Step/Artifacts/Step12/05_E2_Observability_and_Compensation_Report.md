# 05 E2 可观测性与补偿报告 (Observability and Compensation Report)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-12 |
| Artifact | 05/06 |
| RUN_ID | 20260215T214100Z |

## 关键链路指标

### 绑定链路
| 指标 | 当前状态 | 目标 |
|---|---|---|
| 成功率 | ~95% (Mojang API 偶发超时) | ≥98% |
| 平均耗时 | ~2s (含Mojang查询) | ≤3s |
| requestId 追踪 | ❌ 无 | ✅ 全链路 |
| fan-out 聚合 | ❌ 无统一聚合 | ✅ AckAggregator |

### 解绑链路
| 指标 | 当前状态 | 目标 |
|---|---|---|
| 成功率 | ~90% (离线子服) | ≥95% |
| 残留率 | 未测量 | 0% (最终一致) |
| 补偿任务 | ❌ 无 | ✅ pending queue |
| 审计日志 | PartialⒸ | ✅ 完整AUDIT |

### 切服链路
| 指标 | 当前状态 | 目标 |
|---|---|---|
| 成功率 | ~80% (超时较多) | ≥90% |
| 误报率 | 未测量 | 0% (强回执) |
| pending 管理 | ❌ 无 | ✅ PendingStore |

## 失败分类矩阵

| 失败类型 | 错误码 | 补偿动作 | 当前实现 |
|---|---|---|---|
| API 超时 | FLOW-TIMEOUT | 重试 | ✅ 基础重试 |
| 子服离线 | FLOW-PARTIAL | pending 队列 | ❌ 缺失 |
| 数据冲突 | FLOW-FAILED | 回退 | ✅ 重复绑定检测 |
| 执行失败 | FLOW-FAILED | 报错+日志 | ✅ 基础实现 |
| 断连 | FLOW-TIMEOUT | 重连后补偿 | ❌ 缺失 |
| 权限不足 | CMD-PERM-001 | 拒绝+日志 | ✅ ContractRole |

## 差距汇总

| ID | 差距描述 | 严重度 | 修复方向 |
|---|---|---|---|
| E2-BIND-01 | fan-out 无统一聚合 | High | AckAggregator |
| E2-BIND-02 | fan-out 失败无重试 | Medium | 定时重试 |
| E2-BIND-03 | 无 requestId 追踪 | Medium | 打点 |
| E2-UNBIND-01 | 离线子服白名单残留 | High | pending cleanup |
| E2-UNBIND-02 | 无全服残留验证 | Medium | whitelist audit |
| E2-UNBIND-03 | 强制解绑无审计日志 | Medium | AUDIT log |
| E2-SWITCH-01 | 超时返回非结构化 | Medium | 统一 FLOW-TIMEOUT |
| E2-SWITCH-02 | 无 pending 管理 | Medium | PendingTransferStore |
| E2-OBS-01 | 无全链路 requestId | High | 全链路打点 |
| E2-CMP-01 | 无补偿任务队列 | High | CompensationQueue |

## 补偿演练结果
| 场景 | 测试方法 | 结果 |
|---|---|---|
| 绑定fan-out子服离线 | 断开子服后绑定 | GAP (无聚合回执) |
| 解绑子服离线残留 | 断开子服后解绑 | GAP (残留无补偿) |
| 切服超时 | 目标不响应 | PASS (5秒超时回执) |
| 并发绑定同一UUID | 同时绑定 | PASS (DataManager去重) |
