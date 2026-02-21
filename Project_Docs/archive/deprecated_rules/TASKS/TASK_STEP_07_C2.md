# TASK_STEP_07_C2

## 任务标题
C2 安全边界与版本兼容评审（中控模式）

## 规范优先级
当任务包与 `Project_Docs/Re_Step/RE_STEP_07_C2_安全边界与版本兼容评审.md` 存在命名或字段冲突时，
以 `RE_STEP_07` 文档为准。

## 目标
1. 固化安全边界（鉴权、秘钥、接口暴露、敏感字段）。
2. 固化版本兼容策略（兼容窗口、弃用策略、回滚边界）。
3. 输出可执行的 C2 评审结论，决定是否 GO D1。

## 强制输入
- `Project_Docs/Re_Step/RE_STEP_07_C2_安全边界与版本兼容评审.md`
- `Project_Docs/Contracts/*.md`
- `Project_Docs/Architecture/SECURITY_BOUNDARY.md`
- `Project_Docs/Architecture/VERSIONING_AND_COMPATIBILITY.md`
- `Project_Docs/Memory_KB/*`（按 README 顺序）

## 强制输出文档
目录：`Project_Docs/Re_Step/Artifacts/Step07/`
1. `01_Security_Boundary_Review.md`
2. `02_Token_Rotation_and_Rollback_Blueprint.md`
3. `03_Protocol_Version_Governance.md`
4. `04_Deprecation_and_GrayRelease_Gates.md`
5. `05_C2_Risk_Register.md`
6. `06_Solo_Review_Log_C2.md`

## 强制证据输出
目录：`Project_Docs/Re_Step/Evidence/Step07/{RUN_ID}/`
- `preflight_read_manifest.txt`
- `preflight_contract_trace.txt`
- `preflight_code_coverage.txt`
- `build_alpha.log`, `build_alpha.exit`
- `build_reforged.log`, `build_reforged.exit`
- `review_scope.log`, `review_scope.exit`
- `gate01_prev_step.log`, `gate01_prev_step.exit`
- `gate02_artifacts.log`, `gate02_artifacts.exit`
- `gate03_build_alpha.log`, `gate03_build_alpha.exit`
- `gate04_build_reforged.log`, `gate04_build_reforged.exit`
- `gate05_review_scope.log`, `gate05_review_scope.exit`
- `gate06_memory_kb_sync.log`, `gate06_memory_kb_sync.exit`
- `gate09_evidence_completeness.log`, `gate09_evidence_completeness.exit`
- `gate10_commit_not_pending.log`, `gate10_commit_not_pending.exit`
- `gate11_next_taskfile_exists.log`, `gate11_next_taskfile_exists.exit`
- `gate_summary.txt`
- `final_verdict.md`

## 工作分解（编号工作项）
1. 输入与合同 preflight（输入存在性、合同追溯、代码覆盖清单）。
2. 安全边界与密钥治理产物输出（Artifacts 01, 02）。
3. 协议版本与灰度弃用产物输出（Artifacts 03, 04）。
4. 缺口与自审收敛（Artifacts 05, 06）。
5. 门禁执行与结果汇总（gate01..gate06 + final_verdict）。

## 编译与复查（每子步骤后都执行）
### 编译命令
- Alpha: `cd Mapbot-Alpha-V1 && ./gradlew compileJava`
- Reforged: `cd MapBot_Reforged && ./gradlew compileJava`

### 复查命令（全代码范围）
- `rg -n "TODO|FIXME|TEMP|HACK" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`
- 与当前改动相关关键字专项检查（由执行者在证据中给出命令与结果）

## CURRENT_STEP 更新要求
1. 执行前将 `CURRENT_STEP.md` 更新为 `Status: RUNNING`。
2. 出现阻断项时更新为 `Status: BLOCKED` 并填写 `BlockReason`。
3. 本步完成并提交后，推进到下一步并设置 `Status: READY`。
4. 每次更新都必须刷新 `EffectiveDate`（ISO 8601 完整时间戳）。

## 门禁放行标准
1. 6 份 Artifacts 齐全。
2. 编译双端通过。
3. 复查无阻断项。
4. gate01..gate11 全 PASS。
5. Memory_KB 已回写。

## 提交规则
满足门禁后必须：
1. 提交 Git：commit message 包含 `Step-07 C2`。
2. 推送 GitHub。
3. 在 `Project_Docs/Memory_KB/02_Status/CURRENT_STATE.md` 记录提交号与结论。
