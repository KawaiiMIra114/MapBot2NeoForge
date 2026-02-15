# TASK_STEP_08_D1

## 任务标题
D1 Bridge 通道核心重构（中控模式）

## 规范优先级
当任务包与 `Project_Docs/Re_Step/RE_STEP_08_D1_Bridge通道核心重构.md` 存在命名、字段、门禁冲突时，
以 `RE_STEP_08` 文档为准，任务包必须先修订后执行。

## 目标
1. 完成 `protocol_version` 协商与兼容拒绝语义。
2. 为变更型请求落地 `idempotencyKey/requestId` 去重与冲突语义。
3. 实现断连快失败与 pending 回收，避免超时雪崩。
4. 统一错误回包到结构化错误码双栈语义。
5. 统一帧大小门禁（64KiB 单帧 / 46KiB base64 原始载荷）。

## 强制输入
- `Project_Docs/Re_Step/RE_STEP_08_D1_Bridge通道核心重构.md`
- `Project_Docs/SYSTEM_CRITIQUE_AND_REFACTOR_PAPER_V1.md`
- `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`
- `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`
- `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`
- `Project_Docs/Architecture/FAILURE_MODEL.md`
- `Project_Docs/Architecture/VERSIONING_AND_COMPATIBILITY.md`
- `Project_Docs/Re_Step/RE_STEP_03_B1_Bridge消息与错误契约映射.md`
- `Project_Docs/Re_Step/RE_STEP_05_B3_一致性与SLO契约映射.md`
- `Project_Docs/Re_Step/RE_STEP_07_C2_安全边界与版本兼容评审.md`
- `Project_Docs/Re_Step/Artifacts/Step07/*`（前置产物 6 份）
- `Project_Docs/Re_Step/Artifacts/Step07/01_Security_Boundary_Review.md`
- `Project_Docs/Re_Step/Artifacts/Step07/02_Token_Rotation_and_Rollback_Blueprint.md`
- `Project_Docs/Re_Step/Artifacts/Step07/03_Protocol_Version_Governance.md`
- `Project_Docs/Re_Step/Artifacts/Step07/04_Deprecation_and_GrayRelease_Gates.md`
- `Project_Docs/Re_Step/Artifacts/Step07/05_C2_Risk_Register.md`
- `Project_Docs/Re_Step/Artifacts/Step07/06_Solo_Review_Log_C2.md`
- `Project_Docs/Memory_KB/*`

## 强制输出文档
目录：`Project_Docs/Re_Step/Artifacts/Step08/`
1. `01_D1_Change_Scope_and_Gates.md`
2. `02_ProtocolVersion_and_Capability_Design.md`
3. `03_Idempotency_Dedup_Design.md`
4. `04_Disconnect_FastFail_and_Pending_Reclaim.md`
5. `05_D1_Contract_Test_and_Chaos_Result.md`
6. `06_Solo_Review_Log_D1.md`

## 强制证据输出
目录：`Project_Docs/Re_Step/Evidence/Step08/{RUN_ID}/`
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
- `gate06_contract_keywords.log`, `gate06_contract_keywords.exit`
- `gate07_current_step_updated.log`, `gate07_current_step_updated.exit`
- `gate08_required_inputs_exist.log`, `gate08_required_inputs_exist.exit`
- `gate_summary.txt`
- `final_verdict.md`

## 工作分解（编号工作项）
1. preflight：输入存在性、Step07 前置产物 6/6、合同追溯、代码覆盖基线。
2. 范围与门禁冻结：输出 Artifacts 01，明确禁止项与回滚触发。
3. 协议协商与幂等设计：输出 Artifacts 02/03，映射到现有代码改造点。
4. 断连回收与错误/帧门禁设计：输出 Artifacts 04，补充错误与大小门禁一致性。
5. 契约测试与混沌验证结果：输出 Artifacts 05，覆盖超时/断连/重放/超限。
6. 自审与准入结论：输出 Artifacts 06，并完成 gate 汇总与结论。

## 编译与复查（每子步骤后都执行）
### 编译命令
- Alpha: `cd Mapbot-Alpha-V1 && ./gradlew compileJava`
- Reforged: `cd MapBot_Reforged && ./gradlew compileJava`

### 复查命令（全代码范围）
- `rg -n "TODO|FIXME|TEMP|HACK" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`

### D1 专项复查（必须落证据）
- 协议版本：`rg -n "protocol_version|version|capability" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`
- 幂等去重：`rg -n "idempotency|requestId|dedup|duplicate|replay" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`
- 断连回收：`rg -n "disconnect|pending|timeout|completeExceptionally|FAILED_DISCONNECT" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`
- 错误与门禁：`rg -n "errorCode|BRG_|64\\*1024|46\\*1024|isFrameTooLarge|payload too large" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`

## CURRENT_STEP 更新要求
1. 执行前：`Status: RUNNING`
2. 阻断：`Status: BLOCKED` + `BlockReason`
3. 完成后推进下一步：`Status: READY`
4. 每次更新刷新 `EffectiveDate`（ISO 8601）

## 门禁放行标准
1. Step07 前置产物 6/6 存在且可读。
2. Step08 六份 Artifacts 命名与 `RE_STEP_08` 完全一致。
3. 编译双端通过。
4. 全代码复查与 D1 专项复查无阻断项。
5. gate01..gate08 全 PASS。
6. Memory_KB 已回写并记录提交号与证据路径。

## 提交规则
满足门禁后必须：
1. 提交 Git：commit message 包含 `Step-08 D1`。
2. 推送 GitHub。
3. 在 `Project_Docs/Memory_KB/02_Status/CURRENT_STATE.md` 记录提交号与结论。
