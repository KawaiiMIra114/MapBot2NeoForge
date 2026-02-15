# TASK_STEP_XX_TEMPLATE

## 任务标题

## 目标

## 强制输入

## 强制输出文档

## 强制证据输出
- 必须逐项枚举文件名，禁止写 `gate01~gate11` 这类简写。

## 工作分解（编号工作项）
1.
2.
3.

## 编译与复查（每子步骤后）

## CURRENT_STEP 更新要求
1. 执行前：`Status: RUNNING`
2. 阻断：`Status: BLOCKED` + `BlockReason`
3. 完成后推进下一步：`Status: READY`
4. 每次更新刷新 `EffectiveDate`（ISO 8601）

## 门禁放行标准

## 提交规则

## 强制机器验收
```bash
python3 Project_Docs/Control/scripts/validate_delivery.py \
  --task "Project_Docs/Control/TASKS/<TASK_FILE>.md" \
  --evidence-dir "Project_Docs/Re_Step/Evidence/Step<NN>/<RUN_ID>" \
  --current-state "Project_Docs/Memory_KB/02_Status/CURRENT_STATE.md" \
  --current-step "Project_Docs/Control/CURRENT_STEP.md" \
  --step-label "Step-<NN>"
```
