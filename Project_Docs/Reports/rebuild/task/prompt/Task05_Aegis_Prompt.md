# 阶段二·任务05：Alpha 路由解耦与跨服通道建立

**接收智能体**: `Aegis` (Alpha 端开发负责人)

> **任务背景**：目前 Alpha 端收到 MC 各个子服的 Bridge 报文后，全靠 `BridgeMessageHandler.java` 中的一个巨型 `switch-case` 派发。同时，群聊转发强绑定在 `AlphaConfig` 的唯一单例 `playerGroupId` 上，无法支持多个子服对接多个不同 QQ 群（如生存在一群，小游戏在二群）。我们需要解耦路由，建立“策略派发模式”，并支持子服按需动态绑定目标群号。

---

## 📖 一、需要读取的上下文 (Read)
在进行任何改动之前，你**必须**首先读取并理解以下文件：
1. **全局规约**：`Project_Docs/MAPBOT_GLOBAL_PROTOCOLS.md`
   - 重点复习网络通信规范和单端计算原则。
2. **排期板当前状态**：`Project_Docs/Reports/rebuild/task/master_backlog.md`
   - 确认你领取的 Task05 的完整描述。
3. **Alpha 侧目标代码**：
   - 研读 `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/BridgeMessageHandler.java` 中的巨型 `switch(type)` 逻辑。
   - 研读 `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/config/AlphaConfig.java` 及 `ServerRegistry.java` 是如何处理 `playerGroupId` 的。

## 🔨 二、需要输出的工程结构 (Output)
你需要完成以下重构（在 `Mapbot-Alpha-V1` 内）：
1. **重构巨型 Switch 路由**：在 `com.mapbot.alpha.bridge` 下提取出一个 `BridgeRouteDispatcher`（或类似命名），采用策略模式/Handler注册表模式（如 `Map<String, BridgePacketHandler>`），把原来挤在 `BridgeMessageHandler` 中的逻辑拆分为独立的高内聚 Handler 类（例如 `ChatPacketHandler`, `PlaytimePacketHandler` 等）。
2. **打通动态群组绑定**：修改服务器注册握手逻辑 (`register` 包)。允许各个子服在连入 Alpha 时，在 JSON payloads 中声明自己期望绑定的 `targetGroupId`。
3. **重写群组消息派发**：改写聊天流转逻辑。当 Alpha 收到子服文本时，优先转发到该子服握手时声明的 `targetGroupId`；若未声明，才回退到 `AlphaConfig.getPlayerGroupId()`。
4. **增强跨服传送健壮性**：优化 `BridgeMessageHandler` 中的 `switch_server_request`，确保当源服务器和目标服务器的跨服通道失败时，能向源服务器准确、优雅地抛回错误，而不只是粗暴的 `FAIL:` 拼接。

## ⚠️ 三、必须遵守的纪律 (Constraints)
1. **单端原则**：这是 Alpha 端的任务，**严禁跨界修改 Reforged 端**的代码。哪怕需要新增跨端 JSON 字段（如 `targetGroupId`），你只能修改 Alpha 侧解析它的相关代码。如果迫切需要修改 Common 结构或要求 Reforged 发送它，请在任务末尾进行[向 Nexus 汇报]，由 Nexus 协调 Atlas 增加发包，你无权代劳。
2. **无损降级兼容**：现有的所有子服务若未发送新加的 `targetGroupId` 字段，绝对不能崩溃，必须默认回退到原有的单一群聊设计。
3. **禁止删改核心业务**：你只是正在“拆分重组” `BridgeMessageHandler` 中的内容，其中的**业务逻辑本身**（如 Redis 写入、签到转发）不可以被更改，只需将它们挪进独立的 Handler 中。
4. **编译红线**：修改完毕后，必须在 `Mapbot-Alpha-V1` 内独立通过 `gradlew build` 或逻辑分析确认不发生编译错误。
5. **遵循 Memorix 记忆规范**：任务完成后，必须通过 `memorix_store` 存储工作记忆（类型主要为 `decision` 及 `what-changed`）。

## 📝 四、最终回答与汇报模板 (Reporting Template)
当你完成该任务后，请**必须**按照以下模板向用户与 Nexus 汇报（生成单例报告文件，并在直接回答中总结）：

```markdown
### 任务完成汇报：[Aegis] Alpha 路由解耦与跨服通道建立

**1. 实施摘要**
- 取缔了巨型 `switch`，建立了 `BridgeRouteDispatcher` 及 [数量] 个独立的 PacketHandler。
- 修改了 `ServerRegistry.ServerInfo` 与握手逻辑，支持了以子服粒度动态维度的 `targetGroupId`。
- 修改了 `ChatPacketHandler`（原 `handleChat`），优先向子服定制群转发。
- 重构了跨服 `switch_server` 通道的异常返回机制。
- Alpha 侧 Gradle 编译验证：[说明验证结果]。
- Git Commit：[注明生成的 Commit 号]。

**2. 遭遇的兼容性调整 (选填)**
- [详述对于未发送专属群号的老版本子服的回退策略]。

**3. 对 Reforged 端的协调请求 (选填/极度重要)**
- 为使动态联群生效，Reforged 端需要在 `register` 包中增加发射 `targetGroupId`。请 Nexus 为 Atlas 安排微型调整任务。

**4. 上下文追溯更新**
- 我已在 `_AI_CONTEXT_MAP.md` 底部追加了格式化的历史锚点。
- 本次单例报告已落盘至 `Project_Docs/Reports/rebuild/report/Aegis_Task05_Done.md`。
- 我已通过 `memorix_store` 存储了本次工作记忆。

**5. 移交与接管建议**
- 路由结构改造完毕。Alpha 现已具备动态扩展包处理器和多服多群绑定的能力。
```
