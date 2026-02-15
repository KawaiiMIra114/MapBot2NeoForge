# SYSTEM_CONTEXT

## 元数据
| 字段 | 值 |
| --- | --- |
| Doc-ID | A3-SYSTEM_CONTEXT |
| Version | v1.0 |
| Status | Draft (Implemented-Alignment) |
| Owner | Knowledge Base Subtask A3 |
| Last-Updated | 2026-02-14 |
| Scope | Alpha 中枢 + Reforged 子服 + Bridge + OneBot + Redis |
| Source-of-Truth | `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/**`, `MapBot_Reforged/src/main/java/com/mapbot/**` |

## 1. 目标与范围（强制）
- 定义系统与外部系统的交互边界。
- 明确信任边界、端口职责、以及 `25560` 智能分流口的语义。
- 本文不展开模块内部算法，模块内部细节见 `MODULE_BOUNDARY.md` 与 `DATA_FLOW_AND_STATE.md`。

## 2. 架构事实（强制）
### 2.1 外部系统清单
| 外部系统 | 角色 | 与本系统交互 | 默认/关键接口 |
| --- | --- | --- | --- |
| QQ 用户/管理员 | 业务发起方 | 发送群消息/命令，接收回执 | 通过 OneBot 转接 |
| OneBot (NapCat) | 消息网关 | Alpha 通过 WS 主动连接；收发 QQ 事件/动作 | `connection.wsUrl`（Alpha） |
| Web 浏览器 | 管理面客户端 | 访问静态页、调用 `/api/*`、连接 `/ws` | 入口 `25560` |
| Minecraft 玩家客户端 | 游戏流量端 | 连接游戏入口 | 入口 `25560`（被转发到目标 MC） |
| Reforged 子服 | 执行面 | 通过 Bridge 向 Alpha 注册、心跳、执行请求 | `bridge.listenPort`（Alpha，默认 `25661`） |
| Redis | 状态共享存储 | Alpha 持久化/同步 bindings、mutes、permissions、playtime、auth | 默认 `6379`（可配置） |

### 2.2 系统角色划分
- Alpha: 控制面与数据主存（命令编排、鉴权、数据落盘/Redis、跨服调度）。
- Reforged: 执行面（游戏事件捕获、指令执行、状态采集、结果回传）。
- Bridge: 控制面与执行面的双向 RPC 通道（`requestId`-`proxy_response` 关联）。

## 3. 边界定义（强制）
### 3.1 信任边界
- `TB-1` 公网/局域网入口边界: `25560` 接收 HTTP/WS 与 MC TCP，输入默认不可信。
- `TB-2` Bridge 控制边界: `25661` 仅接受通过 `serverId + token` 注册的子服（白名单 `allowedServerIds`）。
- `TB-3` Bot 边界: OneBot 事件内容不可信，需通过 Alpha 的命令鉴权与群组校验。
- `TB-4` 数据边界: Redis 为共享状态层；Redis 不可用时 Alpha 回退本地文件/内存。
- `TB-5` 执行边界: Reforged 只执行 Alpha 下发动作与上报事件，不持有最终业务真相。

### 3.2 端口职责（含 `25560` 智能分流）
| 端口 | 所属 | 职责 | 安全要点 |
| --- | --- | --- | --- |
| `25560` | Alpha | 智能分流入口: `ProtocolDetector` 按首字节判定 HTTP 或 MC；HTTP 走 Web/API/WS，非 HTTP 走 MC 代理 | API/WS 需 token；输入默认不可信 |
| `25570` (默认) | 本机 MC 真实端口 | Alpha 将 MC TCP 代理到该端口（`MinecraftProxyHandler`） | 不应暴露成控制面 API |
| `25661` | Alpha | Bridge 服务端口，供 Reforged 注册/心跳/RPC | 首帧必须 `register` 且通过 token+serverId 鉴权 |
| `wsUrl` 配置端口 | OneBot | Alpha 到 OneBot 的 WebSocket 客户端连接 | 连接失败重连，不代表命令执行失败 |
| `6379` (默认) | Redis | 跨节点状态共享 | 可关闭，关闭后降级为单节点本地状态 |

## 4. ASCII 图（强制）
```text
                        +--------------------+
                        |  QQ Users/Admins   |
                        +---------+----------+
                                  |
                                  | OneBot v11 events/actions (WS)
                                  v
+------------------+       +------+----------------------+
| Web Browser      |------>|  Alpha Core                |
| (HTTP/API/WS)    | 25560 |  - ProtocolDetector        |
+------------------+       |  - Inbound/Command/Auth    |
                           |  - BridgeServer :25661      |
+------------------+       |  - DataManager/Playtime    |
| MC Client        |------>|  - OneBotClient            |
| (Minecraft TCP)  | 25560 |  - RedisManager            |
+------------------+       +------+-----------+----------+
                                  |           |
                                  | Bridge    | Redis protocol
                                  | JSON/TCP  |
                                  v           v
                           +------+----+   +--+--------+
                           | Reforged |   |   Redis    |
                           | SubServer|   | (optional) |
                           +-----------+   +-----------+
```

## 5. 状态与不变量（强制）
- Alpha 是绑定、权限、禁言、在线时长的主写入方（Redis 优先）。
- Reforged 在 Alpha 模式下不应作为 OneBot 主控，不应直接成为业务主存。
- Bridge 请求必须带 `requestId`，回执必须以 `proxy_response` 匹配完成。
- `ServerRegistry` 是在线态缓存，不是持久化事实库。

## 6. 异常与降级（强制）
| 场景 | 行为 |
| --- | --- |
| Bridge 鉴权失败（token/serverId 不匹配） | Alpha 拒绝注册并断开连接，子服不进入在线集 |
| Redis 不可用/未启用 | Alpha 回退到本地文件与内存，失去多节点共享 |
| OneBot 断连 | Alpha 重连；桥接/数据流可继续，QQ 回执可能延迟或失败 |
| `25560` 收到非 HTTP 且非 MC 的流量 | 仍按 MC 代理路径处理（协议探测容错有限） |

## 7. 待澄清项（强制）
- 当前静态资源可匿名访问，是否需要为管理页本身增加登录门禁。
- `25560` 分流算法为首字节启发式，若未来引入更多协议需升级探测策略。

## 8. Known Gaps/Transition Plan（强制）
| Gap | Priority | 问题描述 | 最小过渡方案 | 可观测指标（阈值） |
| --- | --- | --- | --- | --- |
| G1-UnbindWhitelist | P0 | `#unbind` 只解绑 Alpha 绑定，不做跨服 `whitelist_remove` | 在 Alpha 增加 `unbind -> uuid/name -> fan-out whitelist_remove -> 聚合回执` | `whitelist_remove_success_ratio >= 99.9%`；`orphan_whitelist_entries == 0` |
| G2-ApiCommandAck | P0 | `/api/servers/{id}/command` 当前“发送即成功”，不等待子服执行回执 | 改为统一走 `BridgeProxy.sendRequestAsyncToServer(execute_command)`，返回真实 `proxy_response` | `api_command_false_success_count == 0/day`；`api_command_timeout_ratio < 1%` |
| G3-ReforgedLegacyPath | P1 | Reforged 仍保留本地业务态路径（`sign/accept/playtime`） | Alpha 模式下禁用旧 action 或回退为 deprecated，统一走 Alpha 主状态 | `legacy_action_calls_in_alpha_mode == 0`；`local_business_state_write_count == 0/day` |
| G4-StaticAnonExposure(F4) | P0 | 管理静态页匿名可达，存在控制面暴露面 | 仅登录页匿名；管理页与控制台资源强制鉴权 | `protected_page_anonymous_200_count == 0/day`；`protected_route_auth_block_rate >= 99.99%` |

## 9. F4 边界规则（强制）
### 9.1 匿名允许（Allow Anonymous）
- `POST /api/login`
- `GET /login.html`
- `GET /assets/*`（仅登录页静态依赖）
- `GET /vite.svg`（登录页依赖时可放行）

### 9.2 必须鉴权（Require Token）
- `GET /`、`GET /index.html`、`GET /users.html`
- `GET /vue/*`（管理控制台页面）
- `GET/POST/PUT/DELETE /api/*`（除 `/api/login`）
- `GET /ws`（WebSocket 升级）

### 9.3 规则语义
- 规则 R-F4-1: 控制面页面默认拒绝匿名，只有登录入口可匿名。
- 规则 R-F4-2: 资源级放行不等于数据级放行；`/api/*` 永远优先鉴权判定。
- 规则 R-F4-3: 匿名访问受保护页面时，必须返回 `401/403` 或重定向到登录页，不允许 `200`。

## 10. 跨文档一致性断言（强制）
- A01: `25560` 在三文档中必须统一定义为“智能分流入口”，不得写成 Bridge 专口。
- A02: `25661` 必须统一定义为 Bridge 专口，且需 `serverId + token` 注册鉴权。
- A03: Alpha 必须是 bindings/mutes/perms/playtime 的主写入方，Reforged 不得声明主写权限。
- A04: Bridge 请求-回执必须统一 `requestId <-> proxy_response` 关联语义。
- A05: `ServerRegistry` 必须统一定义为在线态缓存而非持久化事实。
- A06: `#unbind` 当前行为（不自动 `whitelist_remove`）必须在三文档口径一致，直到迁移完成。
- A07: `/api/servers/{id}/command` 的回执语义必须在三文档同步（当前不等执行回执）。
- A08: F4 鉴权边界必须在三文档一致：登录入口可匿名，管理页面与 `/ws` 必须鉴权。
- A09: Reforged Alpha 模式下不应直连 OneBot 处理业务命令的结论在三文档必须一致。
- A10: Redis 关闭时系统降级到 Alpha 本地状态的说法在三文档必须一致。
- A11: 失败态表达必须覆盖“超时/部分成功/断连重试”，且术语一致。
- A12: 每次修订若变更端口、角色边界、状态主从，必须同时更新三文档版本与变更记录。

## 11. Gap 关闭条件与降级（强制）
| Gap | Definition of Closed (DoC) | 无法关闭时的降级策略 |
| --- | --- | --- |
| G1-UnbindWhitelist | 连续 30 天 `#unbind` 后跨服 `whitelist_remove` 成功率 >= 99.9%，且孤儿白名单=0 | 明确维持“解绑仅数据解绑”语义；新增每日白名单对账任务，发现孤儿项自动告警并生成待清理清单 |
| G2-ApiCommandAck | `/api/servers/{id}/command` 100% 走 `requestId/proxy_response`；无“发送即成功”路径残留 | API 层返回 `202 accepted` + `pending` 状态，强制客户端轮询结果接口，不再宣称即时成功 |
| G3-ReforgedLegacyPath | Alpha 模式下旧 action (`sign/accept/get_playtime`) 调用量连续 14 天为 0，且本地业务态写入为 0 | 保留旧逻辑但仅在 standalone 模式启用；Alpha 模式返回 `DEPRECATED_USE_ALPHA` 并记录审计日志 |
| G4-StaticAnonExposure(F4) | 受保护页面匿名 200 次数连续 30 天为 0；所有受保护入口命中鉴权拦截链路 | 启用网关层兜底 ACL（路径白名单），即使应用路由误配也拒绝匿名访问控制面 |

## 12. Gap 指标误判与修正（强制）
| Gap | 最易误判指标 | 误判原因 | 修正办法 |
| --- | --- | --- | --- |
| G1 | `whitelist_remove_success_ratio` | 仅统计“有响应子服”，超时子服被排除导致虚高 | 分母改为“解绑时在线目标服总数”；超时计入失败，并记录 `timeout_count` |
| G2 | `api_command_timeout_ratio` | 子服离线与执行超时混在一起，掩盖链路问题 | 拆分 `offline_reject_ratio`、`execution_timeout_ratio`、`ack_parse_error_ratio` 三指标 |
| G3 | `legacy_action_calls_in_alpha_mode` | 只看主流程调用，漏掉重试和异常回退分支 | 在 Reforged 接收层统一埋点（入站 action 级），并做去重与异常分支覆盖 |
| G4 | `protected_route_auth_block_rate` | 分母含健康检查/静态资源，导致拦截率失真 | 分母限定为“受保护资源匿名请求数”；单独维护匿名允许清单命中率 |

## 13. F4 反例与修复（强制）
| 反例类型 | 反例 | 修复策略 |
| --- | --- | --- |
| 本应匿名却被拦截 | `POST /api/login` 被全局鉴权中间件误拦截返回 401 | 在入口路由前置 allowlist，确保 `/api/login` 永远先匹配匿名规则，再进入鉴权链 |
| 本应鉴权却被放行 | `GET /users.html` 或 `GET /vue/*` 被当静态资源直接 200 返回 | 将管理页静态资源从“公共静态处理器”剥离到受保护路由组；匿名访问统一 401/重定向 |
