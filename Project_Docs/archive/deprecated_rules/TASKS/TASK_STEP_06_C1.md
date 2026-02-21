# TASK_STEP_06_C1

## 任务标题
C1 线程模型与故障模型评审（中控模式）

## 目标
1. 审计线程归属边界（主线程/IO/Worker/Scheduler），确认资源访问安全。
2. 扫描禁止模式（阻塞调用/匿名线程/越界写入）。
3. 审计故障状态机一致性（timeout/disconnect/half-success 分类）。
4. 评审 pending 生命周期与补偿闭环。
5. 制定混沌与压测评审计划，输出可执行的 C2 准入结论。

## 强制输入
- `Project_Docs/Re_Step/RE_STEP_06_C1_线程模型与故障模型评审.md`
- `Project_Docs/Architecture/THREADING_MODEL.md`
- `Project_Docs/Architecture/FAILURE_MODEL.md`
- `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`
- `Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`
- `Project_Docs/Re_Step/Artifacts/Step05/*` (前置产物 6 份)
- `Project_Docs/Memory_KB/*`

## 强制输出文档
目录：`Project_Docs/Re_Step/Artifacts/Step06/`
1. `01_Thread_Owner_Matrix_Review.md`
2. `02_ForbiddenPattern_Scan_Report.md`
3. `03_Failure_StateMachine_Review.md`
4. `04_Pending_Lifecycle_and_Compensation.md`
5. `05_Chaos_and_Stress_Review_Plan.md`
6. `06_Solo_Review_Log_C1.md`

## 强制证据输出
目录：`Project_Docs/Re_Step/Evidence/Step06/{RUN_ID}/`
- `preflight_read_manifest.txt`
- `preflight_code_coverage.txt`
- `build_alpha.log`, `build_alpha.exit`
- `build_reforged.log`, `build_reforged.exit`
- `review_scope.log`, `review_scope.exit`
- `gate_summary.txt`
- `final_verdict.md`

## 工作分解（编号工作项）
1. 输入与前置 preflight（输入存在性、前置产物、代码覆盖清单）。
2. 线程归属与禁止模式评审（Artifacts 01, 02）。
3. 故障状态机与 pending 评审（Artifacts 03, 04）。
4. 混沌压测计划与自审收敛（Artifacts 05, 06）。
5. 门禁执行与结果汇总（gate + final_verdict）。

## 编译与复查（每子步骤后都执行）
### 编译命令
- Alpha: `cd Mapbot-Alpha-V1 && ./gradlew compileJava`
- Reforged: `cd MapBot_Reforged && ./gradlew compileJava`

### 复查命令（全代码范围）
- `rg -n "TODO|FIXME|TEMP|HACK" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`
- 线程专项: `rg -n "new Thread|\.join\(|\.get\(\)|Thread\.sleep" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`

## CURRENT_STEP 更新要求
1. 执行前：`Status: RUNNING`
2. 阻断：`Status: BLOCKED` + `BlockReason`
3. 完成后推进下一步：`Status: READY`
4. 每次更新刷新 `EffectiveDate`（ISO 8601）

## 门禁放行标准
1. 6 份 Artifacts 齐全。
2. 编译双端通过。
3. 复查无阻断项。
4. Gate 全 PASS。
5. Memory_KB 已回写。

## 提交规则
满足门禁后必须：
1. 提交 Git：commit message 包含 `Step-06 C1`。
2. 推送 GitHub。
3. 在 `Project_Docs/Memory_KB/02_Status/CURRENT_STATE.md` 记录提交号与结论。
