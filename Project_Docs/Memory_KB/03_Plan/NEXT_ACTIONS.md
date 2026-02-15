# Next Actions (Execution Order)

> B3 映射审计已完成。以下为 C1 阶段行动计划。

1. 进入 C1：端到端集成验证
   - 目标：验证 Alpha → Bridge → Reforged → MC 全链路消息流
   - 范围：绑定/解绑/签到/白名单同步/配置重载

2. P1 整改推进 (2026-03-05 窗口)
   - entity_version + CAS → DataManager 写入路径
   - CONSISTENCY-409 统一错误码
   - SLO 指标基础框架 (Counter/Histogram)
   - 防雪崩基础 (inflight 限额 + 指数退避)

3. 文档同步
   - 更新 CONTRACT_INDEX 标注 B3 映射状态
   - 更新 FAILURE_MODEL 差距表
