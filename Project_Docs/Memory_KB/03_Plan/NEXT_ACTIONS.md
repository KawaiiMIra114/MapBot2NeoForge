# Next Actions (Execution Order)

> Step-06 C1 已完成 (CONDITIONAL PASS)。Step-07 C2 前置门禁已满足。

1. 执行 Step-07 C2 安全边界与版本兼容评审
   - 前置: Step-06 Artifacts 6/6 ✓
   - SEC-01~SEC-04 高风险项需在评审中进一步分析
2. 完成 C2 后进入 D 阶段 (重构实现)
   - D1: 线程安全修复 (TH-01/TH-02/FP-01/FP-02)
   - D2: 故障状态机落地 (FM-01~FM-07)
   - D3: 安全边界修复 (SEC-01~SEC-04)
3. 运行时验证 (T/F 系列实验)
   - T-2/T-3 可即时执行
   - T-1 修复后执行
   - F-1/F-2/F-3 D 阶段后执行
