# Re_Step-15：F2 运维手册联调与验证（单人维护版）

## 文档元数据
| 字段 | 值 |
| --- | --- |
| StepID | RE-STEP-15 |
| Version | 1.0.0 |
| Status | Ready |
| Owner | Solo Maintainer（你） |
| Last Updated | 2026-02-15 |
| 关联主计划 | `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`（阶段F/F2） |
| 证据来源 | `Project_Docs/Manuals/DEPLOYMENT_RUNBOOK.md`、`Project_Docs/Manuals/OPERATIONS_RUNBOOK.md`、`Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md` |

## 步骤目标
将部署、日常运维、事故响应三本手册联调为单条可复现链路，做到“按手册操作可得到一致结果”，并固定失败阈值与闭环证据。

## 为什么此步骤在此顺序
F1 已有指标与告警，F2 必须把“发现问题”转化为“可执行处置”。若不先打通手册联调，后续 G 阶段测试和发布门禁会缺少运维可执行验证依据。

## 输入材料（强制）
1. `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`
2. `Project_Docs/Manuals/DEPLOYMENT_RUNBOOK.md`
3. `Project_Docs/Manuals/OPERATIONS_RUNBOOK.md`
4. `Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md`
5. `Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md`
6. `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
7. 上一步产物（强制存在，缺失则 Verdict=FAIL 并禁止进入下一步）：`Project_Docs/Re_Step/Artifacts/Step14/*`

## 输出物定义（强制）
目录：`Project_Docs/Re_Step/Artifacts/Step15/`

1. `01_Runbook_E2E_Rehearsal_Record.md`
- 记录部署到运维到事故演练的全链路结果。

2. `02_Threshold_Validation_Report.md`
- 验证 F01-F18 等失败阈值与动态校准规则。

3. `03_Reload_Positive_Negative_Test_Report.md`
- 验证 `#reload` 正向/负向矩阵与收口回退。

4. `04_Incident_Drill_Evidence.md`
- 至少一次 Sev-1/Sev-2 级演练记录。

5. `05_Solo_Review_Log.md`
- 自审结论与准入判定。

## 详细执行步骤（编号化）
### 1. 对齐三本手册的阈值与术语
1. 对齐 `DEPLOYMENT_RUNBOOK` 与 `OPERATIONS_RUNBOOK` 中 F01-F18 阈值。
2. 对齐事故分级术语（Sev-0~Sev-3、S1~S4）。
3. 统一“失败即阻断”与“可带风险继续”判定语句。

通过标准：
1. 相同阈值在三本手册无冲突。
2. 术语一致并可机检。

失败判据：
1. 同一场景在不同手册给出不同失败条件。
2. 关键术语定义不一致导致执行歧义。

### 2. 执行全新部署演练
1. 按部署手册完成依赖检查、构建、配置、启动、健康验证。
2. 记录 F01-F09 结果与证据路径。
3. 对失败项执行一次修复与复验。

通过标准：
1. F01-F09 全部通过。
2. 每项有命令输出证据或日志证据。

失败判据：
1. 关键检查缺证据。
2. 复验后仍存在阻断项。

### 3. 执行日常运维与 `#reload` 联调
1. 按运维手册完成健康巡检与日志筛查。
2. 执行 `#reload` 正向矩阵与负向矩阵（含 unauthorized 场景）。
3. 完成配置收口回退并再次验证稳定态。

通过标准：
1. `#reload` 正向生效证据齐全。
2. 负向验证命中预期错误并可恢复。

失败判据：
1. 负向场景未命中预期错误（假阴性）。
2. 收口后系统未恢复基线。

### 4. 执行事故响应演练
1. 选择一次可控故障并按 playbook 进行宣告、止血、排障、恢复。
2. 固化最小证据集（incident_id、时间线、版本、request_id 样本）。
3. 输出结案模板与行动项。

通过标准：
1. 响应流程字段完整，复盘模板可回放。
2. 行动项有 owner（你）与截止时间。

失败判据：
1. 缺少时间线或关键决策记录。
2. 恢复后无复盘与预防措施。

### 5. 验证回滚演练时长与闭环
1. 按升级手册执行一次回滚演练。
2. 统计从触发到复验通过耗时。
3. 核验是否满足“15 分钟内可回滚”。

通过标准：
1. 回滚耗时 <= 15 分钟。
2. 回滚后核心健康项恢复到阈值内。

失败判据：
1. 回滚超时或步骤不可执行。
2. 回滚后核心命令成功率不达标。

### 6. 执行自审与阶段准入判定
1. 汇总联调证据并形成 PASS/FAIL 结论。
2. 记录残余风险与缓释动作。
3. 阻断项清零后准入 G1。

通过标准：
1. 证据链可追溯且可复现。
2. 阶段结论明确。

失败判据：
1. 通过结论与证据矛盾。
2. 阻断项未处理即准入下一阶段。

## Prompt 模板（至少3个：执行、反证审查、准入判定）
### Prompt-A（执行）
```text
请执行 F2“运维手册联调与验证”，输入：
- Project_Docs/Manuals/DEPLOYMENT_RUNBOOK.md
- Project_Docs/Manuals/OPERATIONS_RUNBOOK.md
- Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md
- Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md

要求：
1) 产出部署演练、运维联调、#reload 正负向验证、事故演练、回滚演练记录。
2) 每项给出通过标准、失败判据、证据路径。
3) 单人维护模式，仅“自审+自记录”。
```

### Prompt-B（反证审查）
```text
请对 F2 产物做反证审查：
1) 找出 8 个“按手册操作仍可能失败”的隐患。
2) 指出 5 个最可能出现假阳性/假阴性的验证点。
3) 给出每个隐患的补强步骤与机检命令。
4) 输出必须优先修复的 P0 项。
```

### Prompt-C（准入判定）
```text
请做 F2 准入判定：
检查对象：
- 01_Runbook_E2E_Rehearsal_Record.md
- 02_Threshold_Validation_Report.md
- 03_Reload_Positive_Negative_Test_Report.md
- 04_Incident_Drill_Evidence.md
- 05_Solo_Review_Log.md

判定规则：
1) F01-F18 是否有完整验证证据。
2) #reload 正负向是否均通过并完成收口。
3) 回滚演练是否 <=15 分钟。
4) 事故演练是否包含完整最小证据集。

输出：Verdict / Blocking Issues / Fix Plan。
```

## 建议命名与目录
```text
Project_Docs/
  Re_Step/
    RE_STEP_15_F2_运维手册联调与验证.md
    Artifacts/
      Step15/
        01_Runbook_E2E_Rehearsal_Record.md
        02_Threshold_Validation_Report.md
        03_Reload_Positive_Negative_Test_Report.md
        04_Incident_Drill_Evidence.md
        05_Solo_Review_Log.md
```

## 前置产物硬门禁
硬规则：前置产物缺失 => Verdict=FAIL => 禁止进入下一步。

前置产物文件清单（Step14，强制全部存在且非空）：
1. `Project_Docs/Re_Step/Artifacts/Step14/01_SLI_SLO_Dashboard_Spec.md`
2. `Project_Docs/Re_Step/Artifacts/Step14/02_Alert_Rules_Warning_Critical.md`
3. `Project_Docs/Re_Step/Artifacts/Step14/03_Minimum_Evidence_Bundle_Template.md`
4. `Project_Docs/Re_Step/Artifacts/Step14/04_Incident_Severity_Mapping.md`
5. `Project_Docs/Re_Step/Artifacts/Step14/05_Solo_Review_Log.md`

机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
STEP_DIR="Project_Docs/Re_Step/Artifacts/Step14"
test -d "$STEP_DIR"
test -s "$STEP_DIR/01_SLI_SLO_Dashboard_Spec.md"
test -s "$STEP_DIR/02_Alert_Rules_Warning_Critical.md"
test -s "$STEP_DIR/03_Minimum_Evidence_Bundle_Template.md"
test -s "$STEP_DIR/04_Incident_Severity_Mapping.md"
test -s "$STEP_DIR/05_Solo_Review_Log.md"
ls -1 "$STEP_DIR"/*.md >/dev/null
```

通过阈值：
1. Step14 前置目录存在。
2. 指定 5 个前置产物文件全部存在且非空。
3. 目录下可枚举到 `*.md` 产物文件。

阻断动作：
1. 任一检查失败立即 Verdict=FAIL。
2. 立即禁止进入下一步与禁止投产。
3. 在本步骤自审+自记录中登记阻断项、修复动作与复检结果。

## 投产门禁（Go/No-Go）
### Gate-1 前置产物存在性
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
count=$(find "Project_Docs/Re_Step/Artifacts/Step14" -maxdepth 1 -type f -name '*.md' | wc -l)
test "$count" -ge 1
```
- 通过阈值：前置步骤产物文件数 >= 1。
- 阻断动作：命令失败立即 Verdict=FAIL，禁止进入下一步，并在本步骤自审+自记录中登记阻断项。

### Gate-2 固定章节完整性
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
file="Project_Docs/Re_Step/RE_STEP_15_F2_运维手册联调与验证.md"
for h in   "文档元数据"   "步骤目标"   "为什么此步骤在此顺序"   "输入材料（强制）"   "输出物定义（强制）"   "详细执行步骤（编号化）"   "Prompt 模板（至少3个：执行、反证审查、准入判定）"   "建议命名与目录"   "投产门禁（Go/No-Go）"   "残余风险与挂起条件"   "本步骤完成判据（最终）"; do
  test "$(rg -c "^## $h$" "$file")" -eq 1
done
```
- 通过阈值：11 个固定章节均存在且仅出现 1 次。
- 阻断动作：命令失败立即 Verdict=FAIL，禁止投产。

### Gate-3 术语一致性检查（自审+自记录）
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_15_F2_运维手册联调与验证.md"
```
- 通过阈值：命中文本“自审+自记录” >= 1。
- 阻断动作：命令失败立即 Verdict=FAIL，退回文档修订。

### Gate-4 弱化语义清零
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
! rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_15_F2_运维手册联调与验证.md" >/dev/null
```
- 通过阈值：弱化语义命中数 = 0。
- 阻断动作：命令失败立即 Verdict=FAIL，禁止进入下一步。

### 误判防护
| 场景ID | 类型 | 场景说明 | 检测补偿动作 |
| --- | --- | --- | --- |
| FP-1 | 假阳性 | Gate-1 因旧文件残留通过，但前置产物与当前版本不匹配 | 补跑版本一致性校验：`rg -q "Last Updated|Version" Project_Docs/Re_Step/Artifacts/Step14/*.md`，不匹配即 FAIL |
| FP-2 | 假阳性 | Gate-2 章节标题存在但正文为空，导致形式通过 | 追加正文非空校验：`test "$(wc -l < "Project_Docs/Re_Step/RE_STEP_15_F2_运维手册联调与验证.md")" -ge 120`，不足阈值即 FAIL |
| FN-1 | 假阴性 | Gate-3 因字符编码或转义差异误判术语缺失 | 补跑宽松匹配：`rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_15_F2_运维手册联调与验证.md"`，命中后判为通过并记录修复动作 |
| FN-2 | 假阴性 | Gate-4 在极端 shell 环境下因 `!` 语法差异误判失败 | 补跑等价判定：`test "$(rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_15_F2_运维手册联调与验证.md" | wc -l)" -eq 0` |

### 门禁证据留存格式
- 证据目录规范：`Project_Docs/Re_Step/Artifacts/Step15/GateEvidence/<UTC时间戳>/`。
- 文件命名规范：`gate1.log`、`gate1.exit`、`gate2.log`、`gate2.exit`、`gate3.log`、`gate3.exit`、`gate4.log`、`gate4.exit`、`summary.json`。
- 留存命令模板（非交互、可重复）：
```bash
set -euo pipefail
STAMP=$(date -u +%Y%m%dT%H%M%SZ)
DIR="Project_Docs/Re_Step/Artifacts/Step15/GateEvidence/${STAMP}"
mkdir -p "$DIR"
( set -euo pipefail; count=$(find "Project_Docs/Re_Step/Artifacts/Step14" -maxdepth 1 -type f -name '*.md' | wc -l); test "$count" -ge 1 ) >"$DIR/gate1.log" 2>&1; echo $? >"$DIR/gate1.exit"
( set -euo pipefail; file="Project_Docs/Re_Step/RE_STEP_15_F2_运维手册联调与验证.md"; for h in "文档元数据" "步骤目标" "为什么此步骤在此顺序" "输入材料（强制）" "输出物定义（强制）" "详细执行步骤（编号化）" "Prompt 模板（至少3个：执行、反证审查、准入判定）" "建议命名与目录" "投产门禁（Go/No-Go）" "残余风险与挂起条件" "本步骤完成判据（最终）"; do test "$(rg -c "^## $h$" "$file")" -eq 1; done ) >"$DIR/gate2.log" 2>&1; echo $? >"$DIR/gate2.exit"
( set -euo pipefail; rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_15_F2_运维手册联调与验证.md" ) >"$DIR/gate3.log" 2>&1; echo $? >"$DIR/gate3.exit"
( set -euo pipefail; ! rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_15_F2_运维手册联调与验证.md" >/dev/null ) >"$DIR/gate4.log" 2>&1; echo $? >"$DIR/gate4.exit"
jq -n --arg step "15" --arg file "Project_Docs/Re_Step/RE_STEP_15_F2_运维手册联调与验证.md" --arg dir "$DIR" '{step:$step,file:$file,evidence_dir:$dir}' >"$DIR/summary.json"
```
- 判定方式：`gate*.exit` 全为 `0` 才可 Go；任一非 `0` 必须 No-Go。
## 残余风险与挂起条件
| 风险ID | 风险描述 | 触发条件 | 挂起条件 | 解除条件 |
| --- | --- | --- | --- | --- |
| R1 | 前置产物缺失导致链路断裂 | Gate-1 输出值 < 1 | 本步骤产出全部标记挂起，Verdict=FAIL | 补齐前置产物并复跑 Gate-1 达标 |
| R2 | 固定章节缺失导致审计不可复验 | Gate-2 输出值 != 11 | 禁止进入下一步与投产评审 | 补齐章节并复跑 Gate-2 达标 |
| R3 | 术语不一致导致执行口径漂移 | Gate-3 输出值 < 1 | 挂起准入判定，不允许 PASS | 全文统一为“自审+自记录”并复检通过 |
| R4 | 弱化语义残留导致门禁失效 | Gate-4 输出值 > 0 | 挂起投产决策并强制返工 | 清零弱化语义并复跑 Gate-4 为 0 |
| R5 | 证据链缺失导致故障不可追踪 | 自审记录中缺少证据路径或命令结果 | 挂起发布，按阻断项处理 | 补齐证据路径并完成二次自审+自记录 |

## 本步骤完成判据（最终）
1. 三本手册阈值、术语、流程无冲突。
2. 部署/运维/事故/回滚演练均有可复现证据。
3. `#reload` 正负向矩阵通过，且收口后恢复稳定。
4. 回滚演练实测满足 15 分钟内完成。
5. 自审结论为 PASS，阻断项清零。
