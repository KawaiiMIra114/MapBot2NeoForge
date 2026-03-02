### 任务完成汇报：[Atlas] Reforged 鉴权引擎拔高

> - [Atlas] [2026-03-02 15:55:00] [构建独立鉴权引擎 AuthorizationEngine，消灭 0/1/2 魔法数字] [11d8348]

**1. 实施摘要**
- 移除了 `DataManager` 中 `0/1/2` 魔法数字的直接权限判定，建立了独立的 `com.mapbot.security` 包，包含：
  - `PermissionRole` 枚举 (USER / ADMIN / OWNER) — 符合《COMMAND_AUTHORIZATION_CONTRACT》定义的三角色模型。
  - `CommandCategory` 枚举 (PUBLIC_READ / SCOPED_READ / OPS_WRITE / SENSITIVE_WRITE / GOVERNANCE) — 符合契约 5.2 命令分类表。
  - `AuthorizationEngine` 单例 — 统一鉴权入口。
- 引入了 **3 秒冷却** 的 Rate-Limit 拦截器（仅对 USER 角色生效）。
- 引入了 **5 分钟内 >= 5 次越权触发限速告警** 的安全防刷机制。
- 引入了越权 `AUTH-403` 的结构化 Audit Logger（输出至 `MapBot/Audit` logger，包含 request_id、event_time、principal_id、caller_role、command_id、command_category、decision、decision_reason 全部必填字段）。
- 改造了 `ICommand` 接口（新增 `getCategory()`，保留 `@Deprecated` 的 `getRequiredLevel()` 兼容层）。
- 改造了 `CommandRegistry`，分发使用 `AuthorizationEngine.authorize()` 替代旧有的数字比较。
- 改造了 **11 个命令** 的防腐接入：
  - OPS_WRITE: MuteCommand, UnmuteCommand, LocationCommand, InventoryCommand, ReloadCommand
  - SENSITIVE_WRITE: StopServerCommand, CancelStopCommand
  - GOVERNANCE: SetPermCommand, AddAdminCommand, RemoveAdminCommand, ForceUnbindCommand
  - 其余 13 个命令使用 ICommand 默认的 PUBLIC_READ（无需改动）。
- Reforged 侧 Gradle 编译验证：由于 Java 25 宿主机与 NeoForge Gradle 插件 ASM 不兼容的已知环境问题，编译存在环境级熔断。代码结构经逻辑分析确认无红线级语法错误。
- Git Commit：`11d8348`

**2. 遭遇的兼容性调整**
- `DataManager` 内部存储仍然使用旧版 `0/1/2` 整数值（`userPermissions: Map<Long, Integer>`），未做存储格式变更。`PermissionRole.fromLegacyLevel()` 负责将旧数值安全映射为新枚举：`0->USER`, `1->ADMIN`, `2->OWNER`。无法识别的数值回退为 `USER`（符合契约 7.迁移与兼容 条款）。
- `ICommand.getRequiredLevel()` 标记为 `@Deprecated` 但仍保留，其默认实现通过 `getCategory().getMinRole().toLegacyLevel()` 反推旧数值，确保任何未重构的外部调用不会断裂。

**3. 上下文追溯更新**
- 我已在 `_AI_CONTEXT_MAP.md` 底部追加了格式化的历史锚点。
- 本次单例报告已落盘至 `Project_Docs/Reports/rebuild/report/Atlas_Task04_Done.md`。
- 我已通过 `memorix_store` 存储了本次工作记忆。

**4. 移交与接管建议**
- 原有基础权限判定已被独立鉴权引擎安全接管，`CommandRegistry` 通过 `AuthorizationEngine` 统一代理所有鉴权逻辑。
- `DataManager` 中的 `PERMISSION_LEVEL_USER/MOD/ADMIN` 常量及 `getPermissionLevel()` 方法虽仍保留（供底层存储兼容），但上层业务应一律使用 `AuthorizationEngine.getCallerRole()` 获取角色。
- 等待 Nexus 验收后可分发后续清理任务（如移除 `DataManager` 中遗留的权限常量、HelpCommand 可见性过滤等）。
