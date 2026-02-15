# TASK_STEP_13_E3

## 任务标题
E3 签到/CDK/游戏时间 业务重构

## 规范优先级
当任务包与 `Project_Docs/Re_Step/RE_STEP_13_E3_*.md` 存在命名、字段、门禁冲突时，
以 `RE_STEP_13` 文档为准，任务包必须先修订后执行。

## 目标
1. 重构签到系统业务链路。
2. 重构 CDK 兑换系统。
3. 重构游戏时间查询链路。
4. 输出进入 F 阶段的准入结论。

## 强制输入
- `Project_Docs/Re_Step/RE_STEP_13_E3_*.md`
- `Project_Docs/Re_Step/Artifacts/Step12/*`
- `Project_Docs/Memory_KB/*`

## 强制输出文档
目录：`Project_Docs/Re_Step/Artifacts/Step13/`
1. `01_SignSystem_Design.md`
2. `02_CDK_System_Design.md`
3. `03_PlaytimeQuery_Design.md`
4. `04_E3_Business_Integration_Report.md`
5. `05_E3_Regression_Test_Report.md`
6. `06_Solo_Review_Log_E3.md`

## 编译与复查
- Alpha: `cd Mapbot-Alpha-V1 && ./gradlew compileJava`
- Reforged: `cd MapBot_Reforged && ./gradlew compileJava`
- 复查: `rg -n "TODO|FIXME|TEMP|HACK" Mapbot-Alpha-V1/src/main/java MapBot_Reforged/src/main/java`
