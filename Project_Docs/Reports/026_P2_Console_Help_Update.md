# Task 026 - P2 控制台帮助信息完善

## 任务目标
- 补充 Alpha 控制台帮助信息，明确 `/server` 与 `/back` 的切换规则，并提示 Alpha/服务器控制台的转发差异。

## 变更文件
- `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/network/ConsoleCommandHandler.java`
- `Project_Docs/Reports/026_P2_Console_Help_Update.md`

## 关键实现
- 扩展 `/help` 输出，加入 `/server <服务器名>` 与 `/back` 指令说明，并明确 Alpha 控制台不转发普通文本、服务器控制台自动补全 `/` 并转发的行为规则。

## 测试验证
- 未执行（仅文本帮助信息调整）。

## Git 提交记录
- 9e3ea01 docs: clarify console help switching
