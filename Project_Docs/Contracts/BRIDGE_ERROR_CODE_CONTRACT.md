# Bridge 错误码合同（BRIDGE_ERROR_CODE_CONTRACT）

## Metadata
| Field | Value |
| --- | --- |
| DocID | MB2N-BRIDGE-ERROR-CODE |
| Version | 1.1.0 |
| Status | Active |
| Owner | A1 合同-协议层（知识库搭建） |
| Reviewers | Alpha Core Maintainer, Reforged Maintainer, Bridge QA, SRE |
| Source of Truth | `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`; 实现参考 `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/*`, `MapBot_Reforged/src/main/java/com/mapbot/network/*` |
| Last Updated | 2026-02-14 |
| Related Docs | `Project_Docs/Contracts/CONTRACT_INDEX.md`, `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`, `Project_Docs/CURRENT_STATUS.md` |
| Change Impact | 统一错误语义与可观测字段，新增双栈映射优先级与字符串错误退场计划。 |

## Purpose
定义 Bridge 统一错误码体系，覆盖命名规则、分层边界、用户可见文案策略与审计字段，确保跨端错误可追踪、可聚合、可回归。

## Scope
- 覆盖范围：Alpha 与 Reforged Bridge 通道中的鉴权、参数、传输、执行、超时、内部异常。
- 输出对象：协议响应、日志、审计事件、告警聚合。
- 非覆盖：OneBot 对外错误规范、Web 控制台 HTTP 状态码语义。

## Definitions
- 错误码（Error Code）：机器可解析的稳定标识。
- 分层（Layer）：按故障来源划分的错误域。
- 用户文案（User Message）：给普通用户/管理员的可见提示文本。
- 审计字段（Audit Fields）：用于追溯“谁在何时对哪个请求发生了什么错误”的记录字段。
- 双栈（Dual-stack）：结构化错误与历史字符串错误并行输出的过渡阶段。

## Normative Rules (MUST/SHOULD/MAY)
### 1. 错误码命名规则
- `MUST` 使用格式：`BRG_<LAYER>_<NNN>`。
- `MUST` 满足正则：`^BRG_(AUTH|VALIDATION|TRANSPORT|EXECUTION|TIMEOUT|INTERNAL)_[0-9]{3}$`。
- `MUST` 同一错误语义绑定唯一错误码，不得复用。
- `MUST` 变更错误码语义时升主版本。
- `SHOULD` 在代码中保留“原始错误字符串 -> 错误码”映射，保障兼容。
- `MAY` 在错误响应中附带 `detailKey` 作为二级细分，不改变主错误码。

### 2. 分层模型（auth/validation/transport/execution/timeout/internal）
| Layer | 范围 | 代码段 | 重试建议 |
| --- | --- | --- | --- |
| `auth` | 鉴权/访问控制失败 | `BRG_AUTH_100~199` | 默认不重试，需修配置或权限。 |
| `validation` | 参数缺失、格式不合法、协议前置条件不满足 | `BRG_VALIDATION_200~299` | 修请求后重试。 |
| `transport` | 连接不可达、对端离线、通道异常 | `BRG_TRANSPORT_300~399` | 可重试，需退避。 |
| `execution` | 指令/业务执行失败（非超时） | `BRG_EXECUTION_400~499` | 视业务可重试，默认人工确认。 |
| `timeout` | 请求等待超时或保活超时 | `BRG_TIMEOUT_500~599` | 可重试；变更操作需幂等保护。 |
| `internal` | 未分类异常、代码缺陷、未知错误 | `BRG_INTERNAL_900~999` | 不盲重试，先告警并排障。 |

### 3. 基线错误码注册（兼容当前实现）
| Error Code | Layer | 典型原始错误（现网） | 标准用户文案 |
| --- | --- | --- | --- |
| `BRG_AUTH_101` | auth | `unauthorized` | Bridge 鉴权失败，请联系管理员核对令牌与 serverId。 |
| `BRG_AUTH_102` | auth | `Access denied: path outside server directory` / `path not in mutation whitelist` | 文件操作被拒绝，路径不在允许范围。 |
| `BRG_VALIDATION_201` | validation | `register_required` | 连接初始化失败：必须先发送注册消息。 |
| `BRG_VALIDATION_202` | validation | `invalid_json` / `empty_message` | 请求格式错误，请检查消息结构。 |
| `BRG_VALIDATION_203` | validation | `FAIL:玩家名为空` / `FAIL:目标转移地址为空` | 请求参数不完整，请补充后重试。 |
| `BRG_VALIDATION_204` | validation | `FAIL:玩家名格式非法` | 玩家名格式非法，仅支持 3-16 位字母数字下划线。 |
| `BRG_VALIDATION_205` | validation | `Payload too large` / `File too large` | 消息体超过协议上限，请缩小内容后重试。 |
| `BRG_TRANSPORT_301` | transport | `Server not connected:*` / `[错误] 服务器离线:*` | 目标服务器离线，请稍后再试。 |
| `BRG_TRANSPORT_302` | transport | `Bridge 未连接` / `服务器未就绪` | Bridge 通道未就绪，请稍后重试。 |
| `BRG_EXECUTION_401` | execution | `FAIL:命令执行失败` / `[错误] 指令执行失败或语法错误` | 执行失败，请检查命令和服务器状态。 |
| `BRG_EXECUTION_402` | execution | `FAIL:OFFLINE` / `FAIL:未知物品` | 业务执行失败，请按提示修正后重试。 |
| `BRG_TIMEOUT_501` | timeout | `Request timeout` / `[错误] 请求超时` | 请求超时，请稍后重试。 |
| `BRG_TIMEOUT_502` | timeout | `FAIL:跨服执行超时，请稍后重试` | 跨服操作超时，请稍后重试。 |
| `BRG_INTERNAL_901` | internal | `FAIL:<exception message>` | 系统内部错误，请联系维护者。 |
| `BRG_INTERNAL_999` | internal | 未命中映射规则的未知错误 | 未知内部错误，请联系维护者并附带 requestId。 |

### 4. 双栈期间映射优先级
- `MUST` 优先级 1：若响应包含合法 `errorCode`，以 `errorCode` 为准。
- `MUST` 优先级 2：若无 `errorCode`，使用字符串映射表（第 3 节）推导标准错误码。
- `MUST` 优先级 3：若 `errorCode` 与字符串映射冲突，保留 `errorCode` 并记录 `mappingConflict=true` 审计标记。
- `MUST` 优先级 4：若结构化字段和映射均失效，回退到 `BRG_INTERNAL_999`。
- `SHOULD` 双栈阶段同时输出：结构化错误 + 兼容字符串错误。
- `MAY` 在管理端暴露 `rawError`，普通用户端仅展示标准文案。

### 5. 字符串错误退场计划（按版本移除）
| 版本 | 输出策略 | 约束 |
| --- | --- | --- |
| `v1.0.x` | 历史阶段：可仅输出字符串错误 | 允许字符串单栈，但需可映射。 |
| `v1.1.x` | 双栈强制：结构化 + 字符串并行 | 新增/改造接口 `MUST` 带 `errorCode`。 |
| `v1.2.x` | 双栈收敛：禁止新增字符串专用语义 | `SHOULD` 所有活跃接口完成结构化。 |
| `v2.0.0+` | 结构化单栈：移除机器解析对字符串依赖 | `MUST` 以 `errorCode` 作为唯一机器判定依据。 |

- `MUST` 在 `v2.0.0` 前保留字符串兼容字段，避免旧调用方瞬断。
- `SHOULD` 每次版本升级发布映射差异清单。

### 6. 对用户可见文案策略
- `MUST` 文案先给结论，再给一个可执行动作，不超过两句。
- `MUST` 普通用户文案不得泄露敏感信息：token、完整堆栈、绝对路径、内部密钥。
- `MUST` 管理端文案至少包含：`errorCode`、是否可重试、下一步建议。
- `SHOULD` 使用统一中文术语（如“鉴权失败”“请求超时”“参数非法”）。
- `MAY` 按场景区分“玩家群提示”与“管理群/控制台提示”两个模板。

### 7. 审计字段
- `MUST` 每条错误审计记录包含以下字段：
  - `occurredAt`（UTC 时间）
  - `errorCode`
  - `layer`
  - `type`（消息类型）
  - `requestId`（无则填 `-`）
  - `direction`（`alpha->reforged` 或 `reforged->alpha`）
  - `sourceServerId`
  - `targetServerId`
  - `retryable`
  - `rawError`
- `SHOULD` 补充字段：
  - `idempotencyKey`
  - `actorType`（system/qq_user/admin）
  - `actorIdHash`（哈希脱敏）
  - `traceId`
  - `protocolVersion`
- `MAY` 增加 `payloadDigest`（摘要）用于跨系统对账，不直接落原文。

## Non-goals
- 不替代业务日志全文存储策略。
- 不约束各业务模块内部异常类设计。
- 不提供国际化文案体系（当前仅中文基线）。

## Risks and Failure Modes
- 双栈期间若映射表缺漏，会导致“同错多码”。
- 旧代码未统一填充结构化字段时，告警聚合会被字符串噪声放大。
- 退场窗口过短会导致旧客户端机器解析失效。

## Verification and Acceptance Criteria
- BE-01 命名合规：所有错误码匹配命名正则。
- BE-02 分层完整：六层（auth/validation/transport/execution/timeout/internal）均至少有一个注册错误码。
- BE-03 映射覆盖：高频原始错误（`unauthorized/register_required/Request timeout/FAIL:*`）映射覆盖率 100%。
- BE-04 双栈优先级：构造“`errorCode` 与字符串冲突”样例，验证优先级 1 生效且记录冲突标记。
- BE-05 退场门禁：
  - `v1.1.x`：新增接口必须携带 `errorCode`。
  - `v2.0.0+`：机器判定不得依赖字符串。
- BE-06 文案安全：用户可见文案不得出现 `token`、`auth.bridge.token`、绝对路径、堆栈片段。
- BE-07 审计完整度：`errorCode/layer/requestId/type/direction` 五字段非空率达到 100%。
- 验收标准：BE-01~BE-07 全部满足；否则禁止将错误码合同标记为稳定发布。

## Change Log
| Date | Version | Change | Author |
| --- | --- | --- | --- |
| 2026-02-14 | 1.1.0 | 增加双栈映射优先级、字符串错误退场计划、`BRG_VALIDATION_205` 与 `BRG_INTERNAL_999`。 | A1 合同-协议层 |
| 2026-02-14 | 1.0.0 | 首次定义 Bridge 错误码命名、分层、文案与审计字段合同。 | A1 合同-协议层 |
