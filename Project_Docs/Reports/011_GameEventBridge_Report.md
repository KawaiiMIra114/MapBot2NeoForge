# Task ID: #011 Game-to-QQ Event Bridge

## 执行时间
2026-01-16 23:40 (UTC+8)

---

## Status: ✅ SUCCESS

---

## 创建/修改的文件列表

| 路径 | 操作 | 说明 |
|------|------|------|
| `network/BotClient.java` | 修改 | 添加 sendGroupMessage 辅助方法 |
| `logic/GameEventListener.java` | 新建 | 游戏事件监听器 |

---

## 事件转发规则

| 事件类型 | NeoForge 事件 | 消息格式 |
|----------|---------------|----------|
| 聊天 | `ServerChatEvent` | `[玩家名] 消息内容` |
| 加入 | `PlayerLoggedInEvent` | `[+] 玩家名 加入了服务器` |
| 退出 | `PlayerLoggedOutEvent` | `[-] 玩家名 离开了服务器` |
| 死亡 | `LivingDeathEvent` | `[☠️] 原版死亡消息` |

---

## 技术实现

### BotClient.sendGroupMessage()

```java
public void sendGroupMessage(long groupId, String message) {
    if (!isConnected || groupId == 0L) return;
    
    JsonObject params = new JsonObject();
    params.addProperty("group_id", groupId);
    params.addProperty("message", message);
    
    JsonObject packet = new JsonObject();
    packet.addProperty("action", "send_group_msg");
    packet.add("params", params);
    
    this.sendPacket(packet);
}
```

### 事件监听注解

```java
@EventBusSubscriber(modid = MapBot.MODID, bus = EventBusSubscriber.Bus.GAME)
public class GameEventListener { ... }
```

---

## Git 提交

| Commit | 内容 |
|--------|------|
| `02f3748` | Task #011 游戏事件到QQ桥接 |
