# 05 D1 Contract Test and Chaos Result

## 元数据
| 属性 | 值 |
|---|---|
| Step | Step-08 D1 |
| RUN_ID | 20260215T201100Z |
| 依据 | RE_STEP_08 §6 |

## 1. 契约测试用例

### CT-01: 注册携带 protocol_version
| 项 | 定义 |
|---|---|
| 输入 | register 消息含 protocol_version=1.0.0 |
| 期望 | Alpha 返回 register_ack + protocol_version + capabilities |
| 结果 | 🔜 待实现后验证 |

### CT-02: 缺失 protocol_version 注册
| 项 | 定义 |
|---|---|
| 输入 | register 消息不含 protocol_version |
| 期望 | 临时接受 + 告警日志 (到期 2026-03-08 后拒绝) |
| 结果 | 🔜 待实现后验证 |

### CT-03: MAJOR 不兼容拒绝
| 项 | 定义 |
|---|---|
| 输入 | register 消息含 protocol_version=2.0.0 (本地 1.0.0) |
| 期望 | register_ack status=VERSION_MISMATCH + BRG_VALIDATION_206 |
| 结果 | 🔜 待实现后验证 |

### CT-04: 幂等键首次请求
| 项 | 定义 |
|---|---|
| 输入 | whitelist_add 含新 idempotencyKey |
| 期望 | 正常处理 + 缓存写入 |
| 结果 | 🔜 待实现后验证 |

### CT-05: 幂等键重复请求 (PENDING 中)
| 项 | 定义 |
|---|---|
| 输入 | 同一 idempotencyKey 再次发送 (前次仍 PENDING) |
| 期望 | 返回 BRG_VALIDATION_207 (DEDUP_CONFLICT) |
| 结果 | 🔜 待实现后验证 |

### CT-06: 幂等键重复请求 (已完成)
| 项 | 定义 |
|---|---|
| 输入 | 同一 idempotencyKey 再次发送 (前次已 SUCCESS) |
| 期望 | 返回缓存结果 (不重复执行) |
| 结果 | 🔜 待实现后验证 |

### CT-07: 幂等键重复请求 (已失败)
| 项 | 定义 |
|---|---|
| 输入 | 同一 idempotencyKey 再次发送 (前次已 FAILED) |
| 期望 | 允许重试 (清除旧缓存) |
| 结果 | 🔜 待实现后验证 |

### CT-08: 断连快失败
| 项 | 定义 |
|---|---|
| 输入 | 3 个 pending 请求 → 断连事件 |
| 期望 | 3 个全部 completeExceptionally + BRG_TRANSPORT_303 |
| 结果 | 🔜 待实现后验证 |

### CT-09: 断连后无堆积
| 项 | 定义 |
|---|---|
| 输入 | 断连事件 |
| 期望 | pendingRequests.size() == 0 |
| 结果 | 🔜 待实现后验证 |

### CT-10: 帧大小超限
| 项 | 定义 |
|---|---|
| 输入 | 65KiB payload |
| 期望 | BRG_VALIDATION_205 |
| 依据 | BridgeServer L88, BridgeProxy L733 |
| 结果 | ✅ 代码已实现 |

### CT-11: 结构化错误码回包
| 项 | 定义 |
|---|---|
| 输入 | 未注册连接发送请求 |
| 期望 | BRG_VALIDATION_201 |
| 依据 | BridgeServer L116 |
| 结果 | ✅ 代码已实现 |

## 2. 混沌验证场景

### CH-01: 超时风暴
| 项 | 定义 |
|---|---|
| 场景 | 200 并发请求 + Alpha 不回复 |
| 期望 | 全部 10s 后超时 + BRG_TIMEOUT_501, 无无限等待 |
| 前置 | CT-08 通过后 |
| 状态 | 🔜 |

### CH-02: 重放风暴
| 项 | 定义 |
|---|---|
| 场景 | 对同一 give_item 连续重放 10 次 |
| 期望 | 仅第 1 次执行, 其余返回 BRG_VALIDATION_207 或缓存结果 |
| 前置 | CT-04/05/06 通过后 |
| 状态 | 🔜 |

### CH-03: 断连重连循环
| 项 | 定义 |
|---|---|
| 场景 | 10 次快速断连重连 + 每次 5 pending |
| 期望 | 每次断连 pending 立即清零, 重连后正常注册 |
| 前置 | CT-08/09 通过后 |
| 状态 | 🔜 |

## 3. 验证进度
| 类别 | 总数 | 已通过 | 待验证 |
|---|---|---|---|
| 契约测试 | 11 | 2 (CT-10, CT-11) | 9 |
| 混沌验证 | 3 | 0 | 3 |

## 4. 说明
D1 阶段是**设计评审**，契约测试和混沌验证将在代码实施后执行。当前产物定义测试用例和期望行为，作为实施的验收标准。
已通过的 CT-10、CT-11 基于现有代码静态确认。
