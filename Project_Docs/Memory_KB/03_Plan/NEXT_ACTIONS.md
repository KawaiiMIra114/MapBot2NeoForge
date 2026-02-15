# Next Actions (Execution Order)

> Step-12 E2 完成 (CONDITIONAL PASS → GO E3/F)。Step-13 E3 前置门禁已满足。

1. 执行 Step-13 E3 签到/CDK/游戏时间业务重构
   - 前置: Step-12 Artifacts 6/6 ✓
   - E2 差距 (4 High / 6 Medium): 编码时处理
2. 后续: F 阶段 最终集成与验收
3. Gap Backlog 高风险修复 (46 High 项)
   - E2-BIND-01: fan-out 统一聚合
   - E2-UNBIND-01: 离线子服白名单残留
   - E2-OBS-01: 全链路 requestId
   - E2-CMP-01: 补偿任务队列
   - D3-CAS-01/02: CAS + CONSISTENCY-409
   - E1-SEM-02: ConsoleCommandHandler 统一分发
