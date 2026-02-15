# TASK_STEP_15_F2

## 任务标题
F2 运维联调与最终集成验证

## 规范优先级
当任务包与 `Project_Docs/Re_Step/RE_STEP_15_F2_*.md` 存在命名、字段、门禁冲突时，
以 `RE_STEP_15` 文档为准，任务包必须先修订后执行。

## 目标
1. 运维联调与端到端验证
2. 最终集成验收

## 强制输入
- `Project_Docs/Re_Step/RE_STEP_15_F2_*.md`
- `Project_Docs/Re_Step/Artifacts/Step14/*`

## 强制输出文档
目录：`Project_Docs/Re_Step/Artifacts/Step15/`
(按 RE_STEP_15 定义)

## 编译与复查
- Alpha: `cd Mapbot-Alpha-V1 && ./gradlew compileJava`
- Reforged: `cd MapBot_Reforged && ./gradlew compileJava`
