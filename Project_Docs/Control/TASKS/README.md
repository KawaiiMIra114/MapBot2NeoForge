# TASKS 任务包规范

## 1. 命名规则
- 文件名：`TASK_STEP_<NN>_<STAGE>.md`
- 示例：`TASK_STEP_08_D1.md`

## 1.1 规范优先级（强制）
当任务包与对应 `Project_Docs/Re_Step/RE_STEP_*.md` 的输出命名、字段定义、门禁条款发生冲突时：
1. 以 `RE_STEP` 文档为权威来源。
2. 任务包必须立即修订并对齐。
3. 未对齐前不得宣称通过门禁。

## 2. 必填章节
每个任务包必须包含以下章节，缺一不可：
1. 任务标题
2. 目标（可量化）
3. 强制输入
4. 强制输出文档
5. 强制证据输出
6. 工作分解（编号工作项）
7. 编译与复查（每子步骤后执行）
8. CURRENT_STEP 更新要求
9. 门禁放行标准
10. 提交规则

## 3. 强制输入最小集合
- 对应步骤文档：`Project_Docs/Re_Step/RE_STEP_*.md`
- 合同：`Project_Docs/Contracts/*.md`
- 架构：`Project_Docs/Architecture/*.md`
- 记忆库：`Project_Docs/Memory_KB/*`

## 4. 证据目录规范
- 路径：`Project_Docs/Re_Step/Evidence/Step<NN>/{RUN_ID}/`
- 强制要求：证据清单必须逐项列出，禁止使用 `gate01~gate11`、`a+b`、`..` 等简写。
- 至少包含：
  - `preflight_read_manifest.txt`
  - `build_*.log` + `build_*.exit`
  - `review_scope.log` + `review_scope.exit`
  - `gate_summary.txt`
  - `final_verdict.md`

## 5. 门禁与提交
1. 未通过门禁时禁止提交。
2. 编译与复查未全通过时禁止提交。
3. 提交信息必须含 `Step ID` 与 `Evidence Path`。
4. 提交后必须回写 Memory_KB 当前状态。
5. 交付完整性校验未通过时禁止提交（证据缺失、Commit pending、下一步任务包不存在）。

## 6. 创建新任务包步骤
1. 复制模板：`Project_Docs/Control/TEMPLATES/STEP_TASK_TEMPLATE.md`
2. 填写目标、输出、门禁与命令。
3. 更新 `Project_Docs/Control/CURRENT_STEP.md` 指向新任务包。
4. 在执行前进行一次路径与命令可达性自检。

## 7. CURRENT_STEP 进度更新约束（强制）
执行器与任务包作者必须共同遵守：
1. 开始执行本步前，先将 `CURRENT_STEP.md` 置为 `Status: RUNNING`。
2. 发现阻断项时，置为 `Status: BLOCKED` 并写入 `BlockReason`。
3. 本步通过门禁并完成提交后，必须推进指针到下一步并设为 `Status: READY`。
4. 每次状态变更必须更新 `EffectiveDate`。
5. 未更新 `CURRENT_STEP.md` 的执行结果，不得宣称“已完成”。

## 8. 执行前自检
开始前必须确认：
1. `CURRENT_STEP.md` 中 `TaskFile` 文件存在。
2. 任务包里的输入路径全部存在。
3. 编译命令可执行（`gradlew` 或 `gradlew.bat`）。
4. 证据目录可创建且可写。

## 9. 提交前强制校验（必须落证据）
1. 强制证据文件“逐项存在性”检查，输出 missing 列表（必须为空）。
2. `CURRENT_STATE.md` 当前 Step 的 Commit 字段不得为 `(pending)`。
3. `CURRENT_STEP.md` 若已推进到下一步，则 `TaskFile` 文件必须存在。
4. 任一失败必须输出 `NO-GO`，并回填阻断项，不得口头放行。
5. 必须执行并留存机器验收脚本结果：
```bash
python3 Project_Docs/Control/scripts/validate_delivery.py \
  --task "Project_Docs/Control/TASKS/<TASK_FILE>.md" \
  --evidence-dir "Project_Docs/Re_Step/Evidence/Step<NN>/<RUN_ID>" \
  --current-state "Project_Docs/Memory_KB/02_Status/CURRENT_STATE.md" \
  --current-step "Project_Docs/Control/CURRENT_STEP.md" \
  --step-label "Step-<NN>"
```
