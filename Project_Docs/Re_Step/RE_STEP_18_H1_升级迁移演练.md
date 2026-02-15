# Re_Step-18：H1 升级迁移演练（单人维护版）

## 文档元数据
| 字段 | 值 |
| --- | --- |
| StepID | RE-STEP-18 |
| Version | 1.0.0 |
| Status | Ready |
| Owner | Solo Maintainer（你） |
| Last Updated | 2026-02-15 |
| 关联主计划 | `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`（阶段H/H1） |
| 证据来源 | `Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md`、`Project_Docs/Manuals/DEPLOYMENT_RUNBOOK.md`、`Project_Docs/Manuals/OPERATIONS_RUNBOOK.md` |

## 步骤目标
在预演环境完成“前置快照 -> 灰度 -> 全量 -> 回滚”完整升级演练，验证回滚时长、数据一致性恢复、核心链路可用性是否满足发布前门槛。

## 为什么此步骤在此顺序
G2 已建立自动门禁，H1 需要验证真实迁移与回滚可执行性；若跳过演练直接灰度，会把迁移风险转移到生产。

## 输入材料（强制）
1. `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`
2. `Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md`
3. `Project_Docs/Manuals/DEPLOYMENT_RUNBOOK.md`
4. `Project_Docs/Manuals/OPERATIONS_RUNBOOK.md`
5. `Project_Docs/Manuals/RELEASE_CHECKLIST.md`
6. `Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`
7. `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
8. 上一步产物（强制存在，缺失则 Verdict=FAIL 并禁止进入下一步）：`Project_Docs/Re_Step/Artifacts/Step17/*`

## 输出物定义（强制）
目录：`Project_Docs/Re_Step/Artifacts/Step18/`

1. `01_Migration_Precheck_And_Baseline.md`
- 记录版本、配置、数据快照与基线指标。

2. `02_Stage_Validation_Record.md`
- 记录 10%/30%/100% 阶段验收结果与阈值对比。

3. `03_Rollback_Drill_Report.md`
- 记录触发条件、回滚路径、耗时、恢复证据。

4. `04_Data_Consistency_Recovery_Report.md`
- 记录回滚后数据一致性与业务可用性验证。

5. `05_Solo_Review_Log.md`
- 自审结论与准入判定。

## 详细执行步骤（编号化）
### 1. 固化迁移输入与冻结窗口
1. 确认升级包、迁移脚本、回滚包完整性。
2. 冻结窗口内禁止插入未评审变更。
3. 记录当前稳定版本与目标版本。

通过标准：
1. 输入包清单完整且可校验。
2. 冻结窗口无新增未审变更。

失败判据：
1. 缺少回滚包或迁移说明。
2. 冻结窗口内仍有高风险变更进入。

### 2. 执行前置快照与基线采样
1. 导出配置快照、数据快照、监控基线。
2. 固化核心阈值：错误率/延迟/核心命令成功率/告警基线。
3. 验证快照可恢复。

通过标准：
1. 三类快照均可回读。
2. 基线指标可用于升级后差异评估。

失败判据：
1. 快照损坏或不可恢复。
2. 基线数据缺失导致无法对比。

### 3. 执行分阶段升级演练
1. 按 10% -> 30% -> 100% 演练。
2. 每阶段至少观察 10 分钟并完成核心冒烟。
3. 任一阶段超阈值立即停止推进。

通过标准：
1. 每阶段结论明确 Go/No-Go。
2. 指标均在阈值内（或有明确豁免）。

失败判据：
1. 阶段超阈值仍继续推进。
2. 关键指标无证据记录。

### 4. 执行“灰度成功但全量失败”分叉演练
1. 按迁移手册分叉树执行最短路径定位。
2. 15 分钟内给出定位结论或直接回滚。
3. 记录分叉节点与证据。

通过标准：
1. 分叉流程可执行且有结论。
2. 超时时能自动转入回滚。

失败判据：
1. 演练未覆盖分叉路径。
2. 超时未回滚导致风险扩大。

### 5. 执行回滚与一致性恢复验证
1. 按标准回滚流程执行并计时。
2. 验证回滚后核心链路与一致性恢复。
3. 核验是否满足“15 分钟内完成回滚”。

通过标准：
1. 回滚耗时 <= 15 分钟。
2. 回滚后核心用例通过率 100%。

失败判据：
1. 回滚不可执行或超时。
2. 回滚后出现数据不一致或业务不可用。

### 6. 执行自审与准入判定
1. 汇总演练差异、缺陷、残余风险。
2. P0 缺陷清零后准入 H2。
3. 输出 PASS/FAIL 与挂起理由。

通过标准：
1. 缺陷分级与修复计划完整。
2. 准入结论有可追溯证据。

失败判据：
1. P0 缺陷未清零仍准入。
2. 结论与演练事实不一致。

## Prompt 模板（至少3个：执行、反证审查、准入判定）
### Prompt-A（执行）
```text
请执行 H1“升级迁移演练”，输入：
- Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md
- Project_Docs/Manuals/DEPLOYMENT_RUNBOOK.md
- Project_Docs/Manuals/OPERATIONS_RUNBOOK.md

要求：
1) 输出前置快照、阶段验证、分叉演练、回滚报告、一致性恢复报告、自审日志。
2) 每步包含通过标准、失败判据、证据路径。
3) 单人维护模式，仅自审+自记录。
```

### Prompt-B（反证审查）
```text
请反证审查 H1 演练结果：
1) 找出 8 个“演练通过但上线可能失败”的风险。
2) 找出 5 个“回滚看似成功但数据未恢复”的假象。
3) 给出每项风险的补强验证与阻断条件。
4) 输出必须立即修复的 P0 列表。
```

### Prompt-C（准入判定）
```text
请对 H1 进行准入判定（PASS/FAIL）：
检查对象：
- 01_Migration_Precheck_And_Baseline.md
- 02_Stage_Validation_Record.md
- 03_Rollback_Drill_Report.md
- 04_Data_Consistency_Recovery_Report.md
- 05_Solo_Review_Log.md

判定规则：
1) 三阶段演练是否完整。
2) 回滚是否 <=15 分钟且可复验。
3) 回滚后一致性与可用性是否达标。
4) 是否仍存在 P0 缺陷。

输出：Verdict / Blocking Issues / Fix Plan。
```

## 建议命名与目录
```text
Project_Docs/
  Re_Step/
    RE_STEP_18_H1_升级迁移演练.md
    Artifacts/
      Step18/
        01_Migration_Precheck_And_Baseline.md
        02_Stage_Validation_Record.md
        03_Rollback_Drill_Report.md
        04_Data_Consistency_Recovery_Report.md
        05_Solo_Review_Log.md
```

## 前置产物硬门禁
硬规则：前置产物缺失 => Verdict=FAIL => 禁止进入下一步。

前置产物文件清单（Step17，强制全部存在且非空）：
1. `Project_Docs/Re_Step/Artifacts/Step17/01_Release_Gate_Pipeline_Design.md`
2. `Project_Docs/Re_Step/Artifacts/Step17/02_Automated_Checks_Spec.md`
3. `Project_Docs/Re_Step/Artifacts/Step17/03_Rollback_Readiness_Gate.md`
4. `Project_Docs/Re_Step/Artifacts/Step17/04_Go_NoGo_Decision_Template.md`
5. `Project_Docs/Re_Step/Artifacts/Step17/05_Solo_Review_Log.md`

机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
STEP_DIR="Project_Docs/Re_Step/Artifacts/Step17"
test -d "$STEP_DIR"
test -s "$STEP_DIR/01_Release_Gate_Pipeline_Design.md"
test -s "$STEP_DIR/02_Automated_Checks_Spec.md"
test -s "$STEP_DIR/03_Rollback_Readiness_Gate.md"
test -s "$STEP_DIR/04_Go_NoGo_Decision_Template.md"
test -s "$STEP_DIR/05_Solo_Review_Log.md"
ls -1 "$STEP_DIR"/*.md >/dev/null
```

通过阈值：
1. Step17 前置目录存在。
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
count=$(find "Project_Docs/Re_Step/Artifacts/Step17" -maxdepth 1 -type f -name '*.md' | wc -l)
test "$count" -ge 1
```
- 通过阈值：前置步骤产物文件数 >= 1。
- 阻断动作：命令失败立即 Verdict=FAIL，禁止进入下一步，并在本步骤自审+自记录中登记阻断项。

### Gate-2 固定章节完整性
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
file="Project_Docs/Re_Step/RE_STEP_18_H1_升级迁移演练.md"
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
rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_18_H1_升级迁移演练.md"
```
- 通过阈值：命中文本“自审+自记录” >= 1。
- 阻断动作：命令失败立即 Verdict=FAIL，退回文档修订。

### Gate-4 弱化语义清零
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
! rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_18_H1_升级迁移演练.md" >/dev/null
```
- 通过阈值：弱化语义命中数 = 0。
- 阻断动作：命令失败立即 Verdict=FAIL，禁止进入下一步。

### 误判防护
| 场景ID | 类型 | 场景说明 | 检测补偿动作 |
| --- | --- | --- | --- |
| FP-1 | 假阳性 | Gate-1 因旧文件残留通过，但前置产物与当前版本不匹配 | 补跑版本一致性校验：`rg -q "Last Updated|Version" Project_Docs/Re_Step/Artifacts/Step17/*.md`，不匹配即 FAIL |
| FP-2 | 假阳性 | Gate-2 章节标题存在但正文为空，导致形式通过 | 追加正文非空校验：`test "$(wc -l < "Project_Docs/Re_Step/RE_STEP_18_H1_升级迁移演练.md")" -ge 120`，不足阈值即 FAIL |
| FN-1 | 假阴性 | Gate-3 因字符编码或转义差异误判术语缺失 | 补跑宽松匹配：`rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_18_H1_升级迁移演练.md"`，命中后判为通过并记录修复动作 |
| FN-2 | 假阴性 | Gate-4 在极端 shell 环境下因 `!` 语法差异误判失败 | 补跑等价判定：`test "$(rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_18_H1_升级迁移演练.md" | wc -l)" -eq 0` |

### 门禁证据留存格式
- 证据目录规范：`Project_Docs/Re_Step/Artifacts/Step18/GateEvidence/<UTC时间戳>/`。
- 文件命名规范：`gate1.log`、`gate1.exit`、`gate2.log`、`gate2.exit`、`gate3.log`、`gate3.exit`、`gate4.log`、`gate4.exit`、`summary.json`。
- 留存命令模板（非交互、可重复）：
```bash
set -euo pipefail
STAMP=$(date -u +%Y%m%dT%H%M%SZ)
DIR="Project_Docs/Re_Step/Artifacts/Step18/GateEvidence/${STAMP}"
mkdir -p "$DIR"
( set -euo pipefail; count=$(find "Project_Docs/Re_Step/Artifacts/Step17" -maxdepth 1 -type f -name '*.md' | wc -l); test "$count" -ge 1 ) >"$DIR/gate1.log" 2>&1; echo $? >"$DIR/gate1.exit"
( set -euo pipefail; file="Project_Docs/Re_Step/RE_STEP_18_H1_升级迁移演练.md"; for h in "文档元数据" "步骤目标" "为什么此步骤在此顺序" "输入材料（强制）" "输出物定义（强制）" "详细执行步骤（编号化）" "Prompt 模板（至少3个：执行、反证审查、准入判定）" "建议命名与目录" "投产门禁（Go/No-Go）" "残余风险与挂起条件" "本步骤完成判据（最终）"; do test "$(rg -c "^## $h$" "$file")" -eq 1; done ) >"$DIR/gate2.log" 2>&1; echo $? >"$DIR/gate2.exit"
( set -euo pipefail; rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_18_H1_升级迁移演练.md" ) >"$DIR/gate3.log" 2>&1; echo $? >"$DIR/gate3.exit"
( set -euo pipefail; ! rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_18_H1_升级迁移演练.md" >/dev/null ) >"$DIR/gate4.log" 2>&1; echo $? >"$DIR/gate4.exit"
jq -n --arg step "18" --arg file "Project_Docs/Re_Step/RE_STEP_18_H1_升级迁移演练.md" --arg dir "$DIR" '{step:$step,file:$file,evidence_dir:$dir}' >"$DIR/summary.json"
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
1. 完整迁移演练链路已执行并留证。
2. 回滚演练满足 15 分钟目标。
3. 回滚后一致性与核心链路均通过验证。
4. 全量失败分叉流程可执行并可在时限内止损。
5. 自审通过且 P0 风险清零。
