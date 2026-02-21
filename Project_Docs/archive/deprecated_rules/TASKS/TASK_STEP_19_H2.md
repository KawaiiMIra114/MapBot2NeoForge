# TASK_STEP_19_H2

## 任务标题
H2 灰度发布与回滚控制（中控模式）

## 规范优先级
当任务包与 `Project_Docs/Re_Step/RE_STEP_19_H2_灰度发布与回滚控制.md` 存在冲突时，
以 `RE_STEP_19` 文档为准，任务包必须先修订后执行。

## 目标
1. 执行生产灰度发布：暗发布→5%→25%→100%，以量化门禁控制推进/回滚。
2. 全量后稳定窗口指标达到合同目标，不达标立即回滚并形成 RCA。
3. 确保全阶段不可跳级，门禁超阈值自动回滚。

## 强制输入
- `Project_Docs/Re_Step/RE_STEP_19_H2_灰度发布与回滚控制.md`
- `Project_Docs/Architecture/VERSIONING_AND_COMPATIBILITY.md`
- `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
- `Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md`
- `Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md`
- `Project_Docs/Manuals/RELEASE_CHECKLIST.md`
- `Project_Docs/Re_Step/Artifacts/Step18/*`

## 强制输出文档
目录：`Project_Docs/Re_Step/Artifacts/Step19/`
1. `01_Gray_Rollout_Plan.md`
2. `02_Gate_Threshold_And_Actions.md`
3. `03_Phase_Decision_Log.md`
4. `04_Rollback_And_RCA_Record.md`
5. `05_Solo_Review_Log.md`

## 强制证据输出
目录：`Project_Docs/Re_Step/Evidence/Step19/{RUN_ID}/`
- `preflight_read_manifest.txt`, `preflight_read_manifest.exit`
- `preflight_alignment.log`, `preflight_alignment.exit`
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

## 编译与复查
- Alpha: `cd Mapbot-Alpha-V1 && ./gradlew compileJava`
- Reforged: `cd MapBot_Reforged && ./gradlew compileJava`

## 提交规则
1. 主提交：message 包含 `Step-19 H2`。
2. 回填 hash 提交：`fix: backfill state hash ref`（禁止含 Step-19）。
3. CURRENT_STATE.md 中 Step-19 commit 记录主提交 hash。
