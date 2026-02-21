# MapBot_Reforged 初始产品验收缺陷报告 (First of All Report)

> **文档说明**：本报告仅针对 `MapBot_Reforged` (NeoForge 模组端) 进行代码审核，Alpha (中枢端) 的相关问题由另一智能体负责跟进。本报告梳理了当前项目中严重违反合作契约的设计缺陷、代码漏洞、安全隐患以及陈旧配置。

---

## 一、 契约违规与架构级缺陷 (Contract Violations)

### 1. 严重违反《命令授权契约》(COMMAND_AUTHORIZATION_CONTRACT)
- **角色标识未解耦与违规**：契约规定权限角色严格限制为 `user`、`admin`、`owner`。当前 `DataManager` 中使用的是旧有整数逻辑 `0 (User)`, `1 (Mod)`, `2 (Admin)`。不仅引入了不存在的 `Mod` 角色，还完全缺失了最高权限 `Owner` 角色。
- **命令分类校验缺失**：契约要求将命令分类为 `public_read`, `ops_write`, `sensitive_write` 等，并执行最小许可判定。目前 `ICommand` 和 `CommandRegistry` 仅执行单调的 `getRequiredLevel()` 整数比对。
- **安全拦截与惩罚机制缺失**：契约明确提出“5 分钟内 >= 5次越权触发速率限制与安全告警”。该机制在 `InboundHandler` 路由与 `CommandRegistry` 调度中均未实现。
- **越权静默处理不合规**：契约规定越权必须返回硬编码错误码 `AUTH-403`。当前代码返回中文文本语句（例如 `[权限拒绝] 此命令需要 Level 2...`）。
- **完全缺失审计日志体系 (Audit Log)**：契约规定义务性写入包括 `request_id`, `decision`, `caller_role` 的命令审计落盘日志。目前仅进行控制台 `LOGGER` 打印，不提供追溯性持久化方案。

### 2. 违反《数据一致性契约》(DATA_CONSISTENCY_CONTRACT)
- **并发控制缺陷 (无 CAS 机制)**：契约要求所有事实源实体更新必须携带 `expected_version` 与 `entity_version` 执行乐观并发控制。`DataManager` 当前毫无任何版本与版本冲突解决机制。
- **JSON 持久化极其脆弱**：`DataManager` 中的 `save()` 方法直接通过 `Files.writeString(dataPath, json)` 覆写源文件，面临着**断电必清空（0 字节）**的极高风险。相较之下，本项目中 `SignManager` 通过写入 `.tmp` 并 `AtomicMove` 的行为才是标准的，但底层规范不统一。

### 3. 未对齐《配置规范契约》(CONFIG_SCHEMA_CONTRACT)
- **安全配置字段缺失**：`BotConfig` 缺失了强制要求的 `schema.version` 控制与 `auth.owner_ids` 硬编码入口。

---

## 二、 潜在安全弱点与代码规范问题 (Vulnerabilities & Weaknesses)

### 1. 资源泄漏的隐式线程
- 在 `DataManager.cleanExpiredMute` 方法中，使用了 `new Thread(() -> {...}).start()`。这会创建完全脱离 NeoForge 生命周期及线程池管辖的独立野生线程，在大量高并发访问且带有长时阻塞时极易发生内存和 CPU 泄漏。此线程不会在服务器 `/stop` 钩子中优雅关闭。

### 2. 游戏物品本地化显示缺陷
- `LootConfig.java` 解析通用 `TAG` 类型的随机物品时，如果该 TAG 不包含手动设定的名称，代码会抽取 `id.getPath()`（例如 `diamond`、`iron_ingot`）。这直接发送给用户会导致无法被客户端多语言文件自动翻译的生硬英文名。应利用 Minecraft 原生组件转换器以提供本地化映射 `ResourceKey`。

### 3. 废弃的 API 调用与旧代码残留
- `MapBot.java` 启动钩子中使用了 `@Deprecated` 注解的 `BotConfig.getTargetGroupId()`。该行为未顺应最新配置拆分双群（Player Group 和 Admin Group）的要求。

---

## 三、 冗余与无意义代码 (Redundant Configs & Code)

> **修订批注**：经查证 Alpha 源码库，`transferHost` 和 `transferPort` 实际上被 Alpha Core 中的 `BridgeMessageHandler` (处理 `switch_server_request`) 强依赖作为跨服重定向目标池。它们 **不是** 冗余配置，先前对此处的定性有误，现予以纠正。

在“未来只通过 Alpha 中枢模式连接”的架构前提下，真正冗余的配置文件和代码如下：

### 1. 冗余的直连 WebSocket 客户端模块
- `MapBot_Reforged` 内部存在一个独立完整的 `com.mapbot.network.BotClient`。它的作用是直连 OneBot (NapCat)。由于双端统一采用 Alpha 通信，该类可以且应当被**彻底删除**。

### 2. 冗余的 BotConfig 字段
随 `BotClient` 的作废，以下配置文件属性在 `BotConfig.java` (Reforged 端) 中将再无任何实际调用，应当清理以对齐《配置规范契约》的最小必要原则：
- `wsUrl` 和 `reconnectInterval`：Alpha Core 已经接管并维护自身与 NapCat 的连接，不需要在子服重复配置。
- `playerGroupId` 和 `adminGroupId`：群消息路由鉴权现在由 Alpha 决定并透传，子服不再需要知道群号。
- `botQQ`：@机器人的检测也完全由 Alpha 完成，子服仅处理 Alpha 转发加工过的内部指令。
- *（注：相对应地，这些配置项在 `Mapbot-Alpha-V1` 的 `AlphaConfig.java` 中均被查证为有效使用，Alpha 端不存在对应的跨服冗余字段。）*

---

## 四、 后续建议实施计划

鉴于当前代码底层设施的不稳固，为了保障 MapBot Reforged 端的高效和健壮：
1. **P0 (基础设施)**：彻底重写 `DataManager` 到带有完整并发版本化和原子读写能力的存储引擎库；修正 `BotConfig` 与 `CONFIG_SCHEMA_CONTRACT` 的契约兼容性。
2. **P1 (权限引擎)**：实现并集成全新的 `AuthorizationEngine` 替换现有的 `CommandRegistry` 等级校验，满足 `AUTH-403`、惩罚机制、日志审计的全部诉求。
3. **P2 (业务修整)**：清理 `BotConfig` 中针对跨服的僵死配置，并修复游戏内 Loot 系统 TAG 抽取造成的英文显示等细节缺陷。消除在清理过期禁言时创建的僵死线程。
