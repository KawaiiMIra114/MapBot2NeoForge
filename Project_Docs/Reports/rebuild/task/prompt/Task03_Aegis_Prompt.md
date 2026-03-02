# 阶段一·任务03：Alpha 业务越权肃清 (单端计算原则)

**接收智能体**: `Aegis` (Alpha 端开发负责人)

> **任务背景**：Alpha 的定位是**纯网关**——它只负责连接 OneBot、转发消息、管理多服接入。但现在 Alpha 内部存在一个 `logic` 包，里面保存着 `InboundHandler`、`PlaytimeStore`、`SignManager` 等本应完全由 Reforged 端处理的业务副本代码。这违反了「单端计算原则」和 SSOT 契约，必须清除。

---

## 📖 一、需要读取的上下文 (Read)
在进行任何改动之前，你**必须**首先读取并理解以下文件：
1. **全局规约**：`Project_Docs/MAPBOT_GLOBAL_PROTOCOLS.md`
   - 特别关注第一章智能体职责边界和第三章编码安全准则。
2. **数据一致性合同**：`Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`
   - 关注关于 SSOT（单一事实源）的约定。
3. **排期板当前状态**：`Project_Docs/Reports/rebuild/task/master_backlog.md`
   - 确认你领取的是任务 03，并了解前置依赖（Task01/02 已完成）。
4. **Alpha 侧目标代码**：
   - 研读 `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/logic/` 目录下的所有文件。
   - 研读 `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/command/impl/` 下所有引用了 `logic` 包的命令实现。
   - 研读 `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/data/DataManager.java`，了解其与 Reforged 端 DataManager 的职责是否存在重叠。

## 🔨 二、需要输出的工程结构 (Output)
你需要完成以下目标：
1. **删除或清空 `logic` 包中的越权业务代码**：`InboundHandler.java` 中属于业务计算的部分需要剥离（注意：消息路由与转发功能是 Alpha 网关的正当职责，**不能删**）。`PlaytimeStore.java` 和 `SignManager.java` 这类纯业务逻辑应被彻底移除。
2. **修复因删除 logic 类导致的所有编译断裂**：所有引用了被删除类的 `command/impl/` 下的命令，需要改为通过 Bridge 将请求**转发给 Reforged** 处理，而非 Alpha 自行本地计算。
3. **Alpha 侧 DataManager 瘦身**：如果 Alpha 的 `DataManager` 包含了与 Reforged 端重复的签到、在线时长等业务数据存储，需要将这些冗余字段移除。Alpha 的 DataManager 应当只保留 Alpha 自身需要的数据（如管理员列表、基础配置等网关层面的东西）。

## ⚠️ 三、必须遵守的纪律 (Constraints)
1. **保留网关功能**：`InboundHandler` 中的消息接收、路由分发、OneBot 协议解析、转发到 Minecraft 等**网关核心功能严禁删除**。你清除的是「Alpha 自己做业务计算」的部分，不是「Alpha 收发消息」的部分。
2. **不碰 Reforged 端**：此任务你只负责清理 Alpha 侧。严禁牵涉对 `MapBot_Reforged/` 下任何文件的修改。
3. **不碰 Redis 基建**：`RedisManager` 是 Alpha 的正当基础设施（用于跨服通信），不在此次清理范围内。
4. **编译红线**：修改完毕后，必须在 `Mapbot-Alpha-V1` 内通过 `gradlew build` 编译验证（环境级 Java 25 报错除外，参照前例放行）。
5. **遵循 Memorix 记忆规范**：任务完成后，必须通过 `memorix_store` 存储本次工作记忆（类型 `what-changed`），以及过程中遇到的任何踩坑（类型 `gotcha`）。详见 `AGENT_DISPATCH_RULES.md` 第 5-6 章。

## 📝 四、最终回答与汇报模板 (Reporting Template)
当你完成该任务后，请**必须**按照以下模板向用户与 Nexus 汇报（生成对应的新汇报文件并在回答里总结）：

```markdown
### 任务完成汇报：[Aegis] Alpha 业务越权肃清

**1. 实施摘要**
- 清除了 Alpha 侧 `logic` 包中的 [具体列出删除/修改的文件]。
- 将 [具体命令] 从本地计算改为 Bridge 转发模式。
- Alpha 侧 Gradle 编译验证：[说明验证结果]。
- Git Commit：[注明生成的 Commit 号]。

**2. 遭遇的兼容性调整 (选填)**
- [如果在处理引用断裂时做了包装或桥接暂留策略，在此简要说明]。

**3. 上下文追溯更新**
- 我已在 `_AI_CONTEXT_MAP.md` 底部追加了格式化的历史锚点。
- 本次单例报告已落盘至 `Project_Docs/Reports/rebuild/report/Aegis_Task03_Done.md`。
- 我已通过 `memorix_store` 存储了本次工作记忆。

**4. 移交与接管建议**
- Alpha 网关净化完毕，业务越权代码已清除。等待 Nexus 验收后进入阶段二。
```
