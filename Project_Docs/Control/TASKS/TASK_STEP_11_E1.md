# TASK_STEP_11_E1

## 任务标题
E1 命令语义统一重构（中控模式）

## 规范优先级
当任务包与 `Project_Docs/Re_Step/RE_STEP_11_E1_命令语义统一重构.md` 存在命名、字段、门禁冲突时，
以 `RE_STEP_11` 文档为准，任务包必须先修订后执行。

## 目标
1. 统一命令注册与分发逻辑（CommandRegistry）。
2. 统一权限检查与错误反馈语义。
3. 统一命令帮助与文档生成。
4. 输出进入 E2 的准入结论与回滚边界。

## 强制输入
- `Project_Docs/Re_Step/RE_STEP_11_E1_命令语义统一重构.md`
- `Project_Docs/SYSTEM_CRITIQUE_AND_REFACTOR_PAPER_V1.md`
- `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`
- `Project_Docs/Re_Step/Artifacts/Step10/*`
- `Project_Docs/Memory_KB/*`

## 强制输出文档
目录：`Project_Docs/Re_Step/Artifacts/Step11/`
1. `01_E1_Command_Refactor_Scope.md`
2. `02_CommandRegistry_and_Dispatch_Design.md`
3. `03_Permission_and_ErrorFeedback_Standard.md`
4. `04_Help_and_DocGen_Design.md`
5. `05_E1_Integration_Test_Report.md`
6. `06_Solo_Review_Log_E1.md`

## 强制证据输出
目录：`Project_Docs/Re_Step/Evidence/Step11/{RUN_ID}/`
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
- `gate06_command_keywords.log`, `gate06_command_keywords.exit`
- `gate07_current_step_updated.log`, `gate07_current_step_updated.exit`
- `gate08_required_inputs_exist.log`, `gate08_required_inputs_exist.exit`
- `gate09_evidence_completeness.log`, `gate09_evidence_completeness.exit`
- `gate10_commit_not_pending.log`, `gate10_commit_not_pending.exit`
- `gate11_next_taskfile_exists.log`, `gate11_next_taskfile_exists.exit`
- `gate_summary.txt`
- `final_verdict.md`

## 工作分解（编号工作项）
1. preflight：输入存在性、Step10 前置产物 6/6、合同追溯、代码覆盖基线。
2. 命令统一范围与冻结：输出 Artifacts 01。
3. CommandRegistry 与分发设计：输出 Artifacts 02/03。
4. 帮助与文档生成：输出 Artifacts 04。
5. 集成测试验证：输出 Artifacts 05。
6. 自审与准入结论：输出 Artifacts 06。

## 编译与复查（每子步骤后都执行）
### 编译命令
- Alpha: `cd Mapbot-Alpha-V1 && ./gradlew compileJava`
- Reforged: `cd MapBot_Reforged && ./gradlew compileJava`

### 复查命令（全代码范围）
- `rg -n "TODO|FIXME|TEMP|HACK" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`

### E1 专项复查（必须落证据）
- 命令注册与分发：`rg -n "CommandRegistry|dispatch|register|alias" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`
- 权限与反馈：`rg -n "permission|forbidden|AUTH-403|requires|role|admin|owner" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`
- 帮助与文档：`rg -n "help|菜单|command list|doc" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`

## CURRENT_STEP 更新要求
1. 执行前：`Status: RUNNING`
2. 阻断：`Status: BLOCKED` + `BlockReason`
3. 完成后推进下一步：`Status: READY`
4. 每次更新刷新 `EffectiveDate`（ISO 8601）

## 门禁放行标准
1. Step10 前置产物 6/6 存在且可读。
2. Step11 六份 Artifacts 命名与 `RE_STEP_11` 完全一致。
3. 编译双端通过。
4. 全代码复查无阻断项。
5. gate01..gate11 全 PASS。
6. Memory_KB 已回写。

## 提交规则
满足门禁后必须：
1. 提交 Git：commit message 包含 `Step-11 E1`。
2. 推送 GitHub。
3. 在 `Project_Docs/Memory_KB/02_Status/CURRENT_STATE.md` 记录提交号与结论。
4. 提交前与提交后都必须执行：
```bash
python3 Project_Docs/Control/scripts/validate_delivery.py \
  --task Project_Docs/Control/TASKS/TASK_STEP_11_E1.md \
  --evidence-dir Project_Docs/Re_Step/Evidence/Step11/{RUN_ID} \
  --current-state Project_Docs/Memory_KB/02_Status/CURRENT_STATE.md \
  --current-step Project_Docs/Control/CURRENT_STEP.md \
  --step-label Step-11
```
