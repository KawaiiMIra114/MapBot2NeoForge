# TASK_STEP_15_F2

## 任务标题
F2 运维手册联调与验证（中控模式）

## 规范优先级
当任务包与 `Project_Docs/Re_Step/RE_STEP_15_F2_运维手册联调与验证.md` 存在命名、字段、门禁冲突时，
以 `RE_STEP_15` 文档为准，任务包必须先修订后执行。

## 目标
1. 将部署、日常运维、事故响应三本手册联调为单条可复现链路。
2. 按手册操作可得到一致结果。
3. 固定失败阈值与闭环证据。

## 强制输入
- `Project_Docs/Re_Step/RE_STEP_15_F2_运维手册联调与验证.md`
- `Project_Docs/Manuals/DEPLOYMENT_RUNBOOK.md`
- `Project_Docs/Manuals/OPERATIONS_RUNBOOK.md`
- `Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md`
- `Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md`
- `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
- `Project_Docs/Re_Step/Artifacts/Step14/*`

## 强制输出文档
目录：`Project_Docs/Re_Step/Artifacts/Step15/`
1. `01_Runbook_E2E_Rehearsal_Record.md`
2. `02_Threshold_Validation_Report.md`
3. `03_Reload_Positive_Negative_Test_Report.md`
4. `04_Incident_Drill_Evidence.md`
5. `05_Solo_Review_Log.md`

## 强制证据输出
目录：`Project_Docs/Re_Step/Evidence/Step15/{RUN_ID}/`
- `preflight_read_manifest.txt`
- `preflight_contract_trace.txt`
- `preflight_code_coverage.txt`
- `preflight_prev_step_gate.log`, `preflight_prev_step_gate.exit`
- `build_alpha.log`, `build_alpha.exit`
- `build_reforged.log`, `build_reforged.exit`
- `review_scope.log`, `review_scope.exit`
- `gate01_prev_step.log` ~ `gate11_next_taskfile_exists.log` + `.exit`
- `gate_summary.txt`
- `final_verdict.md`

## 工作分解
1. preflight：输入存在性、Step14 前置 5/5、手册追溯、代码覆盖基线。
2. 对齐三本手册阈值与术语：输出 Artifact 01。
3. 验证 F01-F18 阈值与动态校准：输出 Artifact 02。
4. 执行 #reload 正负向矩阵：输出 Artifact 03。
5. 事故响应演练 + 回滚验证：输出 Artifact 04。
6. 自审与准入判定：输出 Artifact 05。

## 编译与复查
- Alpha: `cd Mapbot-Alpha-V1 && ./gradlew compileJava`
- Reforged: `cd MapBot_Reforged && ./gradlew compileJava`
- 复查: `rg -n "TODO|FIXME|TEMP|HACK" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`

## 提交规则
满足门禁后必须：
1. 提交 Git：commit message 包含 `Step-15 F2`。
2. 推送 GitHub。
3. 在 `CURRENT_STATE.md` 记录提交号与结论。
