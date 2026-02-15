# Next Actions (Execution Order)

> Step-11 E1 完成 (CONDITIONAL PASS → GO E2)。Step-12 E2 前置门禁已满足。

1. 执行 Step-12 E2 关键业务链路重构
   - 前置: Step-11 Artifacts 6/6 ✓
   - E1 差距 (4 Medium / 4 Low): 编码时处理
2. 后续: E3 签到/CDK/游戏时间业务重构
3. Gap Backlog 高风险修复 (42 High 项)
   - D3-CAS-01/02: CAS + CONSISTENCY-409
   - D3-AP-01: DataManager 原子写
   - D3-CMP-02: CDK 半成功补偿
   - E1-SEM-02: ConsoleCommandHandler 统一分发
   - E1-AUTH-01: API 端 ContractRole 迁移
