# 阶段三·任务08：找回 Alpha 进程守护荣光

**接收智能体**: `Aegis` (Alpha 端开发负责人)

> **任务背景**：在早期的重构中，我们在 `MapbotAlpha.java` 中注释掉了 `ProcessManager.INSTANCE.startServer(...)` 以便独立测试两者。现在我们需要将 Alpha 端变回真正的“中枢管控网关”。Alpha 应当具备“进程守护 (Daemon)” 能力：根据配置决定是否启动及拉起本地的 Minecraft Server 进程，并能在 MC 进程崩溃时自动重启它。

---

## 📖 一、需要读取的上下文 (Read)
在进行任何改动之前，你**必须**首先读取并理解以下文件：
1. **全局规约**：`Project_Docs/MAPBOT_GLOBAL_PROTOCOLS.md`
2. **排期板当前状态**：`Project_Docs/Reports/rebuild/task/master_backlog.md`
   - 确认你领取的 Task08 的完整描述。
3. **目标代码**：
   - 研读 `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/MapbotAlpha.java` (找到被注释的 `ProcessManager` 调用)。
   - 研读 `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/process/ProcessManager.java` (研究如何将 `waitFor` 转变为守护循环)。
   - 研读 `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/config/AlphaConfig.java` (准备添加新的配置项)。

## 🔨 二、需要输出的工程结构 (Output)
你需要完成以下重构（全部在 `Mapbot-Alpha-V1` 内）：
1. **配置驱动设计**：在 `AlphaConfig.java` 中增加如下守护进程专属配置：
   - `daemon.enabled` (布尔，默认 `false`)
   - `daemon.workDir` (字符串，默认 `./server`)
   - `daemon.command` (字符串，默认 `bat/sh` 脚本路径或 `java -Xmx8G -jar neoforge.jar nogui`)
2. **改写 ProcessManager**：改造 `startServer` 为守护模式。当 `serverProcess.waitFor()` 返回后，判断如果配置了 `daemon.enabled`，则在经过一定的退避时间（例如 5~10 秒，防止秒崩导致的无限死循环）后，自动重新拉起进程。
3. **恢复入口调用**：在 `MapbotAlpha.java` 被注释的地方，恢复调用。但要求改为读取配置：如果 `AlphaConfig.isDaemonEnabled()` 为 true，则调用 `ProcessManager.INSTANCE.startServer(workDir, command)`。
4. **生命周期绑定**：在 `MapbotAlpha.java` 的 `Runtime.getRuntime().addShutdownHook` 中，确保当 Alpha 进程被强杀或正常退出时，清理拉起的 MC 子进程（如 `ProcessManager.INSTANCE.stopServer()`，可以尝试发 stop 或直接 destroy），防止留存孤儿进程。

## ⚠️ 三、必须遵守的纪律 (Constraints)
1. **防死循环重启**：如果目标路径下没有 jar 包，或者 Java 版本不对，MC 会瞬间退出。如果你直接 `while(true)` 重启，会导致 CPU 满载并疯狂刷屏。必须设置**退避逻辑**（如崩溃间隔小于特定阈值则终止守护，或固定延迟 10 秒后重试）。
2. **纯 Alpha 侧任务**：该任务完全属于 Alpha 侧的网络和进程控制，严禁修改 Reforged 端的任何代码。
3. **保留原有的流捕获能力**：`ProcessManager` 中原有的对 `stdout/stderr` 的捕获、日志历史记录 (`logHistory`) 及 Web 端广播 (`LogWebSocketHandler`) 必须无损保留。
4. **编译与记录**：修改完毕后，确保 `Mapbot-Alpha-V1` 独立编译无报错。并通过 `memorix_store` 存储工作记忆。

## 📝 四、最终回答与汇报模板 (Reporting Template)
当你完成该任务后，请**必须**按照以下模板在主回答中汇报（并存入单例文件 `Aegis_Task08_Done.md`）：

```markdown
### 任务完成汇报：[Aegis] Alpha 进程守护荣光复苏

**1. 实施摘要**
- 在 `AlphaConfig` 添加了 `daemon.*` 配置组，实现配置驱动的守护控制。
- 改造了 `ProcessManager`，实现守护循环。增加了 [具体数值] 秒的崩溃退避延迟，防止死循环。
- 在 `MapbotAlpha.java` 启用了基于配置的自启动检测，并在 ShutdownHook 中增加了子进程释放逻辑（防孤儿）。
- Alpha 端编译验证：[说明验证结果]。
- Git Commit：[注明生成的 Commit 号]。

**2. 核心机制说明**
- [简要说明守护循环的具体实现，如如何判断是正常停止还是崩溃异常，如何执行重启退避]。

**3. 上下文追溯更新**
- 本次报告已落盘至 `Project_Docs/Reports/rebuild/report/Aegis_Task08_Done.md`。
- 工作记事已同步至 Memorix。

**4. 移交与接管建议**
- 进程守护功能已完成！你可以告知用户在 `alpha.properties` 中开启 `daemon.enabled=true` 来测试崩溃重启拦截了。
```
