# MapBot Reforged - AI Context Map

## 项目概览
当前项目由两部分组成：
1. **MapBot Reforged**: NeoForge 1.21.1 模组 (随服启动)。
2. **MapBot Alpha V1**: 独立运行的 Bot 核心与守护进程 (7x24 在线)。

当前版本: **v5.5.0+ (Alpha Phase)**

---

## 🚀 核心进度 (截至 2026-01-21)

### [Mod 端: 已完成]
- **命令系统**: 全量迁移至 `CommandRegistry` 架构。
- **安全管理**: Level 0-2 权限系统、时间戳禁言拦截。
- **高级签到**: Tag 随机奖池、离线 CDK 暂存与兑换。
- **互动**: 双向 @ 提及 (音效 + Title)。

### [Alpha Core 端: 已完成]
- **分流网关**: 25560 端口实现 MC 代理与 Web 服务共用。
- **进程管理**: 独立进程启动与日志流捕获。
- **UI 界面**: 极简 Material You 管理面板 (Animated)。

---

## 📂 关键导航

| 路径 | 状态 | 内容 |
|------|------|------|
| `./MapBot_Reforged/` | 🟢 活跃 | NeoForge 模组源码。 |
| `./Mapbot-Alpha-V1/` | 🔵 活跃 | 独立核心框架源码。 |
| `./Project_Docs/Reports/` | 📖 完整 | 记录了 Task #017 至 #023 的所有技术报告。 |
| `./Mapbot_Alpha_Dashboard_V1.html` | 🎨 预览 | 网页管理面板的最新静态原型。 |

---

## 🎯 下一对话目标
1. **Task #023-STEP5**: 实现后端与 Web 前端的 WebSocket 日志实时推送。
2. **多服同步逻辑**: 准备接入 Redis 实现跨服数据共享。

---
*由 Lazarus 自动更新于对话结束前*
