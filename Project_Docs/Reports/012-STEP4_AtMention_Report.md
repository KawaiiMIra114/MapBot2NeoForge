# Task #012-STEP4 执行报告: @提及游戏内通知

**执行者**: Lazarus  
**日期**: 2026-01-18  
**状态**: ✅ 完成

---

## 任务目标

实现 QQ 群 @提及 的游戏内 Title 通知功能。

## 变更内容

### 修改文件: `InboundHandler.java`

#### 1. 新增导入
```java
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
```

#### 2. 新增方法 `notifyAtMentions()`
- 调用 `CQCodeParser.extractAtTargets()` 提取 @目标 QQ 号
- 通过 `DataManager.getBinding(qq)` 获取绑定的 UUID
- 使用 `server.getPlayerList().getPlayer(uuid)` 查找在线玩家
- 调用 `sendAtNotification()` 发送 Title 通知

#### 3. 新增方法 `sendAtNotification()`
- 设置 Title 动画时间: fadeIn=10, stay=70, fadeOut=20
- 发送格式: `§b[QQ] §f<发送者昵称> §6@了你!`

#### 4. 流程集成
在 `handleGroupMessage()` 消息转发前调用 `notifyAtMentions(rawMessage, nickname)`

---

## 技术说明

### NeoForge Title API
使用原版 `ClientboundSetTitleTextPacket` 直接发送协议包，无需通过 Bukkit API。

### 线程安全
`notifyAtMentions()` 使用 `server.execute()` 确保玩家查找在主线程执行。

### 参考实现
MapBotV4 `QQMessageHandler.java` 中的 @提及处理逻辑 (Line 137-165)。

---

## 编译验证

```
./gradlew build -x test
BUILD SUCCESSFUL
```

---

## 后续建议

1. 可添加 Subtitle 显示消息预览
2. 考虑添加 @全体成员 的处理逻辑 (当前仅处理个人 @)
3. 可配置化 Title 显示时间

---

**签名**: Lazarus - MapBot Reforged 开发执行者
