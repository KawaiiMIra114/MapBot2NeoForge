# STEP04 Contract Checklist

用途：Step04/B2 交接核对单。执行人逐项勾选，未通过项直接挂阻断。

状态约定：`[ ]` 未核对，`[x]` 已通过，`[!]` 阻断。

## 执行信息

- 执行人：`<填写>`
- 执行日期：`<YYYY-MM-DD>`
- 证据批次目录：`Project_Docs/Re_Step/Evidence/Step04/<RUN_ID>/`
- 关联合同版本：
  - `Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md`
  - `Project_Docs/Contracts/CONFIG_SCHEMA_CONTRACT.md`
  - `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`

## A. COMMAND_AUTHORIZATION 合同核对

- [ ] A1 角色集合仅 `user/admin/owner`，未知角色拒绝。
通过标准：API/命令入口不存在第四角色可生效路径。
证据建议路径：`Project_Docs/Re_Step/Artifacts/Step04/01_Command_Authorization_Matrix.md`

- [ ] A2 命令分类与最小权限映射齐全，未映射命令默认拒绝。
通过标准：每个命令能定位到分类与最小角色；空映射命令返回拒绝。
证据建议路径：`Project_Docs/Re_Step/Artifacts/Step04/01_Command_Authorization_Matrix.md`

- [ ] A3 越权返回固定 `AUTH-403`（含 HTTP/API 场景）。
通过标准：越权响应码和错误体稳定可机检，不再返回 200 + 文本拒绝。
证据建议路径：`Project_Docs/Re_Step/Artifacts/Step04/05_B2_Negative_Test_Cases.md`

- [ ] A4 越权请求无副作用。
通过标准：拒绝前后关键数据快照 hash 一致（admins/bindings/perms 无变化）。
证据建议路径：`Project_Docs/Re_Step/Artifacts/Step04/01_Command_Authorization_Matrix.md`

- [ ] A5 审计字段完整：`request_id/principal_id/caller_role/decision/decision_reason/policy_version`。
通过标准：至少抽样 1 条 allow + 1 条 deny，字段齐全可检索。
证据建议路径：`Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md`

- [ ] A6 连续越权（5分钟内>=5次）触发限流与安全告警。
通过标准：能看到拒绝计数增长、限流生效、告警触发记录。
证据建议路径：`Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md`

## B. CONFIG_SCHEMA 合同核对

- [ ] B1 `schema.version` 存在且 `>=1`，缺失/非法阻断加载。
通过标准：删除或改坏该键后，启动或重载明确失败。
证据建议路径：`Project_Docs/Re_Step/Artifacts/Step04/03_Config_Schema_Validation_Profile.md`

- [ ] B2 关键配置键具备类型/默认值/范围定义。
通过标准：关键键不存在“无类型或无范围”空洞。
证据建议路径：`Project_Docs/Contracts/CONFIG_SCHEMA_CONTRACT.md`

- [ ] B3 未知键 fail-closed。
通过标准：注入未知键后加载失败，不允许静默忽略继续运行。
证据建议路径：`Project_Docs/Re_Step/Artifacts/Step04/05_B2_Negative_Test_Cases.md`

- [ ] B4 热重载遵循 `解析->校验->原子替换->审计`，任一步失败整体回滚。
通过标准：故障注入后配置 hash 回到 pre-reload 快照。
证据建议路径：`Project_Docs/Re_Step/Artifacts/Step04/04_HotReload_Rollback_Audit_Flow.md`

- [ ] B5 owner/admin 集合约束生效（owner 非空、集合不冲突）。
通过标准：空 owner 或冲突集合时启动/重载被阻断。
证据建议路径：`Project_Docs/Re_Step/Artifacts/Step04/03_Config_Schema_Validation_Profile.md`

- [ ] B6 配置变更审计最小字段完整：`before_hash/after_hash/schema_version/operator/reload_result`。
通过标准：任一重载都可回溯操作人、版本与结果。
证据建议路径：`Project_Docs/Re_Step/Artifacts/Step04/04_HotReload_Rollback_Audit_Flow.md`

## C. OBSERVABILITY 合同核对

- [ ] C1 核心指标已接入：`auth_decision_total`、`auth_decision_latency_ms`、`config_reload_total`、`audit_log_write_total`。
通过标准：指标可采集、可查询、标签口径与合同一致。
证据建议路径：`Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`

- [ ] C2 SLO/阈值配置与合同一致（尤其鉴权延迟 40/50ms 阈值）。
通过标准：告警规则存在 Warning/Critical 双阈值并可触发。
证据建议路径：`Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`

- [ ] C3 配置重载失败与审计落盘失败有独立告警。
通过标准：两类故障都可在告警系统内单独识别。
证据建议路径：`Project_Docs/Manuals/OPERATIONS_RUNBOOK.md`

- [ ] C4 Critical 事件必须有 RCA，且可按 `incident_id` 检索。
通过标准：抽查 1 条 Critical 事件，RCA 字段完整且保留策略满足合同。
证据建议路径：`Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`

- [ ] C5 排障最小证据集齐全：`request_id` 全链路日志 + `policy_version/schema.version/snapshot_version`。
通过标准：针对 1 起告警可完整回放链路与版本上下文。
证据建议路径：`Project_Docs/Re_Step/Evidence/Step04/20260215T081808Z/final_verdict.md`

## D. Step04 结论与阻断

- [ ] D1 A/B/C 三组无阻断项（无 `[!]`）。
证据建议路径：`Project_Docs/Re_Step/Evidence/Step04/<RUN_ID>/gate_summary.txt`

- [ ] D2 如存在偏差，已登记到缺口清单并绑定修复动作与负责人。
证据建议路径：`Project_Docs/Re_Step/Artifacts/Step04/05_B2_Negative_Test_Cases.md`

- [ ] D3 本次核对结果已回填 Evidence Map。
证据建议路径：`Project_Docs/Memory_KB/04_Evidence/EVIDENCE_MAP.md`

## 交接摘要（填写）

- 核对结果：`PASS/WARN/FAIL`
- 阻断项编号：`<如 A3,B4>`
- 修复负责人与截止日期：`<填写>`
- 下一次复核入口：`Project_Docs/Re_Step/Evidence/Step04/<NEXT_RUN_ID>/`
