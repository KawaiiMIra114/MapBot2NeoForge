# TASK_STEP_13_E3

## 任务标题
E3 管理面API语义统一（中控模式）

## 规范优先级
当任务包与 `Project_Docs/Re_Step/RE_STEP_13_E3_管理面API语义统一.md` 存在命名、字段、门禁冲突时，
以 `RE_STEP_13` 文档为准，任务包必须先修订后执行。

## 目标
1. 统一管理面 HTTP API、QQ 命令入口、Bridge 回执链路为同一语义模型。
2. 同一动作返回同类状态（SUCCESS/FAILED/PENDING）与同一错误码口径。
3. 杜绝"API 假成功、机器人真失败"。
4. 输出进入后续阶段的准入结论。

## 强制输入
- `Project_Docs/Re_Step/RE_STEP_13_E3_管理面API语义统一.md`
- `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`
- `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`
- `Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md`
- `Project_Docs/Architecture/DATA_FLOW_AND_STATE.md`
- `Project_Docs/Architecture/FAILURE_MODEL.md`
- `Project_Docs/Architecture/VERSIONING_AND_COMPATIBILITY.md`
- `Project_Docs/Re_Step/Artifacts/Step12/*`
- `Project_Docs/Memory_KB/*`

## 强制输出文档
目录：`Project_Docs/Re_Step/Artifacts/Step13/`
1. `01_API_Command_Semantics_Matrix.md`
2. `02_Ack_State_Model.md`
3. `03_ErrorCode_And_HTTP_Mapping.md`
4. `04_Compatibility_And_Deprecation_Plan.md`
5. `05_Solo_Review_Log.md`

## 强制证据输出
目录：`Project_Docs/Re_Step/Evidence/Step13/{RUN_ID}/`
- `preflight_read_manifest.txt`
- `preflight_contract_trace.txt`
- `preflight_code_coverage.txt`
- `preflight_prev_step_gate.log`, `preflight_prev_step_gate.exit`
- `build_alpha.log`, `build_alpha.exit`
- `build_reforged.log`, `build_reforged.exit`
- `review_scope.log`, `review_scope.exit`
- `gate01_prev_step.log`, `gate01_prev_step.exit`
- `gate02_artifacts.log`, `gate02_artifacts.exit`
- `gate03_build_alpha.log`, `gate03_build_alpha.exit`
- `gate04_build_reforged.log`, `gate04_build_reforged.exit`
- `gate05_review_scope.log`, `gate05_review_scope.exit`
- `gate06_api_keywords.log`, `gate06_api_keywords.exit`
- `gate07_current_step_updated.log`, `gate07_current_step_updated.exit`
- `gate08_required_inputs_exist.log`, `gate08_required_inputs_exist.exit`
- `gate09_evidence_completeness.log`, `gate09_evidence_completeness.exit`
- `gate10_commit_not_pending.log`, `gate10_commit_not_pending.exit`
- `gate11_next_taskfile_exists.log`, `gate11_next_taskfile_exists.exit`
- `gate_summary.txt`
- `final_verdict.md`

## 工作分解
1. preflight：输入存在性、Step12 前置产物检查、合同追溯、代码覆盖基线。
2. 建立"动作-语义"基线矩阵：输出 Artifact 01。
3. 统一回执等待与 pending 规则：输出 Artifact 02。
4. 统一错误码与 HTTP 映射：输出 Artifact 03。
5. 完成兼容窗口与退场计划：输出 Artifact 04。
6. 自审与准入判定：输出 Artifact 05。

## 编译与复查
- Alpha: `cd Mapbot-Alpha-V1 && ./gradlew compileJava`
- Reforged: `cd MapBot_Reforged && ./gradlew compileJava`
- 复查: `rg -n "TODO|FIXME|TEMP|HACK" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`

## CURRENT_STEP 更新要求
1. 执行前：`Status: RUNNING`
2. 阻断：`Status: BLOCKED` + `BlockReason`
3. 完成后推进下一步：`Status: READY`
4. 每次更新刷新 `EffectiveDate`（ISO 8601）

## 提交规则
满足门禁后必须：
1. 提交 Git：commit message 包含 `Step-13 E3`。
2. 推送 GitHub。
3. 在 `CURRENT_STATE.md` 记录提交号与结论。
