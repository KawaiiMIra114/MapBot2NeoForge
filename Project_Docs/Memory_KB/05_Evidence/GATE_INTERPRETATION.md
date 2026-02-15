# GATE_INTERPRETATION

## 1) Gate Exit 判定语义（统一口径）

### 1.1 三层语义
- 第 1 层：命令退出码（`gate*.exit`）
  - `0`：命令表达式返回真/成功。
  - `1`：命令表达式返回假/未命中/阈值不满足。
- 第 2 层：门禁语义（PASS/FAIL/SKIP）
  - 由 gate 设计决定，不能仅按 `exit` 字面硬判。
- 第 3 层：步骤总判定（`final_verdict.md`）
  - 聚合所有 gate（含阻断级别）后给出 GO/NO-GO。

### 1.2 为什么“无命中但 exit=1”可判 PASS
- 负匹配 gate 的目标是“确认某些坏模式不存在”。
- 典型写法是 `rg -q <bad_pattern>` 或同类匹配命令。
  - 命中坏模式：`exit=0`，应判 FAIL。
  - 未命中坏模式：`exit=1`，应判 PASS。
- 已有实例：
  - `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/final_verdict.md`
  - 其中明确：`gate02_legacy_size_literals: PASS (exit=1, no legacy literal in bridge key path)`。

### 1.3 其他常见 gate 语义
- 正匹配/存在性 gate：通常 `exit=0 => PASS`，`exit!=0 => FAIL`。
- 阈值 gate：例如 `high_count` 超阈值时 `exit=1 => FAIL`（如 Step04 `gate06_blocking`）。
- SKIP with audit：前置条件缺失但经审计可跳过，通常写成 `result=SKIP` 且 `exit=0`（如 Step04 `gate04_api` 在 `ALPHA_TOKEN` 缺失时）。

## 2) Step03/Step04 判定示例
- Step03（`20260215T080030Z`）
  - `gate02_legacy_size_literals.exit=1`，但语义为“未命中旧字符串”，所以 PASS。
- Step04（`20260215T081808Z`）
  - `gate06_blocking.exit=1` 且 `high_count=8`，语义为阻断项未清零，所以 FAIL。
  - `gate04_api` 为 `SKIP with audit` 且 `exit=0`，不单独构成阻断。

## 3) 常见误判与排查流程

### 3.1 常见误判
- 误判 1：把所有 `exit=1` 都当 FAIL。
- 误判 2：只看 `gate*.exit`，不看 `gate*.log` 的 `result=`/计数字段。
- 误判 3：忽略 `SKIP with audit` 的审计前提，直接当 PASS 或 FAIL。
- 误判 4：只看单个 gate，不看 `final_verdict.md` 的聚合结论。

### 3.2 标准排查流程
1. 定位目标 run 的 `final_verdict.md`，确认最终 GO/NO-GO 与阻断项。
2. 打开对应 `gate_summary.txt`（若存在），对齐每个 gate 的 `exit`。
3. 逐个核对 `gate*.log` 的语义字段：`result=`、`reason=`、计数字段（如 `high_count`）。
4. 标记 gate 类型：正匹配 / 负匹配 / 阈值 / SKIP。
5. 若出现“exit 与 PASS/FAIL直觉冲突”，按 gate 类型解释，不做字面硬判。
6. 若仍冲突，以原始日志字段为准，回写 `EVIDENCE_REGISTRY.md` 的备注并触发复核。

## 4) 快速判定口诀
- 先定 gate 类型，再解 exit。
- 先看日志语义，再看数值。
- 最终以 `final_verdict.md` 聚合判定为准。
