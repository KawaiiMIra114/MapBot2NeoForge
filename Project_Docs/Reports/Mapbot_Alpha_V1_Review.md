# Mapbot-Alpha-V1 产品验收审查报告

**审查时间**：2026-02-21  
**审查对象**：`Mapbot-Alpha-V1` 项目源码与其对应契约文档（`RE_STEP_03_B1`等）

---

## 1. 冗余与重复配置项 (Configurations)

- **严重冗余文件**：`src/main/resources/application.toml`。该文件在当前 Alpha 的启动生命周期 (`AlphaConfig.java`) 中完全未被读取。真正作为配置承载核心的是 `config/alpha.properties`。这种双重存在的配置范式极易导致后继维护者配置了 `.toml` 却发现无响应的困惑。
- **配置属性重复命名空间**：`alpha.properties` 完全拷贝自服内的 `BotConfig` 设计。虽然是历史移植遗留，但 Alpha 作为中心节点，其本身的 `messaging.adminGroupId` 等存在一定的语义漂移风险。

👉 **修复方案**：
1. **彻底清理** 删除 `src/main/resources/application.toml`。
2. 在 `README.md` 中增加强调说明：Alpha Core 仅使用 `config/alpha.properties` 进行环境配置。

## 2. 架构缺陷与弱点 (Architecture Weaknesses)

1. **逻辑层的严重冗余复制 (Code Duplication)**
   在项目大洗牌整合中，出现了大量的代码复制：Alpha 的 `com.mapbot.alpha.logic.*`（如 `SignManager`、`DataManager`、`InboundHandler`）和主项目内的 `com.mapbot.logic.*` 几乎存在全量对称的双份。
   - **弱点**：这违背了单一真相源原则（Single Source of Truth, SSOT）。如果需要修改签到防作弊逻辑，维护者不得不在服务端和 Alpha 分别改写两份。

2. **强耦合的 Bridge 处理器分支**
   `BridgeMessageHandler` 内部充满大量的 `handleCheckMute`、`handleGetQqByUuid` 这类定制化的 `if-else / switch` 硬编码指令。这让“消息处理器”不再是一个通用桥接网络。若有新增通信业务需要改变甚至仅仅是拓展，极易导致巨大的耦合方法块。

3. **异常吞噬和进程守护静默失败**
   在 `MapbotAlpha.java` 主类中：
   ```java
   // 5. (可选) 启动 MC 服务器进程
   // ProcessManager.INSTANCE.startServer("./MapBot_Reforged/run", "java -Xmx2G -jar server.jar nogui");
   ```
   原本设计的 `ProcessManager` 监控进程完全处于被注释的闲置状态。此时意味着所谓 Alpha V1 设计草稿中最重要的“守护并重启MC”的设计理念，已经被实际执行中回退。这会导致服务器崩毁后，Alpha **无力干预和恢复**游戏服。

## 3. 代码契约与合规度 (Code Contracts)

- ✅ **安全规范对齐**：契约中明确要求的超长数据包截断 `BRG_VALIDATION_205`，在源码中由 `BridgeFrameSizeGuardHandler` (捕获 `TooLongFrameException`) 精准实现了切面拦截和错误码分发。此部分非常规范。
- ⚠️ **重载机制问题**：对于 `AlphaConfig.java`，存在方法 `reload()` 使用了热更新，但若是对核心网络端口（如更改 Redis / 替换 Web 端口等）做 `reload()` 实际上并不能热拔插 Netty Server 的已连接 Channel 参数池，这会导致“虚假的热加载成功”。

## 4. 修复与优化指南 (Action Plan)

1. **清理环境**：立即删除 `application.toml` 这一“无主亡魂”，消灭歧义。
2. **抽取公共依赖模块** (Common Library)：将 `DataManager` 等游戏-Alpha公共的模型、协议单独拉取做一个 `mapbot-common` 模块供主服务端和 Alpha 层双向引用。
3. **回归核心职能**：启用或完整解耦丢弃 `ProcessManager`。如果是只做外部网络分流器，就剔除管理主进程的代码；如果是全栈控制面板（结合`dashboard`），则应把启动 MC 服务端的职能正式补全修复。
