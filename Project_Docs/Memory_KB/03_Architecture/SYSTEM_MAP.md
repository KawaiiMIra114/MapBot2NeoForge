# SYSTEM_MAP

## 1. 系统拓扑（Alpha / Reforged / Bridge / OneBot / Redis）

```text
QQ群(OneBot/NapCat)
   <-> OneBot WebSocket
Alpha Core
  - Command/API/Auth/Data/Redis Hub
  - Bridge Server (TCP, register/auth/heartbeat/proxy)
   <-> Bridge Protocol (JSON line)
Reforged Mod
  - BridgeClient + BridgeHandlers
  - MC内执行器(命令/白名单/文件/游戏事件)

Alpha <-> Redis（bindings/perms/admins/mutes/auth tokens/users）
```

## 2. 组件职责边界

| 组件 | 主要职责 | 明确不负责 |
|---|---|---|
| Alpha | 统一入口（QQ/HTTP/API/Bridge）、权限与鉴权、跨服编排、Redis主存 | 不直接执行 Minecraft 世界内命令 |
| Reforged | 执行 Minecraft 内动作（指令、白名单、物品、文件落地） | 不保存全局权限真相源（以 Alpha/Redis 为准） |
| Bridge | Alpha-Reforged 的 RPC/事件通道（注册、心跳、代理请求） | 不承担业务权限策略定义 |
| OneBot | QQ 收发协议适配（群/私聊消息） | 不做 MapBot 业务鉴权 |
| Redis | 跨进程状态存储与同步（Data/Auth） | 不做业务流程编排 |

## 3. 核心调用链

### 3.1 QQ 指令链（OneBot -> Alpha -> Bridge -> Reforged）
1. OneBot 推送消息到 Alpha：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/network/OneBotClient.java`
2. Alpha 解析并分发命令：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/command/CommandRegistry.java`
3. 需要子服执行时调用 Bridge：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/BridgeProxy.java`
4. Reforged 收到 `proxy` 请求并执行：`MapBot_Reforged/src/main/java/com/mapbot/network/BridgeClient.java` + `MapBot_Reforged/src/main/java/com/mapbot/network/BridgeHandlers.java`
5. 结果回传 Alpha，再由 Alpha 回 QQ。

### 3.2 游戏事件上报链（Reforged -> Bridge -> Alpha -> OneBot/Redis）
1. Reforged 上报 `event/chat/playtime_add/check_mute/get_qq_by_uuid`：`MapBot_Reforged/src/main/java/com/mapbot/network/BridgeClient.java`
2. Alpha Bridge 入站处理：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/BridgeMessageHandler.java`
3. Alpha 侧写入/查询真相源：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/data/DataManager.java`（Redis同步）
4. Alpha 需要通知 QQ 时走：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/network/OneBotClient.java`

### 3.3 Web/API 链（Dashboard/API -> Auth -> Data/Bridge）
1. 入口：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/network/ProtocolDetector.java`（HTTP/MC 分流）
2. API 分发：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/network/HttpRequestDispatcher.java`
3. Token 与角色校验：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/security/AuthManager.java`
4. 本地数据面：`DataManager`；跨服文件面：`RemoteFileApiHandler -> BridgeFileProxy -> Bridge`

### 3.4 Redis 链（Alpha 主存）
1. 连接池：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/database/RedisManager.java`
2. 业务读写：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/data/DataManager.java`
3. 认证读写：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/security/AuthManager.java`

## 4. 启动与连接顺序（关键）

1. Alpha 启动：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/MapbotAlpha.java`
2. 加载配置/Redis/Auth/Data 后，启动：
- OneBot 客户端
- BridgeServer（子服注册入口）
- 智能分流端口（HTTP/MC）
3. Reforged 启动：`MapBot_Reforged/src/main/java/com/mapbot/MapBot.java`
4. Reforged `BridgeClient.connect()` 读取 `alphaHost/alphaPort/alphaToken` 并注册。

## 5. Step04 阻断链路定位（必须关注）

### 5.1 权限模型阻断
- 角色模型实现：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/security/AuthManager.java`
- QQ 命令权限拦截：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/command/CommandRegistry.java`
- API 鉴权与拒绝码路径：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/network/HttpRequestDispatcher.java`
- 合同/门禁依据：
  - `Project_Docs/Re_Step/Artifacts/Step04/01_Command_Authorization_Matrix.md`
  - `Project_Docs/Re_Step/Artifacts/Step04/05_B2_Negative_Test_Cases.md`

### 5.2 配置 schema 阻断
- Alpha 配置加载与回退行为：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/config/AlphaConfig.java`
- 运行配置源：`Mapbot-Alpha-V1/config/alpha.properties`
- Reforged 对端配置：`1/mapbot-common.toml`
- 合同/门禁依据：`Project_Docs/Re_Step/Artifacts/Step04/03_Config_Schema_Validation_Profile.md`

### 5.3 reload 链路阻断
- 入口命令：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/command/impl/ReloadCommand.java`
- Alpha 配置重载：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/config/AlphaConfig.java`
- 安全配置重载：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/security/AuthManager.java`
- 子服 fan-out：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/BridgeProxy.java`
- 子服执行 `reload_config`：`MapBot_Reforged/src/main/java/com/mapbot/network/BridgeClient.java` + `MapBot_Reforged/src/main/java/com/mapbot/network/BridgeHandlers.java`
- 合同/门禁依据：`Project_Docs/Re_Step/Artifacts/Step04/04_HotReload_Rollback_Audit_Flow.md`

### 5.4 Bridge/API 鉴权阻断
- Bridge 首帧注册鉴权：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/BridgeServer.java`
- Bridge token/allowedServerIds 策略：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/security/AuthManager.java`
- Reforged token 发送端：`MapBot_Reforged/src/main/java/com/mapbot/network/BridgeClient.java`
- HTTP API Bearer Token 校验：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/network/HttpRequestDispatcher.java`

## 6. 新模型快速定位建议

1. 先读入口：`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/MapbotAlpha.java`、`MapBot_Reforged/src/main/java/com/mapbot/MapBot.java`
2. 再读协议面：`BridgeServer` / `BridgeMessageHandler` / `BridgeProxy` / `BridgeClient` / `BridgeHandlers`
3. 最后读 Step04 阻断四件套：`AuthManager`、`HttpRequestDispatcher`、`AlphaConfig`、`ReloadCommand`
