# Bridge 消息合同（BRIDGE_MESSAGE_CONTRACT）

## Metadata
| Field | Value |
| --- | --- |
| DocID | MB2N-BRIDGE-MESSAGE |
| Version | 1.1.0 |
| Status | Active |
| Owner | A1 合同-协议层（知识库搭建） |
| Reviewers | Alpha Core Maintainer, Reforged Maintainer, Bridge QA |
| Source of Truth | `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`; 实现参考 `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/*`, `MapBot_Reforged/src/main/java/com/mapbot/network/*` |
| Last Updated | 2026-02-14 |
| Related Docs | `Project_Docs/Contracts/CONTRACT_INDEX.md`, `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`, `Project_Docs/CURRENT_STATUS.md` |
| Change Impact | 统一 Alpha/Reforged Bridge 消息语义、状态机与重试边界；补齐字段实现状态、消息体大小合同、执行矩阵。 |

## Purpose
定义 Alpha Core 与 Reforged Mod 之间 Bridge 通道的消息包、消息分类、连接状态机、超时/重试策略、幂等键与兼容策略，作为跨端协议唯一依据。

## Scope
- 覆盖链路：`Alpha <-> Bridge TCP <-> Reforged`。
- 覆盖类型：注册、心跳、请求-响应、事件上报、文件代理消息。
- 不覆盖：OneBot v11 外部协议、Web HTTP API、业务命令权限模型。

## Definitions
- 帧（Frame）：一条 UTF-8 JSON 文本，以 `\n` 结尾。
- 请求（Request）：需要对端回包的消息，使用 `requestId` 关联。
- 响应（Response）：对请求的回包，必须带原 `requestId`。
- 事件（Event）：单向上报消息，不保证同步回包。
- 变更型请求（Mutation）：会产生状态副作用的请求（如 `whitelist_add`、`give_item`、`file_write`）。
- 幂等键（Idempotency Key）：用于识别“同一业务操作重放”的去重键。

## Normative Rules (MUST/SHOULD/MAY)
### 1. 传输与封包
- `MUST` 使用 UTF-8 JSON 行协议（每条消息 `\n` 结尾）。
- `MUST` 每条消息包含 `type` 字段。
- `MUST` 控制入站帧长度不超过 `64 KiB`（Alpha 侧 `LineBasedFrameDecoder(65536)` 为硬约束）。
- `SHOULD` 新字段采用追加方式，不破坏旧字段语义。
- `MAY` 追加扩展字段（如 `traceId`、`protocolVersion`），接收端忽略未知字段。

### 2. 消息包通用字段
| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| `type` | string | 必填 | 消息类型。 |
| `requestId` | string | 条件必填 | 请求和对应响应必须一致。 |
| `serverId` | string | 条件必填 | `register` 必填；事件上报建议携带。 |
| `arg1` | string | 可选 | 兼容字段，保留当前实现。 |
| `arg2` | string | 可选 | 兼容字段，保留当前实现。 |
| `payload` | object | 可选 | 新增结构化参数容器。 |
| `result` | string | 响应常用 | `proxy_response` 的业务结果。 |
| `error` | string | 失败时可选 | `register_ack`/`file_response` 失败原因。 |
| `success` | boolean | 条件必填 | `register_ack` 使用。 |
| `idempotencyKey` | string | 可选 | 重试去重键（见第 7 条）。 |
| `protocolVersion` | string | 可选 | 协议版本标记，默认视为 `bridge.v1`。 |
| `ts` | number | 可选 | 发送方时间戳（epoch ms）。 |

#### 2.1 字段实现状态表（implemented/planned/deprecated）
- `MUST` 状态为 `planned` 的字段默认仅为 `MAY`，不得作为互通门禁。
- `MUST` 状态为 `deprecated` 的字段在兼容窗口内继续可读可写。

| 字段 | 状态 | 当前口径 | 规范级别默认值 |
| --- | --- | --- | --- |
| `type` | implemented | 双端已读写 | `MUST` |
| `requestId` | implemented | 双端请求-响应关联已使用 | `MUST` |
| `serverId` | implemented | 注册链路已使用 | `MUST`（注册场景） |
| `arg1` | deprecated | 现网仍大量使用 | `MAY`（新功能不应新增依赖） |
| `arg2` | deprecated | 现网仍大量使用 | `MAY`（新功能不应新增依赖） |
| `result` | implemented | `proxy_response` 主载体 | `MUST`（response场景） |
| `error` | implemented | `register_ack/file_response` 使用 | `SHOULD`（失败场景） |
| `success` | implemented | `register_ack` 使用 | `MUST`（register_ack） |
| `payload` | planned | 合同预留，代码未统一消费 | `MAY` |
| `idempotencyKey` | planned | 合同预留，代码未统一去重 | `MAY` |
| `protocolVersion` | planned | 合同预留，未协商 | `MAY` |
| `ts` | planned | 合同预留，未使用 | `MAY` |

### 3. request/response/event 分类
#### 3.1 Request
- Alpha -> Reforged：
  - `command`, `qq_message`, `get_players`, `has_player`, `get_status`, `bind_player`, `whitelist_add`, `whitelist_remove`, `resolve_uuid`, `resolve_name`, `reload_config`, `switch_server`, `sign_in`, `accept_reward`, `get_inventory`, `get_location`, `execute_command`, `broadcast`, `get_playtime`, `get_cdk`, `stop_server`, `cancel_stop`, `file_list`, `file_read`, `file_write`, `file_delete`, `file_mkdir`, `file_upload`, `roll_loot`, `give_item`。
- Reforged -> Alpha：
  - `redeem_cdk`, `check_mute`, `get_qq_by_uuid`, `switch_server_request`。
- 注册与保活请求：`register`, `heartbeat`。

#### 3.2 Response
- `register_ack`
- `heartbeat_ack`
- `proxy_response`
- `file_response`

#### 3.3 Event
- `event`, `chat`, `status_update`, `playtime_add`

### 4. 状态机
- `MUST` 首帧为 `register`，否则 Alpha 返回 `register_ack(success=false,error=register_required)` 并断开连接。
- `MUST` 注册鉴权失败时返回 `register_ack(success=false,error=unauthorized)` 并断开。
- `MUST` 注册成功后进入 Active 态，允许业务请求与事件。
- `MUST` Reforged 每 30 秒发送一次 `heartbeat`。
- `MUST` Alpha 在读空闲 60 秒时主动断连。
- `MUST` Reforged 断连后按 3 秒固定回退重连。

状态流：
`DISCONNECTED -> TCP_CONNECTED -> REGISTER_PENDING -> ACTIVE -> DISCONNECTED`

### 5. 超时策略
| 场景 | 当前实现超时 | 约束 |
| --- | --- | --- |
| Alpha 同步请求（`BridgeProxy.sendRequestToServer`） | 10 秒 | `MUST` 超时后返回空或错误并记录日志。 |
| Alpha fan-out 总预算（`FANOUT_TIMEOUT`） | 10 秒 | `MUST` 汇总完成/超时数量。 |
| Alpha 文件代理请求（`BridgeFileProxy`） | 10 秒 | `MUST` 返回 `Request timeout`。 |
| Reforged -> Alpha `redeem_cdk` | 3000 ms | `MUST` 超时报 `INVALID:Alpha 未响应 (超时)`。 |
| Reforged -> Alpha `check_mute/get_qq_by_uuid` | 500 ms | `SHOULD` 使用短超时避免主流程阻塞。 |
| Reforged -> Alpha `switch_server_request` | 15000 ms | `MUST` 超时报 `FAIL:Alpha 未响应 (超时)`。 |
| Reforged 本地执行回包等待 | 5~8 秒 | `MUST` 返回明确超时文本。 |

### 6. 重试策略
- `MUST` 连接级重试由 Reforged 客户端负责，固定 3 秒回退。
- `MUST NOT` 对变更型请求进行“隐式自动重试”。
- `SHOULD` 对只读请求（如 `get_status`、`get_players`）最多重试 1 次，且两次请求间隔 >= 200 ms。
- `MAY` 由上层命令层触发人工重试，但必须记录审计字段（见错误码合同）。

### 7. 幂等键（Idempotency Key）
- 当前实现状态：`planned`（双端尚未内建去重缓存）。
- `MAY` 在请求中携带 `idempotencyKey`。
- `SHOULD` 对变更型请求重试时复用同一 `idempotencyKey`，避免“新键重放同一业务”。
- `SHOULD` 幂等键格式：`ik:{sourceServerId}:{type}:{bizKey}:{epochBucket}`。
- `MAY` 在未携带 `idempotencyKey` 时使用 `requestId` 做弱关联（仅追踪，不保证去重）。

### 8. 兼容策略
- `MUST` 保持 `arg1/arg2` 在兼容窗口内可用，新增结构化参数时并行提供 `payload`。
- `MUST` 保持 `proxy_response.result` 作为兼容输出；可附加结构化错误字段（见错误码合同）。
- `SHOULD` 新增消息类型时保持旧类型可运行至少一个次版本窗口。
- `SHOULD` 接收端采用“宽松读取”：忽略未知字段、保留已知字段语义。
- `MAY` 引入 `protocolVersion` 做能力协商；未携带时按 `bridge.v1` 处理。

### 9. 消息体大小合同
#### 9.1 单帧上限
- `MUST` 单帧大小（含 JSON 文本与换行）不超过 `65536` 字节。
- `MUST` 任何发送端在本地可预判超限时直接失败，不得发送超限帧。

#### 9.2 base64 安全上限
- `MUST` 对 `file_upload` 的 base64 场景，原始二进制载荷默认不超过 `46 KiB`（47104 字节）。
- `SHOULD` 该上限用于保证 base64 膨胀后仍可落在单帧 64 KiB 内，避免链路层截断。

#### 9.3 超限错误码
- `MUST` 超限统一映射为 `BRG_VALIDATION_205`（消息体超过合同上限）。
- `SHOULD` 兼容输出可保留现有字符串（如 `Payload too large`、`File too large`）。

#### 9.4 兼容窗口
| 版本窗口 | 口径 | 兼容说明 |
| --- | --- | --- |
| `v1.0.x` | 历史实现并存（Alpha 64 KiB，Reforged 文件逻辑上限 256 KiB） | 可能出现“发送端放行、接收端拒绝/断连”。 |
| `v1.1.x` | 合同生效：发送端按 64 KiB/46 KiB 预检 | 超限走 `BRG_VALIDATION_205`，避免链路层失败。 |
| `v2.0.0+` | 移除旧上限口径，仅保留合同口径 | 若需更大消息，必须采用分片协议再升版本。 |

## Non-goals
- 不定义业务命令本身的权限/玩法规则。
- 不定义日志系统、指标系统的完整数据模型。
- 不承诺跨进程 Exactly-once；本合同只定义 At-least-once 下的幂等边界。

## Risks and Failure Modes
- 帧大小不一致（64 KiB vs 256 KiB）导致超限请求在链路层失败。
- 变更型请求尚无服务端去重缓存，超时后重放会产生重复副作用。
- 当前大量返回字符串（`FAIL:*` / `[错误]`）而非结构化错误，调用方解析脆弱。
- 文件请求与消息处理共线程时，大 I/O 会阻塞后续消息处理。

### 协议缺口整改优先级（P0/P1）
- `P0-1` 帧大小合同未统一：先统一 64 KiB + 46 KiB 发送端预检与错误码回传。
- `P0-2` 变更型请求无去重：先实现 `idempotencyKey` 去重缓存与重复请求幂等回包。
- `P1-1` 错误返回双栈未完全落地：补齐 `errorCode` 结构化输出并推进字符串退场。

## Verification and Acceptance Criteria
### A. Case ID 列表
- `CT-01` 注册成功。
- `CT-02` 非 `register` 首帧被拒绝。
- `CT-03` 非法 JSON 首帧被拒绝。
- `CT-04` 鉴权失败被拒绝。
- `CT-05` 心跳正常回 ACK。
- `CT-06` `requestId` 关联一致。
- `CT-07` 未知字段兼容（忽略未知字段）。
- `CT-08` Alpha 同步请求 10 秒超时。
- `CT-09` 文件代理 10 秒超时。
- `CT-10` `switch_server_request` 15 秒超时。
- `CT-11` 60 秒读空闲断连。
- `CT-12` 3 秒回退重连。
- `CT-13` 单帧超过 64 KiB 处理。
- `CT-14` base64 载荷超过 46 KiB 处理。
- `CT-15` 变更型请求禁止隐式重试。
- `CT-16` 响应乱序仍可正确归并。
- `CT-17` 孤儿响应（未知 `requestId`）不致崩溃。
- `CT-18` 文件路径越界拒绝。

### B. 契约测试最小执行矩阵（Local/CI/Preprod）
| 环境 | 必须覆盖的 case id |
| --- | --- |
| Local（开发机） | `CT-01, CT-02, CT-03, CT-04, CT-05, CT-06, CT-13, CT-14, CT-15` |
| CI（合并门禁） | `CT-01, CT-02, CT-03, CT-04, CT-05, CT-06, CT-07, CT-08, CT-09, CT-10, CT-13, CT-14, CT-15, CT-16, CT-17` |
| 预发（发布门禁） | `CT-01` 至 `CT-18` 全覆盖 |

- 验收标准：
  - `MUST` Local/CI/预发均完成各自矩阵内所有 case。
  - `MUST` 预发阶段 `CT-13/CT-14/CT-15/CT-16` 全通过后方可发布。

## Change Log
| Date | Version | Change | Author |
| --- | --- | --- | --- |
| 2026-02-14 | 1.1.0 | 增加字段实现状态表、消息体大小合同、最小执行矩阵、P0/P1 协议缺口优先级。 | A1 合同-协议层 |
| 2026-02-14 | 1.0.0 | 首次定义 Bridge 消息包、分类、状态机、超时/重试/幂等与兼容策略。 | A1 合同-协议层 |
