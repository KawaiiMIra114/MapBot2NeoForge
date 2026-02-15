# COMMAND_AUTHORIZATION_CONTRACT

## 1. 统一元数据（强制）
| 字段 | 值 |
| --- | --- |
| Contract-ID | A2-CMD-AUTH |
| 版本 | 1.0.0 |
| 状态 | Active |
| 生效日期 | 2026-02-14 |
| 最后更新 | 2026-02-14 |
| 责任角色 | owner |
| 适用系统 | MapBot2NeoForge |
| 依赖合同 | CONFIG_SCHEMA_CONTRACT, DATA_CONSISTENCY_CONTRACT, OBSERVABILITY_SLO_CONTRACT |

## 2. 强制章节合规声明（强制）
本文件包含统一强制章节：`统一元数据`、`适用范围`、`术语与角色`、`规范条款`、`审计与证据`、`迁移与兼容`、`验证步骤`、`可机检规则`、`反向测试`、`脆弱假设与缓释`。

## 3. 适用范围（强制）
本合同约束所有命令入口（聊天命令、控制台命令、API 命令）在鉴权、展示、执行、审计中的一致行为。

## 4. 术语与角色（强制）
### 4.1 角色集合
权限角色严格限制为且仅为：
- `user`：普通使用者，可执行公开且低风险命令。
- `admin`：运维管理员，可执行管理类与敏感运维命令。
- `owner`：系统所有者，可执行全量命令并管理权限模型本身。

除上述三角色外，任何字符串都不得被解释为有效权限角色。

### 4.2 命令对象
- `command_id`：命令唯一标识。
- `command_category`：命令分类，用于最小权限决策。
- `principal_id`：命令调用主体标识。

## 5. 规范条款（强制）
### 5.1 角色定义与职责边界
- `user` 不得修改权限映射、密钥配置、系统级一致性参数。
- `admin` 可执行日常运营与风险可控变更，不得变更 `owner` 集合。
- `owner` 负责权限模型根配置、越权应急处置和最终审批。

### 5.2 命令分类
| 分类 | 说明 | 示例 | 最小角色 |
| --- | --- | --- | --- |
| `public_read` | 无副作用查询 | `help`, `status` | `user` |
| `scoped_read` | 与自身上下文相关查询 | `my_profile` | `user` |
| `ops_write` | 常规运维写操作 | `reload_cache` | `admin` |
| `sensitive_write` | 高风险写操作 | `set_admin`, `rotate_key` | `owner` |
| `governance` | 权限/合同/策略治理 | `set_owner`, `policy_migrate` | `owner` |

### 5.3 最小权限规则
- 默认拒绝：未显式映射分类的命令必须拒绝执行。
- 最小授权：命令仅绑定一个最小角色阈值，判断条件为 `caller_role >= min_role`。
- 禁止隐式提权：通配符映射（如 `* -> owner`）在加载时必须失败。

### 5.4 可见性规则
- `help`/命令自动补全仅返回调用者可执行命令。
- 对无权限命令，默认不暴露具体命令名与参数结构。
- 若开启审计模式下的受控提示，仅返回统一文案：`permission denied`，不泄露策略细节。

### 5.5 越权处理
- 任何越权请求必须返回拒绝结果，错误码固定为 `AUTH-403`。
- 连续越权触发（5 分钟内 >= 5 次）必须触发速率限制与安全告警。
- 越权请求不得触发命令副作用，不得进入业务执行阶段。

### 5.6 审计要求
每次命令鉴权都必须写入审计事件，最少字段如下：
- `event_time`、`request_id`、`principal_id`、`caller_role`
- `command_id`、`command_category`、`decision`（allow/deny）
- `decision_reason`、`policy_version`、`target_resource`

审计日志要求：
- 不可静默丢弃，落盘失败必须上报告警。
- 保留周期不少于 180 天。
- 支持按 `request_id` 与 `principal_id` 检索。

## 6. 审计与证据（强制）
最小证据集：
- 一条允许事件 + 一条拒绝事件（含完整字段）
- 当前命令分类表快照
- 对应 `policy_version` 的变更记录

## 7. 迁移与兼容（强制）
- 旧权限模型迁移必须映射到三角色：`legacy_user -> user`，`legacy_admin -> admin`，`legacy_owner -> owner`。
- 对无法映射的旧角色必须回退为 `user` 并产生人工复核任务。
- 灰度迁移期间允许双读（旧策略 + 新策略）并以新策略判定为准，持续时间不得超过 14 天。

## 8. 验证步骤（强制）
1. 以 `user/admin/owner` 三类账号分别调用所有命令分类，校验最小权限矩阵。
2. 使用未定义角色（如 `superadmin`）发起请求，确认鉴权加载失败或请求拒绝。
3. 连续制造越权请求，确认 `AUTH-403`、速率限制与告警三者同时生效。
4. 抽样审计日志，核验必填字段完整性、检索能力与保留策略。

## 9. 可机检规则（强制）
1. 角色集合声明存在：`rg -n "权限角色严格限制为且仅为" Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md`
2. 非三角色拒绝声明存在：`rg -n "不得被解释为有效权限角色" Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md`
3. 越权错误码固定：`rg -n "AUTH-403" Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md`
4. 越权频次窗口显式：`rg -n "5 分钟内 >= 5 次" Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md`

## 10. 反向测试（强制）
- 前置条件：存在 `owner` 账号，`set_owner` 命令分类为 `governance`。
- 执行步骤：以 `owner` 调用 `set_owner` 1 次并记录 `request_id`。
- 通过判据：请求被允许执行，返回非 `AUTH-403`，且审计日志出现 `decision=allow`。

## 11. 脆弱假设与缓释（强制）
| 假设ID | 失效条件 | 后果 | 缓释条款（MUST） |
| --- | --- | --- | --- |
| ASSUMP-01 | 生产环境 `owner` 集合为空或全失效 | 高危治理命令无法执行或应急失效 | 生产启动时若 `auth.owner_ids` 为空必须拒绝启动；必须提供 break-glass 恢复流程并要求双人审批。 |
| ASSUMP-02 | 权限策略缓存滞后（>30 秒） | 出现错误拒绝或错误放行窗口 | 鉴权前必须校验 `policy_version` 新鲜度；超过 30 秒必须强制回源读取事实源。 |
