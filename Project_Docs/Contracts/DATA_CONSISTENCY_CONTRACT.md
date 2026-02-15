# DATA_CONSISTENCY_CONTRACT

## 1. 统一元数据（强制）
| 字段 | 值 |
| --- | --- |
| Contract-ID | A2-DATA-CONSISTENCY |
| 版本 | 1.0.0 |
| 状态 | Active |
| 生效日期 | 2026-02-14 |
| 最后更新 | 2026-02-14 |
| 责任角色 | owner |
| 适用系统 | MapBot2NeoForge |
| 依赖合同 | COMMAND_AUTHORIZATION_CONTRACT, CONFIG_SCHEMA_CONTRACT, OBSERVABILITY_SLO_CONTRACT |

## 2. 强制章节合规声明（强制）
本文件包含统一强制章节：`统一元数据`、`适用范围`、`术语与角色`、`规范条款`、`审计与证据`、`迁移与兼容`、`验证步骤`、`可机检规则`、`反向测试`、`脆弱假设与缓释`。

## 3. 适用范围（强制）
本合同定义权限与配置数据在写入、复制、回放、恢复场景中的一致性边界与故障处理规范。

## 4. 术语与角色（强制）
### 4.1 角色集合
权限角色严格限制为：`user`、`admin`、`owner`。

### 4.2 一致性术语
- `事实源 (Source of Truth, SoT)`：最终判定依据的数据源。
- `entity_version`：单实体单调递增版本号。
- `snapshot`：某一时刻的全量状态快照。
- `replay`：从事件日志重建状态的过程。

## 5. 规范条款（强制）
### 5.1 事实源定义
| 数据域 | 事实源 | 说明 |
| --- | --- | --- |
| 命令授权策略 | `authorization_policy_store` | 决定命令是否可执行的唯一依据 |
| 运行时配置 | `config_store` | 生效配置唯一依据 |
| 状态变更历史 | `event_log` | 追溯与重放的唯一依据 |
| 读优化缓存 | `materialized_cache` | 派生视图，非事实源 |

规则：若缓存与事实源冲突，必须以事实源为准并触发缓存修复。

### 5.2 版本号规则
- 每个事实源实体必须维护 `entity_version:uint64`，每次成功写入 `+1`。
- 每个快照必须包含 `snapshot_version` 与 `snapshot_checksum`。
- 读请求响应应携带 `version_hint` 便于客户端做幂等与重试判断。

### 5.3 冲突解决策略
- 写入必须携带 `expected_version`，采用乐观并发控制（CAS）。
- 版本冲突时返回 `CONSISTENCY-409`，禁止静默覆盖。
- 对同一目标的并发治理写入，冲突决策顺序：
1. `owner` 提交优先于 `admin` 与 `user`。
2. 同角色下，`request_timestamp` 更早者先提交，后者重试。
3. 若时间戳一致，以 `request_id` 字典序最小者提交。

### 5.4 回放策略
- 采用 `snapshot + append-only event_log` 模型恢复状态。
- 回放顺序：按 `event_sequence` 严格递增执行。
- 每条事件必须具备 `idempotency_key`，重复事件仅执行一次。
- 回放过程中发现非法事件必须进入隔离队列，不得阻断其他合法事件回放。

### 5.5 失败恢复
- 启动恢复路径：加载最新有效快照 -> 校验 checksum -> 回放增量日志。
- 快照校验失败时自动回退到上一个有效快照并触发 `Critical` 告警。
- 恢复目标：`RTO <= 10 分钟`，`RPO <= 60 秒`。

### 5.6 一致性级别
- 强一致：命令鉴权决策、权限策略写入确认。
- 会话读己之写：同一调用主体在同会话内读取刚写入的配置结果。
- 最终一致：可观测聚合、统计报表，最大允许滞后 `60 秒`。

## 6. 审计与证据（强制）
最小审计证据：
- 冲突事件记录（含 `expected_version` 与 `actual_version`）
- 最近一次恢复链路日志（快照版本、回放条数、耗时）
- 一致性级别判定说明（请求类型 -> 一致性策略）

## 7. 迁移与兼容（强制）
- 版本号字段改动必须保持向后读取兼容至少 1 个主版本。
- 事件 schema 变更采用“新增字段优先，删除字段延后”的双写策略。
- 回放引擎升级期间必须支持旧事件格式解析，不得造成历史日志失效。

## 8. 验证步骤（强制）
1. 构造并发写入同一实体，验证 CAS 冲突返回 `CONSISTENCY-409`。
2. 人工制造缓存与事实源偏差，确认读取以事实源为准并自动修复缓存。
3. 从快照恢复并回放增量日志，核验恢复时间与回放幂等性。
4. 注入损坏快照，验证自动回退、告警、服务可继续恢复。

## 9. 可机检规则（强制）
1. 事实源冲突处理必须存在：`rg -n "必须以事实源为准并触发缓存修复" Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`
2. CAS 先决条件必须存在：`rg -n "写入必须携带.*expected_version" Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`
3. 冲突错误码必须存在：`rg -n "CONSISTENCY-409" Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`
4. 恢复目标单位必须显式：`rg -n "RTO <= 10 分钟|RPO <= 60 秒" Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`

## 10. 反向测试（强制）
- 前置条件：目标实体当前 `entity_version=10`，单写请求携带 `expected_version=10`。
- 执行步骤：发送一次配置写入并等待提交完成。
- 通过判据：请求成功，版本递增到 `11`，返回非 `CONSISTENCY-409`，且无冲突告警。

## 11. 脆弱假设与缓释（强制）
| 假设ID | 失效条件 | 后果 | 缓释条款（MUST） |
| --- | --- | --- | --- |
| ASSUMP-04 | 节点时钟偏移导致 `request_timestamp` 不可靠 | 同角色并发写入顺序误判，冲突决策不稳定 | 冲突决策必须优先使用 `event_sequence`；若时钟偏移超过 1 秒必须降级为仅按 `request_id` 裁决并告警。 |
