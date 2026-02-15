# 05_B3_Negative_Test_Cases

## 合同引用
- `A2-DATA-CONSISTENCY` §5.3 冲突行为
- `A2-OBS-SLO` §5.3 告警触发
- `ARCH-A4-FAILURE-MODEL` §3.1-3.7

## 负面测试用例

### N-01: 并发绑定竞态
| 属性 | 值 |
|---|---|
| 场景 | 两个 QQ 同时绑定同一 UUID |
| 操作 | 并发调用 DataManager.bind(qq1, uuid) 和 bind(qq2, uuid) |
| 合同预期 | 第一个成功，第二个返回 CONSISTENCY-409 + expected_version |
| 当前行为 | 第二个返回 false (FAIL:OCCUPIED)，无版本号 |
| 合同条款 | A2-DATA-CONSISTENCY §5.3 |
| 差距 | 错误码非标准，缺 entity_version |
| 验收标准 | 返回包含 CONSISTENCY-409 和 version 信息 |

### N-02: 重复发放物品
| 属性 | 值 |
|---|---|
| 场景 | 同一签到奖励因网络超时被重试发放 |
| 操作 | 发送 give_item 请求 → 超时 → 重试 → 实际前一条请求已成功 |
| 合同预期 | 幂等键去重，第二次请求返回 "已处理" |
| 当前行为 | 无去重，物品可能被双倍发放 |
| 合同条款 | MB2N-BRIDGE-MESSAGE §7, FAILURE_MODEL §3.4 |
| 差距 | 无 idempotencyKey 机制 |
| 验收标准 | 同一 idempotencyKey 的请求不重复执行 |

### N-03: Bridge 半成功无补偿
| 属性 | 值 |
|---|---|
| 场景 | giveItemToOnlineServers 部分服务器成功部分失败 |
| 操作 | 3 服在线，服A成功，服B超时，服C返回FAIL |
| 合同预期 | 标记为半成功，触发补偿任务 |
| 当前行为 | 返回 "SUCCESS:1/3"，无补偿 |
| 合同条款 | FAILURE_MODEL §3.3 |
| 差距 | 无补偿机制 |
| 验收标准 | 半成功触发后台补偿任务 |

### N-04: 无限重试雪崩
| 属性 | 值 |
|---|---|
| 场景 | Bridge 服务器网络中断，客户端无限重连 |
| 操作 | 断开网络 → BridgeClient 持续固定3s重试 |
| 合同预期 | 指数退避(500ms→10s) + 3次上限 + 熔断30s |
| 当前行为 | 固定3s间隔无限重试 |
| 合同条款 | FAILURE_MODEL §3.7 |
| 差距 | 无指数退避/上限/熔断 |
| 验收标准 | 实现指数退避 + 重试上限 + 熔断机制 |

### N-05: 指标丢失无告警
| 属性 | 值 |
|---|---|
| 场景 | MetricsCollector 线程异常退出 |
| 操作 | MetricsCollector.collect() 抛出未捕获异常 |
| 合同预期 | 触发 S2 告警，启动自愈或报警 |
| 当前行为 | 异常被 catch 后仅日志，无告警 |
| 合同条款 | A2-OBS-SLO §5.3 |
| 差距 | 无告警触发机制 |
| 验收标准 | 采集线程异常触发告警 + 自动恢复 |

### N-06: 快照数据损坏无回退
| 属性 | 值 |
|---|---|
| 场景 | bindings.properties 文件损坏 |
| 操作 | 手动破坏 bindings.properties → 启动系统 |
| 合同预期 | checksum 校验失败 → 回退到上一有效快照 |
| 当前行为 | Properties.load 异常 → LOGGER.error → 数据丢失 |
| 合同条款 | A2-DATA-CONSISTENCY §5.5 |
| 差距 | 无 checksum 校验和回退 |
| 验收标准 | 校验失败后回退到备份快照 |

### N-07: 超时分类不正确
| 属性 | 值 |
|---|---|
| 场景 | 查询请求使用10s超时（合同要求2s） |
| 操作 | 发送 get_status 查询请求 |
| 合同预期 | 查询类 2s，变更类 3s，文件类 30s |
| 当前行为 | 全部统一 10s |
| 合同条款 | FAILURE_MODEL §3.1 |
| 差距 | 超时不分类 |
| 验收标准 | 按请求类型使用不同超时值 |

### N-08: Pending 积压无上限
| 属性 | 值 |
|---|---|
| 场景 | 大量 Bridge 请求积压未响应 |
| 操作 | 网络慢，发送 500+ 请求到 pendingRequests |
| 合同预期 | inflight ≤200，超额拒绝 |
| 当前行为 | 无上限 |
| 合同条款 | FAILURE_MODEL §3.7 |
| 差距 | 无 inflight 限额 |
| 验收标准 | pendingRequests.size() ≥ 200 时拒绝新请求 |

## 综合判定
- 8 个负面测试用例全部标注差距
- 0 个当前可通过
- 全部属于合同整改窗口内的中期改进项
