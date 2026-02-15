# TASK_STEP_10_D3

## 任务标题
D3 数据一致性与恢复重构（中控模式）

## 规范优先级
当任务包与 `Project_Docs/Re_Step/RE_STEP_10_D3_数据一致性与恢复重构.md` 存在命名、字段、门禁冲突时，
以 `RE_STEP_10` 文档为准，任务包必须先修订后执行。

## 目标
1. 建立统一 CAS 写入与冲突语义（`CONSISTENCY-409`）。
2. 建立 `snapshot + event_log` 可回放恢复链路。
3. 统一原子持久化策略（tmp -> fsync -> atomic replace -> backup）。
4. 建立半成功补偿闭环与终态收敛规则。
5. 输出进入 E1 的准入结论与回滚边界。

## 强制输入
- `Project_Docs/Re_Step/RE_STEP_10_D3_数据一致性与恢复重构.md`
- `Project_Docs/SYSTEM_CRITIQUE_AND_REFACTOR_PAPER_V1.md`
- `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`
- `Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`
- `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
- `Project_Docs/Architecture/FAILURE_MODEL.md`
- `Project_Docs/Architecture/DATA_FLOW_AND_STATE.md`
- `Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md`
- `Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md`
- `Project_Docs/Re_Step/RE_STEP_05_B3_一致性与SLO契约映射.md`
- `Project_Docs/Re_Step/RE_STEP_09_D2_线程与执行模型重构.md`
- `Project_Docs/Re_Step/Artifacts/Step09/01_D2_Threading_Refactor_Scope.md`
- `Project_Docs/Re_Step/Artifacts/Step09/02_IO_to_MainThread_Route_Plan.md`
- `Project_Docs/Re_Step/Artifacts/Step09/03_Blocking_Call_Removal_List.md`
- `Project_Docs/Re_Step/Artifacts/Step09/04_Snapshot_Read_and_Scheduler_Shutdown.md`
- `Project_Docs/Re_Step/Artifacts/Step09/05_D2_Stress_and_Boundary_Test_Report.md`
- `Project_Docs/Re_Step/Artifacts/Step09/06_Solo_Review_Log_D2.md`
- `Project_Docs/Memory_KB/*`

## 强制输出文档
目录：`Project_Docs/Re_Step/Artifacts/Step10/`
1. `01_D3_CAS_WritePath_Design.md`
2. `02_Snapshot_EventLog_Recovery_Design.md`
3. `03_Atomic_Persistence_Standard.md`
4. `04_Compensation_and_Replay_Closure.md`
5. `05_D3_FaultInjection_Test_Report.md`
6. `06_Solo_Review_Log_D3.md`

## 强制证据输出
目录：`Project_Docs/Re_Step/Evidence/Step10/{RUN_ID}/`
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
- `gate06_consistency_keywords.log`, `gate06_consistency_keywords.exit`
- `gate07_current_step_updated.log`, `gate07_current_step_updated.exit`
- `gate08_required_inputs_exist.log`, `gate08_required_inputs_exist.exit`
- `gate09_evidence_completeness.log`, `gate09_evidence_completeness.exit`
- `gate10_commit_not_pending.log`, `gate10_commit_not_pending.exit`
- `gate11_next_taskfile_exists.log`, `gate11_next_taskfile_exists.exit`
- `gate_summary.txt`
- `final_verdict.md`

## 工作分解（编号工作项）
1. preflight：输入存在性、Step09 前置产物 6/6、合同追溯、代码覆盖基线。
2. CAS 写入与冲突语义设计：输出 Artifacts 01（含 `CONSISTENCY-409` 路径）。
3. 快照回放与原子持久化设计：输出 Artifacts 02/03。
4. 补偿与重放闭环：输出 Artifacts 04（终态收敛规则）。
5. 故障注入与恢复验证：输出 Artifacts 05（冲突/损坏/乱序/重启风暴）。
6. 自审与准入结论：输出 Artifacts 06，并完成 gate 汇总。

## 编译与复查（每子步骤后都执行）
### 编译命令
- Alpha: `cd Mapbot-Alpha-V1 && ./gradlew compileJava`
- Reforged: `cd MapBot_Reforged && ./gradlew compileJava`

### 复查命令（全代码范围）
- `rg -n "TODO|FIXME|TEMP|HACK" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`

### D3 专项复查（必须落证据）
- CAS/版本：`rg -n "CAS|compareAndSet|version|CONSISTENCY-409|conflict" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`
- 恢复/回放：`rg -n "snapshot|event_log|replay|recover|restore" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`
- 原子持久化：`rg -n "atomic|fsync|tmp|backup|replace|move" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`
- 补偿闭环：`rg -n "compensat|COMMITTED|COMPENSATED|PENDING" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`

## CURRENT_STEP 更新要求
1. 执行前：`Status: RUNNING`
2. 阻断：`Status: BLOCKED` + `BlockReason`
3. 完成后推进下一步：`Status: READY`
4. 每次更新刷新 `EffectiveDate`（ISO 8601）

## 门禁放行标准
1. Step09 前置产物 6/6 存在且可读。
2. Step10 六份 Artifacts 命名与 `RE_STEP_10` 完全一致。
3. 编译双端通过。
4. 全代码复查与 D3 专项复查无阻断项。
5. gate01..gate11 全 PASS。
6. Memory_KB 已回写并记录提交号与证据路径。

## 提交规则
满足门禁后必须：
1. 提交 Git：commit message 包含 `Step-10 D3`。
2. 推送 GitHub。
3. 在 `Project_Docs/Memory_KB/02_Status/CURRENT_STATE.md` 记录提交号与结论。
4. 提交前与提交后都必须执行：
```bash
python3 Project_Docs/Control/scripts/validate_delivery.py \
  --task Project_Docs/Control/TASKS/TASK_STEP_10_D3.md \
  --evidence-dir Project_Docs/Re_Step/Evidence/Step10/{RUN_ID} \
  --current-state Project_Docs/Memory_KB/02_Status/CURRENT_STATE.md \
  --current-step Project_Docs/Control/CURRENT_STEP.md \
  --step-label Step-10
```
