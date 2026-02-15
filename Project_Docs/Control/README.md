# Control Center 使用手册（C2+）

## 1. 目的
本目录用于统一驱动“中控 + 任务包”执行模式，确保每一步都可追溯、可审计、可复现。

## 2. 目录说明
- `MASTER_PROMPT.md`：全局硬规则（所有步骤必须遵守）。
- `CURRENT_STEP.md`：当前生效步骤指针（执行器只读这个文件决定当前任务）。
- `CURRENT_STEP_TEMPLATE.md`：`CURRENT_STEP.md` 标准字段模板。
- `TASKS/`：每个步骤对应一个任务包。
- `TEMPLATES/`：任务包、报告、门禁清单模板。

## 3. 执行入口（固定顺序）
1. 读取 `MASTER_PROMPT.md`。
2. 读取 `TASKS/README.md`（任务包规范）。
3. 读取 `CURRENT_STEP.md`，取出 `TaskFile`。
4. 读取 `TASKS/<TaskFile>`。
5. 按任务包执行：preflight -> 实施 -> 每子步骤编译+复查 -> 证据落盘 -> 门禁 -> Git 提交。

## 4. 必须遵守的硬规则
1. 每个子步骤后必须双端编译与全代码复查。
2. 编译/复查失败必须自动修复并重跑，最多 3 轮。
3. 只有“编译通过 + 复查通过 + 门禁通过”才允许提交。
4. 每步结束必须回写 `Project_Docs/Memory_KB/`。
5. 禁止跳步、禁止口头放行。
6. 如任务包与 `Project_Docs/Re_Step/RE_STEP_*.md` 冲突，以 `RE_STEP` 为准并立即回补任务包。

## 5. 如何切换到下一步
1. 新建下一个任务包：`Project_Docs/Control/TASKS/TASK_STEP_xx_xx.md`。
2. 更新 `Project_Docs/Control/CURRENT_STEP.md` 的 `StepID/Stage/TaskFile/Status/EffectiveDate`。
3. 执行器下一次读取时会自动切换到新步骤。

## 5.1 CURRENT_STEP.md 更新协议（强制）
每次步骤状态变化都必须更新 `Project_Docs/Control/CURRENT_STEP.md`，不得省略：
1. 任务开始执行前：`Status: RUNNING`。
2. 任务执行失败需返工：`Status: BLOCKED`，并新增 `BlockReason: <明确阻断原因>`。
3. 任务通过门禁并完成提交后：
- 先将当前步标记完成（`Status: DONE`，可在证据中记录）。
- 再切换到下一步：更新 `StepID/Stage/TaskFile`，并设置 `Status: READY`。
4. 每次更新都必须刷新 `EffectiveDate`（建议使用完整时间戳）。
5. 若 `CURRENT_STEP.md` 未更新，视为流程违规，禁止进入下一步。

## 6. 给执行模型的一次性提示词（可直接复用）
请严格按以下顺序执行，不得跳步：
1. 读取 `Project_Docs/Control/MASTER_PROMPT.md`。
2. 读取 `Project_Docs/Control/TASKS/README.md`。
3. 读取 `Project_Docs/Control/CURRENT_STEP.md`。
4. 读取 `Project_Docs/Control/TASKS/` 中 `CURRENT_STEP.md` 指定的任务包。
5. 执行前将 `CURRENT_STEP.md` 置为 `RUNNING`。
6. 完成任务包中的全部强制输入检查、强制输出、证据落盘、门禁校验。
7. 每个子步骤后必须执行编译与全代码复查；失败自动修复并重试（最多 3 轮）。
8. 若阻断，更新 `CURRENT_STEP.md` 为 `BLOCKED` 并填写 `BlockReason`。
9. 只有全部门禁通过才可提交并推送；提交后回写 `Project_Docs/Memory_KB/`，并推进 `CURRENT_STEP.md` 到下一步 `READY`。
10. 最终仅输出：Verdict、Blocking Issues、Fix Actions、Next Step Decision、Files Changed、Evidence Path、Build & Review Result、Git Commit/Push Result。

## 7. 当前限制
当前仅落地了 `TASK_STEP_07_C2.md`。后续步骤需按模板补齐任务包后再执行。
