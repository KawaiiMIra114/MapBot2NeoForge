# Next Actions (Execution Order)

> Step-08 D1 已完成 (PASS)。Step-09 D2 前置门禁已满足。

1. 执行 Step-09 D2 线程与执行模型重构
   - 前置: Step-08 Artifacts 6/6 ✓
   - D1 设计待编码: protocol_version + idempotency + disconnect
2. 后续: D3 数据一致性重构
3. E1-E3 业务层重构
4. Gap Backlog 高风险修复 (30 High 项)
   - SEC-01~04 密钥安全 (截止 2026-02-21)
   - TM-01~03 线程越界 (截止 2026-02-28)
