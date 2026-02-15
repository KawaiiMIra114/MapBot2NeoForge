# Re_Step-19：H2 灰度发布与回滚控制（单人维护版）

## 文档元数据
| 字段 | 值 |
| --- | --- |
| StepID | RE-STEP-19 |
| Version | 1.0.0 |
| Status | Ready |
| Owner | Solo Maintainer（你） |
| Last Updated | 2026-02-15 |
| 关联主计划 | `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`（阶段H/H2） |
| 证据来源 | `Project_Docs/Architecture/VERSIONING_AND_COMPATIBILITY.md`、`Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`、`Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md` |

## 步骤目标
执行生产灰度发布并以量化门禁控制推进/回滚，确保全量后稳定窗口指标达到合同目标，不达标立即回滚并形成 RCA。

## 为什么此步骤在此顺序
H1 已验证迁移与回滚可行，H2 才能把该能力用于真实灰度。若无 H1 先验，H2 一旦失控会缺少可信回滚路径。

## 输入材料（强制）
1. `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`
2. `Project_Docs/Architecture/VERSIONING_AND_COMPATIBILITY.md`
3. `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
4. `Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md`
5. `Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md`
6. `Project_Docs/Manuals/RELEASE_CHECKLIST.md`
7. 上一步产物（强制存在，缺失则 Verdict=FAIL 并禁止进入下一步）：`Project_Docs/Re_Step/Artifacts/Step18/*`

## 输出物定义（强制）
目录：`Project_Docs/Re_Step/Artifacts/Step19/`

1. `01_Gray_Rollout_Plan.md`
- 定义暗发布/小流量/中流量/全量推进条件。

2. `02_Gate_Threshold_And_Actions.md`
- 定义阶段门禁、触发阈值、自动回滚动作。

3. `03_Phase_Decision_Log.md`
- 每阶段 Go/No-Go 决策与证据记录。

4. `04_Rollback_And_RCA_Record.md`
- 记录回滚事件与 RCA（如触发）。

5. `05_Solo_Review_Log.md`
- 自审记录与准入结论。

## 详细执行步骤（编号化）
### 1. 固化灰度分阶段计划
1. 阶段定义：暗发布 -> 5% -> 25% -> 100%。
2. 每阶段定义最短观察时长与退出条件。
3. 明确“不可跳阶段”规则。

通过标准：
1. 阶段计划包含进入条件、退出条件、回滚条件。
2. 每阶段责任与证据路径明确。

失败判据：
1. 任一阶段无退出条件。
2. 存在越级放量路径。

### 2. 固化门禁阈值与自动动作
1. 对齐 SLO 与告警阈值，定义阶段门禁指标集。
2. 任一门禁超阈值触发自动回滚到前一阶段。
3. 回滚后 10 分钟内校验是否回到基线 +0.2% 区间。

通过标准：
1. 门禁与动作一一对应。
2. 自动回滚可在 2 分钟内启动。

失败判据：
1. 超阈值无自动动作。
2. 回滚触发后状态不可追踪。

### 3. 执行阶段推进与差异评估
1. 每阶段执行核心冒烟与指标差异对比。
2. 固化“通过/不通过”结论，不允许模糊状态。
3. 对不通过阶段立即停止推进并回滚。

通过标准：
1. 每阶段都有差异报告与结论。
2. 不通过阶段均执行了回滚或降级。

失败判据：
1. 未完成差异评估即推进下一阶段。
2. 不通过阶段继续放量。

### 4. 处理异常并输出 RCA
1. 若触发回滚，按事故手册进入应急流程。
2. 记录根因、遏制、长期修复项。
3. RCA 必须绑定 `incident_id` 与时间线。

通过标准：
1. 回滚事件 100% 有 RCA。
2. RCA 行动项有截止时间与责任人（你）。

失败判据：
1. 回滚后无 RCA 记录。
2. RCA 无可执行行动项。

### 5. 验证全量稳定窗口
1. 全量后进入稳定观察窗口（>=60 分钟，强制）。
2. 对照合同指标判定是否达标。
3. 记录最终发布结论。

通过标准：
1. 稳定窗口内无 Sev-0/Sev-1。
2. 核心指标达标且无持续恶化。

失败判据：
1. 观察窗口未完成即宣告成功。
2. 指标未达标仍保留全量。

### 6. 执行自审与阶段准入
1. 汇总阶段记录、回滚记录、RCA。
2. 判断是否准入 I1 稳定化冲刺。
3. 输出 PASS/FAIL。

通过标准：
1. 阶段证据完整且可回放。
2. 阻断项清零或有挂起依据。

失败判据：
1. 关键阶段缺记录。
2. 有阻断项仍判 PASS。

## Prompt 模板（至少3个：执行、反证审查、准入判定）
### Prompt-A（执行）
```text
请执行 H2“灰度发布与回滚控制”，输入：
- Project_Docs/Architecture/VERSIONING_AND_COMPATIBILITY.md
- Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md
- Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md

要求：
1) 输出灰度计划、门禁阈值与动作、阶段决策日志、回滚与RCA记录、自审日志。
2) 必须包含自动回滚触发条件与时效要求。
3) 单人维护模式，仅自审+自记录。
```

### Prompt-B（反证审查）
```text
请反证审查 H2 方案：
1) 找出 8 个“灰度通过但全量失败”的风险点。
2) 找出 5 个“回滚触发过晚”的条件缺陷。
3) 给出每个缺陷的修复阈值或动作调整。
4) 输出最小 P0 修复集。
```

### Prompt-C（准入判定）
```text
请判定 H2 是否可准入 I1：
检查对象：
- 01_Gray_Rollout_Plan.md
- 02_Gate_Threshold_And_Actions.md
- 03_Phase_Decision_Log.md
- 04_Rollback_And_RCA_Record.md
- 05_Solo_Review_Log.md

判定规则：
1) 各阶段门禁与回滚动作是否完整。
2) 是否存在越级放量路径。
3) 回滚事件是否全部有 RCA。
4) 全量稳定窗口是否满足达标条件。

输出：Verdict / Blocking Issues / Fix Plan。
```

## 建议命名与目录
```text
Project_Docs/
  Re_Step/
    RE_STEP_19_H2_灰度发布与回滚控制.md
    Artifacts/
      Step19/
        01_Gray_Rollout_Plan.md
        02_Gate_Threshold_And_Actions.md
        03_Phase_Decision_Log.md
        04_Rollback_And_RCA_Record.md
        05_Solo_Review_Log.md
```

## 前置产物硬门禁
硬规则：前置产物缺失 => Verdict=FAIL => 禁止进入下一步。

前置产物文件清单（Step18，强制全部存在且非空）：
1. `Project_Docs/Re_Step/Artifacts/Step18/01_Migration_Precheck_And_Baseline.md`
2. `Project_Docs/Re_Step/Artifacts/Step18/02_Stage_Validation_Record.md`
3. `Project_Docs/Re_Step/Artifacts/Step18/03_Rollback_Drill_Report.md`
4. `Project_Docs/Re_Step/Artifacts/Step18/04_Data_Consistency_Recovery_Report.md`
5. `Project_Docs/Re_Step/Artifacts/Step18/05_Solo_Review_Log.md`

机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
STEP_DIR="Project_Docs/Re_Step/Artifacts/Step18"
test -d "$STEP_DIR"
test -s "$STEP_DIR/01_Migration_Precheck_And_Baseline.md"
test -s "$STEP_DIR/02_Stage_Validation_Record.md"
test -s "$STEP_DIR/03_Rollback_Drill_Report.md"
test -s "$STEP_DIR/04_Data_Consistency_Recovery_Report.md"
test -s "$STEP_DIR/05_Solo_Review_Log.md"
ls -1 "$STEP_DIR"/*.md >/dev/null
```

通过阈值：
1. Step18 前置目录存在。
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
count=$(find "Project_Docs/Re_Step/Artifacts/Step18" -maxdepth 1 -type f -name '*.md' | wc -l)
test "$count" -ge 1
```
- 通过阈值：前置步骤产物文件数 >= 1。
- 阻断动作：命令失败立即 Verdict=FAIL，禁止进入下一步，并在本步骤自审+自记录中登记阻断项。

### Gate-2 固定章节完整性
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
file="Project_Docs/Re_Step/RE_STEP_19_H2_灰度发布与回滚控制.md"
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
rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_19_H2_灰度发布与回滚控制.md"
```
- 通过阈值：命中文本“自审+自记录” >= 1。
- 阻断动作：命令失败立即 Verdict=FAIL，退回文档修订。

### Gate-4 弱化语义清零
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
! rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_19_H2_灰度发布与回滚控制.md" >/dev/null
```
- 通过阈值：弱化语义命中数 = 0。
- 阻断动作：命令失败立即 Verdict=FAIL，禁止进入下一步。

### 误判防护
| 场景ID | 类型 | 场景说明 | 检测补偿动作 |
| --- | --- | --- | --- |
| FP-1 | 假阳性 | Gate-1 因旧文件残留通过，但前置产物与当前版本不匹配 | 补跑版本一致性校验：`rg -q "Last Updated|Version" Project_Docs/Re_Step/Artifacts/Step18/*.md`，不匹配即 FAIL |
| FP-2 | 假阳性 | Gate-2 章节标题存在但正文为空，导致形式通过 | 追加正文非空校验：`test "$(wc -l < "Project_Docs/Re_Step/RE_STEP_19_H2_灰度发布与回滚控制.md")" -ge 120`，不足阈值即 FAIL |
| FN-1 | 假阴性 | Gate-3 因字符编码或转义差异误判术语缺失 | 补跑宽松匹配：`rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_19_H2_灰度发布与回滚控制.md"`，命中后判为通过并记录修复动作 |
| FN-2 | 假阴性 | Gate-4 在极端 shell 环境下因 `!` 语法差异误判失败 | 补跑等价判定：`test "$(rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_19_H2_灰度发布与回滚控制.md" | wc -l)" -eq 0` |

### 门禁证据留存格式
- 证据目录规范：`Project_Docs/Re_Step/Artifacts/Step19/GateEvidence/<UTC时间戳>/`。
- 文件命名规范：`gate1.log`、`gate1.exit`、`gate2.log`、`gate2.exit`、`gate3.log`、`gate3.exit`、`gate4.log`、`gate4.exit`、`summary.json`。
- 留存命令模板（非交互、可重复）：
```bash
set -euo pipefail
STAMP=$(date -u +%Y%m%dT%H%M%SZ)
DIR="Project_Docs/Re_Step/Artifacts/Step19/GateEvidence/${STAMP}"
mkdir -p "$DIR"
( set -euo pipefail; count=$(find "Project_Docs/Re_Step/Artifacts/Step18" -maxdepth 1 -type f -name '*.md' | wc -l); test "$count" -ge 1 ) >"$DIR/gate1.log" 2>&1; echo $? >"$DIR/gate1.exit"
( set -euo pipefail; file="Project_Docs/Re_Step/RE_STEP_19_H2_灰度发布与回滚控制.md"; for h in "文档元数据" "步骤目标" "为什么此步骤在此顺序" "输入材料（强制）" "输出物定义（强制）" "详细执行步骤（编号化）" "Prompt 模板（至少3个：执行、反证审查、准入判定）" "建议命名与目录" "投产门禁（Go/No-Go）" "残余风险与挂起条件" "本步骤完成判据（最终）"; do test "$(rg -c "^## $h$" "$file")" -eq 1; done ) >"$DIR/gate2.log" 2>&1; echo $? >"$DIR/gate2.exit"
( set -euo pipefail; rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_19_H2_灰度发布与回滚控制.md" ) >"$DIR/gate3.log" 2>&1; echo $? >"$DIR/gate3.exit"
( set -euo pipefail; ! rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_19_H2_灰度发布与回滚控制.md" >/dev/null ) >"$DIR/gate4.log" 2>&1; echo $? >"$DIR/gate4.exit"
jq -n --arg step "19" --arg file "Project_Docs/Re_Step/RE_STEP_19_H2_灰度发布与回滚控制.md" --arg dir "$DIR" '{step:$step,file:$file,evidence_dir:$dir}' >"$DIR/summary.json"
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
1. 灰度阶段计划完整且不可跳级。
2. 门禁超阈值可自动回滚并记录证据。
3. 全量稳定窗口达标或已按规则回滚。
4. 回滚事件均形成 RCA 与行动项。
5. 自审通过且无阻断项。
