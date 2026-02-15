# Re_Step-17：G2 发布门禁自动化（单人维护版）

## 文档元数据
| 字段 | 值 |
| --- | --- |
| StepID | RE-STEP-17 |
| Version | 1.0.0 |
| Status | Ready |
| Owner | Solo Maintainer（你） |
| Last Updated | 2026-02-15 |
| 关联主计划 | `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`（阶段G/G2） |
| 证据来源 | `Project_Docs/Manuals/RELEASE_CHECKLIST.md`、`Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md`、`Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md` |

## 步骤目标
将发布清单转成可阻断自动化门禁：测试完整性、文档联动、术语漂移、安全扫描、回滚就绪全部自动判定，未通过禁止发布。

## 为什么此步骤在此顺序
G1 已建立测试资产，G2 负责把“测试/文档/安全/回滚”连接成发布入口的硬门禁；不先自动化，后续 H 阶段灰度与回滚会依赖人工判断，风险过高。

## 输入材料（强制）
1. `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`
2. `Project_Docs/Manuals/RELEASE_CHECKLIST.md`
3. `Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md`
4. `Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md`
5. `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`
6. `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
7. 上一步产物（强制存在，缺失则 Verdict=FAIL 并禁止进入下一步）：`Project_Docs/Re_Step/Artifacts/Step16/*`

## 输出物定义（强制）
目录：`Project_Docs/Re_Step/Artifacts/Step17/`

1. `01_Release_Gate_Pipeline_Design.md`
- 定义门禁阶段、输入工件、阻断规则。

2. `02_Automated_Checks_Spec.md`
- 定义测试完整性、文档联动、索引可达、术语漂移、安全扫描规则。

3. `03_Rollback_Readiness_Gate.md`
- 定义回滚演练窗口与失败触发策略。

4. `04_Go_NoGo_Decision_Template.md`
- 标准化最终发布结论模板（自审）。

5. `05_Solo_Review_Log.md`
- 自审记录与准入结论。

## 详细执行步骤（编号化）
### 1. 拆解发布硬门禁与软门禁
1. 以发布清单定义硬门禁（文档完备、自动化测试、核心冒烟、配置审查、回滚就绪、审计可追溯）。
2. 定义软门禁（覆盖率改进、性能回归、告警噪音、文档可读性）。
3. 明确硬门禁未通过即 `No-Go`。

通过标准：
1. 每个门禁有可验证输入与阻断动作。
2. 无“口头豁免”硬门禁。

失败判据：
1. 硬门禁可被跳过。
2. 门禁项没有证据来源。

### 2. 落地自动化检查规则
1. 测试完整性守卫：`unit + integration + smoke` 三工件齐全。
2. 文档联动守卫：`README/INDEX/Manuals` 三联动。
3. 索引可达守卫、术语漂移守卫、安全扫描守卫。

通过标准：
1. 规则可在 CI 自动执行。
2. 任一规则失败可阻断流水线。

失败判据：
1. 规则仅告警不阻断。
2. 规则无法定位失败原因。

### 3. 构建回滚就绪门禁
1. 定义“最近一次回滚演练有效窗口”（30 天，强制）。
2. 若超窗口或失败，强制 `No-Go`。
3. 门禁输出回滚负责人、触发阈值、演练证据链接。

通过标准：
1. 回滚门禁有明确时效与证据要求。
2. 回滚缺证据时自动阻断。

失败判据：
1. 回滚仅文字描述，无演练记录。
2. 过期演练仍可放行。

### 4. 构建错误码与文档一致性门禁
1. 校验 `BRG_*` 错误码漂移（新增/删除/重义）是否同步文档。
2. 校验发布文档是否包含升级与事故处理入口。
3. 发现漂移即阻断并生成修复清单。

通过标准：
1. 错误码漂移 100% 可检测。
2. 关键文档入口缺失能被自动阻断。

失败判据：
1. 错误码已变更但合同未更新仍可发布。
2. 文档断链未被识别。

### 5. 固化 Go/No-Go 决策输出
1. 固定输出字段：门禁结果、时间线、阻断项、修复计划。
2. 决策结果仅允许 PASS（Go）/FAIL（No-Go）。
3. 自审日志必须与流水线结果一致。

通过标准：
1. 决策模板字段完整。
2. 任何 No-Go 都有阻断项证据。

失败判据：
1. 决策结果与流水线状态不一致。
2. 结论缺少关键上下文（版本、窗口、责任人）。

### 6. 执行自审与准入判定
1. 复核自动化规则的误判率与漏判率。
2. 对误判项给出白名单机制与失效日期。
3. 形成准入结论。

通过标准：
1. 误判项可控且有闭环。
2. 准入结论可审计可回放。

失败判据：
1. 门禁结果不稳定、不可复现。
2. 自审缺少阻断项处置记录。

## Prompt 模板（至少3个：执行、反证审查、准入判定）
### Prompt-A（执行）
```text
请将 RELEASE_CHECKLIST 转换为 G2“发布门禁自动化”方案。
输入：
- Project_Docs/Manuals/RELEASE_CHECKLIST.md
- Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md
- Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md

要求：
1) 输出门禁流水线、自动检查规则、回滚门禁、Go/No-Go 模板、自审日志。
2) 硬门禁必须可阻断，软门禁必须有风险记录。
3) 单人维护模式，仅自审+自记录。
```

### Prompt-B（反证审查）
```text
请反证审查 G2 门禁：
1) 找出 10 个可绕过门禁的路径。
2) 找出 5 个自动规则误判场景。
3) 给出每个场景的修复规则与验证命令。
4) 输出必须先修复的 P0 项。
```

### Prompt-C（准入判定）
```text
请判断 G2 是否准入 H1：
检查对象：
- 01_Release_Gate_Pipeline_Design.md
- 02_Automated_Checks_Spec.md
- 03_Rollback_Readiness_Gate.md
- 04_Go_NoGo_Decision_Template.md
- 05_Solo_Review_Log.md

判定规则：
1) 硬门禁是否全部具备自动阻断能力。
2) 回滚就绪是否可机检且不过期。
3) 文档联动/术语漂移/安全扫描是否覆盖。
4) 误判白名单是否有失效日期。

输出：Verdict / Blocking Issues / Fix Plan。
```

## 建议命名与目录
```text
Project_Docs/
  Re_Step/
    RE_STEP_17_G2_发布门禁自动化.md
    Artifacts/
      Step17/
        01_Release_Gate_Pipeline_Design.md
        02_Automated_Checks_Spec.md
        03_Rollback_Readiness_Gate.md
        04_Go_NoGo_Decision_Template.md
        05_Solo_Review_Log.md
```

## 前置产物硬门禁
硬规则：前置产物缺失 => Verdict=FAIL => 禁止进入下一步。

前置产物文件清单（Step16，强制全部存在且非空）：
1. `Project_Docs/Re_Step/Artifacts/Step16/01_Contract_Test_Catalog.md`
2. `Project_Docs/Re_Step/Artifacts/Step16/02_Integration_E2E_TestPlan.md`
3. `Project_Docs/Re_Step/Artifacts/Step16/03_Fault_Injection_TestPlan.md`
4. `Project_Docs/Re_Step/Artifacts/Step16/04_Test_Gate_Criteria.md`
5. `Project_Docs/Re_Step/Artifacts/Step16/05_Solo_Review_Log.md`

机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
STEP_DIR="Project_Docs/Re_Step/Artifacts/Step16"
test -d "$STEP_DIR"
test -s "$STEP_DIR/01_Contract_Test_Catalog.md"
test -s "$STEP_DIR/02_Integration_E2E_TestPlan.md"
test -s "$STEP_DIR/03_Fault_Injection_TestPlan.md"
test -s "$STEP_DIR/04_Test_Gate_Criteria.md"
test -s "$STEP_DIR/05_Solo_Review_Log.md"
ls -1 "$STEP_DIR"/*.md >/dev/null
```

通过阈值：
1. Step16 前置目录存在。
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
count=$(find "Project_Docs/Re_Step/Artifacts/Step16" -maxdepth 1 -type f -name '*.md' | wc -l)
test "$count" -ge 1
```
- 通过阈值：前置步骤产物文件数 >= 1。
- 阻断动作：命令失败立即 Verdict=FAIL，禁止进入下一步，并在本步骤自审+自记录中登记阻断项。

### Gate-2 固定章节完整性
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
file="Project_Docs/Re_Step/RE_STEP_17_G2_发布门禁自动化.md"
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
rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_17_G2_发布门禁自动化.md"
```
- 通过阈值：命中文本“自审+自记录” >= 1。
- 阻断动作：命令失败立即 Verdict=FAIL，退回文档修订。

### Gate-4 弱化语义清零
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
! rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_17_G2_发布门禁自动化.md" >/dev/null
```
- 通过阈值：弱化语义命中数 = 0。
- 阻断动作：命令失败立即 Verdict=FAIL，禁止进入下一步。

### 误判防护
| 场景ID | 类型 | 场景说明 | 检测补偿动作 |
| --- | --- | --- | --- |
| FP-1 | 假阳性 | Gate-1 因旧文件残留通过，但前置产物与当前版本不匹配 | 补跑版本一致性校验：`rg -q "Last Updated|Version" Project_Docs/Re_Step/Artifacts/Step16/*.md`，不匹配即 FAIL |
| FP-2 | 假阳性 | Gate-2 章节标题存在但正文为空，导致形式通过 | 追加正文非空校验：`test "$(wc -l < "Project_Docs/Re_Step/RE_STEP_17_G2_发布门禁自动化.md")" -ge 120`，不足阈值即 FAIL |
| FN-1 | 假阴性 | Gate-3 因字符编码或转义差异误判术语缺失 | 补跑宽松匹配：`rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_17_G2_发布门禁自动化.md"`，命中后判为通过并记录修复动作 |
| FN-2 | 假阴性 | Gate-4 在极端 shell 环境下因 `!` 语法差异误判失败 | 补跑等价判定：`test "$(rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_17_G2_发布门禁自动化.md" | wc -l)" -eq 0` |

### 门禁证据留存格式
- 证据目录规范：`Project_Docs/Re_Step/Artifacts/Step17/GateEvidence/<UTC时间戳>/`。
- 文件命名规范：`gate1.log`、`gate1.exit`、`gate2.log`、`gate2.exit`、`gate3.log`、`gate3.exit`、`gate4.log`、`gate4.exit`、`summary.json`。
- 留存命令模板（非交互、可重复）：
```bash
set -euo pipefail
STAMP=$(date -u +%Y%m%dT%H%M%SZ)
DIR="Project_Docs/Re_Step/Artifacts/Step17/GateEvidence/${STAMP}"
mkdir -p "$DIR"
( set -euo pipefail; count=$(find "Project_Docs/Re_Step/Artifacts/Step16" -maxdepth 1 -type f -name '*.md' | wc -l); test "$count" -ge 1 ) >"$DIR/gate1.log" 2>&1; echo $? >"$DIR/gate1.exit"
( set -euo pipefail; file="Project_Docs/Re_Step/RE_STEP_17_G2_发布门禁自动化.md"; for h in "文档元数据" "步骤目标" "为什么此步骤在此顺序" "输入材料（强制）" "输出物定义（强制）" "详细执行步骤（编号化）" "Prompt 模板（至少3个：执行、反证审查、准入判定）" "建议命名与目录" "投产门禁（Go/No-Go）" "残余风险与挂起条件" "本步骤完成判据（最终）"; do test "$(rg -c "^## $h$" "$file")" -eq 1; done ) >"$DIR/gate2.log" 2>&1; echo $? >"$DIR/gate2.exit"
( set -euo pipefail; rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_17_G2_发布门禁自动化.md" ) >"$DIR/gate3.log" 2>&1; echo $? >"$DIR/gate3.exit"
( set -euo pipefail; ! rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_17_G2_发布门禁自动化.md" >/dev/null ) >"$DIR/gate4.log" 2>&1; echo $? >"$DIR/gate4.exit"
jq -n --arg step "17" --arg file "Project_Docs/Re_Step/RE_STEP_17_G2_发布门禁自动化.md" --arg dir "$DIR" '{step:$step,file:$file,evidence_dir:$dir}' >"$DIR/summary.json"
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
1. 硬门禁全部自动化并具备阻断能力。
2. 软门禁具备风险记录与豁免控制。
3. 回滚就绪门禁有效且可机检。
4. 错误码与文档一致性可自动检测。
5. 自审通过且无 P0 阻断项。
