# EVIDENCE_REGISTRY

## 1) 范围与口径
- 范围：Step01 ~ Step05。
- 最新 run_id 选取口径：优先按时间戳命名判定（`YYYYMMDDTHHMMSSZ` 或 `YYYYMMDD_HHMMSS`），并与目录修改时间交叉核对。
- 备注：Step01 的证据目录位于 `Artifacts/Step01/Gate_Evidence`；Step02~05 位于 `Re_Step/Evidence`。

## 2) Step01~Step05 关键证据目录与最新 run_id

| Step | 最新 run_id | 关键证据目录 | final_verdict 定位 | 当前结论 |
|---|---|---|---|---|
| Step01 (A1) | `20260215_134455` | `Project_Docs/Re_Step/Artifacts/Step01/Gate_Evidence/20260215_134455` | `Project_Docs/Re_Step/Artifacts/Step01/Gate_Evidence/20260215_134455/final_verdict.md` | PASS, GO A2 |
| Step02 (A2) | `20260215T072111Z` | `Project_Docs/Re_Step/Evidence/Step02/20260215T072111Z` | `Project_Docs/Re_Step/Evidence/Step02/20260215T072111Z/final_verdict.md` | PASS, GO B1 |
| Step03 (B1) | `20260215T080030Z` | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z` | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/final_verdict.md` | PASS, GO B2 |
| Step04 (B2) | `20260215T163900Z` | `Project_Docs/Re_Step/Evidence/Step04/20260215T163900Z` | `Project_Docs/Re_Step/Evidence/Step04/20260215T163900Z/final_verdict.md` | PASS, GO B3 |
| Step05 (B3) | `20260215T165400Z` | `Project_Docs/Re_Step/Evidence/Step05/20260215T165400Z` | `Project_Docs/Re_Step/Evidence/Step05/20260215T165400Z/final_verdict.md` | PASS, GO C1 |

## 3) 各 Step 最小必查证据清单

### Step01 (A1)
- `final_verdict.md`
- `gate_inputs.log` + `gate_inputs.exit`
- `gate_outputs.log` + `gate_outputs.exit`
- `subagent_rounds.md`

### Step02 (A2)
- `final_verdict.md`
- `gate_inputs.log` + `gate_inputs.exit`
- `gate_outputs.log` + `gate_outputs.exit`
- `00_context.txt`（上下文口径）

### Step03 (B1)
- `final_verdict.md`
- `gate01_structured_fields.log/.exit`
- `gate02_legacy_size_literals.log/.exit`（负匹配 gate）
- `gate03_required_codes.log/.exit`
- `gate04_step03_artifacts.log/.exit`

### Step04 (B2)
- `final_verdict.md`
- `gate01_prev_artifacts.log/.exit`
- `gate02_sections.log/.exit`
- `gate03_blocking.log/.exit`
- `gate04_evidence_integrity.log/.exit`
- `gate05_summary.txt`

### Step05 (B3)
- `final_verdict.md`
- `preflight_read_manifest.txt`
- `preflight_contract_trace.txt`
- `preflight_code_coverage.txt`
- `gate01_prev_artifacts.log/.exit`
- `gate02_sections.log/.exit`
- `gate03_blocking.log/.exit`
- `gate04_evidence_integrity.log/.exit`
- `gate05_summary.txt`
- `subagent_rounds.md`

## 4) final_verdict 快速定位键
- Step01：`Verdict: PASS`、`Decision: GO A2`
- Step02：`Verdict: PASS`、`是否允许进入 B1：GO B1`
- Step03：`Verdict: PASS`、`Decision: GO B2`
- Step04：`Verdict: PASS`、`GO B3: YES`
- Step05：`Verdict: PASS`、`GO C1: YES`

## 5) 索引维护规则
- 任何新 run 产生后，先更新"最新 run_id"与 `final_verdict` 路径，再补充关键 gate 证据。
- 当 `final_verdict.md` 与单个 gate 结果表述出现冲突时，优先复核 `gate_summary.txt`（若有）与各 `gate*.log` 的原始字段再更新本索引。
