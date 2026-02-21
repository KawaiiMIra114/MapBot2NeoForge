# Mapbot Alpha V1 阶段性缺陷与漏洞审计报告

**文档元数据**
- **创建时间**: 2026-02-21
- **所属模块**: `Mapbot-Alpha-V1` (独立中枢层)
- **审查人**: 负责 Alpha 模块的智能体

> **注**: 本报告专精于 `Mapbot-Alpha-V1` 核心组件的架构审计，关于 `MapBot_Reforged` (服务端 Mod) 的部分，交由对应的负责智能体输出，双方协同推进重构基线。

---

## 摘要 (Executive Summary)
在针对先期完成的 Alpha Core 基础设施进行全面代码跑查与协议映射 (`RE_STEP_03_B1`) 后，发现当前实现虽然在网络基础通道（Netty）与硬性安全防御上达标，但在**领域业务边界、代码重用度、核心职能完整度**上存在严重的设计偏移与隐患，亟待在 Rebuild 阶段修正。

---

## 1. 架构级缺陷 (Architecture Vulnerabilities)

### 1.1 灾难性的业务逻辑重叠 (Violating SSOT)
**表现**: 在 `Mapbot-Alpha-V1` 项目的 `com.mapbot.alpha.logic.*` 包下，存在几乎与 `MapBot_Reforged` 项目全量照搬的代码副本。包含不限于 `DataManager`, `SignManager`, `InboundHandler`, `PlaytimeStore`。
**危害风险**: 
- **极高**。这是极度危险的双重事实源（Dual Source of Truth）。当 Alpha 和 Reforged 均持有签到逻辑与玩家数据管理逻辑时，任何一端的规则变动（如签到奖励池变更、时区更改）若未进行双边同步代码修改，将导致严重的数据不一致和行为异常。
- 业务边界极其模糊，Alpha 应该作为纯粹的网关/分发中心，还是承接所有游戏外逻辑的运算节点，当前代码体现出摇摆不定。

### 1.2 核心职能弃用静默 (Abandoned Core Function)
**表现**: `MapbotAlpha.java` 主入口中，原本负责拉起并守护 Minecraft 服务端生命周期的 `ProcessManager.INSTANCE.startServer(...)` 调用被人为注释。
**危害风险**:
- **高**。Alpha 的立项初衷之一就是成为脱离 MC 独立存活的监控面板与守护进程。在失去 `ProcessManager` 的实际调用后，一旦 Minecraft 服务器因 OOM 或其它异常崩溃，Alpha 将毫无察觉，且完全丧失了崩溃重启与故障接管的能力，退化为一个单纯的消息转发中间件。

---

## 2. 设计与耦合级缺陷 (Design & Coupling Issues)

### 2.1 Bridge 通道过度硬编码 (Hardcoded Routing)
**表现**: `BridgeMessageHandler` (处理来自 Reforged 端的消息路由类) 中，使用了冗长的 `if-else / switch` 链来识别并处理具体的业务（如 `handleCheckMute`, `handleGetQqByUuid`, `handlePlaytimeAdd` 等）。
**危害风险**:
- **中高**。这违反了开闭原则 (OCP)。每一次协议的扩充（比如新增一个积分兑换业务），都要求修改这个底层的通信 Channel Handler，极易在合并分支或迭代中引入不可控的上下文冲撞。

### 2.2 虚假的热加载支持 (Fake Hot-Reload)
**表现**: `AlphaConfig.java` 虽然提供了具有事务特性的 `reload()` 方法，但对于网络通信层面的核心挂载点（例如 Redis 的连接池更新、甚至 WebSocket 长连接重连）未能实现真正的无缝平滑切块。
**危害风险**:
- **中**。管理员通过控制台触发 reload 命令认为配置已生效，但实际上底层 Netty Pipeline 或 Redis 客户端并未以新参数热重启，导致表象与实质割裂。

---

## 3. 配置与环境冗余 (Redundancy in Environment)

### 3.1 废弃的 TOML 配置文件 (已紧急清理)
**表现**: 目录内曾提供 `src/main/resources/application.toml`，但在引擎启动阶段（`AlphaConfig`）仅读取 `config/alpha.properties`。
**危害风险**: 
- **低 (已解决)**。极易误导协作者修改虚假配置，该问题已在本次审查行动中主动移除文件。
### 3.2 歧义的命名空间
**表现**: `alpha.properties` 直接挪用了旧版 `BotConfig` 词汇（例如 `messaging.adminGroupId`，`botQQ`）。
**危害风险**: 
- **低**。如果 Alpha 定位成为多服务器集群网关，其不再应该强行与单一的“玩家群”、“管理员群”绑定，这些属性应随未来路由转发机制重构被解耦或重新设计层级。

---

## 4. 下一步整改建议 (Proposed Fix Actions)
给接下来的开发/重构计划提供的抓手：
1. **剥离出 Common 模块**：提取 Alpha 与 Reforged 共享的底层数据结构（Message/Protocol），削减代码复制。
2. **清理本地业务态**：让 Alpha 变为无状态的数据中转与分发网关，签到查分等强业务运算全部交由 Reforged 服务端计算后将封包返回 Alpha 转发，或反之，但必须唯一归属。
3. **修复守护进程**：恢复并强化 `ProcessManager` 的作用。
