# Step-01 A1 Change Gate Rules

## 0. 总判定
- BLOCKER 集合：`Gate-1`、`Gate-2`、`Gate-3`、`Gate-4`。
- 判定公式：`PASS = Gate-1 ∧ Gate-2 ∧ Gate-3 ∧ Gate-4`。
- 任一 Gate 失败即 `FAIL/NO-GO`。
- 修复后要求：必须重跑全部 Gate（`Gate-1 -> Gate-2 -> Gate-3 -> Gate-4`）。

## 1. REQ-ID 追溯要求
- Gate-1 覆盖 REQ-001, REQ-002, REQ-005, REQ-006, REQ-007, REQ-008, REQ-009。
- Gate-2 覆盖 REQ-004, REQ-007, REQ-008。
- Gate-3 覆盖 REQ-002, REQ-003, REQ-005, REQ-009。
- Gate-4 覆盖 REQ-001~REQ-009 文档一致性。

## Gate-1（合同优先）
- 检查命令：
```bash
set -euo pipefail
BASE="Project_Docs/Re_Step/Artifacts/Step01"
rg -n "依据路径" "$BASE/01_Refactor_Charter.md"
rg -n "验证方法|失败判据|依据路径" "$BASE/02_DoD_Checklist.md"
rg -n "BRIDGE_MESSAGE_CONTRACT|BRIDGE_ERROR_CODE_CONTRACT|COMMAND_AUTHORIZATION_CONTRACT|CONFIG_SCHEMA_CONTRACT|DATA_CONSISTENCY_CONTRACT|OBSERVABILITY_SLO_CONTRACT" "$BASE"/0*.md
```
- 阻断动作：合同引用缺失或 DoD 三字段不完整，立即标记 `FAIL` 并禁止进入 A2。
- 恢复动作：补全合同映射和 DoD 三字段；修复后重跑 Gate-1~Gate-4。

## Gate-2（回滚路径）
- 检查命令：
```bash
set -euo pipefail
BASE="Project_Docs/Re_Step/Artifacts/Step01"
rg -n "回滚|恢复|失败判据|退出条件" "$BASE/01_Refactor_Charter.md" "$BASE/02_DoD_Checklist.md" "$BASE/03_Change_Gate_Rules.md"
rg -n "VERSIONING_AND_COMPATIBILITY|FAILURE_MODEL|CONFIG_SCHEMA_CONTRACT" "$BASE/02_DoD_Checklist.md"
```
- 阻断动作：缺少可执行回滚阈值、回退动作或恢复判据时，立即 `FAIL`。
- 恢复动作：补齐触发阈值与恢复条件；修复后重跑 Gate-1~Gate-4。

## Gate-3（反向测试）
- 检查命令：
```bash
set -euo pipefail
BASE="Project_Docs/Re_Step/Artifacts/Step01"
rg -n "失败判据|越权|断连|超时|重放|冲突|反向测试" "$BASE/02_DoD_Checklist.md"
rg -n "第1轮|第2轮|第3轮|主代理问题|子代理回答|主代理复核" "$BASE/Gate_Evidence"/*/subagent_rounds.md
```
- 阻断动作：反向测试条目不足、或子代理追问轮次 < 3，立即 `FAIL`。
- 恢复动作：补齐负向场景与反证记录；修复后重跑 Gate-1~Gate-4。

## Gate-4（文档同步与证据有效性）
- 检查命令：
```bash
set -euo pipefail
BASE="Project_Docs/Re_Step/Artifacts/Step01"
EVIDENCE_DIR="$(ls -1dt "$BASE/Gate_Evidence"/* | head -n1)"
PLACEHOLDER_PATTERN='(<TODO>|<TBD>|<PLACEHOLDER>|\[待补\]|\{\{待补[^}]*\}\}|__PLACEHOLDER__)'
test -f "$BASE/01_Refactor_Charter.md"
test -f "$BASE/02_DoD_Checklist.md"
test -f "$BASE/03_Change_Gate_Rules.md"
test -f "$BASE/04_Solo_Review_Log.md"
! rg -n "多方签核|待外部签核" "$BASE"/0*.md >/dev/null
rg -n "Gate-1|Gate-2|Gate-3|Gate-4|阻断动作|恢复动作|检查命令" "$BASE/03_Change_Gate_Rules.md"
rg -n "commit SHA|CI Run ID|artifact hash|timestamp|证据.*失效|重跑全部 Gate" "$BASE/02_DoD_Checklist.md" "$BASE/03_Change_Gate_Rules.md"
test -f "$EVIDENCE_DIR/gate_inputs.log"
test -f "$EVIDENCE_DIR/gate_outputs.log"
test -s "$EVIDENCE_DIR/gate_inputs.log"
test -s "$EVIDENCE_DIR/gate_outputs.log"
! rg -n "$PLACEHOLDER_PATTERN" "$BASE/01_Refactor_Charter.md" "$BASE/02_DoD_Checklist.md" "$BASE/04_Solo_Review_Log.md" "$EVIDENCE_DIR/final_verdict.md" "$EVIDENCE_DIR/subagent_rounds.md" >/dev/null
```
- 阻断动作：文件缺失、术语不一致、证据绑定字段缺失、多方签核语义命中、`gate_inputs.log`/`gate_outputs.log` 为空、或文档/证据存在占位符残留时，立即 `FAIL`。
- 恢复动作：补齐文档与证据绑定四元组，修复后重跑 Gate-1~Gate-4。

## 2. 证据失效规则
- 若 `commit SHA` 变化，视为证据失效，必须重新运行全部 Gate 并更新证据。
- 若 `timestamp` 超过 24 小时，视为证据过期，必须重采证据。
- 若 `gate_inputs.log` 或 `gate_outputs.log` 为 0 字节（空日志），视为证据无效并直接 `FAIL`。
- 若门禁文档或证据文件存在占位符残留（如 `<TODO>`、`<TBD>`、`<PLACEHOLDER>`、`[待补]`、`{{待补字段}}`、`__PLACEHOLDER__`），视为证据无效并直接 `FAIL`。

## 3. 单人执行约束
- 只允许“自审 + 自记录”，不引入多方签核流程。
