# EVIDENCE_REGISTRY

## 1) 范围与口径
- 范围：Step01 ~ Step-C1。
- 最新 run_id 选取口径：优先按时间戳命名判定（`YYYYMMDDTHHMMSSZ` 或 `YYYYMMDD_HHMMSS`）。
- 备注：Step01 的证据目录位于 `Artifacts/Step01/Gate_Evidence`；Step02~C1 位于 `Re_Step/Evidence`。

## 2) Step01~StepC1 关键证据目录与最新 run_id

| Step | 最新 run_id | 证据目录 | final_verdict | 当前结论 |
|---|---|---|---|---|
| Step01 (A1) | `20260215_134455` | `Re_Step/Artifacts/Step01/Gate_Evidence/20260215_134455` | 同目录 `final_verdict.md` | PASS, GO A2 |
| Step02 (A2) | `20260215T072111Z` | `Re_Step/Evidence/Step02/20260215T072111Z` | 同目录 `final_verdict.md` | PASS, GO B1 |
| Step03 (B1) | `20260215T080030Z` | `Re_Step/Evidence/Step03/20260215T080030Z` | 同目录 `final_verdict.md` | PASS, GO B2 |
| Step04 (B2) | `20260215T163900Z` | `Re_Step/Evidence/Step04/20260215T163900Z` | 同目录 `final_verdict.md` | PASS, GO B3 |
| Step05 (B3) | `20260215T165400Z` | `Re_Step/Evidence/Step05/20260215T165400Z` | 同目录 `final_verdict.md` | PASS, GO C1 |
| StepC1 | `20260215T170600Z` | `Re_Step/Evidence/StepC1/20260215T170600Z` | 同目录 `final_verdict.md` | PASS, GO C2 |

## 3) StepC1 最小必查证据清单
- `final_verdict.md`
- `preflight_manifest.txt`
- `preflight_scope.txt`
- `gate01_preflight.log/.exit`
- `gate02_core_paths.log/.exit`
- `gate03_auth_config.log/.exit`
- `gate04_protocol_error.log/.exit`
- `gate05_consistency_slo.log/.exit`
- `gate_summary.txt`

## 4) final_verdict 快速定位键
- Step01：`Verdict: PASS`、`Decision: GO A2`
- Step02：`Verdict: PASS`、`GO B1`
- Step03：`Verdict: PASS`、`Decision: GO B2`
- Step04：`Verdict: PASS`、`GO B3: YES`
- Step05：`Verdict: PASS`、`GO C1: YES`
- StepC1：`Verdict: PASS`、`GO C2: YES`

## 5) 索引维护规则
- 任何新 run 产生后，先更新"最新 run_id"与 `final_verdict` 路径。
- 当 `final_verdict.md` 与 gate 结果出现冲突时，优先复核 `gate_summary.txt`。
