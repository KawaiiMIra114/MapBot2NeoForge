# TASK_STEP_20_I1

## 任务标题
I1 稳定化冲刺（单人维护版）

## 规范优先级
当任务包与 `Project_Docs/Re_Step/RE_STEP_20_I1_稳定化冲刺.md` 存在冲突时，
以 `RE_STEP_20` 文档为准，任务包必须先修订后执行。

## 目标
1. 收敛灰度遗留问题与技术债，按 P0/P1/P2 分级并定义修复窗口。
2. 完成"合同-实现-手册"三方一致性复审，输出差异清单。
3. 更新稳定基线与阈值依据。
4. 固化 RC 准入门槛。
5. 验证稳定窗口（≥7 天，Sev-0/Sev-1 = 0）。

## 强制输入
- `Project_Docs/Re_Step/RE_STEP_20_I1_稳定化冲刺.md`
- `Project_Docs/Contracts/CONTRACT_INDEX.md`
- `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`
- `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`
- `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
- `Project_Docs/Architecture/SYSTEM_CONTEXT.md`
- `Project_Docs/Architecture/FAILURE_MODEL.md`
- `Project_Docs/Manuals/RELEASE_CHECKLIST.md`
- `Project_Docs/Re_Step/Artifacts/Step19/*`

## 强制输出文档
目录：`Project_Docs/Re_Step/Artifacts/Step20/`
1. `01_Stabilization_Backlog.md`
2. `02_Contract_Impl_Manual_Consistency_Report.md`
3. `03_RC_Readiness_Checklist.md`
4. `04_Updated_Baseline_And_Thresholds.md`
5. `05_Solo_Review_Log.md`

## 强制证据输出
目录：`Project_Docs/Re_Step/Evidence/Step20/{RUN_ID}/`
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
1. 主提交：message 包含 `Step-20 I1`。
2. 回填 hash 提交：`fix: backfill state hash ref`（禁止含 Step-20）。
3. CURRENT_STATE.md 中 Step-20 commit 记录主提交 hash。
