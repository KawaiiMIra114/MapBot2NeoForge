# MapBot Reforged - Task #013 执行提示词 (鲁班修订版)

本文件包含 3 个 STEP 提示词，用于实现群昵称缓存和 @提及优化。

---

## STEP 1: 群成员缓存管理器

```
📋 DIRECTIVE: MAPBOT REFORGED 开发执行 (Task Order #013-STEP1)

**Role**: Lazarus - MapBot Reforged 专属开发执行者
**Project**: d:\riserver\Napcat2NeoForge

---

## 本次任务

**创建群成员昵称缓存管理器**

### 新建文件: `src/main/java/com/mapbot/data/GroupMemberCache.java`

```java
/*
 * MapBot Reforged - 群成员缓存管理器
 * 
 * 功能:
 * - 缓存 QQ 群成员的昵称信息
 * - 避免频繁调用 OneBot API 导致延迟
 */
package com.mapbot.data;

import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupMemberCache {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/MemberCache");
    
    public static final GroupMemberCache INSTANCE = new GroupMemberCache();
    
    // 缓存结构: QQ号 -> 昵称
    private final ConcurrentHashMap<Long, String> nicknameCache = new ConcurrentHashMap<>();
    
    private GroupMemberCache() {}
    
    /**
     * 批量加载群成员列表
     * @param members Map<QQ号, 昵称>
     */
    public void loadMembers(java.util.Map<Long, String> members) {
        nicknameCache.putAll(members);
        LOGGER.info("已加载 {} 个群成员昵称", members.size());
    }
    
    /**
     * 更新单个成员昵称 (成员入群/改名时调用)
     */
    public void updateMember(long qq, String nickname) {
        nicknameCache.put(qq, nickname);
    }
    
    /**
     * 移除成员 (成员退群时调用)
     */
    public void removeMember(long qq) {
        nicknameCache.remove(qq);
    }
    
    /**
     * 获取成员昵称
     * @return 昵称，未找到返回 null
     */
    public String getNickname(long qq) {
        return nicknameCache.get(qq);
    }
    
    /**
     * 清空缓存
     */
    public void clear() {
        nicknameCache.clear();
    }
}
```

---

请确认理解后开始执行。
```

---

## STEP 2: @提及昵称解析

```
📋 DIRECTIVE: MAPBOT REFORGED 开发执行 (Task Order #013-STEP2)

**Role**: Lazarus - MapBot Reforged 专属开发执行者
**Project**: d:\riserver\Napcat2NeoForge

---

## 本次任务

**改进 CQ码解析器的 @提及 处理**

### 修改 CQCodeParser.java

1. **添加导入**:
```java
import com.mapbot.data.DataManager;
import com.mapbot.data.GroupMemberCache;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import java.util.Optional;
import java.util.UUID;
```

2. **替换 parseAt() 方法**:
```java
/**
 * 解析 @ 提及 CQ 码
 * 优先级: 绑定玩家名 > 群昵称缓存 > QQ号
 * 
 * @param params CQ 码参数部分
 * @return 可读文本
 */
private static String parseAt(String params) {
    Pattern qqPattern = Pattern.compile("qq=([^,\\]]+)");
    Matcher qqMatcher = qqPattern.matcher(params);
    
    if (qqMatcher.find()) {
        String qq = qqMatcher.group(1);
        
        // @全体成员
        if ("all".equalsIgnoreCase(qq)) {
            return "@全体成员";
        }
        
        try {
            long qqNum = Long.parseLong(qq);
            
            // 1. 优先查绑定的玩家名
            String uuid = DataManager.INSTANCE.getBinding(qqNum);
            if (uuid != null) {
                MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                if (server != null) {
                    Optional<GameProfile> profile = server.getProfileCache().get(UUID.fromString(uuid));
                    if (profile.isPresent()) {
                        return "@" + profile.get().getName();
                    }
                }
            }
            
            // 2. 其次查群昵称缓存
            String nickname = GroupMemberCache.INSTANCE.getNickname(qqNum);
            if (nickname != null && !nickname.isEmpty()) {
                return "@" + nickname;
            }
            
        } catch (Exception e) {
            LOGGER.debug("解析@目标失败: {}", e.getMessage());
        }
        
        // 3. 兜底: 显示 QQ 号
        return "@" + qq;
    }
    return "@未知";
}
```

---

请确认理解后开始执行。
```

---

## STEP 3: 群成员列表预加载

```
📋 DIRECTIVE: MAPBOT REFORGED 开发执行 (Task Order #013-STEP3)

**Role**: Lazarus - MapBot Reforged 专属开发执行者
**Project**: d:\riserver\Napcat2NeoForge

---

## 本次任务

**Bot 连接时预加载群成员列表**

### 修改 BotClient.java

在 WebSocket 连接成功后，自动请求群成员列表：

```java
// 在连接成功回调中添加 (onOpen 或 lifecycle 事件)
private void requestGroupMemberList() {
    long playerGroupId = BotConfig.getPlayerGroupId();
    if (playerGroupId == 0L) return;
    
    JsonObject params = new JsonObject();
    params.addProperty("group_id", playerGroupId);
    
    JsonObject packet = new JsonObject();
    packet.addProperty("action", "get_group_member_list");
    packet.add("params", params);
    packet.addProperty("echo", "load_members_" + System.currentTimeMillis());
    
    sendPacket(packet);
    LOGGER.info("已请求玩家群成员列表");
}
```

### 修改 InboundHandler.java

处理 `get_group_member_list` 的 echo 响应：

```java
// 在 handleMessage() 中添加
if (rawJson.contains("\"echo\":\"load_members_")) {
    handleGroupMemberListResponse(json);
    return;
}

private static void handleGroupMemberListResponse(JsonObject json) {
    JsonArray data = json.getAsJsonArray("data");
    if (data == null) return;
    
    Map<Long, String> members = new HashMap<>();
    for (JsonElement elem : data) {
        JsonObject member = elem.getAsJsonObject();
        long userId = member.get("user_id").getAsLong();
        // 优先使用群名片，其次昵称
        String card = getStringOrNull(member, "card");
        String nickname = getStringOrNull(member, "nickname");
        members.put(userId, (card != null && !card.isEmpty()) ? card : nickname);
    }
    
    GroupMemberCache.INSTANCE.loadMembers(members);
}
```

---

请确认理解后开始执行。
```

---

## STEP 4: 延迟排查 (可选)

```
📋 DIRECTIVE: MAPBOT REFORGED 开发执行 (Task Order #013-STEP4)

**Role**: Lazarus - MapBot Reforged 专属开发执行者
**Project**: d:\riserver\Napcat2NeoForge

---

## 本次任务

**添加调试日志定位延迟点**

### 修改 BotClient.java

在 WebSocket onMessage 回调添加时间戳:
```java
long receiveTime = System.currentTimeMillis();
LOGGER.debug("[TIMING] WS收到: {}", receiveTime);
InboundHandler.handleMessage(message);
LOGGER.debug("[TIMING] 处理完成: {}ms", System.currentTimeMillis() - receiveTime);
```

### 修改 InboundHandler.java

在 `handleGroupMessage()` 关键位置添加时间戳:
```java
long t0 = System.currentTimeMillis();
// ... 消息解析 ...
LOGGER.debug("[TIMING] 解析完成: {}ms", System.currentTimeMillis() - t0);

server.execute(() -> {
    long t1 = System.currentTimeMillis();
    LOGGER.debug("[TIMING] 主线程开始 (排队: {}ms)", t1 - t0);
    // ... 广播消息 ...
    LOGGER.debug("[TIMING] 主线程完成: {}ms", System.currentTimeMillis() - t1);
});
```

---

此 STEP 为可选，用于排查延迟问题。
```
