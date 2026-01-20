# Task #017 执行报告: 双向 @ 互通

**执行者**: Lazarus
**日期**: 2026-01-20
**状态**: ✅ 完成

---

## 任务目标

实现游戏与 QQ 群之间的双向 @ 提及互通，解决游戏内输入长 QQ 昵称不便的问题，并增强消息通知体验。

---

## 变更内容

### 1. QQ @ 游戏 (QQ -> Game)

**修改文件**: `InboundHandler.java`

- **新增功能**: 声音提醒
- **逻辑**: 当群消息包含 `[CQ:at]` 且目标玩家在线时，除了原有的 Title 通知和高亮文本外，额外播放 `SoundEvents.EXPERIENCE_ORB_PICKUP` 音效。
- **效果**: 玩家不仅能看到屏幕上的大字提示，还能听到清脆的提示音，防止漏看消息。

### 2. 游戏 @ 群友 (Game -> QQ)

**修改文件**: `GameEventListener.java`

- **新增功能**: 智能 `@ID` 解析
- **逻辑**: 
    1. 监听 `ServerChatEvent`。
    2. 使用正则 `^@([a-zA-Z0-9_]{3,16})(\s.*)?$` 捕获以 `@` 开头的消息。
    3. 提取目标游戏名 (例如 `@Notch`)。
    4. 调用 `server.getProfileCache().get(targetName)` 获取 UUID。
    5. 查询 `DataManager` 绑定数据，找到对应的 QQ 号。
    6. 将消息中的 `@Notch` 替换为 `[CQ:at,qq=12345]` 发送到 QQ 群。
- **优势**: 玩家无需输入复杂的 QQ 昵称，只需输入对方的游戏 ID 即可触发真实的 @ 通知。

---

## 技术细节

| 功能 | 实现类 | 关键 API |
|------|--------|----------|
| 声音通知 | `InboundHandler` | `player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, ...)` |
| 档案查询 | `GameEventListener` | `server.getProfileCache().get(name)` |
| 绑定反查 | `GameEventListener` | `DataManager.INSTANCE.getQQByUUID(uuid)` |

## 测试用例

1. **QQ @ 玩家**:
   - 群内发送: `@Steve 别挖了`
   - 游戏内 Steve: 收到 Title 提示 + "叮"的一声 + 聊天栏高亮。

2. **玩家 @ QQ**:
   - 游戏内发送: `@Notch 上号！`
   - 后台逻辑: 解析 Notch -> 查找 UUID -> 查找 QQ -> 替换 CQ 码。
   - QQ 群显示: `[Steve] @Notch(CQ码) 上号！` (Notch 手机收到强提醒)。

---

**签名**: Lazarus - MapBot Reforged 开发执行者
