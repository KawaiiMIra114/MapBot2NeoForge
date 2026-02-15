# Next Actions (Execution Order)

> Step-07 C2 已完成 (CONDITIONAL PASS)。C 阶段评审全部结束。进入 D 阶段重构。

1. 执行 Step-08 D1 Bridge 通道核心重构
   - 前置: Step-06 + Step-07 Artifacts 全部 ✓
   - 按 Gap Backlog 优先级开始修复
2. D1 重点修:
   - SB-02 密钥从仓库移除 (紧急, 截止 2026-02-21)
   - TH-01/TH-02 线程安全修复 (截止 2026-02-28)
   - SB-01 硬编码移除 (截止 2026-02-28)
3. 后续:
   - D2: 故障状态机落地
   - D3: 安全传输+轮换
   - E: 弃用+灰度基础设施
