# CONFIG_SCHEMA_CONTRACT

## 1. 统一元数据（强制）
| 字段 | 值 |
| --- | --- |
| Contract-ID | A2-CONFIG-SCHEMA |
| 版本 | 1.0.0 |
| 状态 | Active |
| 生效日期 | 2026-02-14 |
| 最后更新 | 2026-02-14 |
| 责任角色 | owner |
| 适用系统 | MapBot2NeoForge |
| 依赖合同 | COMMAND_AUTHORIZATION_CONTRACT, DATA_CONSISTENCY_CONTRACT, OBSERVABILITY_SLO_CONTRACT |

## 2. 强制章节合规声明（强制）
本文件包含统一强制章节：`统一元数据`、`适用范围`、`术语与角色`、`规范条款`、`审计与证据`、`迁移与兼容`、`验证步骤`、`可机检规则`、`反向测试`、`脆弱假设与缓释`。

## 3. 适用范围（强制）
本合同约束权限、配置、一致性、可观测相关配置项的结构、校验、热重载、安全边界与迁移流程。

## 4. 术语与角色（强制）
### 4.1 角色集合
权限角色严格限制为：`user`、`admin`、`owner`。

### 4.2 Schema 术语
- `schema.version`：配置结构主版本，整数递增。
- `hot_reload`：运行时无重启应用配置变更的能力。
- `sensitive key`：含密钥、身份映射、授权策略等高风险键。

## 5. 规范条款（强制）
### 5.1 关键配置键定义
| 配置键 | 类型 | 默认值 | 合法范围/约束 | 热重载 | 安全约束 |
| --- | --- | --- | --- | --- | --- |
| `schema.version` | int | `1` | `>=1` | 否 | 启动时必校验 |
| `auth.owner_ids` | array<string> | `[]` | 元素唯一；生产环境长度 `>=1` | 否 | 仅 `owner` 可改 |
| `auth.admin_ids` | array<string> | `[]` | 元素唯一；不得与 `owner_ids` 重叠 | 是 | 仅 `owner` 可改 |
| `auth.default_role` | enum | `user` | 仅 `user/admin/owner` | 是 | 禁止设置为未知角色 |
| `command.visibility_mode` | enum | `strict` | `strict/discoverable` | 是 | 默认 `strict` 防信息泄露 |
| `command.denial_backoff_ms` | int | `500` | `100..10000` | 是 | 防暴力越权探测 |
| `consistency.snapshot_interval_sec` | int | `300` | `30..3600` | 是 | 变更需记录审计 |
| `consistency.replay_batch_size` | int | `500` | `1..5000` | 是 | 超范围即拒绝加载 |
| `observability.sli_window_sec` | int | `60` | `10..300` | 是 | 与告警窗口联动校验 |
| `observability.alert_channel` | string | `ops-main` | `^[a-z0-9_-]{3,32}$` | 是 | 仅白名单目标 |
| `security.secret_source` | enum | `env` | `env/vault` | 否 | 禁止明文 secret |
| `security.audit_hmac_key_ref` | string | `AUDIT_HMAC_KEY` | 非空，长度 `>=8` | 否 | 仅引用，不存储值 |

### 5.2 类型与默认值规则
- 所有配置键必须声明类型、默认值、合法范围。
- 缺失键按默认值补齐，补齐动作必须记录 `config_default_applied` 事件。
- 默认值不得引入提权（如默认 `owner`）或禁用审计。

### 5.3 热重载能力
- 标记为“是”的配置项可热重载，应用流程：`解析 -> 校验 -> 原子替换 -> 审计落盘`。
- 任一校验失败必须整体回滚到上一个有效版本。
- 标记为“否”的配置项变更后需重启生效，且必须提前告警。

### 5.4 安全约束
- 配置文件不得存储明文口令、token、私钥。
- `auth.owner_ids` 变更必须要求二次确认并写入高优先级审计事件。
- 配置加载器对未知键默认拒绝（fail-closed），不得忽略并继续运行。

### 5.5 迁移策略
- 使用 `schema.version` 进行显式迁移，禁止隐式“猜测转换”。
- 仅允许 `N -> N+1` 单步迁移；跨版本升级需串行执行迁移脚本。
- 降级仅允许在“无破坏字段”版本区间内执行，并要求先做快照备份。

## 6. 审计与证据（强制）
配置变更最小审计字段：
- `change_id`、`operator_id`、`operator_role`
- `before_hash`、`after_hash`、`schema_version`
- `reload_result`、`validation_errors`

## 7. 迁移与兼容（强制）
- 新增键必须“向后兼容 + 有默认值”。
- 废弃键至少保留 2 个小版本，并在日志中输出弃用告警。
- 删除键前必须提供自动迁移映射与回滚路径。

## 8. 验证步骤（强制）
1. 运行 schema 校验，确认所有关键键具备类型/默认值/范围定义。
2. 修改一个可热重载键与一个不可热重载键，验证行为分别符合合同。
3. 注入未知键与非法范围值，确认加载器 fail-closed 并输出错误。
4. 执行一次 `schema.version` 升级迁移，验证可回滚且审计字段完整。

## 9. 可机检规则（强制）
1. 关键键 schema.version 必须存在：`rg -n "schema.version.*int.*>=1" Project_Docs/Contracts/CONFIG_SCHEMA_CONTRACT.md`
2. 角色枚举限制必须存在：`rg -n "user/admin/owner" Project_Docs/Contracts/CONFIG_SCHEMA_CONTRACT.md`
3. 热重载失败回滚规则必须存在：`rg -n "校验失败必须整体回滚" Project_Docs/Contracts/CONFIG_SCHEMA_CONTRACT.md`
4. 未知键 fail-closed 必须存在：`rg -n "未知键默认拒绝（fail-closed）" Project_Docs/Contracts/CONFIG_SCHEMA_CONTRACT.md`

## 10. 反向测试（强制）
- 前置条件：当前配置有效，目标键 `command.denial_backoff_ms=500`。
- 执行步骤：热重载提交合法值 `command.denial_backoff_ms=800`，其余不变。
- 通过判据：热重载成功且新值生效，无回滚事件，审计记录 `reload_result=success`。

## 11. 脆弱假设与缓释（强制）
| 假设ID | 失效条件 | 后果 | 缓释条款（MUST） |
| --- | --- | --- | --- |
| ASSUMP-03 | 迁移脚本不可重复执行或不可回滚 | 配置损坏、跨版本不可恢复 | 每次迁移必须先做 dry-run 与 checksum 校验；迁移失败必须自动回滚到迁移前快照。 |
