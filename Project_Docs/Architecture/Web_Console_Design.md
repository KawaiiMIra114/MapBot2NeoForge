# MapBot Alpha V1 - Web 控制台设计方案

## 1. 视觉风格对齐
参考 `Project_Docs/stitch` 中的设计：
- **配色系统**: 采用 Lime Green (#a7f042) 作为主色调，搭配深色背景 (#1a2210)。
- **设计语言**: Material Design 3 (M3) 风格，包含：
  - 28px 大圆角容器。
  - 悬浮式底部导航栏 (Floating Navigation Bar)。
  - 磨砂玻璃效果 (Backdrop Blur)。
- **字体**: 使用 `Space Grotesk` 和 `Roboto Mono` (用于控制台)。

## 2. 功能模块
控制台将分为四个主要视图：
1. **仪表盘 (Dashboard)**: 服务器状态概览、快捷开关、资源占用。
2. **服务器控制台 (Server Console)**: MC 实时日志流、命令输入。
3. **机器人管理 (Bot Control)**: OneBot 状态、插件/模块开关 (参考 AstrBot)。
4. **系统设置 (Settings)**: 端口配置、密钥管理、数据持久化设置。

## 3. 技术实现 (后端)
- **静态资源**: 由 Netty HTTP Handler 分发。
- **动态数据**:
  - **WebSocket (/ws/console)**: 用于推送 MC Stdout 日志。
  - **WebSocket (/ws/bot)**: 用于推送 OneBot 运行日志。
  - **REST API**: 用于执行特定动作 (如 #reload, #setperm)。

## 4. 目录结构
- `Mapbot-Alpha-V1/src/main/resources/web/`
  - `index.html` (主页面，包含导航逻辑)
  - `assets/` (CSS, JS, Images)
