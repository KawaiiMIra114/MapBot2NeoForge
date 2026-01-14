# Task ID: #006 Configuration System Implementation

## 执行时间
2026-01-14 22:55 (UTC+8)

---

## Status: ✅ SUCCESS

---

## 创建/修改的文件列表

| 路径 | 操作 | 说明 |
|------|------|------|
| `config/BotConfig.java` | 新建 | NeoForge ModConfigSpec 配置类 |
| `MapBot.java` | 修改 | 注册配置系统，添加服务器生命周期事件 |
| `network/BotClient.java` | 修改 | 使用配置值 (wsUrl, reconnectInterval, debugMode) |
| `logic/GameEventListener.java` | 修改 | 使用配置值 (targetGroupId) |
| `logic/InboundHandler.java` | 修改 | 添加群号验证安全检查 |
| `network/MainTest.java` | 修改 | 标记为过时，添加配置依赖说明 |

---

## 配置项详情

配置文件将自动生成于: `config/mapbot-common.toml`

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `wsUrl` | String | `ws://127.0.0.1:3000` | NapCat WebSocket 地址 |
| `targetGroupId` | Long | `0` | 目标 QQ 群号 (0=禁用) |
| `reconnectInterval` | Integer | `5` | 重连间隔 (1-60秒) |
| `debugMode` | Boolean | `false` | 调试模式 (记录原始数据包) |

---

## 为什么选择 Type.COMMON？

> [!NOTE]
> **设计决策**: 使用 `ModConfig.Type.COMMON` 而非 `Type.SERVER`

**理由**:

1. **Type.SERVER** 仅在专用服务器上加载，客户端无法访问。
2. **Type.COMMON** 在客户端和服务端都加载，配置文件共享。
3. MapBot 的配置 (WebSocket URL, 群号) 主要用于服务端逻辑，但如果玩家在单人模式下运行带有集成服务器的客户端，仍需要这些配置。
4. 使用 COMMON 确保了无论是专用服务器还是单人游戏（集成服务器），配置都能正确加载。

如果确定 MapBot **永远只在专用服务器运行**，可以改为 `Type.SERVER` 以获得更严格的隔离。

---

## 安全改进

1. **群号验证**: `InboundHandler` 现在会验证消息来源群号，忽略非目标群的消息。
2. **空配置保护**: 当 `targetGroupId = 0` 时，所有消息同步功能自动禁用。
3. **优雅关闭**: `BotClient.disconnect()` 方法确保服务器停止时正确关闭 WebSocket。

---

## Next Step Recommendation

配置系统已完成，所有硬编码已消除。

建议下一步:
1. **集成测试**: 启动 Minecraft 服务器验证配置文件自动生成
2. **功能扩展**: 实现更多 MapBotv4 的功能 (如 #help 命令)
