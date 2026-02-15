# TASK_STEP_09_D2

## 任务标题
D2 线程与执行模型重构（中控模式）

## 规范优先级
当任务包与 `Project_Docs/Re_Step/RE_STEP_09_D2_线程与执行模型重构.md` 存在命名、字段、门禁冲突时，
以 `RE_STEP_09` 文档为准，任务包必须先修订后执行。

## 目标
1. 将网络线程写路径收敛为“IO 解析 -> DTO/队列 -> 主线程提交”。
2. 清除主线程阻塞调用（`get/join/sleep`）与高风险等待。
3. 建立快照读与并发容器规范，避免跨线程实体引用。
4. 统一调度器生命周期（可命名、可停机、可回收）。
5. 输出进入 D3 的准入结论与回滚边界。

## 强制输入
- `Project_Docs/Re_Step/RE_STEP_09_D2_线程与执行模型重构.md`
- `Project_Docs/SYSTEM_CRITIQUE_AND_REFACTOR_PAPER_V1.md`
- `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`
- `Project_Docs/Architecture/THREADING_MODEL.md`
- `Project_Docs/Architecture/MODULE_BOUNDARY.md`
- `Project_Docs/Architecture/FAILURE_MODEL.md`
- `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`
- `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
- `Project_Docs/Re_Step/RE_STEP_06_C1_线程模型与故障模型评审.md`
- `Project_Docs/Re_Step/RE_STEP_08_D1_Bridge通道核心重构.md`
- `Project_Docs/Re_Step/Artifacts/Step08/01_D1_Change_Scope_and_Gates.md`
- `Project_Docs/Re_Step/Artifacts/Step08/02_ProtocolVersion_and_Capability_Design.md`
- `Project_Docs/Re_Step/Artifacts/Step08/03_Idempotency_Dedup_Design.md`
- `Project_Docs/Re_Step/Artifacts/Step08/04_Disconnect_FastFail_and_Pending_Reclaim.md`
- `Project_Docs/Re_Step/Artifacts/Step08/05_D1_Contract_Test_and_Chaos_Result.md`
- `Project_Docs/Re_Step/Artifacts/Step08/06_Solo_Review_Log_D1.md`
- `Project_Docs/Memory_KB/*`

## 强制输出文档
目录：`Project_Docs/Re_Step/Artifacts/Step09/`
1. `01_D2_Threading_Refactor_Scope.md`
2. `02_IO_to_MainThread_Route_Plan.md`
3. `03_Blocking_Call_Removal_List.md`
4. `04_Snapshot_Read_and_Scheduler_Shutdown.md`
5. `05_D2_Stress_and_Boundary_Test_Report.md`
6. `06_Solo_Review_Log_D2.md`

## 强制证据输出
目录：`Project_Docs/Re_Step/Evidence/Step09/{RUN_ID}/`
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
- `gate06_threading_keywords.log`, `gate06_threading_keywords.exit`
- `gate07_current_step_updated.log`, `gate07_current_step_updated.exit`
- `gate08_required_inputs_exist.log`, `gate08_required_inputs_exist.exit`
- `gate_summary.txt`
- `final_verdict.md`

## 工作分解（编号工作项）
1. preflight：输入存在性、Step08 前置产物 6/6、合同追溯、代码覆盖基线。
2. 线程改造范围冻结：输出 Artifacts 01（范围、冻结项、回滚边界）。
3. IO->主线程路由与阻塞点治理：输出 Artifacts 02/03。
4. 快照读与调度器生命周期：输出 Artifacts 04。
5. 边界与压力验证：输出 Artifacts 05（越界/死锁/预算）。
6. 自审与准入结论：输出 Artifacts 06，并完成 gate 汇总。

## 编译与复查（每子步骤后都执行）
### 编译命令
- Alpha: `cd Mapbot-Alpha-V1 && ./gradlew compileJava`
- Reforged: `cd MapBot_Reforged && ./gradlew compileJava`

### 复查命令（全代码范围）
- `rg -n "TODO|FIXME|TEMP|HACK" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`

### D2 专项复查（必须落证据）
- 主线程阻塞：`rg -n "Thread\\.sleep|\\.join\\(|\\.get\\(" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`
- 越界线程：`rg -n "new Thread\\(|CompletableFuture\\.supplyAsync|ExecutorService" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`
- 主线程回切：`rg -n "server\\.execute|MinecraftServer|main thread|thread owner" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`
- 并发快照：`rg -n "snapshot|Concurrent|CopyOnWrite|volatile|Atomic" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`

## CURRENT_STEP 更新要求
1. 执行前：`Status: RUNNING`
2. 阻断：`Status: BLOCKED` + `BlockReason`
3. 完成后推进下一步：`Status: READY`
4. 每次更新刷新 `EffectiveDate`（ISO 8601）

## 门禁放行标准
1. Step08 前置产物 6/6 存在且可读。
2. Step09 六份 Artifacts 命名与 `RE_STEP_09` 完全一致。
3. 编译双端通过。
4. 全代码复查与 D2 专项复查无阻断项。
5. gate01..gate08 全 PASS。
6. Memory_KB 已回写并记录提交号与证据路径。

## 提交规则
满足门禁后必须：
1. 提交 Git：commit message 包含 `Step-09 D2`。
2. 推送 GitHub。
3. 在 `Project_Docs/Memory_KB/02_Status/CURRENT_STATE.md` 记录提交号与结论。
