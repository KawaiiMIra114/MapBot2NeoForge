# 阶段一·任务01：建立双端 Common 共享模块

**执行智能体**: `Aegis` (Alpha 端开发负责人)
**协同与权限限制**: 允许为提取协议目的跨界读取 `MapBot_Reforged` 目录的代码，但不允许修改游戏 Mod 具体实现逻辑。

## 一、 任务背景与目标
我们在诊断报告中确认了极其危险的“双重事实源 (SSOT) 违背”行为：Alpha 和 Reforged 端各自复制并维护着一致的通信载体甚至业务实体（例如两边都有极其相似的 `com.mapbot.network.*` 与 `com.mapbot.alpha.bridge.*`）。
为了打下后续解耦的基础，你的首要任务是**创建一个双方共用的 `Common` 库**，将 Bridge 底层协议（消息格式、错误码分配）合并，以此彻底消除因为单独更新某一方的 JSON 字段而造成的双端崩溃风险。

## 二、 工作输入（请务必先仔细阅读）
1. **全局统揽规约 (最高约束)**: `Project_Docs/MAPBOT_GLOBAL_PROTOCOLS.md`
   - 你必须随时回顾你在其中被定义的边界。
2. **具体架构合同**: 
   - 消息体规范: `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`
   - 错误码规范: `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`

## 三、 明确的修改指示 (Implementation Plan)
请按照以下步骤规划并实施你的修改：

### 1. 创建 Common 工程
- **位置**: 在代码仓库某个能同时被 Alpha 的 Gradle 和 Reforged 的 Gradle 引入的位置（推荐建一个并列的 `MapBot_Common` 文件夹或在根目录下配置跨项目 Gradle 引用）。
- **内容**: 构建干净无依赖（仅依赖基础序列化库如 `Gson`/`Jackson`）的标准 Java 库。

### 2. 提取并合并数据结构
- 将 `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/` 下关于封包结构的类（如 `BridgeMessage`）以及对应的 `MapBot_Reforged` 内的实体抽离至 `Common` 模块的 `com.mapbot.common.protocol` 包中。
- 集成并在枚举或常量类中反映 `BRIDGE_ERROR_CODE_CONTRACT.md` （例如抽出 `BridgeErrorCode` 常量表）。

### 3. 重塑两端的依赖
- 配置 `Mapbot-Alpha-V1` 与 `MapBot_Reforged` 的 `build.gradle`，使它们编译时 `implementation project(':MapBot_Common')`（或其他有效的本地发布引用方式）。
- 替换双端遗留的旧有独立实体文件引入（`import com.mapbot.network.BridgeMessage` -> `import com.mapbot.common.protocol.BridgeMessage`）。

## 四、 强制约束与验收标准
- **绝对底线 01**: 抽出模块时，**不能**带上与特定平台绑定的方法（比如 NeoForge 的 `ServerLifecycleHooks` 或 Alpha 独有的 `OneBotClient` WebSocket 逻辑）。Common 应当仅仅是一个纯粹的数据协议载体（POJO）。
- **绝对底线 02**: 在你执行 `gradlew build` 或 `build` Task 之前，禁止向 User 汇报完成。必须保证两端的编译均能通过。
- **任务产出**:
  - 完成任务后，请使用 `git commit` 保存工作。
  - 请在 `Project_Docs/Reports/rebuild/report/` 创建形如 `Aegis_Task01_Done.md` 的工作汇报，并在项目根目录的 `_AI_CONTEXT_MAP.md` 追加一条自己的上下文痕迹。
  - 当你在任务过程中遇到技术方向与本 Prompt 冲突或在 Gradle 配置上无法逾越时，请立即唤醒 `Nexus` 获取帮助。
