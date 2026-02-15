# TASK_STEP_14_F1

## 任务标题
F1 可观测与告警落地（中控模式）

## 规范优先级
当任务包与 `Project_Docs/Re_Step/RE_STEP_14_F1_可观测与告警落地.md` 存在命名、字段、门禁冲突时，
以 `RE_STEP_14` 文档为准，任务包必须先修订后执行。

## 目标
1. 按 SLO 合同落地指标、告警、证据采集三件套。
2. 故障出现后 10 分钟内可定位到模块级根因范围。
3. Critical 事件可复盘可审计。

## 强制输入
- `Project_Docs/Re_Step/RE_STEP_14_F1_可观测与告警落地.md`
- `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
- `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`
- `Project_Docs/Architecture/FAILURE_MODEL.md`
- `Project_Docs/Architecture/THREADING_MODEL.md`
- `Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md`
- `Project_Docs/Manuals/OPERATIONS_RUNBOOK.md`
- `Project_Docs/Re_Step/Artifacts/Step13/*`

## 强制输出文档
目录：`Project_Docs/Re_Step/Artifacts/Step14/`
1. `01_SLI_SLO_Dashboard_Spec.md`
2. `02_Alert_Rules_Warning_Critical.md`
3. `03_Minimum_Evidence_Bundle_Template.md`
4. `04_Incident_Severity_Mapping.md`
5. `05_Solo_Review_Log.md`

## 强制证据输出
目录：`Project_Docs/Re_Step/Evidence/Step14/{RUN_ID}/`
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
- `gate06_obs_keywords.log`, `gate06_obs_keywords.exit`
- `gate07_current_step_updated.log`, `gate07_current_step_updated.exit`
- `gate08_required_inputs_exist.log`, `gate08_required_inputs_exist.exit`
- `gate09_evidence_completeness.log`, `gate09_evidence_completeness.exit`
- `gate10_commit_not_pending.log`, `gate10_commit_not_pending.exit`
- `gate11_next_taskfile_exists.log`, `gate11_next_taskfile_exists.exit`
- `gate_summary.txt`
- `final_verdict.md`

## 工作分解
1. preflight：输入存在性、Step13 前置 5/5、合同追溯、代码覆盖基线。
2. 固化指标字典与采集边界：输出 Artifact 01。
3. 落地 SLO 与窗口计算 + 配置 Warning/Critical 告警：输出 Artifact 02。
4. 建立最小证据集自动采集：输出 Artifact 03。
5. 故障注入与阈值校准 + 事件严重度映射：输出 Artifact 04。
6. 自审与准入判定：输出 Artifact 05。

## 编译与复查
- Alpha: `cd Mapbot-Alpha-V1 && ./gradlew compileJava`
- Reforged: `cd MapBot_Reforged && ./gradlew compileJava`
- 复查: `rg -n "TODO|FIXME|TEMP|HACK" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`

## 提交规则
满足门禁后必须：
1. 提交 Git：commit message 包含 `Step-14 F1`。
2. 推送 GitHub。
3. 在 `CURRENT_STATE.md` 记录提交号与结论。
