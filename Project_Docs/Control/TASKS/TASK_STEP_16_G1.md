# TASK_STEP_16_G1

## 任务标题
G1 契约与集成测试体系建设（中控模式）

## 规范优先级
当任务包与 `Project_Docs/Re_Step/RE_STEP_16_G1_契约与集成测试体系建设.md` 存在命名、字段、门禁冲突时，
以 `RE_STEP_16` 文档为准，任务包必须先修订后执行。

## 目标
1. 建立"合同即测试"体系：Bridge 契约、权限契约、一致性故障注入全自动化。
2. 关键链路覆盖成功/失败/超时/重试四类场景。
3. 固化 Local/CI/Preprod 三层测试门禁。

## 强制输入
- `Project_Docs/Re_Step/RE_STEP_16_G1_契约与集成测试体系建设.md`
- `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`
- `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`
- `Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md`
- `Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`
- `Project_Docs/Architecture/FAILURE_MODEL.md`
- `Project_Docs/Architecture/THREADING_MODEL.md`
- `Project_Docs/Manuals/OPERATIONS_RUNBOOK.md`
- `Project_Docs/Re_Step/Artifacts/Step15/*`

## 强制输出文档
目录：`Project_Docs/Re_Step/Artifacts/Step16/`
1. `01_Contract_Test_Catalog.md`
2. `02_Integration_E2E_TestPlan.md`
3. `03_Fault_Injection_TestPlan.md`
4. `04_Test_Gate_Criteria.md`
5. `05_Solo_Review_Log.md`

## 强制证据输出
目录：`Project_Docs/Re_Step/Evidence/Step16/{RUN_ID}/`
- `preflight_read_manifest.txt`
- `preflight_read_manifest.exit`
- `preflight_alignment.log`
- `preflight_alignment.exit`
- `build_alpha.log`, `build_alpha.exit`
- `build_reforged.log`, `build_reforged.exit`
- `review_scope.log`, `review_scope.exit`
- `task_sync_fix.log`, `task_sync_fix.exit`
- `gate01_precondition.log`, `gate01_precondition.exit`
- `gate02_sections.log`, `gate02_sections.exit`
- `gate03_term_consistency.log`, `gate03_term_consistency.exit`
- `gate04_weakened_semantics.log`, `gate04_weakened_semantics.exit`
- `gate09_evidence_completeness.log`, `gate09_evidence_completeness.exit`
- `gate10_commit_not_pending.log`, `gate10_commit_not_pending.exit`
- `gate11_next_taskfile_exists.log`, `gate11_next_taskfile_exists.exit`
- `gate_summary.txt`
- `final_verdict.md`

## 工作分解
1. preflight：输入存在性、Step15 前置 5/5、合同追溯、对齐检查。
2. 固化契约测试目录 CT-01~CT-18：输出 Artifact 01。
3. 建立端到端集成测试计划：输出 Artifact 02。
4. 落地故障注入测试计划：输出 Artifact 03。
5. 固化三层测试门禁标准：输出 Artifact 04。
6. 自审与准入判定：输出 Artifact 05。

## 编译与复查
- Alpha: `cd Mapbot-Alpha-V1 && ./gradlew compileJava`
- Reforged: `cd MapBot_Reforged && ./gradlew compileJava`

## 提交规则
满足门禁后必须：
1. 提交 Git：commit message 包含 `Step-16 G1`。
2. 推送 GitHub。
3. 在 `CURRENT_STATE.md` 记录提交号与结论。
