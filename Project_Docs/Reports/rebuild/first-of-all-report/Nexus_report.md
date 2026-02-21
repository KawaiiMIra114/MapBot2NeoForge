# MapBot 项目综合双端缺陷与漏洞全景审计报告

> **文档说明与元数据**
- **创建时间**: 2026-02-21
- **所属模块**: `Mapbot-Alpha-V1` (中枢层) 与 `MapBot_Reforged` (NeoForge 模组层)
- **审阅基础**: 本报告无损整合了双端的初期代码跑查与协议映射 (`RE_STEP_03_B1`) 结果。梳理了当前项目中严重违反合作契约的设计缺陷、双端业务职责重叠、代码级漏洞、安全隐患以及长期遗留配置污染。

---

## 一、 架构级缺陷与违背 SSOT / 契约 (Architecture & Contract Violations)

### 1. 灾难性的业务逻辑重叠 (Violating SSOT) - [Alpha 端聚焦]
- **表现**: 在 Alpha 项目的 `com.mapbot.alpha.logic.*` 包下，存在几乎与 Reforged 项目全量照搬的代码副本，包含 `DataManager`、`SignManager`、`InboundHandler`、`PlaytimeStore`。
- **危险等级 (极高)**: 这是极度危险的**双重事实源 (Dual Source of Truth)**。双方均持有签到逻辑与玩家数据管理，任何一端的业务规则变动若未同步代码级修改，将直接导致严重的数据不一致。这也暴露出 Alpha 在“纯粹分发网关”和“业务运算节点”间的定位摇摆。

### 2. 严重违反《命令授权契约》 (COMMAND_AUTHORIZATION_CONTRACT) - [Reforged 端聚焦]
- **角色标识未解耦与违规**: 契约规定权限角色严格限制为 `user`、`admin`、`owner`。`DataManager` 却仍在使用旧有整数逻辑 `0 (User)`, `1 (Mod)`, `2 (Admin)`，凭空引入了 `Mod` 且缺失了最高特权 `Owner`。
- **命令分类校验缺失**: 契约要求命令必须分离 `public_read`, `ops_write` 等，且进行最小许可判定。目前 `CommandRegistry` 仅执行单调的 `getRequiredLevel()` 整数比对。
- **安全拦截与惩罚机制缺失**: 未实现契约规定的“速率限制与安全告警”（5分钟内 >= 5次越权自动预警）。
- **越权静默处理不合规**: 未返回硬编码错误码 `AUTH-403`，而是采用中文文本语句抛出警示。
- **审计日志体系 (Audit Log) 缺失**: 缺少命令路由义务写入，未对 `request_id`, `decision`, `caller_role` 落地做持久化追溯，仅存在控制台打印。

### 3. 多服群组绑定的架构级定死 (Hardcoded Global Routing) - [Alpha 端聚焦]
- **表现**: `alpha.properties` 全局写死了唯一的 `playerGroupId` 和 `adminGroupId`。在所有 QQ 消息流经 Alpha 时，消息被无差别广播给所有连接的 Reforged 子服。
- **危险等级 (高)**: 这彻底违背了未来拓展为**多服中枢**的设计初衷。当引入 `survival`、`creative` 等多个独立服时，这种硬编码将导致跨服消息链路彻底混乱和串流。

### 4. 核心职能弃用静默 (Abandoned Core Function) - [Alpha 端聚焦]
- **表现**: `MapbotAlpha.java` 主入口中，原负责拉起并守护 Minecraft 进程的 `ProcessManager.INSTANCE.startServer(...)` 被硬编码注释。
- **危险等级 (高)**: 失去守护职能后，一旦 MC 服务器因 OOM 崩溃，Alpha 将毫无察觉并完全丧失拉起重连机制，退化为无生命周期的中间件转发机器。

### 5. 违反《数据一致性契约》 (DATA_CONSISTENCY_CONTRACT) - [Reforged 端聚焦]
- **无 CAS 乐观并发机制**: 实体更新完全丢失 `expected_version` 控制。
- **JSON 持久化极其脆弱**: `DataManager.save()` 单纯使用原生文件覆写 (`Files.writeString`) 操作，高并发和断电场景极易造成 0 字节清空。与之形成鲜明对比的是 `SignManager` 拥有的 `.tmp` 原子移动标准实现。

### 6. 未对齐《配置规范契约》 (CONFIG_SCHEMA_CONTRACT) - [Reforged 端聚焦]
- **缺失安全字段**: `BotConfig` 缺失 `schema.version` 以及 `auth.owner_ids` 的强制接入口。

---

## 二、 设计结构与代码耦合缺陷 (Design & Implementation Weaknesses)

### 1. Bridge 通道过度硬编码 (Hardcoded Routing) - [Alpha 端]
- **表现**: 负责网关路由的 `BridgeMessageHandler` 对业务的识别极其生硬，堆砌了巨大的 `if-else / switch` 链（如 `handleCheckMute`, `handleGetQqByUuid` 等）。
- **危害级别 (中高)**: 破坏开闭原则 (OCP)。未来每次微小业务的扩展都极易干扰并阻塞底层 Netty 协议链。

### 2. 虚假的热加载支持 (Fake Hot-Reload) - [Alpha 端]
- **表现**: `AlphaConfig.java` 虽提供 `reload()`，但在网络协议底座挂载点（如 Redis 池初始化、WebSocket 常驻监听）未能实现真正的无缝平滑卸载与重连。
- **危害级别 (中)**: 表象欺诈，管理员键入重载指令后，后台仍以老配置驻留连接。

### 3. 资源泄漏的隐式僵死线程 - [Reforged 端]
- **表现**: `DataManager.cleanExpiredMute` 使用了即用即抛式的 `new Thread(() -> {...}).start()`。脱离线程池管理与游戏心跳周期管辖，关闭服务端时不会进行优雅回收，极易堆积。

### 4. 物品本地化显示缺陷 - [Reforged 端]
- **表现**: `LootConfig` 对未命名的通用 `TAG` 物品抽取 `id.getPath()`，发给用户将呈现由于缺少多语言文件关联的死板全英文（如 `diamond`）。

### 5. 废弃的 API 调用 - [Reforged 端]
- **表现**: `MapBot.java` 中依然存有对已标注为 `@Deprecated` 的旧配置方法调用（如请求拉取 target 群组）。

---

## 三、 配置与环境冗余重灾区 (Redundancy & Misdirection)

### 1. Reforged 端的历史遗留配置重度污染 (Cross-project Misdirection)
在“未来有且仅有连接 Alpha 运行”的大前提下，Reforged 端内包含了极其误导的冗余存留物：
- **大量僵死配置字段**: `BotConfig.java` 目前仍留有 `wsUrl`, `playerGroupId`, `adminGroupId`, `botQQ`, `reconnectInterval`。这些在采用 Alpha 前置的情况下已经完全交接给了 Alpha，留在子服只会引发使用者的双开故障与幻觉，应直接砍掉。*(注：Alpha端已查证不存在跨服主机的冗余变量，它们实质用在 `switch_server` Bridge 请求上，前期定论已拨乱反正。)*
- **多余的 WebSocket 重连库**: 内部依然持有用于直接沟通 NapCat 的 `BotClient` 网络组件类，这一整套逻辑链应被彻底裁撤。

### 2. Alpha 端的歧义命名空间
- **表现**: 核心配置文件 `alpha.properties` 挪用了过去单服版的死属性单词 (如 `messaging.adminGroupId`)。多服网关架构中应立刻对集群与子频道映射做字典化重新设计。*(另外：废弃的 `application.toml` 文件在之前动作中已查清和清理)*

---

## 四、 综合整改建议与落地规划 (Proposed Fix Actions)

为迅速消除架构层面的死锁和高压危害，建议依照以下执行路线图实施大修：

1. **P0 (底层设施与 SSOT 收敛)**：
   - 抽出 `Common` 模块聚合双端的协议通讯规范与数据载体结构，干掉全部重复轮子。
   - 彻底剪除 Alpha 内业务运行相关的多余拷贝存储组件，让其回归无状态的中枢转发信使。所有运算回归且仅归属单端计算。
   - 彻底覆写 Reforged 端的 `DataManager` 引擎，引入严密的读写锁、CAS 更新、与原子落地写入特性。
2. **P1 (权限引擎与网络网关升级)**：
   - 为 Reforged 端置换新的 `AuthorizationEngine` 拦截层，干掉老式整数 `Level` 比对，提供详尽的 Audit Logger。
   - 为 Alpha 的 `BridgeMessageHandler` 提供面向接口或注解的策略派遣模式，取缔大型 switch 链块。
   - 设计并铺设多服的独立群落路由系统，打下分布式节点基座。
3. **P2 (应用业务层清洁)**：
   - 对 `BotConfig` 和 `BotClient` 进行清扫，清除冗余字段。
   - 补齐 Alpha 内遭隐没的守护进程启动能力，修正假装的 Reload 热加载。
   - 修复 Mute 死锁线程问题以及游戏物品 TAG 多语言解析失灵的弱点。
