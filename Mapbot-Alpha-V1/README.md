# Mapbot Alpha V1 - 多服中枢系统

Mapbot Alpha 是一个高性能的 Minecraft 反向 WebSocket 网关与多服权限管理中心。
从 **Reforged** 版本完整移植，现已支持分布式架构。

## 🚀 核心特性
- **WebSocket 反向网关**: 对接 OneBot 协议（如 NapCat, Lagrange）。
- **Bridge 互联**: 与 Minecraft 子服的 Bridge Mod 通信，实现跨服聊天转发与控制。
- **Web 控制台**: 现代化的玻璃拟态 UI，支持文件管理、控制台指令、实时监控。
- **分布式同步**: 基于 **Redis** 的跨服数据同步（权限、白名单、Web 会话）。

## 🛠️ 快速开始

### 1. 环境准备
- JDK 21+
- Redis (可选，用于跨服同步)

### 2. 启动 Redis
如果你本地安装了 Docker，可以直接运行：
```bash
docker-compose up -d
```
这将在本地 `6379` 端口启动 Redis 服务。

### 3. 构建与运行
```bash
# 构建
./gradlew build

# 运行
./gradlew run
```

## ⚙️ Redis 配置
进入 Web 控制台 -> **Settings** 页面：
1. 开启 **Redis 跨服同步镜像**。
2. 填写主机地址 (默认 `127.0.0.1`) 和端口 (`6379`)。
3. 点击保存。

一旦连接成功，控制台将显示 `[Redis] Redis 连接成功`。
此时这一台 Alpha 核心对 **用户权限/白名单** 的修改，将实时同步到所有连接了同一个 Redis 的 Alpha 实例。

## 📦 目录结构
- `src/main/java`: 后端源码 (Netty, Gson, Jedis)
- `src/main/resources/web`: Web 控制台前端源码
- `config/`: 配置文件
- `data/`: 本地数据持久化 (作为 Redis 的备份)
