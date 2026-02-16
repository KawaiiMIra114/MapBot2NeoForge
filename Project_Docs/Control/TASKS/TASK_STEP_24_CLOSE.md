# TASK_STEP_24_CLOSE

## 任务标题
CLOSE 全量核查与最终验收

## 目标
1. 建立 Step-01~Step-23 功能映射矩阵 → 01_Step01_23_Functionality_Coverage_Matrix.md
2. 全代码逻辑审计 → 02_Full_Code_Logic_Audit_Report.md
3. 修复与差异汇总 → 03_Fixes_And_Diff_Summary.md
4. 回归与门禁验证 → 04_Regression_And_Gate_Verification.md
5. 中文 walkthrough 验收报告 → 05_Walkthrough_最终验收报告_中文.md
6. 自审与最终收口判定 → 06_Solo_Review_Log.md

## 对齐规范
- RE_STEP: `Project_Docs/Re_Step/RE_STEP_24_CLOSE_全量核查与最终验收.md`
- 优先级: RE_STEP_24 > TASK_STEP_24 > 其他

## 输入材料
1. `Project_Docs/Re_Step/RE_STEP_01_*.md` ~ `RE_STEP_23_*.md`
2. `Project_Docs/Contracts/*.md`
3. `Project_Docs/Architecture/*.md`
4. `Project_Docs/Manuals/*.md`
5. `Project_Docs/Memory_KB/02_Status/CURRENT_STATE.md`
6. 全量代码 (Alpha 55 files, Reforged 44 files)

## 强制输出文档
目录: `Project_Docs/Re_Step/Artifacts/Step24/`
1. `01_Step01_23_Functionality_Coverage_Matrix.md`
2. `02_Full_Code_Logic_Audit_Report.md`
3. `03_Fixes_And_Diff_Summary.md`
4. `04_Regression_And_Gate_Verification.md`
5. `05_Walkthrough_最终验收报告_中文.md`
6. `06_Solo_Review_Log.md`

## 前置硬门禁
Step23 产物 5/5 必须存在且非空。

## 强制证据输出
目录: `Project_Docs/Re_Step/Evidence/Step24/{RUN_ID}/`
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

## 提交规则
1. 主提交: message 包含 `Step-24 CLOSE`。
2. 回填 hash 提交: `fix: backfill state hash ref` (禁止含 Step-24)。

## 状态
RUNNING