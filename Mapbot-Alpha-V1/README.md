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

## 🧭 上手体验
新管理员/新用户低门槛流程，依次完成首次配置、常用指令和故障自查即可稳住。

### 1. 首次配置
1. 安装 JDK 21+，需要时用 `docker-compose up -d` 启动 Redis 6379。
2. 执行 `./gradlew build`，再通过 `./gradlew run` 启动 Alpha 核心。
3. 在 `config/alpha.properties` 将 `auth.bootstrapAdmin.enabled` 设为 `true`，并填入用户名与密码。
4. 启动后在控制台日志里找到 Web 控制台地址，用刚填的管理员账号登录并确认 `auth` 状态。

### 2. 日常常用指令
1. 使用 `#status` 或 `#tps` 查询所有服的 TPS/内存，`#status all` 可加玩家分布。
2. 执行 `#list` 观察在线玩家，必要时配合 `#time @用户` 获取在线时长。
3. 需要停服就用 `#stopserver [秒数] [serverId]`，变更权限时用 `#addadmin`/`#removeadmin`。
4. Web 控制台右上命令面板可直接复用上述命令并查看反馈，遇到提示请复查日志页面。

### 3. 故障自查
1. 第一时间看 `logs/alpha.log` 与 `logs/alpha.YYYY-MM-DD.log`，留意 `ERROR`/`WARN` 关键字。
2. 核对 `config/alpha.properties` 中 `redis.*`、`auth.bridge.token`、`messaging.adminQQs` 与实际值是否一致。
3. 用 `redis-cli -h 127.0.0.1 -p 6379 ping` 或 Web 控制台状态卡确认 Redis 与控制端互通，再重启 `./gradlew run`.

## 🧭 上手体验
新管理员/新用户低门槛流程，依次完成首次配置、常用指令和故障自查即可稳住。

### 1. 首次配置
1. 安装 JDK 21+，需要时用 `docker-compose up -d` 启动 Redis 6379。
2. 执行 `./gradlew build`，再通过 `./gradlew run` 启动 Alpha 核心。
3. 在 `config/alpha.properties` 将 `auth.bootstrapAdmin.enabled` 设为 `true`，并填入用户名与密码。
4. 启动后在控制台日志里找到 Web 控制台地址，用刚填的管理员账号登录并确认 `auth` 状态。

### 2. 日常常用指令
1. 使用 `#status` 或 `#tps` 查询所有服的 TPS/内存，`#status all` 可加玩家分布。
2. 执行 `#list` 观察在线玩家，必要时配合 `#time @用户` 获取在线时长。
3. 需要停服就用 `#stopserver [秒数] [serverId]`，变更权限时用 `#addadmin`/`#removeadmin`。
4. Web 控制台右上命令面板可直接复用上述命令并查看反馈，遇到提示请复查日志页面。

### 3. 故障自查
1. 第一时间看 `logs/alpha.log` 与 `logs/alpha.YYYY-MM-DD.log`，留意 `ERROR`/`WARN` 关键字。
2. 核对 `config/alpha.properties` 中 `redis.*`、`auth.bridge.token`、`messaging.adminQQs` 与实际值是否一致。
3. 用 `redis-cli -h 127.0.0.1 -p 6379 ping` 或 Web 控制台状态卡确认 Redis 与控制端互通，再重启 `./gradlew run`。
