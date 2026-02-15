# DATA_FLOW_AND_STATE

## 元数据
| 字段 | 值 |
| --- | --- |
| Doc-ID | A3-DATA_FLOW_AND_STATE |
| Version | v1.0 |
| Status | Draft (Implemented-Alignment) |
| Owner | Knowledge Base Subtask A3 |
| Last-Updated | 2026-02-14 |
| Scope | 绑定、白名单、在线状态、命令回执 |
| Source-of-Truth | `BridgeProxy`, `BridgeMessageHandler`, `DataManager`, `PlaytimeStore`, `BridgeClient`, `BridgeHandlers` |

## 1. 目标与范围（强制）
- 记录控制面与执行面的关键数据流。
- 以状态迁移方式定义绑定、白名单、在线状态、命令回执的生命周期。
- 本文聚焦运行态，不覆盖 UI 视觉与部署脚本。

## 2. 架构事实（强制）
### 2.1 关键状态存储
| 状态对象 | 所有者 | 存储位置 | 说明 |
| --- | --- | --- | --- |
| QQ->UUID 绑定 `bindings` | Alpha `DataManager` | Redis(`mapbot:bindings`) + 本地 `data/bindings.txt` | Alpha 主写；Reforged 只查询 |
| UUID->玩家名 `player_names` | Alpha `DataManager` | Redis(`mapbot:player_names`) + 本地文件 | 用于白名单同步与展示 |
| 禁言 `mutes` | Alpha `DataManager` | Redis(`mapbot:mutes`) + 本地文件 | Reforged 通过 `check_mute` 查询（带短缓存） |
| 在线时长 `playtime` | Alpha `PlaytimeStore` | Redis(`mapbot:playtime:<uuid>`) 或本地内存 | Reforged 仅上报增量 `playtime_add` |
| 在线服务器视图 | Alpha `ServerRegistry` | 内存 | 来源于 register/heartbeat/disconnect |
| 命令待回执 | Alpha `BridgeProxy.pendingRequests` | 内存 | `requestId -> future` |
| 子服待响应请求 | Reforged `BridgeClient.pendingRequests` | 内存 | 用于 `requestAlpha()` 同步等待 |

## 3. 边界定义（强制）
### 3.1 流 A: 绑定与白名单
1. QQ 用户发 `#id <name>` 到 OneBot 群。
2. Alpha `InboundHandler -> CommandRegistry -> BindCommand` 调用 `BridgeProxy.resolveAndBind`。
3. Alpha 向在线子服查询 `resolve_uuid/resolve_name`，解析身份。
4. Alpha 写入 `DataManager.bind`（本地 + Redis）。
5. Alpha 对所有在线子服 fan-out `whitelist_add`。
6. 汇总子服响应，返回绑定结果与同步摘要到 QQ。

状态迁移:
- `UNBOUND` -> `RESOLVING_IDENTITY` -> `BOUND_PERSISTED` -> `WHITELIST_SYNCED_FULL | WHITELIST_SYNCED_PARTIAL`
- 冲突分支: `UNBOUND` -> `REJECTED_OCCUPIED(FAIL:OCCUPIED:<qq>)`
- 当前实现: `UNBIND` 不自动触发 `whitelist_remove`（白名单可残留）

### 3.2 流 B: 新子服注册后的白名单快照
1. Reforged 发送 `register(serverId, token, transferHost, transferPort)`。
2. Alpha 鉴权通过后注册到 `ServerRegistry`。
3. Alpha 异步执行 `syncWhitelistSnapshotToServer(serverId)`。
4. 对已有绑定逐条 `whitelist_add`（尽力而为，不阻塞注册成功回包）。

状态迁移:
- `SERVER_NEW` -> `REGISTERED` -> `SNAPSHOT_SYNCING` -> `SNAPSHOT_DONE(success/skip/fail)`

### 3.3 流 C: 在线状态（服务器与玩家）
服务器在线态:
1. Reforged 连接 Bridge 并注册。
2. Reforged 每 30s 发送 `heartbeat(players,tps,memory,uptime)`。
3. Alpha 更新 `ServerRegistry`。
4. 若 60s 无读事件，Alpha `IdleStateHandler` 关闭连接并移除服务器。

玩家在线态:
1. Alpha 查询玩家是否在线时，fan-out `has_player(uuid)` 到在线子服。
2. 结果聚合为在线服务器集合（`YES/NO`）。

状态迁移:
- 服务器: `DISCONNECTED -> CONNECTED -> REGISTERED -> HEARTBEATING -> TIMEOUT_OR_CLOSE -> DISCONNECTED`
- 玩家: `UNKNOWN -> ONLINE(serverSet!=empty) | OFFLINE(serverSet=empty)`

### 3.4 流 D: 命令回执（QQ/WS）
1. 命令入口来自 QQ(`InboundHandler`)或 WebSocket(`LogWebSocketHandler`)。
2. Alpha 生成 `requestId`，通过 `BridgeProxy.sendRequest*` 下发到指定子服。
3. Reforged `BridgeHandlers` 在主线程执行命令，回发 `proxy_response(requestId,result)`。
4. Alpha `BridgeMessageHandler` 调用 `BridgeProxy.completeRequest(requestId,result)`。
5. Alpha 将结果回执给来源通道（QQ 群/私聊或 WS 控制台）。

状态迁移:
- `RECEIVED -> AUTHORIZED -> DISPATCHED -> EXECUTING -> ACKED -> REPLIED`
- 超时分支: `DISPATCHED -> TIMEOUT -> REPLIED_WITH_ERROR`

## 4. ASCII 图（强制）
```text
绑定 + 白名单 (A)
QQ User -> OneBot -> Alpha(Command) -> BridgeProxy
BridgeProxy -> Reforged[*] : resolve_uuid/resolve_name
Reforged[*] -> BridgeProxy : proxy_response
BridgeProxy -> DataManager : bind(qq, uuid, name)
BridgeProxy -> Reforged[*] : whitelist_add(name)
Reforged[*] -> BridgeProxy : SUCCESS/FAIL
BridgeProxy -> OneBot -> QQ User : 绑定结果 + 同步摘要

命令回执 (D)
QQ/WS -> Alpha -> BridgeProxy(requestId)
BridgeProxy -> Reforged : execute_command
Reforged -> Alpha : proxy_response(requestId, result)
Alpha -> QQ/WS : 回执(result)
```

### 4.2 失败态时序（强制）
```text
失败态-1: 命令超时 (execute_command timeout)
QQ/WS -> Alpha BridgeProxy(requestId)
Alpha -> Reforged : execute_command
... no proxy_response before TIMEOUT ...
Alpha BridgeProxy : future timeout, pendingRequests.remove(requestId)
Alpha -> QQ/WS : [错误] 请求超时
```
- 告警触发点: Alpha `request_timeout_count{action="execute_command"}` 5 分钟内 >= 3 或 `timeout_ratio` > 1%。
- 人工干预点: 运维在 Alpha 控制台检查目标服连接态与线程堆栈，必要时隔离问题子服并切换只读模式。

```text
失败态-2: 白名单部分成功 (partial success)
QQ User -> Alpha : #id <name>
Alpha -> Reforged[S1,S2,S3] : whitelist_add(name)
Reforged[S1] -> Alpha : SUCCESS
Reforged[S2] -> Alpha : FAIL:...
Reforged[S3] -> Alpha : (timeout)
Alpha : aggregate = success 1/3 + failed list
Alpha -> QQ User : [白名单同步] 成功 1/3 + 失败摘要
```
- 告警触发点: `whitelist_sync_partial_count` 连续 10 分钟 >= 1 且 `failed_servers > 0`。
- 人工干预点: 管理员按失败摘要逐服执行补偿同步（或重试命令），并核对白名单最终一致性。

```text
失败态-3: Bridge 断连与重试注册 (disconnect/retry)
Reforged -> Alpha : register + heartbeat
<network drop>
Alpha : IdleState READER_IDLE -> close -> unregister(serverId)
Reforged : readLoop break -> handleDisconnect -> sleep(RECONNECT_DELAY_MS)
Reforged -> Alpha : reconnect + register
Alpha -> Reforged : register_ack(success)
```
- 告警触发点: `bridge_disconnect_count{serverId}` 15 分钟内 >= 3 或 `reconnect_duration_p95_ms` > 10000。
- 人工干预点: 排查网络与 token/serverId 配置；必要时手动下线震荡子服并执行灰度重连。

## 5. 状态与不变量（强制）
- 绑定唯一性不变量: 一个 QQ 只能绑定一个 UUID；一个 UUID 只能被一个 QQ 占用。
- 回执关联不变量: `proxy_response.requestId` 必须命中且只完成一次 pending future。
- 在线状态不变量: `ServerRegistry` 只反映“当前连接”，重启后需重新注册恢复。
- 数据主从不变量: Alpha 写主状态；Reforged 仅查询/执行/上报（带短 TTL 缓存）。

## 6. 异常与降级（强制）
| 场景 | 状态影响 | 对外表现 |
| --- | --- | --- |
| 无在线子服时执行绑定 | 流 A 停在 `RESOLVING_IDENTITY` 前 | 返回“当前无在线服务器” |
| 部分子服白名单同步失败 | 进入 `WHITELIST_SYNCED_PARTIAL` | 返回成功数/失败服摘要 |
| Bridge 请求超时 | 流 D 进入 `TIMEOUT` | 返回“请求超时/无响应” |
| Bridge 注册被拒 | 流 C 无法进入 `REGISTERED` | 子服不在在线列表，命令不可达 |
| Redis 不可用 | Alpha 改用本地状态 | 跨节点一致性下降，单节点仍可运行 |

## 7. 待澄清项（强制）
- `#unbind` 仅解除 Alpha 绑定，不自动移除各子服白名单，需确认是否保留该策略。
- HTTP `/api/servers/{id}/command` 当前不等待子服执行回执，是否需要改为与 QQ/WS 同步语义。
- Reforged 仍保留旧 `get_playtime/sign_in/accept_reward` 本地路径，需明确是否完全停用以避免口径漂移。

## 8. Known Gaps/Transition Plan（强制）
| Gap | Priority | 受影响链路 | 问题描述 | 最小过渡方案 | 可观测指标（阈值） |
| --- | --- | --- | --- | --- | --- |
| G1-UnbindWhitelist | P0 | 流 A | `#unbind` 未触发跨服 `whitelist_remove` | 增加解绑后 `uuid/name -> fan-out whitelist_remove` 并回执聚合 | `unbind_whitelist_remove_success_ratio >= 99.9%`；`orphan_whitelist_entries == 0` |
| G2-ApiCommandAck | P0 | 流 D（Web API） | API 命令未等待执行回执，状态不一致 | API 路径统一接入 `requestId/proxy_response` 机制 | `api_command_false_success_count == 0/day`；`api_command_timeout_ratio < 1%` |
| G3-ReforgedLegacyPath | P1 | 流 A/D（旧 action） | Alpha 模式仍可触发本地业务态逻辑 | Alpha 模式下禁用旧 action 或返回 deprecated | `legacy_action_calls_in_alpha_mode == 0`；`local_business_state_write_count == 0/day` |
| G4-StaticAnonExposure(F4) | P0 | 流 D 入口边界 | 管理静态页匿名暴露导致未鉴权访问控制面 | 按“登录匿名、控制面鉴权”规则收口 | `protected_page_anonymous_200_count == 0/day`；`auth_guard_violation_count == 0` |

## 9. F4 规则在数据流中的映射（强制）
- 规则 DF4-1: 匿名流量仅允许进入登录链路，不得进入命令与状态流（A/B/C/D）。
- 规则 DF4-2: 任一进入流 A/B/C/D 的请求，必须已通过 token 鉴权（含 `/ws`）。
- 规则 DF4-3: 违反 DF4-1/DF4-2 的请求必须在入口即拒绝，不得创建 `requestId`。

## 10. 一致性锚点（强制）
- 本文链路命名（A/B/C/D）与 `MODULE_BOUNDARY.md` 模块方向必须可一一映射。
- 本文优先级与指标阈值必须与 `SYSTEM_CONTEXT.md` 第 8 章一致。
- 跨文档统一断言以 `SYSTEM_CONTEXT.md` 第 10 章（A01-A12）为准。

## 11. Gap 闭环引用（强制）
- G1/G2/G3/G4 的关闭条件（DoC）与不可关闭降级策略，统一以 `SYSTEM_CONTEXT.md` 第 11 章为准。
- G1/G2/G3/G4 的“最易误判指标与修正办法”，统一以 `SYSTEM_CONTEXT.md` 第 12 章为准。
