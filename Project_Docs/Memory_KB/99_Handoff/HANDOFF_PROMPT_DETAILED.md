# Handoff Prompt (Detailed, High-Constraint)

你现在接管 `MapBot2NeoForge` 的 Step-04 修复执行。

请严格遵守以下顺序与约束，不得跳步：

1. 先读 Memory_KB（必读）
- `Project_Docs/Memory_KB/README.md`
- `Project_Docs/Memory_KB/02_Status/CURRENT_STATE.md`
- `Project_Docs/Memory_KB/02_Status/BLOCKERS_STEP04.md`
- `Project_Docs/Memory_KB/03_Plan/NEXT_ACTIONS.md`
- `Project_Docs/Memory_KB/06_Execution/STEP04_FIX_RUNBOOK.md`

2. 再执行修复流程（按 Runbook）
- 读取上下文
- 定位代码根因
- 最小化修复
- 本地非构建验证
- 回填文档与证据
- 门禁判定
- 输出结论

3. 强约束（必须满足）
- 默认不构建、不打包、不做全量流水线。
- 构建验证由主控侧执行，你只提供可复核修复与非构建证据。
- 不回退他人改动，不处理与 Step-04 无关的重构。
- 结论必须可追溯到文档与证据。

4. 输出格式（必须按此结构）
- `Verdict`: GO / NO-GO
- `Blocking Issues`: 每条阻断的状态（Closed/Open）
- `Fix Actions`: 本轮实际修改
- `Verification Scope`: 已验证 / 未验证（含原因）
- `Build Validation`: 明确“主控侧执行”
- `Risks & Next Actions`: 剩余风险与最小闭环动作

执行原则：先正确、后完整；先证据、后结论；未满足门禁一律 `NO-GO`。
