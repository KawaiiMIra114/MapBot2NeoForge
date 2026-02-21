# MapBot Reforged - 分步执行提示词

本文件包含 5 个独立的 STEP 提示词，按顺序复制到新对话中执行。

---

## STEP 1: CQ码解析器

```
📋 DIRECTIVE: MAPBOT REFORGED 开发执行 (Task Order #012-STEP1)

**Role**: Lazarus - MapBot Reforged 专属开发执行者
**Project**: d:\riserver\Napcat2NeoForge

---

## 核心约束
1. 只改 `./MapBot_Reforged/` 目录
2. 代码注释使用简体中文
3. 任务完成后在 `./Project_Docs/Reports/` 写执行报告
4. 严格遵守 `.ai_rules.md` 规则

## 技术栈
- NeoForge 1.21.1 + Java 21
- 协议: OneBot v11 (NapCat)

---

## 本次任务

**创建 CQ码解析器** `src/main/java/com/mapbot/utils/CQCodeParser.java`

功能要求:
1. `parse(String raw)` - 解析 CQ 码，返回可读文本
   - `[CQ:image,file=...]` → `[图片]`
   - `[CQ:image,summary=动画表情...]` → `[动画表情]`
   - `[CQ:reply,id=...]` → 移除 (返回空字符串)
   - `[CQ:at,qq=xxx]` → `@xxx`

2. `extractAtTargets(String raw)` - 提取所有被 @ 的 QQ 号列表

技术提示:
- 使用正则表达式 `\[CQ:(\w+)([^\]]*)]` 匹配 CQ 码
- 返回 `List<Long>` 存储被 @ 的 QQ 号

---

请确认理解后开始执行。
```

---

## STEP 2: 双群结构配置

```
📋 DIRECTIVE: MAPBOT REFORGED 开发执行 (Task Order #012-STEP2)

**Role**: Lazarus - MapBot Reforged 专属开发执行者
**Project**: d:\riserver\Napcat2NeoForge

---

## 本次任务

**修改配置系统** `src/main/java/com/mapbot/config/BotConfig.java`

变更内容:
1. 将原 `targetGroupId` 重命名为 `playerGroupId`
2. 新增 `adminGroupId` 配置项
3. 新增 getter 方法:
   - `getPlayerGroupId()` - 玩家群 (消息转发源)
   - `getAdminGroupId()` - 管理群 (敏感命令)

配置注释:
```toml
[messaging]
playerGroupId = 0      # 玩家群 - 普通消息转发到游戏
adminGroupId = 0       # 管理群 - 敏感命令专用
```

注意:
- 修改后需同步更新 `InboundHandler.java` 中对 `getTargetGroupId()` 的调用

---

请确认理解后开始执行。
```

---

## STEP 3: 消息处理增强 + 权限分离

```
📋 DIRECTIVE: MAPBOT REFORGED 开发执行 (Task Order #012-STEP3)

**Role**: Lazarus - MapBot Reforged 专属开发执行者
**Project**: d:\riserver\Napcat2NeoForge

---

## 本次任务

**修改消息处理器** `src/main/java/com/mapbot/logic/InboundHandler.java`

变更内容:

### 1. 导入 CQCodeParser
```java
import com.mapbot.utils.CQCodeParser;
```

### 2. 消息来源判断
- 接收来自 `playerGroupId` 和 `adminGroupId` 的消息
- 记录消息来源群 ID

### 3. CQ码解析
在广播消息前调用:
```java
message = CQCodeParser.parse(message);
```

### 4. 命令权限分离
- `#inv`, `#stopserver`, `#reload`, `#addadmin`, `#removeadmin`, `#adminunbind` → 仅 adminGroup 可用
- 玩家群执行这些命令时返回 `❌ 此命令仅限管理群使用`

### 5. 消息转发规则
- 仅从 `playerGroupId` 转发普通消息到游戏
- `adminGroupId` 的普通消息不转发

---

请确认理解后开始执行。
```

---

## STEP 4: @提及游戏通知

```
📋 DIRECTIVE: MAPBOT REFORGED 开发执行 (Task Order #012-STEP4)

**Role**: Lazarus - MapBot Reforged 专属开发执行者
**Project**: d:\riserver\Napcat2NeoForge

---

## 本次任务

**实现 @提及 游戏内通知**

修改 `InboundHandler.java`:

### 逻辑流程
1. 使用 `CQCodeParser.extractAtTargets(rawMessage)` 获取被 @ 的 QQ 号列表
2. 对每个 QQ 号:
   - 调用 `DataManager.INSTANCE.getBinding(qq)` 获取绑定的 UUID
   - 如果已绑定，查找该玩家是否在线
   - 在线则发送 Title 通知

### NeoForge Title API
```java
// 发送 Title 通知
player.connection.send(new ClientboundSetTitleTextPacket(
    Component.literal("§b[QQ] 有人@你!")
));
// 可选: 设置 Title 显示时间
player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
```

### 需要导入
```java
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
```

---

请确认理解后开始执行。
```

---

## STEP 5: 末影箱查询

```
📋 DIRECTIVE: MAPBOT REFORGED 开发执行 (Task Order #012-STEP5)

**Role**: Lazarus - MapBot Reforged 专属开发执行者
**Project**: d:\riserver\Napcat2NeoForge

---

## 本次任务

**实现末影箱查询功能**

### 1. 修改 InventoryManager.java

新增方法:
```java
/**
 * 获取玩家末影箱的可读字符串
 */
public static String getPlayerEnderChest(ServerPlayer player) {
    if (player == null) {
        return "❌ 玩家不存在或已离线";
    }
    
    var enderChest = player.getEnderChestInventory();
    // 遍历 27 格末影箱，复用 formatItemStack() 方法
    // 返回格式化字符串
}
```

### 2. 修改 InboundHandler.java

更新 `handleInventoryCommand` 方法:
- 解析 `-e` 参数
- `#inv <玩家名>` → 调用 `getPlayerInventory()`
- `#inv <玩家名> -e` → 调用 `getPlayerEnderChest()`

命令示例:
```
#inv Steve      → 返回 Steve 的背包
#inv Steve -e   → 返回 Steve 的末影箱
```

---

请确认理解后开始执行。
```
