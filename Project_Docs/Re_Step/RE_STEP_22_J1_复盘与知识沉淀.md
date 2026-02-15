# Re_Step-22：J1 复盘与知识沉淀（单人维护版）

## 文档元数据
| 字段 | 值 |
| --- | --- |
| StepID | RE-STEP-22 |
| Version | 1.0.0 |
| Status | Ready |
| Owner | Solo Maintainer（你） |
| Last Updated | 2026-02-15 |
| 关联主计划 | `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`（阶段J/J1） |
| 证据来源 | `Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md`、`Project_Docs/INDEX.md`、`Project_Docs/GLOSSARY.md` |

## 步骤目标
形成可复用复盘资产：沉淀做对/做错/待优化，固化 ADR 与索引术语更新，确保后续版本能复用同一治理流程而非重复踩坑。

## 为什么此步骤在此顺序
I 阶段完成稳定与治理后，J1 负责结构化总结并转换为组织记忆；若不做沉淀，J2 长期机制会缺少高质量输入。

## 输入材料（强制）
1. `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`
2. `Project_Docs/SYSTEM_CRITIQUE_AND_REFACTOR_PAPER_V1.md`
3. `Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md`
4. `Project_Docs/INDEX.md`
5. `Project_Docs/GLOSSARY.md`
6. `Project_Docs/Architecture/SYSTEM_CONTEXT.md`
7. 上一步产物（强制存在，缺失则 Verdict=FAIL 并禁止进入下一步）：`Project_Docs/Re_Step/Artifacts/Step21/*`

## 输出物定义（强制）
目录：`Project_Docs/Re_Step/Artifacts/Step22/`

1. `01_Refactor_Retrospective_Report.md`
- 记录目标达成度、关键得失、偏差原因。

2. `02_ADR_Consolidation_Plan.md`
- 将关键决策整理为 ADR 列表与追踪关系。

3. `03_Index_Glossary_Update_Report.md`
- 记录索引与术语新增、修订、废弃项。

4. `04_Reusable_Playbook_Summary.md`
- 输出可复用流程模板与触发条件。

5. `05_Solo_Review_Log.md`
- 自审结论与准入判定。

## 详细执行步骤（编号化）
### 1. 收集阶段证据与结果
1. 汇总 A~I 阶段关键产物、门禁结果、事故与回滚记录。
2. 按目标、结果、偏差三列建立总表。
3. 标注证据路径与可信等级。

通过标准：
1. 覆盖所有阶段关键结论。
2. 每条结论可追溯到证据。

失败判据：
1. 结论无证据路径。
2. 关键阶段缺失总结。

### 2. 形成“做对/做错/待优化”复盘
1. 对每条关键决策给出正负面影响。
2. 提炼可复用经验与不可复用条件。
3. 给出后续优化优先级。

通过标准：
1. 复盘条目具体可执行。
2. 每条有后续动作要求。

失败判据：
1. 复盘停留在口号层面。
2. 缺少可执行改进项。

### 3. 沉淀 ADR 与决策链
1. 抽取关键架构/流程决策并编号。
2. 建立“决策 -> 实现 -> 证据”链路。
3. 标注仍有效/待废弃状态。

通过标准：
1. 关键决策有唯一 ADR 标识。
2. ADR 与现状一致。

失败判据：
1. ADR 缺少上下文与结论。
2. 决策状态不明导致误用。

### 4. 更新索引与术语
1. 将新增流程与文档入口纳入 `INDEX`。
2. 将新增术语、废弃术语纳入 `GLOSSARY`。
3. 校验引用可达性与术语一致性。

通过标准：
1. 索引无断链。
2. 术语无同义冲突。

失败判据：
1. 索引遗漏关键入口。
2. 同一术语出现多种定义。

### 5. 固化可复用模板
1. 提炼可复用模板：门禁检查、事故复盘、迁移演练、灰度评估。
2. 为每个模板定义触发条件与最小输入。
3. 给出禁用条件（何时不可套用）。

通过标准：
1. 模板可独立使用。
2. 触发/禁用条件明确。

失败判据：
1. 模板无法独立执行。
2. 无触发条件导致滥用。

### 6. 执行自审与准入判定
1. 检查沉淀材料完整性与可复用性。
2. 判定是否准入 J2。
3. 输出 PASS/FAIL。

通过标准：
1. 复盘材料结构完整。
2. 残余风险有明确去向。

失败判据：
1. 材料碎片化不可检索。
2. 准入结论缺证据支撑。

## Prompt 模板（至少3个：执行、反证审查、准入判定）
### Prompt-A（执行）
```text
请执行 J1“复盘与知识沉淀”，输入：
- Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md
- Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md
- Project_Docs/INDEX.md
- Project_Docs/GLOSSARY.md

要求：
1) 输出复盘报告、ADR沉淀、索引术语更新、可复用模板、自审日志。
2) 每条结论必须带证据路径与改进动作。
3) 单人维护模式，仅自审+自记录。
```

### Prompt-B（反证审查）
```text
请反证审查 J1 产物：
1) 找出 10 条“复盘结论不可操作”的问题。
2) 找出 5 条“术语/索引漂移”风险。
3) 给出每条风险的修复动作与截止时间。
4) 输出最可能导致知识失真的3个点。
```

### Prompt-C（准入判定）
```text
请对 J1 进行准入判定（PASS/FAIL）：
检查对象：
- 01_Refactor_Retrospective_Report.md
- 02_ADR_Consolidation_Plan.md
- 03_Index_Glossary_Update_Report.md
- 04_Reusable_Playbook_Summary.md
- 05_Solo_Review_Log.md

判定规则：
1) 复盘结论是否有证据和行动项。
2) ADR 是否完整且状态明确。
3) INDEX/GLOSSARY 是否更新且可达。
4) 模板是否可复用且边界清晰。

输出：Verdict / Blocking Issues / Fix Plan。
```

## 建议命名与目录
```text
Project_Docs/
  Re_Step/
    RE_STEP_22_J1_复盘与知识沉淀.md
    Artifacts/
      Step22/
        01_Refactor_Retrospective_Report.md
        02_ADR_Consolidation_Plan.md
        03_Index_Glossary_Update_Report.md
        04_Reusable_Playbook_Summary.md
        05_Solo_Review_Log.md
```

## 前置产物硬门禁
硬规则：前置产物缺失 => Verdict=FAIL => 禁止进入下一步。

前置产物文件清单（Step21，强制全部存在且非空）：
1. `Project_Docs/Re_Step/Artifacts/Step21/01_OpenSource_Governance_Checklist.md`
2. `Project_Docs/Re_Step/Artifacts/Step21/02_Sanitized_Example_Config_Spec.md`
3. `Project_Docs/Re_Step/Artifacts/Step21/03_External_Contributor_Onboarding_Test.md`
4. `Project_Docs/Re_Step/Artifacts/Step21/04_Release_Artifact_Layout_Spec.md`
5. `Project_Docs/Re_Step/Artifacts/Step21/05_Solo_Review_Log.md`

机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
STEP_DIR="Project_Docs/Re_Step/Artifacts/Step21"
test -d "$STEP_DIR"
test -s "$STEP_DIR/01_OpenSource_Governance_Checklist.md"
test -s "$STEP_DIR/02_Sanitized_Example_Config_Spec.md"
test -s "$STEP_DIR/03_External_Contributor_Onboarding_Test.md"
test -s "$STEP_DIR/04_Release_Artifact_Layout_Spec.md"
test -s "$STEP_DIR/05_Solo_Review_Log.md"
ls -1 "$STEP_DIR"/*.md >/dev/null
```

通过阈值：
1. Step21 前置目录存在。
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
count=$(find "Project_Docs/Re_Step/Artifacts/Step21" -maxdepth 1 -type f -name '*.md' | wc -l)
test "$count" -ge 1
```
- 通过阈值：前置步骤产物文件数 >= 1。
- 阻断动作：命令失败立即 Verdict=FAIL，禁止进入下一步，并在本步骤自审+自记录中登记阻断项。

### Gate-2 固定章节完整性
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
file="Project_Docs/Re_Step/RE_STEP_22_J1_复盘与知识沉淀.md"
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
rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_22_J1_复盘与知识沉淀.md"
```
- 通过阈值：命中文本“自审+自记录” >= 1。
- 阻断动作：命令失败立即 Verdict=FAIL，退回文档修订。

### Gate-4 弱化语义清零
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
! rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_22_J1_复盘与知识沉淀.md" >/dev/null
```
- 通过阈值：弱化语义命中数 = 0。
- 阻断动作：命令失败立即 Verdict=FAIL，禁止进入下一步。

### 误判防护
| 场景ID | 类型 | 场景说明 | 检测补偿动作 |
| --- | --- | --- | --- |
| FP-1 | 假阳性 | Gate-1 因旧文件残留通过，但前置产物与当前版本不匹配 | 补跑版本一致性校验：`rg -q "Last Updated|Version" Project_Docs/Re_Step/Artifacts/Step21/*.md`，不匹配即 FAIL |
| FP-2 | 假阳性 | Gate-2 章节标题存在但正文为空，导致形式通过 | 追加正文非空校验：`test "$(wc -l < "Project_Docs/Re_Step/RE_STEP_22_J1_复盘与知识沉淀.md")" -ge 120`，不足阈值即 FAIL |
| FN-1 | 假阴性 | Gate-3 因字符编码或转义差异误判术语缺失 | 补跑宽松匹配：`rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_22_J1_复盘与知识沉淀.md"`，命中后判为通过并记录修复动作 |
| FN-2 | 假阴性 | Gate-4 在极端 shell 环境下因 `!` 语法差异误判失败 | 补跑等价判定：`test "$(rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_22_J1_复盘与知识沉淀.md" | wc -l)" -eq 0` |

### 门禁证据留存格式
- 证据目录规范：`Project_Docs/Re_Step/Artifacts/Step22/GateEvidence/<UTC时间戳>/`。
- 文件命名规范：`gate1.log`、`gate1.exit`、`gate2.log`、`gate2.exit`、`gate3.log`、`gate3.exit`、`gate4.log`、`gate4.exit`、`summary.json`。
- 留存命令模板（非交互、可重复）：
```bash
set -euo pipefail
STAMP=$(date -u +%Y%m%dT%H%M%SZ)
DIR="Project_Docs/Re_Step/Artifacts/Step22/GateEvidence/${STAMP}"
mkdir -p "$DIR"
( set -euo pipefail; count=$(find "Project_Docs/Re_Step/Artifacts/Step21" -maxdepth 1 -type f -name '*.md' | wc -l); test "$count" -ge 1 ) >"$DIR/gate1.log" 2>&1; echo $? >"$DIR/gate1.exit"
( set -euo pipefail; file="Project_Docs/Re_Step/RE_STEP_22_J1_复盘与知识沉淀.md"; for h in "文档元数据" "步骤目标" "为什么此步骤在此顺序" "输入材料（强制）" "输出物定义（强制）" "详细执行步骤（编号化）" "Prompt 模板（至少3个：执行、反证审查、准入判定）" "建议命名与目录" "投产门禁（Go/No-Go）" "残余风险与挂起条件" "本步骤完成判据（最终）"; do test "$(rg -c "^## $h$" "$file")" -eq 1; done ) >"$DIR/gate2.log" 2>&1; echo $? >"$DIR/gate2.exit"
( set -euo pipefail; rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_22_J1_复盘与知识沉淀.md" ) >"$DIR/gate3.log" 2>&1; echo $? >"$DIR/gate3.exit"
( set -euo pipefail; ! rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_22_J1_复盘与知识沉淀.md" >/dev/null ) >"$DIR/gate4.log" 2>&1; echo $? >"$DIR/gate4.exit"
jq -n --arg step "22" --arg file "Project_Docs/Re_Step/RE_STEP_22_J1_复盘与知识沉淀.md" --arg dir "$DIR" '{step:$step,file:$file,evidence_dir:$dir}' >"$DIR/summary.json"
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
1. A~I 阶段复盘结论完整且证据可追溯。
2. 关键决策已沉淀为 ADR 并标注状态。
3. INDEX 与 GLOSSARY 已更新并校验可达。
4. 可复用模板可独立执行且边界明确。
5. 自审通过并准入 J2。
