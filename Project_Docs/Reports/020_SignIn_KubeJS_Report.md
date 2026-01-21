# Task #020 执行报告: 每日签到与 KubeJS 联动

**执行者**: Lazarus
**日期**: 2026-01-20
**状态**: ✅ 完成

---

## 任务目标

实现 `#sign` 每日签到功能，并提供与 KubeJS 脚本的深度联动能力，允许服主自定义奖励逻辑。

---

## 变更内容

### 1. 每日冷却机制 (`DataManager`)

*   **新增字段**: `Map<Long, String> lastSignIn` (存储 `QQ -> yyyy-MM-dd`)。
*   **逻辑**: `SignCommand` 执行前检查 `hasSignedInToday(qq)`，如果已签到则直接拒绝，防止刷取奖励。

### 2. KubeJS 联动机制 (`SignCommand`)

*   **事件总线**: 使用 `NeoForge.EVENT_BUS.post(event)` 抛出 `MapBotSignInEvent`。
*   **可取消性**: 事件实现了 `ICancellableEvent`。
*   **逻辑流**:
    1.  玩家执行 `#sign`。
    2.  MapBot 抛出事件。
    3.  **KubeJS** 监听到事件 -> 发放自定义奖励 -> `event.setCanceled(true)`。
    4.  MapBot 检查 `event.isCanceled()` -> 如果为 true，跳过保底奖励；如果为 false，发放金苹果。

### 3. 保底奖励

*   如果服务器未安装 KubeJS 或脚本未处理事件，MapBot 将默认发放 **1x 金苹果**。

---

## KubeJS 使用指南

已在 `Project_Docs/Manuals/KubeJS_Example.js` 生成示例脚本。

**安装方法**:
1.  将示例脚本复制到服务器的 `kubejs/server_scripts/` 目录下。
2.  执行 `/reload` 重载脚本。

**脚本片段**:
```javascript
ForgeEvents.on('com.mapbot.event.MapBotSignInEvent', event => {
    event.setCanceled(true) // 阻止保底
    event.player.give('minecraft:diamond') // 发钻石
})
```

---

**签名**: Lazarus - MapBot Reforged 开发执行者
