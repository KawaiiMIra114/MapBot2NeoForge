# MapBot Reforged - 全量项目审视报告

**日期**: 2026-01-17
**执行角色**: Luban (DeepMind Antigravity)
**状态**: ✅ 通过 (Passed with Recommendations)

---

## 1. 项目概览

MapBot Reforged 旨在将 Bukkit 平台的 MapBot 插件迁移至 NeoForge 1.21.1 模组平台。
目前项目处于 **早期开发阶段 (Early Development)**，核心架构已搭建完成，基础连接与绑定功能已上线。

| 维度 | 状态 | 评价 |
|------|------|------|
| **架构规范性** | ⭐⭐⭐⭐⭐ | 严格遵循分层架构 (Logic/Network/Data)，职责清晰 |
| **代码质量** | ⭐⭐⭐⭐⭐ | 使用 Java 21 新特性，标准库实现 WebSocket，无额外依赖 |
| **文档完整性** | ⭐⭐⭐⭐☆ | 拥有 `_AI_CONTEXT_MAP` 和 `.ai_rules`，治理极其规范 |
| **迁移进度** | ⭐⭐☆☆☆ | 约完成 20% 功能 (核心系统已就绪，业务功能待迁移) |

---

## 2. 深度技术分析

### 🏗️ 架构与构建
- **构建系统**: 使用 Gradle 8.8 + NeoForge 2.0.139，配置正确。
- **依赖管理**: **极其优秀**。项目未使用 `Java-WebSocket` 等外部库，而是采用 JDK 11+ 标准库 `java.net.http.HttpClient` 和 `WebSocket`，大大减少了 Jar 包体积和潜在冲突。
- **并发模型**: WebSocket 客户端使用单线程调度器 (`ScheduledExecutorService`) 处理重连，消息处理通过 `InboundHandler` 异步分发，避免了阻塞主线程。

### 🛡️ 核心安全性
- **白名单系统**: 已集成到绑定流程 (`#id`)，逻辑闭环。
- **权限管理**: 目前较为薄弱。`ServerStatusManager.stopServer` 标记了 `TODO` 权限检查。目前仅依赖 QQ 群管理权限（尚未完全实施代码级验证）。
- **线程安全**: `InventoryManager` 和 `ServerStatusManager` 正确处理了从非主线程访问 Minecraft 数据的问题（大部分读取操作是安全的，写入操作需注意并发，目前看似安全）。

---

## 3. 迁移差距分析 (MapBotV4 vs Reforged)

### ✅ 已迁移 (Implemented)
- **核心连接**: WebSocket 通信, 自动重连
- **账号体系**: ID绑定 (`#id`), 解绑 (`#unbind`), 白名单同步
- **服务器管理**: 状态查询 (`#tps`/`#status`), 在线列表 (`#list`), 停服 (`#stopserver`)
- **库存查询**: 查看背包 (`#inv`) - *[增强: 支持 1.21 DataComponents]*

### ⚠️ 待迁移 (Missing / Planned)
| 功能模块 | 优先级 | 说明 |
|----------|--------|------|
| **权限系统** | 🔥 High | 管理员名单管理 (`#addadmin`), 命令权限校验 |
| **经济系统** | 🟡 Medium | 余额查询, 转账 (原 `Pay`/`CheckMoney`) |
| **娱乐功能** | 🟢 Low | 网易云音乐, 一言, 天气, 猫图 |
| **群管功能** | 🟡 Medium | 禁言, 踢出 (需评估是否由 NapCat 直接处理而非 MC 处理) |
| **公用设施** | 🟢 Low | 获取邀请码, 世界新闻 |

---

## 4. 建议与行动计划

### 🛑 短期 (Fix Immediately)
1. **完善权限检查**: 在 `stopServer` 和敏感命令中完成 `TODO`，实现基于配置的管理员校验。
2. **配置热重载**: 移植 `#reload` 命令，允许在不重启服务器的情况下更新配置 (如 admin 列表)。

### 🚀 中期 (Next Phase)
1. **经济系统接入**: 确定 NeoForge 端的经济标准 (是自建还是接驳 Vault/其它模组)。
2. **丰富互动**: 逐步迁移娱乐功能，建议优先迁移 "签到" 或 "每日奖励" 类功能以增加用户粘性。

### 💡 长期 (Optimization)
- **跨服支持**: 当前架构支持单服。如需群服互通，需考虑 Redis 或数据库同步 (原 MapBotV4 有 Database 模块)。

---

**总结**: 项目基础非常扎实，代码风格现代化且规范。建议按照 "权限 -> 经济 -> 娱乐" 的顺序继续开发。
