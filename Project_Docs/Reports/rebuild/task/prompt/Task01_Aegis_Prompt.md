# 阶段一·任务01：建立双端 Common 共享模块

**接收智能体**: `Aegis` (Alpha 端开发负责人)

> **任务背景**：诊断报告指出存在极度危险的“双重事实源 (SSOT)”，Alpha 和 Reforged 端各自复制着一致的通信载体。为了后续解耦，你需要将 Bridge 底层协议（封包、错误码）合并为双方共用的 `Common` 库。

---

## 📖 一、需要读取的上下文 (Read)
在进行任何改动之前，你**必须**首先读取并理解以下文件：
1. **全局规约**：`Project_Docs/MAPBOT_GLOBAL_PROTOCOLS.md` (明确你的权限与纪律)
2. **错误与消息合同**: 
   - `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`
   - `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`
3. **现有代码结构**: 
   - 阅读 `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/` 下的封包类。
   - 允许跨界读取 `MapBot_Reforged/` 内对应的网络实体类以供分析（但不可破坏 Reforged 内游戏构建规则）。

## 🔨 二、需要输出的工程结构 (Output)
1. **新建 Common 库**: 在根目录建立 `MapBot_Common` 模块（无特定框架绑定，仅含基础 JSON 相关依赖）。
2. **抽取统一协议包**: 产出 `com.mapbot.common.protocol` 包，包含聚合双端的 `BridgeMessage`, `BridgeErrorCode` 等 POJO。
3. **改造双端依赖**: 调整 Alpha 与 Reforged 的 `build.gradle` 及 `settings.gradle`，使其合法引入新生成的 Common 工程；并将原先项目内部导入的旧引用替换为 `com.mapbot.common.protocol.*`。

## ⚠️ 三、必须遵守的纪律 (Constraints)
1. **纯净提取法则**: Common 模块**绝对禁止**混入诸如 NeoForge `ServerLifecycleHooks` 或 Alpha `OneBotClient` 等特定平台逻辑。
2. **编译红线**: 你必须在执行 `gradlew build` 且两端（含 Common 本身）**全部编译成功**之后，才允许宣告任务完成。
3. **冲突熔断机制**: 如果你在 Gradle 跨项目配置或者提取逻辑中遇到阻碍当前任务的死锁，必须立即暂停并向 `Nexus` 或 User 汇报。
4. **统一提交**: 修改验证全绿后执行 `git commit`。

## 📝 四、最终回答与汇报模板 (Reporting Template)
当你完成该任务后，请**必须**按照以下模板向用户与 Nexus 汇报（生成对应的新汇报文件并在回答里总结）：

```markdown
### 任务完成汇报：[Aegis] 抽取 Common 模块

**1. 实施摘要**
- 创建了 `MapBot_Common` 库。
- 抽离并统一了以下核心协议实体：[列出核心类名]。
- 双端 Gradle 编译结果：[说明测试是否全数通过]。
- Git Commit：[注明生成的 Commit 号]。

**2. 遭遇的兼容性调整 (选填)**
- [如果在合并实体时发现原先的微小差异并作了调整，在此处简要说明]。

**3. 上下文追溯更新**
- 我已在 `_AI_CONTEXT_MAP.md` 底部追加了格式化的历史锚点。
- 本次单例报告已落盘至 `Project_Docs/Reports/rebuild/report/Aegis_Task01_Done.md`。

**4. 移交与接管建议**
- 共享层已就位，呼叫 Nexus / Atlas 开启下一进程。
```
