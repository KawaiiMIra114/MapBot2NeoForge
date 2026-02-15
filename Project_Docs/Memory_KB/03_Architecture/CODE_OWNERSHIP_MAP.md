# CODE_OWNERSHIP_MAP

## 1. 说明

- 目标：高频关键文件的能力边界、修改风险、验证建议。
- 适用：新模型接手时，优先按本表做最小改动与定向验证。
- `Step04-Blocker` 列：标记 Step04 阻断关键链路（权限/配置/reload/API鉴权）。

## 2. 高频关键文件地图

| 文件 | 对应能力 | 修改风险 | 验证建议 | Step04-Blocker |
|---|---|---|---|---|
| `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/MapbotAlpha.java` | Alpha 启动编排（Config/Redis/Auth/Data/OneBot/Bridge） | 启动顺序错会导致 Bridge 或鉴权失效 | 冷启动日志核对组件就绪顺序；检查端口监听 | 否 |
| `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/config/AlphaConfig.java` | Alpha 配置加载/保存/reload、端口归一化 | fail-open 回退可能掩盖配置错误；热重载一致性风险 | 注入非法值/未知键后 reload 结果与旧值保持性检查 | 是（配置/reload） |
| `Mapbot-Alpha-V1/config/alpha.properties` | Auth/Redis/群组/连接实配 | 键名误写会静默失效（当前实现） | 改动后执行 reload 并核对生效键 | 是（配置/鉴权） |
| `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/security/AuthManager.java` | Web Token、角色权限、Bridge 注册鉴权 | 角色模型变更会影响全 API；Bridge token 策略高敏 | 登录/鉴权/Bridge 注册负例回归（错误 token、非法 serverId） | 是（权限/API鉴权） |
| `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/network/HttpRequestDispatcher.java` | API 路由与鉴权拦截、WebSocket 升级鉴权 | 拒绝码不统一会破坏契约；越权副作用风险 | `/api/users*`、`/api/mapbot*`、`/api/config` 的 401/403 负例测试 | 是（API鉴权） |
| `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/command/CommandRegistry.java` | QQ 命令权限判定与执行门禁 | 命令越权或群组边界绕过 | user/admin 两类账号执行敏感命令差分测试 | 是（权限） |
| `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/command/impl/ReloadCommand.java` | `#reload` 编排入口（Config/Auth/子服） | 无事务回滚时易半提交 | 注入子服 reload 失败，检查 Alpha/子服配置一致性 | 是（reload） |
| `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/BridgeServer.java` | Bridge TCP 服务、首帧 register 强制鉴权 | 鉴权放松会导致未授权子服接入 | 错误 token 注册必须被拒绝且断连 | 是（API鉴权/Bridge） |
| `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/BridgeMessageHandler.java` | Bridge 入站消息分发（register/chat/event/proxy/data） | 类型分发错误会导致消息丢失/副作用 | 回放 register/heartbeat/proxy_response 样本帧 | 否 |
| `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/BridgeProxy.java` | Alpha->子服请求、fan-out、pending request 管理 | 超时/并发处理会引发错配响应 | 多服并发下验证 requestId 对应与 timeout 回收 | 是（reload链路） |
| `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/network/OneBotClient.java` | OneBot 收发与重连 | 连接抖动可能丢消息 | 断链重连测试 + 群消息收发烟测 | 否 |
| `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/data/DataManager.java` | bindings/perms/admins/mutes 主逻辑 + Redis 同步 | 本地与 Redis 双写不一致风险 | 执行 bind/mute/setperm 后校验 Redis 与本地快照 | 否 |
| `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/database/RedisManager.java` | Redis 连接池、执行与订阅 | 连接失败时降级路径不清晰 | Redis 断连恢复测试、订阅回调验证 | 否 |
| `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/network/RemoteFileApiHandler.java` | 远程文件 API 到 Bridge 代理 | 参数解析粗糙导致错误码漂移 | list/read/write/delete/mkdir/upload 全链路负例 | 否 |
| `MapBot_Reforged/src/main/java/com/mapbot/MapBot.java` | Reforged 生命周期入口（Bridge connect/disconnect） | 生命周期变更会造成重复连接或断连泄漏 | 服启动/停服流程日志与连接状态校验 | 否 |
| `MapBot_Reforged/src/main/java/com/mapbot/config/BotConfig.java` | Reforged 配置 schema（含 alphaToken） | 错配会造成 Bridge 无法注册 | 配置改动后验证 connect/register ack | 是（配置/Bridge鉴权） |
| `1/mapbot-common.toml` | Reforged 实际运行配置（alphaHost/alphaPort/alphaToken） | token/端口错误会阻断 Bridge | 改后重载并观察注册成功/拒绝原因 | 是（配置/Bridge鉴权） |
| `MapBot_Reforged/src/main/java/com/mapbot/network/BridgeClient.java` | Reforged Bridge 客户端（register/heartbeat/proxy） | 协议字段变更易破坏 Alpha 兼容 | register、heartbeat、reload_config、file_* 回归 | 是（reload/鉴权） |
| `MapBot_Reforged/src/main/java/com/mapbot/network/BridgeHandlers.java` | 子服侧请求执行器（命令/白名单/reload/file） | 执行副作用高，命令注入面广 | 针对 reload_config、file_write、switch_server 做负例 | 是（reload） |
| `Project_Docs/Re_Step/Artifacts/Step04/01_Command_Authorization_Matrix.md` | Step04 权限契约与差距清单 | 文档与实现脱节会误导修复优先级 | 每次权限改造后回填差距项状态 | 是（权限） |
| `Project_Docs/Re_Step/Artifacts/Step04/03_Config_Schema_Validation_Profile.md` | Step04 配置契约与验证剖面 | 未同步会让 gate 判定失真 | 配置改造后对照 PASS/WARN/FAIL 阈值复核 | 是（配置） |
| `Project_Docs/Re_Step/Artifacts/Step04/04_HotReload_Rollback_Audit_Flow.md` | Step04 reload/rollback 审计流程要求 | 缺失回滚设计会持续 gate fail | 每次 reload 链改造后按节点逐条验收 | 是（reload） |
| `Project_Docs/Re_Step/Artifacts/Step04/05_B2_Negative_Test_Cases.md` | Step04 反向用例与阻断条件 | 未执行负例会漏掉越权/回滚失败 | 按 N01~N05 至少跑一轮并留证据 | 是（权限/配置/API） |

## 3. Step04 阻断最小检查清单

1. 权限：`AuthManager` + `CommandRegistry` + `HttpRequestDispatcher`，确保拒绝路径统一且无副作用。
2. 配置：`AlphaConfig` + `alpha.properties` + `BotConfig/mapbot-common.toml`，确保非法/未知配置可阻断。
3. reload：`ReloadCommand -> AlphaConfig.reload -> AuthManager.reloadSecurityConfig -> BridgeProxy.reloadSubServerConfigs -> Reforged reload_config` 全链路可回归。
4. Bridge/API 鉴权：`BridgeServer` 首帧鉴权与 HTTP Bearer 鉴权同时验证负例。
