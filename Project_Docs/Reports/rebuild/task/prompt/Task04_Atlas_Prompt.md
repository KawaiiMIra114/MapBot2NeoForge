# 阶段二·任务04：Reforged 鉴权引擎 (AuthorizationEngine) 拔高

**接收智能体**: `Atlas` (Reforged 端开发负责人)

> **任务背景**：目前命令的权限判定硬编码在 `DataManager` 中，采用了简陋的 `0(User)/1(Mod)/2(Admin)` 魔法数字。各个 `ICommand` 实现类在执行时强耦合这种数字判断，且无操作防刷限制（Rate-Limit）和越权操作记录（Audit Log）。这不符合《命令授权契约》。我们需要构建一个独立的鉴权引擎，拔高安全防御层级。

---

## 📖 一、需要读取的上下文 (Read)
在进行任何改动之前，你**必须**首先读取并理解以下文件：
1. **全局规约**：`Project_Docs/MAPBOT_GLOBAL_PROTOCOLS.md`
   - 特别关注第一章职责边界，以及第三章编码安全准则。
2. **命令授权契约**：`Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md`（如果存在，请依据此文件的最新约束进行重构）
3. **排期板当前状态**：`Project_Docs/Reports/rebuild/task/master_backlog.md`
   - 确认你领取的是阶段二的 Task04。
4. **Reforged 侧目标代码**：
   - 研读 `MapBot_Reforged/src/main/java/com/mapbot/data/DataManager.java` 中现有的 `getPermissionLevel`, `isAdmin`, `0/1/2` 等相关逻辑。
   - 研读 `MapBot_Reforged/src/main/java/com/mapbot/command/CommandRegistry.java` 和各个实现类（如 `StopServerCommand`、`MuteCommand` 等）是如何调用权限判定的。

## 🔨 二、需要输出的工程结构 (Output)
你需要完成以下目标（在 `MapBot_Reforged` 内）：
1. **剥离鉴权逻辑**：创建一个独立的 `com.mapbot.security.AuthorizationEngine`（或类似命名）类，接管所有的权限判定。
2. **消灭魔法数字**：引入枚举 `PermissionRole { USER, MOD, ADMIN, OWNER }` 等取代现有的 `0/1/2`。改造 `DataManager` 的底层存储使得其向新枚举结构对齐（注意做好旧版数据的平滑迁移或兼容读取）。
3. **引入防刷机制 (Rate-Limit)**：在鉴权引擎中，针对普通用户的命令调用频率进行限制。例如 3 秒内不得连发指令，触发告警。
4. **引入审计日志 (Audit Log)**：对任何触发高级权限（MOD及以上）的命令执行，或是越权失败的尝试日志（输出为 `AUTH-403`），进行专门的日志记录（可使用现有的 `LOGGER` 输出为特定格式）。
5. **改造全量 Command**：修改 `CommandRegistry` 以及现有的 `ICommand` 实现类，让它们全部改为向新的 `AuthorizationEngine` 申请通行证，而非直接粗暴比较数字。

## ⚠️ 三、必须遵守的纪律 (Constraints)
1. **单端原则**：这是 Reforged 端的任务，**严禁跨界修改 Alpha 端**的代码（不要碰跨端协议定义和 Alpha 的配置）。
2. **安全向下兼容**：考虑到 `DataManager` 涉及到磁盘文件（以前用 `0/1/2` 保存在 JSON 里），你的反序列化逻辑必须能够安全读取旧格式并映射到新枚举。
3. **禁止破坏其他功能**：只改发包所涉的安全鉴权逻辑。签到、绑定、白名单等现有正常业务的**逻辑本身不可改动**，只改它们【运行前的权限检查那一步】。
4. **编译红线**：修改完毕后，必须在 `MapBot_Reforged` 内独立通过 `gradlew build` 或逻辑分析确认不会有红线级代码结构编译错误（由于 Java 25 宿主机环境导致的 Gradle+NeoForge ASM 插件环境级熔断报错可暂行放通）。
5. **遵循 Memorix 记忆规范**：任务完成后，必须通过 `memorix_store` 存储本次工作记忆（类型 `what-changed` 和 `decision`，如果是填平旧数据兼容的坑可记 `problem-solution`）。

## 📝 四、最终回答与汇报模板 (Reporting Template)
当你完成该任务后，请**必须**按照以下模板向用户与 Nexus 汇报（生成对应的新汇报文件并在回答里总结）：

```markdown
### 任务完成汇报：[Atlas] Reforged 鉴权引擎拔高

**1. 实施摘要**
- 移除了 `DataManager` 中的 `0/1/2` 魔法数字，建立独立的 `AuthorizationEngine` 和 `PermissionRole` 枚举。
- 引入了 [具体描述规则，如：3秒冷却] 的 Rate-Limit 拦截器。
- 引入了越权 `AUTH-403` 的 Audit Logger。
- 改造了 `CommandRegistry` 及 [数量] 个命令的防腐接入。
- Reforged 侧 Gradle 编译验证：[说明验证结果]。
- Git Commit：[注明生成的 Commit 号]。

**2. 遭遇的兼容性调整 (选填)**
- [详述对旧有 JSON 存储 `0/1/2` 数据的反序列化兼容处理策略]。

**3. 上下文追溯更新**
- 我已在 `_AI_CONTEXT_MAP.md` 底部追加了格式化的历史锚点。
- 本次单例报告已落盘至 `Project_Docs/Reports/rebuild/report/Atlas_Task04_Done.md`。
- 我已通过 `memorix_store` 存储了本次工作记忆。

**4. 移交与接管建议**
- 原有基础权限判定已被安全引擎接管，等待鉴权验收。
```
