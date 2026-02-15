# Re_Step-23：J2 长期治理机制（单人维护版）

## 文档元数据
| 字段 | 值 |
| --- | --- |
| StepID | RE-STEP-23 |
| Version | 1.0.0 |
| Status | Ready |
| Owner | Solo Maintainer（你） |
| Last Updated | 2026-02-15 |
| 关联主计划 | `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`（阶段J/J2） |
| 证据来源 | `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`、`Project_Docs/Contracts/*.md`、`Project_Docs/Architecture/*.md`、`Project_Docs/Manuals/*.md` |

## 步骤目标
建立事件触发型长期治理机制，使系统在“重构中、功能变更后、重大安全事件后”都能自动触发一致性巡检、故障演练、版本兼容审计，进入可持续演进状态。

## 为什么此步骤在此顺序
J1 已沉淀经验，J2 负责将经验制度化。若没有 J2，流程会在一次重构后失效并重新退化为临时救火。

## 输入材料（强制）
1. `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`
2. `Project_Docs/Contracts/CONTRACT_INDEX.md`
3. `Project_Docs/Architecture/VERSIONING_AND_COMPATIBILITY.md`
4. `Project_Docs/Architecture/FAILURE_MODEL.md`
5. `Project_Docs/Manuals/RELEASE_CHECKLIST.md`
6. `Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md`
7. `Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md`
8. 上一步产物（强制存在，缺失则 Verdict=FAIL 并禁止进入下一步）：`Project_Docs/Re_Step/Artifacts/Step22/*`

## 输出物定义（强制）
目录：`Project_Docs/Re_Step/Artifacts/Step23/`

1. `01_Event_Driven_Governance_Policy.md`
- 定义事件触发条件、触发动作、最小证据要求。

2. `02_Consistency_Audit_Playbook.md`
- 定义合同一致性巡检流程与阻断规则。

3. `03_Drill_And_Compatibility_Audit_Plan.md`
- 定义故障演练与版本兼容审计周期（事件驱动）。

4. `04_Governance_KPI_And_Reporting.md`
- 定义治理成效指标与报告模板。

5. `05_Solo_Review_Log.md`
- 自审记录与最终结论。

## 详细执行步骤（编号化）
### 1. 定义事件触发模型
1. 固化触发事件：重构执行中、功能变更后、重大安全事件后、发布前。
2. 为每个事件定义必执行动作与时限。
3. 定义未触发执行的阻断后果。

通过标准：
1. 触发事件清单完整且无歧义。
2. 每个事件有时限与责任动作。

失败判据：
1. 事件触发条件模糊无法判定。
2. 触发后无强制动作。

### 2. 建立合同一致性巡检机制
1. 对合同、实现、手册做事件驱动巡检。
2. 定义巡检输出：差异、风险、修复计划、截止时间。
3. 对 P0 漂移设置发布阻断。

通过标准：
1. 巡检结果可机检与可审计。
2. P0 漂移可自动阻断。

失败判据：
1. 巡检仅人工口头结论。
2. 漂移发现后无修复闭环。

### 3. 建立故障演练常态机制
1. 定义发布前、关键链路改造后、事故复盘后必须演练。
2. 固化演练最小覆盖：超时、断连、回滚、补偿。
3. 固化演练失败后的升级路径。

通过标准：
1. 演练触发条件明确。
2. 失败升级路径可执行。

失败判据：
1. 演练可长期跳过。
2. 演练失败未触发升级。

### 4. 建立版本兼容与弃用审计机制
1. 对协议版本、特性开关、弃用窗口执行事件驱动审计。
2. 定义兼容违规处理（回退、延期、禁发）。
3. 输出兼容矩阵与风险趋势。

通过标准：
1. 兼容审计能覆盖协议/字段/命令/错误码。
2. 违规处理动作可执行。

失败判据：
1. 兼容违规仅记录不处置。
2. 弃用窗口无遥测依据。

### 5. 定义治理 KPI 与报告节奏
1. 定义 KPI：巡检及时率、阻断命中率、回滚演练达标率、RCA 完成率。
2. 定义报告触发：事件后 24 小时内出具。
3. 定义超标与改进动作。

通过标准：
1. KPI 可量化、可计算。
2. 报告模板固定且可追踪。

失败判据：
1. KPI 无数据源。
2. 报告无结论或无行动项。

### 6. 执行自审与最终收口判定
1. 自审治理机制是否可持续执行至少一个版本周期。
2. 列出残余风险与后续计划。
3. 输出最终 PASS/FAIL 与长期维护注意项。

通过标准：
1. 机制可执行、可验证、可审计。
2. 最终结论与证据一致。

失败判据：
1. 机制依赖多人审批或固定人力节奏。
2. 结论缺失证据或不可复验。

## Prompt 模板（至少3个：执行、反证审查、准入判定）
### Prompt-A（执行）
```text
请执行 J2“长期治理机制”建设，输入：
- Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md
- Project_Docs/Contracts/*.md
- Project_Docs/Architecture/*.md
- Project_Docs/Manuals/*.md

要求：
1) 输出事件触发治理策略、一致性巡检方案、演练与兼容审计方案、治理KPI报告模板、自审日志。
2) 单人维护模式，仅自审+自记录。
3) 每项必须给出通过标准和失败判据。
```

### Prompt-B（反证审查）
```text
请反证审查 J2 治理机制：
1) 找出 10 个“制度存在但无法执行”的点。
2) 找出 5 个“事件触发会漏掉高风险变更”的条件漏洞。
3) 给出修复动作、验证方法与阻断级别。
4) 输出最脆弱的3个治理断点。
```

### Prompt-C（准入判定）
```text
请对 J2 最终机制做准入判定（PASS/FAIL）：
检查对象：
- 01_Event_Driven_Governance_Policy.md
- 02_Consistency_Audit_Playbook.md
- 03_Drill_And_Compatibility_Audit_Plan.md
- 04_Governance_KPI_And_Reporting.md
- 05_Solo_Review_Log.md

判定规则：
1) 事件触发条件是否完整且可判定。
2) 巡检/演练/兼容审计是否具备阻断能力。
3) KPI 是否可量化与可追踪。
4) 是否符合单人维护“自审+自记录”模式。

输出：Verdict / Blocking Issues / Fix Plan。
```

## 建议命名与目录
```text
Project_Docs/
  Re_Step/
    RE_STEP_23_J2_长期治理机制.md
    Artifacts/
      Step23/
        01_Event_Driven_Governance_Policy.md
        02_Consistency_Audit_Playbook.md
        03_Drill_And_Compatibility_Audit_Plan.md
        04_Governance_KPI_And_Reporting.md
        05_Solo_Review_Log.md
```

## 前置产物硬门禁
硬规则：前置产物缺失 => Verdict=FAIL => 禁止进入下一步。

前置产物文件清单（Step22，强制全部存在且非空）：
1. `Project_Docs/Re_Step/Artifacts/Step22/01_Refactor_Retrospective_Report.md`
2. `Project_Docs/Re_Step/Artifacts/Step22/02_ADR_Consolidation_Plan.md`
3. `Project_Docs/Re_Step/Artifacts/Step22/03_Index_Glossary_Update_Report.md`
4. `Project_Docs/Re_Step/Artifacts/Step22/04_Reusable_Playbook_Summary.md`
5. `Project_Docs/Re_Step/Artifacts/Step22/05_Solo_Review_Log.md`

机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
STEP_DIR="Project_Docs/Re_Step/Artifacts/Step22"
test -d "$STEP_DIR"
test -s "$STEP_DIR/01_Refactor_Retrospective_Report.md"
test -s "$STEP_DIR/02_ADR_Consolidation_Plan.md"
test -s "$STEP_DIR/03_Index_Glossary_Update_Report.md"
test -s "$STEP_DIR/04_Reusable_Playbook_Summary.md"
test -s "$STEP_DIR/05_Solo_Review_Log.md"
ls -1 "$STEP_DIR"/*.md >/dev/null
```

通过阈值：
1. Step22 前置目录存在。
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
count=$(find "Project_Docs/Re_Step/Artifacts/Step22" -maxdepth 1 -type f -name '*.md' | wc -l)
test "$count" -ge 1
```
- 通过阈值：前置步骤产物文件数 >= 1。
- 阻断动作：命令失败立即 Verdict=FAIL，禁止进入下一步，并在本步骤自审+自记录中登记阻断项。

### Gate-2 固定章节完整性
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
file="Project_Docs/Re_Step/RE_STEP_23_J2_长期治理机制.md"
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
rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_23_J2_长期治理机制.md"
```
- 通过阈值：命中文本“自审+自记录” >= 1。
- 阻断动作：命令失败立即 Verdict=FAIL，退回文档修订。

### Gate-4 弱化语义清零
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
! rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_23_J2_长期治理机制.md" >/dev/null
```
- 通过阈值：弱化语义命中数 = 0。
- 阻断动作：命令失败立即 Verdict=FAIL，禁止进入下一步。

### 误判防护
| 场景ID | 类型 | 场景说明 | 检测补偿动作 |
| --- | --- | --- | --- |
| FP-1 | 假阳性 | Gate-1 因旧文件残留通过，但前置产物与当前版本不匹配 | 补跑版本一致性校验：`rg -q "Last Updated|Version" Project_Docs/Re_Step/Artifacts/Step22/*.md`，不匹配即 FAIL |
| FP-2 | 假阳性 | Gate-2 章节标题存在但正文为空，导致形式通过 | 追加正文非空校验：`test "$(wc -l < "Project_Docs/Re_Step/RE_STEP_23_J2_长期治理机制.md")" -ge 120`，不足阈值即 FAIL |
| FN-1 | 假阴性 | Gate-3 因字符编码或转义差异误判术语缺失 | 补跑宽松匹配：`rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_23_J2_长期治理机制.md"`，命中后判为通过并记录修复动作 |
| FN-2 | 假阴性 | Gate-4 在极端 shell 环境下因 `!` 语法差异误判失败 | 补跑等价判定：`test "$(rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_23_J2_长期治理机制.md" | wc -l)" -eq 0` |

### 门禁证据留存格式
- 证据目录规范：`Project_Docs/Re_Step/Artifacts/Step23/GateEvidence/<UTC时间戳>/`。
- 文件命名规范：`gate1.log`、`gate1.exit`、`gate2.log`、`gate2.exit`、`gate3.log`、`gate3.exit`、`gate4.log`、`gate4.exit`、`summary.json`。
- 留存命令模板（非交互、可重复）：
```bash
set -euo pipefail
STAMP=$(date -u +%Y%m%dT%H%M%SZ)
DIR="Project_Docs/Re_Step/Artifacts/Step23/GateEvidence/${STAMP}"
mkdir -p "$DIR"
( set -euo pipefail; count=$(find "Project_Docs/Re_Step/Artifacts/Step22" -maxdepth 1 -type f -name '*.md' | wc -l); test "$count" -ge 1 ) >"$DIR/gate1.log" 2>&1; echo $? >"$DIR/gate1.exit"
( set -euo pipefail; file="Project_Docs/Re_Step/RE_STEP_23_J2_长期治理机制.md"; for h in "文档元数据" "步骤目标" "为什么此步骤在此顺序" "输入材料（强制）" "输出物定义（强制）" "详细执行步骤（编号化）" "Prompt 模板（至少3个：执行、反证审查、准入判定）" "建议命名与目录" "投产门禁（Go/No-Go）" "残余风险与挂起条件" "本步骤完成判据（最终）"; do test "$(rg -c "^## $h$" "$file")" -eq 1; done ) >"$DIR/gate2.log" 2>&1; echo $? >"$DIR/gate2.exit"
( set -euo pipefail; rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_23_J2_长期治理机制.md" ) >"$DIR/gate3.log" 2>&1; echo $? >"$DIR/gate3.exit"
( set -euo pipefail; ! rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_23_J2_长期治理机制.md" >/dev/null ) >"$DIR/gate4.log" 2>&1; echo $? >"$DIR/gate4.exit"
jq -n --arg step "23" --arg file "Project_Docs/Re_Step/RE_STEP_23_J2_长期治理机制.md" --arg dir "$DIR" '{step:$step,file:$file,evidence_dir:$dir}' >"$DIR/summary.json"
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
1. 事件触发型治理策略覆盖重构、开发、发布、事故四类核心场景。
2. 合同巡检、故障演练、兼容审计都具备可执行与阻断能力。
3. 治理 KPI 可持续计算并驱动改进闭环。
4. 全流程符合单人维护“自审+自记录”。
5. 最终结论可审计、可复验、可复用。
