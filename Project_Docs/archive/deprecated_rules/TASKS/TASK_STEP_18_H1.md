# TASK_STEP_18_H1

## 任务标题
H1 升级迁移演练（中控模式）

## 规范优先级
当任务包与 `Project_Docs/Re_Step/RE_STEP_18_H1_升级迁移演练.md` 存在命名、字段、门禁冲突时，
以 `RE_STEP_18` 文档为准，任务包必须先修订后执行。

## 目标
1. 完成"前置快照 → 灰度 → 全量 → 回滚"完整升级演练。
2. 验证回滚时长 ≤15 分钟、数据一致性恢复、核心链路可用性。
3. 覆盖"灰度成功但全量失败"分叉路径。

## 强制输入
- `Project_Docs/Re_Step/RE_STEP_18_H1_升级迁移演练.md`
- `Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md`
- `Project_Docs/Manuals/DEPLOYMENT_RUNBOOK.md`
- `Project_Docs/Manuals/OPERATIONS_RUNBOOK.md`
- `Project_Docs/Manuals/RELEASE_CHECKLIST.md`
- `Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`
- `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
- `Project_Docs/Re_Step/Artifacts/Step17/*`

## 强制输出文档
目录：`Project_Docs/Re_Step/Artifacts/Step18/`
1. `01_Migration_Precheck_And_Baseline.md`
2. `02_Stage_Validation_Record.md`
3. `03_Rollback_Drill_Report.md`
4. `04_Data_Consistency_Recovery_Report.md`
5. `05_Solo_Review_Log.md`

## 强制证据输出
目录：`Project_Docs/Re_Step/Evidence/Step18/{RUN_ID}/`
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

## 工作分解
1. preflight：输入存在性、Step17 前置 5/5、合同追溯。
2. 固化迁移输入与冻结窗口 + 前置快照基线：输出 Artifact 01。
3. 分阶段升级演练 10%→30%→100% + 分叉演练：输出 Artifact 02。
4. 回滚演练 (≤15min) + 恢复验证：输出 Artifact 03。
5. 回滚后数据一致性 + 业务可用性：输出 Artifact 04。
6. 自审与准入判定：输出 Artifact 05。

## 编译与复查
- Alpha: `cd Mapbot-Alpha-V1 && ./gradlew compileJava`
- Reforged: `cd MapBot_Reforged && ./gradlew compileJava`

## 提交规则
满足门禁后必须：
1. 提交 Git：commit message 包含 `Step-18 H1`。
2. 推送 GitHub。
3. 回填 hash commit message 不含 `Step-18`。
4. 在 `CURRENT_STATE.md` 记录主提交号与结论。
