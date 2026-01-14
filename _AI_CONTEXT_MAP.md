# MapBot Reforged - AI Context Map

## 项目概览
这是 **MapBot Reforged** 项目的上下文索引文件，帮助 AI 代理快速理解项目结构和导航。

---

## 目录用途说明

| 目录 | 类型 | 用途 |
|------|------|------|
| `./MapBotv4/` | 🔴 只读 | **遗留参考代码** - 包含原始 Bukkit 插件的完整源代码。用于理解业务逻辑和功能需求。 |
| `./MapBot_Reforged/` | 🟢 工作区 | **新项目代码** - NeoForge 1.21.1 模组的活动开发目录。所有新代码都在此创建。 |
| `./Project_Docs/` | 🔵 知识库 | **文档中心** - 存储所有项目文档。 |
| `./Project_Docs/Architecture/` | 🔵 知识库 | **技术设计文档** - 架构设计、API设计、数据流图等。 |
| `./Project_Docs/Contracts/` | 🔵 知识库 | **许可证与法律文本** - 开源协议、第三方库许可证等。 |
| `./Project_Docs/Manuals/` | 🔵 知识库 | **设置指南** - 安装手册、配置说明、部署指南等。 |
| `./Project_Docs/Reports/` | 🔵 知识库 | **执行报告** - 每个任务完成后的状态报告。 |

---

## 快速导航指南

> **理解旧业务逻辑**
> 
> 请阅读 `/MapBotv4` 目录中的文件。主要关注:
> - `src/main/java/` - Java 源代码
> - `README.md` - 功能概述
> - `pom.xml` - 依赖项列表

> **理解新技术方向**
> 
> 请阅读项目根目录的 `.ai_rules.md` 文件，其中定义了:
> - 技术栈: NeoForge 1.21.1 + Java 21
> - 架构模式: 微服务 (NeoForge <-> WebSocket <-> NapCat/OneBot v11)
> - 编码标准和操作工作流程

---

## 核心治理文件

| 文件 | 用途 |
|------|------|
| `.ai_rules.md` | **宪法** - 全局 AI 治理协议，所有 AI 代理必须遵守 |
| `_AI_CONTEXT_MAP.md` | **本文件** - 项目上下文索引 |

---

## 技术栈

- **游戏端**: Minecraft NeoForge 1.21.1 (Java 21)
- **通信协议**: WebSocket
- **机器人端**: NapCat / OneBot v11 协议
- **构建工具**: Gradle

---

*此文件由项目初始化流程自动生成于 2026-01-14*
