# 02_Integration_Findings — Step-C1

## 发现总览

| 分类 | 发现数 | P0 | P1(已知) | P2 |
|---|---|---|---|---|
| 编译 | 0 | 0 | 0 | 0 |
| 核心链路 | 0 | 0 | 0 | 0 |
| 权限配置 | 0 | 0 | 0 | 0 |
| 协议错误 | 0 | 0 | 0 | 0 |
| 一致性观测 | 2 | 0 | 2 | 0 |

## 详细发现

### F-01: FAIL:OCCUPIED 未使用 CONSISTENCY-409 (已知 P1)
| 属性 | 值 |
|---|---|
| 类别 | 一致性 |
| 严重性 | P1 (B3 已标注) |
| 位点 | BridgeProxy.java L216/L224 |
| 当前行为 | 返回 `FAIL:OCCUPIED:{qq}` 字符串 |
| 合同要求 | 返回 `CONSISTENCY-409` + entity_version |
| 影响范围 | 绑定冲突用户体验，不影响功能正确性 |
| 修复建议 | DataManager.bind() 返回结构化 ConflictResult，BridgeProxy 转换为 CONSISTENCY-409 |
| 整改窗口 | 2026-03-05 (P1) |
| 阻塞 C2 | 否 |

### F-02: SLO 业务指标未实现 (已知 P1)
| 属性 | 值 |
|---|---|
| 类别 | 观测 |
| 严重性 | P1 (B3 已标注) |
| 位点 | MetricsCollector (仅 TPS/内存/玩家) |
| 当前行为 | 缺 auth_decision/config_reload/bridge_request 等 9 个 Counter/Histogram |
| 影响范围 | SLO 目标不可度量，告警无法触发 |
| 修复建议 | 扩展 MetricsCollector，逐步接入各业务路径 |
| 整改窗口 | 2026-03-05 (P1) |
| 阻塞 C2 | 否 |

## 新发现 (C1 阶段首次识别): **0 个**
> C1 集成验证未发现 B3 映射审计之外的新问题。全部差距均在 Step-05 B3 阶段已完整标注。
