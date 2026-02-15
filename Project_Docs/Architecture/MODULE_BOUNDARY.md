# MODULE_BOUNDARY

## 元数据
| 字段 | 值 |
| --- | --- |
| Doc-ID | A3-MODULE_BOUNDARY |
| Version | v1.0 |
| Status | Draft (Implemented-Alignment) |
| Owner | Knowledge Base Subtask A3 |
| Last-Updated | 2026-02-14 |
| Scope | Alpha 中枢 + Reforged 子服 + Bridge + OneBot + Redis |
| Source-of-Truth | `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/**`, `MapBot_Reforged/src/main/java/com/mapbot/**` |

## 1. 目标与范围（强制）
- 定义模块职责、输入输出、依赖方向与禁止依赖。
- 约束“控制面（Alpha）-执行面（Reforged）-数据面（Redis）”的边界，避免回退到单体耦合。

## 2. 架构事实（强制）
### 2.1 模块清单
| 模块 | 主要职责 | 主要输入 | 主要输出 |
| --- | --- | --- | --- |
| `M1 EdgeGateway` | `25560` 协议分流、静态资源/API/WS、MC TCP 代理 | HTTP/WS/MC TCP | API 响应、WS 帧、转发后的 MC TCP |
| `M2 AuthAccess` | Web token、RBAC、Bridge 注册鉴权 | 登录请求、Bearer Token、Bridge register 首帧 | 认证结果、角色判定、register allow/deny |
| `M3 CommandPlane` | OneBot 入站解析、命令分发、QQ 回执 | OneBot 事件、`#command` | 对 M4/M5 的调用、OneBot 回复 |
| `M4 BridgeControl` | 子服注册/心跳、RPC 下发、回执关联、跨服 fan-out | 来自 M3/M1 的桥接请求，来自子服的 `proxy_response` | 执行结果、在线服务器视图 |
| `M5 StatePlane` | bindings/mutes/perms/admins/playtime 主状态管理 | 命令动作、子服上报（如 `playtime_add`） | Redis/本地持久化、状态查询接口 |
| `M6 ReforgedBridgeAdapter` | Reforged 与 Alpha 的 TCP 会话、请求处理、事件上报 | Alpha 下发请求、游戏事件 | `proxy_response`、heartbeat/chat/event |
| `M7 ReforgedExecution` | 游戏内命令执行、在线玩家判定、事件监听 | 来自 M6 的执行请求，游戏事件总线 | 命令执行结果、游戏事件数据 |
| `M8 OneBotPlatform`(外部) | QQ 协议适配 | Alpha 动作请求 | OneBot 事件 |
| `M9 Redis`(外部) | 跨节点共享状态 | M5 读写 | 状态读写与发布订阅 |

### 2.2 模块与代码映射（核心）
- `M1`: `ProtocolDetector`, `MinecraftProxyHandler`, `HttpRequestDispatcher`, `LogWebSocketHandler`
- `M2`: `AuthManager`
- `M3`: `OneBotClient`, `InboundHandler`, `CommandRegistry`, `command/impl/*`
- `M4`: `BridgeServer`, `BridgeMessageHandler`, `BridgeProxy`, `ServerRegistry`, `BridgeFileProxy`
- `M5`: `DataManager`, `PlaytimeStore`, `RedisManager`
- `M6`: `MapBot_Reforged/network/BridgeClient`, `BridgeHandlers`
- `M7`: `GameEventListener`, `ServerStatusManager`, `InventoryManager`, `PlaytimeManager`

## 3. 边界定义（强制）
### 3.1 依赖方向（必须）
- Web/API 路径: `M1 -> M2 -> M4/M5`
- QQ 命令路径: `M3 -> M4/M5 -> M6 -> M7`（群组/权限校验由 `M3` + `M5` 协作完成）
- Bridge 回执路径: `M7 -> M6 -> M4 -> M3/M1`
- 外部依赖: `M3 <-> M8`，`M5 <-> M9`
- 关键原则: Alpha 内部模块可组合；Reforged 只能通过 Bridge 与 Alpha 交换控制语义。

### 3.2 禁止依赖（必须）
- `M7 -> M9` 禁止: Reforged 执行层不得直接读写 Redis 主状态。
- `M6/M7 -> Alpha.DataManager` 禁止: 不允许绕过 Bridge 直接访问 Alpha 内部类。
- `M6/M7 -> OneBot(在 Alpha 模式)` 禁止: `alphaHost` 已配置时不得直连 OneBot 处理业务命令。
- `M1 -> M5` 直写禁止: HTTP/WS 层不得绕过 `M2` 鉴权后直接改业务状态。
- `M4 -> M8` 禁止: Bridge 控制层不得直接承担 QQ 机器人职责。
- `M3 -> Reforged 内部类` 禁止: 命令层只能经 `M4` 访问执行面。

## 4. ASCII 图（强制）
```text
            +------------------- External -------------------+
            |                                                |
            |  OneBot(M8) <------> M3 CommandPlane          |
            |  Redis(M9) <-------> M5 StatePlane            |
            +------------------------------------------------+

M1 EdgeGateway --> M2 AuthAccess --> M3 CommandPlane --> M4 BridgeControl --> M6 BridgeAdapter --> M7 ReforgedExecution
      |                  |                    |                   |                    |                    |
      +------------------+--------------------+-------------------+--------------------+--------------------+
                           (Alpha Core control/data boundary)                         (Reforged execute boundary)
```

## 5. 状态与不变量（强制）
- `M5` 是业务主状态写入者；`M6/M7` 只允许短 TTL 缓存与事件上报。
- `M4` 的 `pendingRequests` 与 `M6` 的 `pendingRequests` 都必须以 `requestId` 关联并可超时清理。
- `M4.ServerRegistry` 仅代表“当前连接态”，不应被当作持久化事实。
- `M1` 的 `/api/*` 与 `/ws` 必须走 token 校验，权限决策统一由 `M2`。

## 6. 异常与降级（强制）
| 模块故障 | 影响 | 降级路径 |
| --- | --- | --- |
| `M8` 不可达 | QQ 收发失败 | 命令执行与 Bridge 仍可运行，等待重连 |
| `M9` 不可达 | 跨节点共享丢失 | `M5` 回退本地文件/内存 |
| `M6` 断连 | 子服离线 | 从 `ServerRegistry` 移除，命令返回离线/超时 |
| `M1` 异常 | Web/MC 入口受影响 | Bridge 端口 `25661` 不依赖 `25560`，可保持子服控制链路 |

## 7. 待澄清项（强制）
- `Reforged BridgeHandlers` 中仍存在旧路径（如 `handleSignIn/handleAcceptReward` 依赖本地 `DataManager`），需明确是否彻底退役。
- HTTP `/api/servers/{id}/command` 当前为“发送即成功”语义，不等待执行回执，和 WS/QQ 通道不一致。

## 8. Known Gaps/Transition Plan（强制）
| Gap | Priority | 影响模块 | 问题描述 | 最小过渡方案 | 可观测指标（阈值） |
| --- | --- | --- | --- | --- | --- |
| G1-UnbindWhitelist | P0 | `M3/M4/M6/M7` | 解绑后执行面白名单未收敛 | 增加 `unbind -> whitelist_remove fan-out` 模块链路 | `whitelist_remove_success_ratio >= 99.9%`；`unbind_orphan_whitelist_count == 0` |
| G2-ApiCommandAck | P0 | `M1/M4/M6` | Web API 命令语义弱于 QQ/WS | 将 `M1` 的命令 API 改为等待 `M4` 真回执 | `api_command_false_success_count == 0/day`；`api_command_ack_p95_ms < 2000` |
| G3-ReforgedLegacyPath | P1 | `M6/M7` | Alpha 模式下仍有本地业务态 handler | 对旧 action 加模式门禁并迁移到 Alpha 统一路径 | `legacy_action_calls_in_alpha_mode == 0`；`local_business_state_write_count == 0/day` |
| G4-StaticAnonExposure(F4) | P0 | `M1/M2` | 管理静态页匿名暴露导致边界漂移 | 执行“登录页匿名、管理页鉴权”统一规则 | `protected_page_anonymous_200_count == 0/day`；`protected_route_auth_block_rate >= 99.99%` |

## 9. 一致性锚点（强制）
- 本文的依赖方向、禁止依赖、优先级与指标阈值必须与 `SYSTEM_CONTEXT.md` 第 8/9/10 章一致。
- 数据流语义（成功/超时/部分成功/重试）必须与 `DATA_FLOW_AND_STATE.md` 的失败态时序一致。

## 10. Gap 闭环映射（强制）
| Gap | 模块闭环责任 | 关闭判据引用 | 无法关闭降级引用 |
| --- | --- | --- | --- |
| G1-UnbindWhitelist | `M3/M4/M6/M7` | `SYSTEM_CONTEXT.md` 第 11 章 | `SYSTEM_CONTEXT.md` 第 11 章 |
| G2-ApiCommandAck | `M1/M4/M6` | `SYSTEM_CONTEXT.md` 第 11 章 | `SYSTEM_CONTEXT.md` 第 11 章 |
| G3-ReforgedLegacyPath | `M6/M7` | `SYSTEM_CONTEXT.md` 第 11 章 | `SYSTEM_CONTEXT.md` 第 11 章 |
| G4-StaticAnonExposure(F4) | `M1/M2` | `SYSTEM_CONTEXT.md` 第 11/13 章 | `SYSTEM_CONTEXT.md` 第 11 章 |
