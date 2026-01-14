# Task ID: #004 Network Layer Implementation

## 执行时间
2026-01-14 22:40 (UTC+8)

---

## Status: ✅ SUCCESS

---

## 创建的文件列表

| 路径 | 类型 | 说明 |
|------|------|------|
| `MapBot_Reforged/src/main/java/com/mapbot/network/BotClient.java` | 核心实现 | WebSocket 客户端单例，包含连接管理与发送逻辑 |
| `MapBot_Reforged/src/main/java/com/mapbot/network/MainTest.java` | 验证工具 | 独立的 `main` 方法测试类 |

## 技术实现细节

### 1. 通信栈 (Tech Stack)
采用了 **Java 21 `java.net.http.WebSocket`** 标准库，未引入额外的 Netty 依赖，保持了模组的轻量化。

### 2. 重连机制 (Reconnect Logic)
`BotClient` 内部维护了一个 `ScheduledExecutorService` (单线程)。
- **触发条件**: 当 `buildAsync` 失败、`onClose` 被调用或 `onError` 发生时。
- **机制**: 调用 `scheduleReconnect()`，设置标志位 `isReconnecting=true`。
- **策略**: 延迟 **5秒** 后，通过 Executor 再次调用 `connect()`。
- **并发控制**: 使用了 `isConnected` 和 `isReconnecting` 标志位防止重复调度或并发连接。

### 3. 数据发送
实现了 `sendPacket(JsonObject json)` 方法，接受 Gson 对象，序列化为 String 后通过 WebSocket 发送。

### 4. 错误处理
所有网络操作均包裹在 try-catch 块中，异常信息会通过 SLF4J 记录到标准日志中 (`[MapBot/WS]`).

---

## Next Step Recommendation
网络层基础已就绪。

建议下一步 (**Task #005**):
实现 **业务逻辑层 (Service Layer)**
1. 在 `BotClient.onText` 中接入 JSON 解析器。
2. 创建 `GameListener` 监听 Minecraft 事件 (如聊天)，并调用 `BotClient.sendPacket`。
