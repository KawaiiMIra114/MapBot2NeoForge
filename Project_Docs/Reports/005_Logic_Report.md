# Task ID: #005 Business Logic & Event Bridging

## 执行时间
2026-01-14 22:45 (UTC+8)

---

## Status: ✅ SUCCESS

---

## 创建/修改的文件列表

| 路径 | 类型 | 说明 |
|------|------|------|
| `logic/GameEventListener.java` | 新建 | 监听 Minecraft 事件，转发到 QQ |
| `logic/InboundHandler.java` | 新建 | 解析 QQ 消息，广播到服务器 |
| `network/BotClient.java` | 修改 | 集成 InboundHandler 处理入站消息 |

---

## 技术实现细节

### 1. 出站桥接 (Minecraft -> QQ)

`GameEventListener` 使用 `@EventBusSubscriber` 注解自动注册到 NeoForge 事件总线。

监听的事件:
- `ServerChatEvent` - 玩家聊天 (忽略 `/` 开头的命令)
- `PlayerEvent.PlayerLoggedInEvent` - 玩家登录
- `PlayerEvent.PlayerLoggedOutEvent` - 玩家登出

消息格式符合 OneBot v11 标准:
```json
{
    "action": "send_group_msg",
    "params": {
        "group_id": 123456789,
        "message": "[服务器] <PlayerName> Message"
    },
    "echo": "chat_timestamp"
}
```

### 2. 入站桥接 (QQ -> Minecraft)

`InboundHandler` 负责解析来自 NapCat 的 JSON 推送。

**处理的事件类型**:
- `post_type: "message"` - 群消息，格式化为 `[QQ] <Nickname> Message`
- `post_type: "meta_event"` - 心跳/生命周期事件

### 3. 跨线程安全处理 (CRITICAL)

> [!IMPORTANT]
> **问题**: WebSocket 回调 (`onText`) 在独立的 I/O 线程中执行，而 Minecraft 的 `broadcastSystemMessage()` 必须在服务器主线程调用，否则会导致 ConcurrentModificationException 或数据竞争。

**解决方案**:
```java
// 在 InboundHandler.broadcastToServer() 中
MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
server.execute(() -> {
    // 此代码块在主线程执行
    server.getPlayerList().broadcastSystemMessage(chatComponent, false);
});
```

`ServerLifecycleHooks.getCurrentServer().execute()` 是 NeoForge 提供的 API，用于将任务安全地调度到服务器主线程的 tick 循环中执行。

### 4. 集成点

`BotClient.onText()` 现在调用 `InboundHandler.handleMessage(data.toString())`，完成了从网络层到业务层的解耦。

---

## 已知限制

1. `TARGET_GROUP_ID` 目前是硬编码的，后续需要从配置文件读取。
2. 消息格式化较为简单，可能需要支持更多 OneBot CQ 码。

---

## Next Step Recommendation

**Task #006**: 配置系统实现
1. 创建 `config/` 包，实现 NeoForge 配置文件读取。
2. 将 `TARGET_GROUP_ID`、`WS_URL` 等参数移至配置文件。
