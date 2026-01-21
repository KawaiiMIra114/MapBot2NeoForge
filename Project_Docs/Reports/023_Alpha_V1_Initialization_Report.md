# Task #023 执行报告: MapBot Alpha V1 基础设施搭建

**执行者**: Lazarus
**日期**: 2026-01-21
**状态**: ⏳ 推进中 (UI 已就绪)

---

## 任务目标
构建独立于 Minecraft 进程的 MapBot Alpha 核心框架，实现 7x24 小时在线、多服管理及远程 Web 控制台。

---

## 已完成内容

### 1. 项目初始化
- 创建了 `Mapbot-Alpha-V1` 独立项目 (Gradle + Java 21)。
- 引入 **Netty 4.1** 用于高性能网络处理。
- 建立 `application.toml` 配置文件，支持监听端口与 MC 转发端口配置。

### 2. 端口分流器 (Multiplexer)
- 实现 `ProtocolDetector`: 监听 25560 端口，自动识别 TCP 流量。
- **流量分发**:
    - Minecraft 流量 -> 代理至本地 25565。
    - HTTP 流量 -> 分发出 Web 管理面板。

### 3. 进程守护 (Process Manager)
- 实现 `ProcessManager`: 独立启动并监控 MC 服务器进程。
- 实现了标准输出流 (Stdout) 的实时捕获与内存缓存。

### 4. Material You 管理面板 (UI V5 Hybrid)
- **视觉风格**: 基于 Stitch 设计稿，采用 Lime Green (#a7f042) 与 深色玻璃拟态。
- **导航系统**: 实现纤细悬浮导航栏，具备物理级精准居中的弹性滑动指示器。
- **视图模块**: 预设仪表盘、实时控制台预览、插件管理及系统设置。

---

## 待执行计划 (Next Steps)

1. **STEP 5**: 实现 WebSocket 服务，将捕获到的 MC 日志实时推送到 Web 前端。
2. **STEP 6**: 移植 `CommandRegistry` 逻辑至 Alpha 核心，实现独立于游戏的指令处理。
3. **STEP 7**: 编写 Alpha-Mod Bridge 协议，实现核心与游戏内 Mod 的数据同步。

---

**签名**: Lazarus - MapBot Reforged 架构执行者
