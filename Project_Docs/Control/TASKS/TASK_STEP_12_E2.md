# TASK_STEP_12_E2

## 任务标题
E2 关键业务链路重构（中控模式）

## 规范优先级
当任务包与 `Project_Docs/Re_Step/RE_STEP_12_E2_关键业务链路重构.md` 存在命名、字段、门禁冲突时，
以 `RE_STEP_12` 文档为准，任务包必须先修订后执行。

## 目标
1. 重构 Bot event 处理链路。
2. 重构 Bridge handler 分发逻辑。
3. 统一 event/handler 的错误处理和回退。
4. 输出进入 E3 的准入结论与回滚边界。

## 强制输入
- `Project_Docs/Re_Step/RE_STEP_12_E2_关键业务链路重构.md`
- `Project_Docs/SYSTEM_CRITIQUE_AND_REFACTOR_PAPER_V1.md`
- `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`
- `Project_Docs/Re_Step/Artifacts/Step11/*`
- `Project_Docs/Memory_KB/*`

## 强制输出文档
目录：`Project_Docs/Re_Step/Artifacts/Step12/`
1. `01_E2_Event_Handler_Scope.md`
2. `02_BridgeHandler_Dispatch_Design.md`
3. `03_Error_Handling_and_Rollback_Standard.md`
4. `04_Event_Flow_Consistency_Design.md`
5. `05_E2_Integration_Test_Report.md`
6. `06_Solo_Review_Log_E2.md`

## 强制证据输出
目录：`Project_Docs/Re_Step/Evidence/Step12/{RUN_ID}/`
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
- `gate06_event_keywords.log`, `gate06_event_keywords.exit`
- `gate07_current_step_updated.log`, `gate07_current_step_updated.exit`
- `gate08_required_inputs_exist.log`, `gate08_required_inputs_exist.exit`
- `gate09_evidence_completeness.log`, `gate09_evidence_completeness.exit`
- `gate10_commit_not_pending.log`, `gate10_commit_not_pending.exit`
- `gate11_next_taskfile_exists.log`, `gate11_next_taskfile_exists.exit`
- `gate_summary.txt`
- `final_verdict.md`

## 工作分解（编号工作项）
1. preflight：输入存在性、Step11 前置产物 6/6、合同追溯、代码覆盖基线。
2. Event/Handler 范围盘点：输出 Artifacts 01。
3. BridgeHandler 分发设计：输出 Artifacts 02/03。
4. Event流一致性设计：输出 Artifacts 04。
5. 集成测试验证：输出 Artifacts 05。
6. 自审与准入结论：输出 Artifacts 06。

## 编译与复查（每子步骤后都执行）
### 编译命令
- Alpha: `cd Mapbot-Alpha-V1 && ./gradlew compileJava`
- Reforged: `cd MapBot_Reforged && ./gradlew compileJava`

### 复查命令（全代码范围）
- `rg -n "TODO|FIXME|TEMP|HACK" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`

## CURRENT_STEP 更新要求
1. 执行前：`Status: RUNNING`
2. 阻断：`Status: BLOCKED` + `BlockReason`
3. 完成后推进下一步：`Status: READY`
4. 每次更新刷新 `EffectiveDate`（ISO 8601）

## 门禁放行标准
1. Step11 前置产物 6/6 存在且可读。
2. Step12 六份 Artifacts 命名与 `RE_STEP_12` 完全一致。
3. 编译双端通过。
4. 全代码复查无阻断项。
5. gate01..gate11 全 PASS。
6. Memory_KB 已回写。

## 提交规则
满足门禁后必须：
1. 提交 Git：commit message 包含 `Step-12 E2`。
2. 推送 GitHub。
3. 在 `Project_Docs/Memory_KB/02_Status/CURRENT_STATE.md` 记录提交号与结论。
4. 提交前与提交后都必须执行：
```bash
python3 Project_Docs/Control/scripts/validate_delivery.py \
  --task Project_Docs/Control/TASKS/TASK_STEP_12_E2.md \
  --evidence-dir Project_Docs/Re_Step/Evidence/Step12/{RUN_ID} \
  --current-state Project_Docs/Memory_KB/02_Status/CURRENT_STATE.md \
  --current-step Project_Docs/Control/CURRENT_STEP.md \
  --step-label Step-12
```
