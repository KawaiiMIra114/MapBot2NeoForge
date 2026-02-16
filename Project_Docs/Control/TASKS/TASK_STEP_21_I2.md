# TASK_STEP_21_I2

## 任务标题
I2 开源治理落地（单人维护版）

## 规范优先级
当任务包与 `Project_Docs/Re_Step/RE_STEP_21_I2_开源治理落地.md` 存在冲突时，
以 `RE_STEP_21` 文档为准，任务包必须先修订后执行。

## 目标
1. 完成许可证与社区治理基线（LICENSE/CONTRIBUTING/SECURITY/CODE_OF_CONDUCT）。
2. 固化去敏示例配置规则、建立自动扫描策略。
3. 验证外部贡献者可独立 clone→build→test→run。
4. 固化发布产物结构与文档入口。
5. 执行安全与合规检查，自审通过并准入 J1。

## 强制输入
- `Project_Docs/Re_Step/RE_STEP_21_I2_开源治理落地.md`
- `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`
- `Project_Docs/SYSTEM_CRITIQUE_AND_REFACTOR_PAPER_V1.md`
- `Project_Docs/Manuals/RELEASE_CHECKLIST.md`
- `Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md`
- `Project_Docs/Architecture/VERSIONING_AND_COMPATIBILITY.md`
- `Project_Docs/Contracts/PRIVACY_POLICY.md`
- `Project_Docs/Re_Step/Artifacts/Step20/*`

## 强制输出文档
目录：`Project_Docs/Re_Step/Artifacts/Step21/`
1. `01_OpenSource_Governance_Checklist.md`
2. `02_Sanitized_Example_Config_Spec.md`
3. `03_External_Contributor_Onboarding_Test.md`
4. `04_Release_Artifact_Layout_Spec.md`
5. `05_Solo_Review_Log.md`

## 强制证据输出
目录：`Project_Docs/Re_Step/Evidence/Step21/{RUN_ID}/`
- `preflight_read_manifest.txt`, `preflight_read_manifest.exit`
- `preflight_alignment.log`, `preflight_alignment.exit`
- `task_sync_fix.log`, `task_sync_fix.exit`
- `review_scope.log`, `review_scope.exit`
- `build_alpha.log`, `build_alpha.exit`
- `build_reforged.log`, `build_reforged.exit`
- `gate01_precondition.log`, `gate01_precondition.exit`
- `gate02_sections.log`, `gate02_sections.exit`
- `gate03_term_consistency.log`, `gate03_term_consistency.exit`
- `gate04_weakened_semantics.log`, `gate04_weakened_semantics.exit`
- `validate_precommit.log`, `validate_precommit.exit`
- `validate_policy_exception.log`, `validate_policy_exception.exit`
- `gate09_evidence_completeness.log`, `gate09_evidence_completeness.exit`
- `gate10_commit_not_pending.log`, `gate10_commit_not_pending.exit`
- `gate11_next_taskfile_exists.log`, `gate11_next_taskfile_exists.exit`
- `validate_postcommit.log`, `validate_postcommit.exit`
- `delivery_integrity_summary.log`
- `gate_summary.txt`
- `final_verdict.md`

## 编译与复查
- Alpha: `cd Mapbot-Alpha-V1 && ./gradlew compileJava`
- Reforged: `cd MapBot_Reforged && ./gradlew compileJava`

## 提交规则
1. 主提交：message 包含 `Step-21 I2`。
2. 回填 hash 提交：`fix: backfill state hash ref`（禁止含 Step-21）。
3. CURRENT_STATE.md 中 Step-21 commit 记录主提交 hash。
