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
   - `daemon.command` (字符串，必须是 `.bat`, `.cmd` 等可执行脚本路径，如 `run.bat`。如果后缀不合法，需在启动前提出警告或拒接启动。)
2. **改写 ProcessManager 弹出独立终端**：
   - 用户的真实操作系统是 **Windows**。要求在启动 MC 服务端时，**单独打开一个新的终端窗口**来显示控制台。
   - 对策：你需要将启动命令拼接为 `cmd.exe /c start /WAIT "MapBot-Minecraft-Server" <daemon.command>`。
   - 关键说明：使用 `start /WAIT` 能够让 Java 的 `serverProcess.waitFor()` 正常阻塞，直到那个新弹出的黑窗口被关闭，从而不破坏你的崩溃重启判定。
3. **改造守护与退避逻辑**：当 `serverProcess.waitFor()` 返回（即黑窗口关闭后），判断如果 `AlphaConfig.isDaemonEnabled()` 仍为 true，则在经过一定的退避时间（例如 5~10 秒，防止秒崩导致的无限刷屏弹窗）后，自动重新拉起进程。
4. **生命周期绑定**：在 `MapbotAlpha.java` 的 ShutdownHook 中，确保 Alpha 进程关闭时，如果有办法就一起关闭那个弹窗子进程（可选，尽力而为即可）。

## ⚠️ 三、必须遵守的纪律 (Constraints)
1. **防死循环重启体验**：弹窗式启动一旦陷入死循环会产生极度恶劣的体验（满屏弹黑窗口）。你**必须**实现崩溃时间检测：如果是10秒内的瞬间退出，应触发较长延迟（比如等待30秒）或者直接终止守护并报错，绝不能无限极速弹窗。
2. **I/O 捕获的妥协**：由于弹出了独立终端，原来的 `ProcessBufferedReader` 将无法再捕获到内容，Web 端也收不到日志。你需要把原本的 `captureLog` 逻辑废弃或注释掉，并在日志中明确打印：“MC 服务器已在独立窗口运行，Alpha 不再接管控制台输入输出”。
3. **纯 Alpha 侧任务**：该任务完全属于 Alpha 侧的网络和进程控制，严禁修改 Reforged 端的任何代码。
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
