# TASK_STEP_17_G2

## 任务标题
G2 发布门禁自动化（中控模式）

## 规范优先级
当任务包与 `Project_Docs/Re_Step/RE_STEP_17_G2_发布门禁自动化.md` 存在命名、字段、门禁冲突时，
以 `RE_STEP_17` 文档为准，任务包必须先修订后执行。

## 目标
1. 将发布清单转成可阻断自动化门禁。
2. 测试完整性、文档联动、术语漂移、安全扫描、回滚就绪全部自动判定。
3. 未通过禁止发布。

## 强制输入
- `Project_Docs/Re_Step/RE_STEP_17_G2_发布门禁自动化.md`
- `Project_Docs/Manuals/RELEASE_CHECKLIST.md`
- `Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md`
- `Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md`
- `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`
- `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
- `Project_Docs/Re_Step/Artifacts/Step16/*`

## 强制输出文档
目录：`Project_Docs/Re_Step/Artifacts/Step17/`
1. `01_Release_Gate_Pipeline_Design.md`
2. `02_Automated_Checks_Spec.md`
3. `03_Rollback_Readiness_Gate.md`
4. `04_Go_NoGo_Decision_Template.md`
5. `05_Solo_Review_Log.md`

## 强制证据输出
目录：`Project_Docs/Re_Step/Evidence/Step17/{RUN_ID}/`
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
1. preflight：输入存在性、Step16 前置 5/5、合同追溯。
2. 拆解发布硬/软门禁：输出 Artifact 01。
3. 落地自动化检查规则：输出 Artifact 02。
4. 构建回滚就绪门禁：输出 Artifact 03。
5. 固化 Go/No-Go 决策模板：输出 Artifact 04。
6. 自审与准入判定：输出 Artifact 05。

## 编译与复查
- Alpha: `cd Mapbot-Alpha-V1 && ./gradlew compileJava`
- Reforged: `cd MapBot_Reforged && ./gradlew compileJava`

## 提交规则
满足门禁后必须：
1. 提交 Git：commit message 包含 `Step-17 G2`。
2. 推送 GitHub。
3. 在 `CURRENT_STATE.md` 记录提交号与结论。
