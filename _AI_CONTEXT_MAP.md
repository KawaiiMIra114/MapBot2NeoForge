# MapBot Reforged - AI Context Map

## 入口规则
- 当前状态唯一真相: `Project_Docs/CURRENT_STATUS.md`
- 报告索引入口: `Project_Docs/Reports/README.md`
- 如本文件与以上两者冲突，以以上两者为准。

## 当前项目形态
- `MapBot_Reforged/`: NeoForge 1.21.1 游戏侧桥接与事件上报。
- `Mapbot-Alpha-V1/`: 独立核心服务（指令、数据主存、多服状态、Web 管理）。

## 最近任务口径
- 最近完整报告链覆盖到 Task `#025`（报告日期: `2026-01-26`）。
- `#023` 文档中的“推进中”历史描述已不再作为最新状态判断依据，最新状态请看报告索引与 CURRENT_STATUS 自动区块。

## 文档治理
- 过时架构文档已转入 `Project_Docs/Architecture/archive/`。
- 报告索引与状态自动区块通过以下命令刷新：
```bash
python3 Project_Docs/scripts/docs_sync.py
```

## 智能体对话轨迹 (Agent Trace Context)
- [Nexus] [2026-02-21 21:19:00] [创建初始缺陷报告与设定全体智能体管控契约《MAPBOT_GLOBAL_PROTOCOLS.md》，清理废弃的旧版控制脚本，确立上下文汇报追溯原则。] [Nexus-002]
- [Nexus] [2026-02-21 21:22:00] [创建全景重构任务排期表 `master_backlog.md` 以供三端智能体领取任务。] [Nexus-003]
- [Nexus] [2026-02-21 21:27:00] [为 Aegis 撰写阶段一任务 01 的 Prompt 并标记 master_backlog 为处理中] [pending-commit]
