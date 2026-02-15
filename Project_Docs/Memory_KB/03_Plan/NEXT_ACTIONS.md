# Next Actions (Execution Order)

> C1 端到端集成验证已完成。以下为后续行动计划。

1. 进入 C2（如有定义），或标记 Re_Step 流程全链完成
   - C1 已确认: 全部核心链路可达、编译通过、协议一致、权限配置闭环

2. P1 整改推进 (2026-03-05 窗口)
   - entity_version + CAS → DataManager 写入路径
   - CONSISTENCY-409 统一错误码
   - SLO 指标基础框架 (Counter/Histogram)
   - 防雪崩基础 (inflight 限额 + 指数退避)

3. 运行时验证 (可选)
   - 部署到测试环境进行实际运行验证
   - 执行 B3 负面测试用例 (05_B3_Negative_Test_Cases)
