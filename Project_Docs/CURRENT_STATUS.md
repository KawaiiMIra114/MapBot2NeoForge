# CURRENT STATUS (Single Source of Truth)

本文件是 `Project_Docs` 的唯一当前状态入口。
如与其他文档描述冲突，以本文件为准。

## 人工维护区块
- 项目阶段: Alpha Phase
- 当前版本口径: v5.7.x（基于现有 Task #024/#025 报告链）
- 双端形态: `MapBot_Reforged/` 负责游戏侧事件与桥接。
- 双端形态: `Mapbot-Alpha-V1/` 负责核心指令、多服状态与数据主存。
- 维护原则: 任务完成状态以 `Project_Docs/Reports/README.md` 为索引入口。

## 活跃文档集
- `Project_Docs/CURRENT_STATUS.md`
- `Project_Docs/Reports/README.md`
- `Project_Docs/Architecture/Migration_1.21.md`
- `Project_Docs/Architecture/Web_Console_Design.md`
- `Project_Docs/Contracts/Lazarus_Prompt_Template.md`
- `Project_Docs/Contracts/Lazarus_Step_Prompts.md`
- `Project_Docs/Contracts/Lazarus_Persona.md`
- `Project_Docs/Contracts/Lemon_Persona.md`
- `Project_Docs/Contracts/Task013_Step_Prompts.md`
- `Project_Docs/Contracts/PRIVACY_POLICY.md`
- `Project_Docs/Manuals/KubeJS_Example.js`
- `Project_Docs/stitch/dashboard_overview/code.html`
- `Project_Docs/stitch/dashboard_overview/screen.png`
- `Project_Docs/stitch/bot_plugin_settings/code.html`
- `Project_Docs/stitch/bot_plugin_settings/screen.png`
- `Project_Docs/stitch/server_console_control/code.html`
- `Project_Docs/stitch/server_console_control/screen.png`

## 归档文档集
- `Project_Docs/Architecture/archive/Protocol_Spec.md`
- `Project_Docs/Architecture/archive/System_Design.md`

## 文档同步脚本
- 脚本路径: `Project_Docs/scripts/docs_sync.py`
- 运行方式:
```bash
python3 Project_Docs/scripts/docs_sync.py
```
- 脚本职责: 重新生成 `Project_Docs/Reports/README.md`
- 脚本职责: 刷新本文件中的“自动同步区块”

<!-- AUTO_SYNC:START -->
## 自动同步区块
- 最近同步 (UTC): 2026-02-13 09:45:56
- 报告总数: 35
- 最新任务: #025
- 最新任务状态: 已完成
- 最新任务文件: 025_P0_P4_Followup_Report.md
- 生成器: `python3 Project_Docs/scripts/docs_sync.py`
<!-- AUTO_SYNC:END -->
