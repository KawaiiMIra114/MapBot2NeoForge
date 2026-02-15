# Next Actions (Execution Order)

> Step-10 D3 复验通过 (validate_delivery.py EXIT:0)。Step-11 E1 前置门禁已满足。

1. 执行 Step-11 E1 命令语义统一重构
   - 前置: Step-10 Artifacts 6/6 ✓ + 复验 PASS ✓
   - D3 差距 Batch 1 (7 High): 编码时优先处理 CAS + 原子持久化
2. 后续: E2 Bot event 与 Bridge handler 重构
3. 后续: E3 签到/CDK/游戏时间业务重构
4. Gap Backlog 高风险修复 (42 High 项)
   - D3-CAS-01/02: CAS + CONSISTENCY-409
   - D3-AP-01: DataManager 原子写
   - D3-CMP-02: CDK 半成功补偿
   - D3-REC-01/02: 恢复框架 + event_log
