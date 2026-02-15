---
doc_id: ADR-000
title: ADR-000-template
owner: Tech Owner
status: template
version: 1.0.0
last_updated: 2026-02-14
review_cycle: per-decision
audience: maintainer, architect, reviewer
summary: 标准 ADR 模板，用于记录技术决策背景、方案对比、结论与后果。
---

# ADR-000: <Decision Title>

## Status
`Proposed | Accepted | Rejected | Superseded | Deprecated`

## Date
`YYYY-MM-DD`

## Owners
- Primary: `<name/role>`
- Reviewers: `<name/role>`

## Decision Owner (Required)
- Decision Owner: `<single accountable role>`
- Constraint:
  - 必须且只能有一个 Decision Owner，负责最终拍板与后续追踪。
  - `Status` 从 `Proposed` 变为 `Accepted` 时，必须由 Decision Owner 签署。

## Ownership Fallback (Required)
当出现“无人负责”或“负责人离职/不可用”时，按以下顺序兜底：
1. 连续 5 个工作日无 Decision Owner：由 Tech Owner 指定 `Acting Decision Owner`。
2. Decision Owner 离职/转岗：在 3 个工作日内完成交接并更新本 ADR。
3. 若 Tech Owner 也不可用：升级到项目负责人临时接管，直到新 Owner 生效。
4. 兜底期间限制：
   - 禁止将 `Proposed` 提升为 `Accepted`（除非项目负责人签署紧急批准）。
   - 必须在 Changelog 记录“Owner 变更”与生效日期。

## Context
描述触发本决策的业务背景、技术约束、已知风险和非目标。

建议至少回答：
- 当前痛点是什么？
- 不做决策会造成什么后果？
- 约束条件有哪些（时间、兼容性、资源、合规）？

## Decision Drivers
- `<driver 1>`
- `<driver 2>`
- `<driver 3>`

示例：可靠性、性能、可维护性、成本、安全性、交付速度。

## Considered Options
### Option A: `<name>`
- 描述：`<one paragraph>`
- Pros:
  - `<pro 1>`
  - `<pro 2>`
- Cons:
  - `<con 1>`
  - `<con 2>`

### Option B: `<name>`
- 描述：`<one paragraph>`
- Pros:
  - `<pro 1>`
- Cons:
  - `<con 1>`

### Option C: `<name>` (Optional)
- 描述：`<one paragraph>`
- Pros:
  - `<pro 1>`
- Cons:
  - `<con 1>`

## Decision
明确最终选择及理由，要求可验证、可复查：
- Selected Option: `<A/B/C>`
- Why: `<core reasoning>`
- Scope: `<where this decision applies>`
- Effective From: `<date/version>`

## Consequences
### Positive
- `<impact 1>`
- `<impact 2>`

### Negative
- `<tradeoff 1>`
- `<tradeoff 2>`

### Neutral / Follow-up
- `<note 1>`

## Validation Plan
说明如何验证该决策有效：
- 指标：`<metric>`
- 验证窗口：`<duration>`
- 验收标准：`<pass criteria>`

## Rollback / Exit Plan
若决策失败，如何退出：
1. `<rollback trigger>`
2. `<rollback actions>`
3. `<restoration checks>`

## Revisit Trigger (Required)
出现以下任一条件，必须重新评审该 ADR：
1. 关键指标连续两个观察周期不达标（如错误率、P95、成本）。
2. 外部约束发生变化（依赖协议、合规要求、平台版本）。
3. 相关重大事故（Sev-0/Sev-1）被归因到当前决策。
4. 决策前提失效（流量规模、团队容量、业务目标变化）。

## Rejected Change Record (Required)
当 ADR 或其后续变更请求被拒绝时，必须记录：
- Rejected Request ID: `<id>`
- Rejected At: `<YYYY-MM-DD>`
- Rejected By: `<Decision Owner>`
- Rejection Reason: `<why rejected>`
- Evidence/Context: `<metrics, incidents, constraints>`
- Reopen Condition: `<what must change to revisit>`

## Review SLA and Overdue Escalation (Required)
- Next Review Date: `<YYYY-MM-DD>`（在 `Accepted` 时必须填写）
- SLA:
  - 到期前 7 天提醒 Decision Owner。
  - 到期未复审 +7 天：升级到 Tech Owner。
  - 到期未复审 +14 天：升级到项目负责人，并将状态标记为 `Review-Overdue`（注记）。
- 逾期处置：
  - 禁止在未复审状态下继续扩大该 ADR 的适用范围。
  - 若该 ADR 关联高风险模块，需触发风险评审或临时冻结相关变更。

## References
- `<link or file path 1>`
- `<link or file path 2>`

## Changelog
| Date | Change | Author |
|---|---|---|
| YYYY-MM-DD | Initial draft | `<name>` |
