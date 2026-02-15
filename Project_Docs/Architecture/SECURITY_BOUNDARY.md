# SECURITY_BOUNDARY

## 0. 元数据
| 字段 | 值 |
|---|---|
| Document-ID | ARCH-A4-SECURITY-BOUNDARY |
| Status | Active |
| Owner | KnowledgeBase A4 |
| Last-Updated | 2026-02-14 |
| Applicable-Version | v5.7.x（参考 `Project_Docs/CURRENT_STATUS.md`） |
| Scope | `MapBot_Reforged/` + `Mapbot-Alpha-V1/` + 管理面板/API |
| Depends-On | 权限分级、Bridge 协议、Web 管理接口 |
| Review-Cadence | 每月一次，或权限/接口变更时立即复审 |

## 1. 目标与边界
- 目标: 定义认证鉴权、token 治理、传输安全与最小暴露面，减少越权与横向移动风险。
- 边界: 本文关注系统边界与控制面，不涵盖业务合规条款全文。

## 2. 术语与版本口径
- `AuthN`: 认证，确认请求主体身份（QQ 用户、服务实例、Web 会话）。
- `AuthZ`: 鉴权，确认主体是否具备执行该操作的权限级别。
- `Service Token`: 服务间调用凭据（Alpha <-> Reforged/管理接口）。
- `Least Exposure`: 仅开放必需端口、必需路径、必需方法。

## 3. 强制规则
### 3.1 认证与鉴权
- 所有写操作（管理员命令、数据改写、控制台执行）必须同时通过 AuthN + AuthZ。
- 权限分级采用显式级别（Level 0/1/2），默认拒绝，白名单放行。
- “首次管理员初始化”仅允许一次性引导路径；系统已有管理员后必须强制 Admin 验证。
- 服务间调用必须校验服务身份，不得仅依赖源 IP 判断可信。

### 3.2 Token 治理
- Token 分类:
1. Service Token（服务间）。
2. Session Token（Web 管理会话）。
3. Bootstrap Token（一次性初始化）。
- 生成要求: 高熵随机值，禁止硬编码、禁止提交到仓库。
- 生命周期:
1. Service Token 设轮换周期与失效时间。
2. Session Token 设短 TTL + 滑动续期上限。
3. Bootstrap Token 使用后立即失效。
- 存储要求: 仅可存放在安全配置源（环境变量/密钥管理），禁止落入日志与前端静态资源。
- 撤销要求: 支持主动吊销并立即生效，吊销事件需审计留痕。

### 3.3 传输安全
- 非本机回环流量默认使用 TLS（HTTPS/WSS）；禁止公网明文管理接口。
- 内网明文仅允许在明确受控网络域，并有替代迁移计划。
- 所有敏感头/令牌必须在日志中脱敏显示。
- 拒绝弱加密与过期证书；证书更新必须可演练并可回滚。

### 3.4 最小暴露面
- 端口最小化: 仅暴露业务必需端口，其他管理端口默认关闭。
- 接口最小化: 未使用的调试路由、测试入口、目录浏览必须禁用。
- 方法最小化: 对外接口限制 HTTP 方法与请求体大小，启用速率限制。
- 数据最小化: 响应体不泄露内部拓扑、堆栈、密钥片段、用户隐私明文。

### 3.5 密钥轮换 SOP（最小流程）
#### 阶段 1: 准备（Prepare）
- 生成新密钥 `K_new`（Service Token / Token Secret），分配轮换工单与 Owner。
- 记录轮换窗口、回滚点、影响范围（Bridge、Web Token、自动化脚本）。
- 预检查:
1. 现网依赖方清单完整。
2. 审计日志可用。
3. 回滚密钥 `K_old` 保留且可恢复。

#### 阶段 2: 双写（Dual-Write / Dual-Accept）
- 服务端进入“双接收模式”：同时接受 `K_old` 与 `K_new`。
- 签发端仍优先签发 `K_old`，同时对灰度客户端下发 `K_new`。
- 观察指标:
1. `auth_success_rate` 不低于基线 -1%。
2. `auth_failed_invalid_token` 不高于基线 +0.5%。

#### 阶段 3: 切换（Cutover）
- 在发布窗口将签发端默认切换为 `K_new`。
- 保持服务端“`K_old` + `K_new` 双接收”至少 30 分钟观察窗口。
- 若认证失败率超阈值立即回滚签发端到 `K_old`。

#### 阶段 4: 吊销（Revoke）
- 观察窗口通过后，移除 `K_old` 接收能力并吊销旧会话。
- 对旧密钥请求返回统一错误码（建议 `SEC_TOKEN_REVOKED`）。
- 吊销动作必须写审计记录（操作者、时间、影响范围）。

#### 阶段 5: 审计（Audit）
- 输出轮换报告:
1. 轮换开始/结束时间。
2. 失败请求统计。
3. 回滚是否发生。
4. 遗留客户端清单。
- 审计完成后关闭工单，并更新密钥到期日与下次轮换计划。

### 3.6 轮换失败回滚点与中断预算
| 阶段 | 回滚点（失败即回退） | 回滚动作 | 最大中断时间预算 |
|---|---|---|---|
| 准备（Prepare） | 依赖方清单不完整或预检查失败 | 终止轮换工单，保留 `K_old`，补齐依赖后重启流程 | 0 秒（不允许中断） |
| 双写（Dual-Write） | 双接收启用后 `auth_failed_invalid_token` 超基线 +0.5% | 关闭 `K_new` 接收，仅保留 `K_old` | 30 秒 |
| 切换（Cutover） | 切换后 5 分钟内认证成功率低于基线 -1% | 签发端回滚到 `K_old`，保持双接收 | 60 秒 |
| 吊销（Revoke） | 吊销后出现关键客户端大面积 `SEC_TOKEN_REVOKED` | 恢复 `K_old` 接收窗口 30 分钟并执行应急通知 | 120 秒 |
| 审计（Audit） | 审计数据缺失或不可追溯 | 标记轮换失败，不关闭工单，恢复“限制变更”模式 | 0 秒（控制面不中断） |

- 全流程最长可接受控制面中断预算: 3 分钟（累计）。
- 若累计中断 > 3 分钟，必须触发故障流程并按 Sev-2 处理。

## 4. 可操作检查点
- [ ] 鉴权检查: 所有写接口均有 AuthN + AuthZ，且默认拒绝未授权请求。
- [ ] 首管检查: 验证“首次管理员初始化”只能触发一次，后续必须 Admin。
- [ ] Token 检查: Token 不在仓库与日志中出现明文，具备轮换与吊销流程。
- [ ] 传输检查: 公网入口全部为 HTTPS/WSS，无明文管理流量。
- [ ] 暴露面检查: 端口扫描结果与设计白名单一致，无意外开放端口。
- [ ] 审计检查: 关键安全事件（登录失败、越权、吊销、配置变更）均可追踪。

## 5. 可执行实验计划
### 实验 S-1: 越权调用拦截实验
- 目标: 验证写接口均强制 AuthN + AuthZ，未授权请求被拒绝并产生审计事件。
- 步骤:
1. 分别使用无 token、过期 token、低权限 token 访问 `/api/servers/*/command`、`/api/mapbot/*`、`/ws`。
2. 记录返回码（401/403）和响应体错误码。
3. 检查审计日志是否包含来源、路径、操作者、失败原因。
- 采样窗口: 连续 30 分钟，每 1 分钟发起一轮探测。
- 通过阈值:
1. 未授权成功次数 = 0。
2. 拒绝响应准确率 = 100%（401/403 分类正确）。
3. 审计事件完整率 >= 99%。
- 失败处置:
1. 立即下线受影响接口外网入口。
2. 追加最小权限热修复并触发紧急回归。
3. 24 小时内完成越权根因报告与策略加固。

### 实验 S-2: 密钥泄露与吊销恢复实验
- 目标: 验证 token 泄露后可在目标时间内完成吊销、轮换与业务恢复。
- 步骤:
1. 生成测试 Service Token 并模拟泄露（受控演练环境）。
2. 对泄露 token 发起请求确认“泄露前可用”。
3. 执行吊销并触发轮换 SOP（双写->切换->吊销）。
4. 再次验证旧 token 不可用、新 token 可用。
- 采样窗口: 60 分钟（吊销动作窗口 10 分钟）。
- 通过阈值:
1. 旧 token 吊销生效时间 <= 5 分钟。
2. 轮换后认证成功率不低于基线 -1%。
3. 演练期间控制面累计中断 <= 3 分钟。
- 失败处置:
1. 立即回滚到 `K_old` 双接收模式。
2. 触发密钥再轮换并扩大影响面排查。
3. 升级为安全事件并冻结高风险发布。

### 实验 S-3: 传输安全与暴露面收敛实验
- 目标: 验证控制面传输链路与暴露面符合最小暴露原则。
- 步骤:
1. 对部署节点执行端口扫描，核对白名单端口。
2. 验证控制面入口协议（HTTP/WS 与 HTTPS/WSS）及证书状态。
3. 扫描 CORS 头与调试路由，确认无宽松策略泄露。
4. 对随机 API 请求检查响应是否泄露敏感字段。
- 采样窗口: 每日一次，连续 7 天。
- 通过阈值:
1. 非白名单开放端口数 = 0。
2. 公网入口明文管理流量 = 0。
3. `Access-Control-Allow-Origin:*` 在管理接口中命中数 = 0（目标态）。
- 失败处置:
1. 立即封禁异常端口与调试路由。
2. 强制切换内网访问并撤销公网入口。
3. 将整改项加入发布阻断门禁，未清零不得上线。

## 6. 现状差距表（As-Is vs To-Be）
| 主题 | As-Is（当前实现） | To-Be（目标） | 证据路径 | 风险等级 | 整改期限 | Owner |
|---|---|---|---|---|---|---|
| 密钥落库与仓库暴露 | `alpha.properties` 已提交且包含 `auth.bridge.token`、`auth.tokenSecret` | 密钥移入外部密钥源，仓库只保留占位符 | `Mapbot-Alpha-V1/config/alpha.properties:8`, `Mapbot-Alpha-V1/config/alpha.properties:9` | High | 2026-02-21 | Security Owner + DevOps |
| 默认密钥常量回退 | 存在 `DEFAULT_TOKEN_SECRET` 回退常量 | 禁止固定默认密钥；配置缺失时拒绝启动控制面 | `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/security/AuthManager.java:28`, `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/security/AuthManager.java:199` | High | 2026-02-28 | Alpha Core Maintainer |
| 明文传输与广域 CORS | 控制面构造 `ws://`，并设置 `Access-Control-Allow-Origin: *` | 仅 HTTPS/WSS + 最小 CORS 白名单 | `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/network/HttpRequestDispatcher.java:175`, `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/network/HttpRequestDispatcher.java:254`, `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/network/HttpRequestDispatcher.java:292` | High | 2026-03-15 | Platform Owner |

## 7. 短期阈值与逾期处理
| 项 | 临时可接受阈值（到期前） | 到期日 | 逾期处理 |
|---|---|---|---|
| 密钥仍在仓库配置 | 允许在受控内网环境临时存在，但必须限制仓库访问并启用审计 | 2026-02-21 | 逾期后强制轮换全部已暴露密钥，冻结外网发布，启动安全事件复盘 |
| 明文 WS/HTTP 控制面 | 仅允许回环/专线内网使用，禁止公网入口映射 | 2026-03-15 | 逾期后关闭远程控制入口，只保留本机运维通道直至 TLS 上线 |
