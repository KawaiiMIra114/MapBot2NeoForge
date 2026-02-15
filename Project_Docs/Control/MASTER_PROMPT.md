# MASTER_PROMPT (C2+ Unified Control)

## 目标
从 C2 开始，所有任务统一采用“中控 + 任务包”执行。执行必须严谨、科学、细致，且每步必须形成可审计证据。

## 强制输入
1. `Project_Docs/Memory_KB/README.md`
2. `Project_Docs/Control/README.md`
3. `Project_Docs/Control/TASKS/README.md`
4. `Project_Docs/Control/CURRENT_STEP.md`
5. 对应任务包：`Project_Docs/Control/TASKS/<TASK_FILE>.md`

## 强制执行规则
1. 开工前先完成 preflight：阅读清单、代码覆盖清单、合同追踪清单。
2. 严禁跳步：必须按任务包顺序执行。
3. 每一子步骤（任务包中的每个编号工作项）执行后必须：
- 运行编译（按任务包定义模块）。
- 运行复查（按“工作内容 + 全部代码”范围做静态核查）。
- 写入证据目录（log + exit + summary）。
4. 若编译或复查失败：
- 自动修复并重跑（最多 3 轮）。
- 不得口头放行。
5. 每一步结束必须回写 Memory_KB（状态、证据、决策、时间线）。
6. 必须维护 `CURRENT_STEP.md`：
- 开始执行前设为 `RUNNING`。
- 发现阻断项设为 `BLOCKED` 并写 `BlockReason`。
- 门禁通过并提交后，推进到下一步并设为 `READY`。
7. 仅当“编译通过 + 复查通过 + 门禁通过”三者同时满足，才允许提交 GitHub。
8. 提交前必须执行“交付完整性校验”（必须留证据）：
- 强制证据文件清单零缺失。
- `CURRENT_STATE.md` 中当前 Step 的 Commit 不得为 `(pending)`。
- `CURRENT_STEP.md` 切到下一步前，下一步 `TaskFile` 必须存在。
 - 必须运行脚本：`python3 Project_Docs/Control/scripts/validate_delivery.py ...`，且退出码必须为 0。
9. 任一校验失败，`Verdict` 必须为 `NO-GO`，且禁止推进 `CURRENT_STEP`。

## 提交规则
1. 先 `git add` 仅相关文件。
2. `git commit` 必须包含：Step ID、核心修复点、证据目录。
3. `git push` 到当前分支（默认 main），并记录提交号到 Memory_KB。
4. 提交后必须二次核验：
- `git rev-parse --short HEAD` 与 `CURRENT_STATE.md` 一致。
- 证据目录与任务包强制证据清单一致。
- `CURRENT_STEP.md` 指向的下一步任务包存在。
 - 再次运行 `validate_delivery.py`，结果必须仍为 PASS。

## 输出格式（固定）
- Verdict: PASS/FAIL
- Blocking Issues
- Fix Actions
- Next Step Decision
- Files Changed
- Evidence Path
- Build & Review Result
- Git Commit/Push Result
