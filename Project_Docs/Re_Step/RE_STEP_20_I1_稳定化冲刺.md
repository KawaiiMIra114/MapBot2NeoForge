# Re_Step-20：I1 稳定化冲刺（单人维护版）

## 文档元数据
| 字段 | 值 |
| --- | --- |
| StepID | RE-STEP-20 |
| Version | 1.0.0 |
| Status | Ready |
| Owner | Solo Maintainer（你） |
| Last Updated | 2026-02-15 |
| 关联主计划 | `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`（阶段I/I1） |
| 证据来源 | `Project_Docs/Contracts/*.md`、`Project_Docs/Manuals/RELEASE_CHECKLIST.md`、`Project_Docs/Architecture/*` |

## 步骤目标
在灰度后进行稳定化冲刺：清理遗留缺陷与技术债，完成“合同-实现-手册”三方一致性复审，产出可发布 RC 候选并验证稳定窗口无 Sev-0/Sev-1 事故。

## 为什么此步骤在此顺序
H2 已确认上线路径可控，I1 负责把短期补丁转为稳定基线。若不做 I1 就进入开源与收尾，将把迁移期技术债带入长期维护。

## 输入材料（强制）
1. `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`
2. `Project_Docs/Contracts/CONTRACT_INDEX.md`
3. `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`
4. `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`
5. `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
6. `Project_Docs/Architecture/SYSTEM_CONTEXT.md`
7. `Project_Docs/Architecture/FAILURE_MODEL.md`
8. `Project_Docs/Manuals/RELEASE_CHECKLIST.md`
9. 上一步产物（强制存在，缺失则 Verdict=FAIL 并禁止进入下一步）：`Project_Docs/Re_Step/Artifacts/Step19/*`

## 输出物定义（强制）
目录：`Project_Docs/Re_Step/Artifacts/Step20/`

1. `01_Stabilization_Backlog.md`
- 收敛灰度遗留问题、技术债与优先级。

2. `02_Contract_Impl_Manual_Consistency_Report.md`
- 对账合同、实现、手册三方差异。

3. `03_RC_Readiness_Checklist.md`
- RC 准入条件与阻断项。

4. `04_Updated_Baseline_And_Thresholds.md`
- 更新运行基线与阈值依据。

5. `05_Solo_Review_Log.md`
- 自审结论与准入判定。

## 详细执行步骤（编号化）
### 1. 收敛灰度遗留问题
1. 汇总 H2 期间所有问题与 RCA 行动项。
2. 按 P0/P1/P2 分级并定义修复窗口。
3. 对无法立即修复项定义降级与监控补强。

通过标准：
1. 遗留问题清单完整且可追踪。
2. P0 问题全部进入修复闭环。

失败判据：
1. 遗留问题无 owner（你）或截止时间。
2. P0 问题无降级方案。

### 2. 执行三方一致性复审
1. 逐条核对合同条款与现实现状。
2. 核对手册是否反映当前行为与阈值。
3. 输出差异清单并确定修复顺序。

通过标准：
1. 关键合同条款无未解释偏差。
2. 手册与实现口径一致。

失败判据：
1. 合同与实现冲突未记录。
2. 手册仍使用过期行为描述。

### 3. 更新稳定基线与阈值
1. 以灰度后稳定窗口更新延迟、错误率、恢复时长基线。
2. 标注阈值收紧或放宽的依据与影响。
3. 同步到后续发布门禁输入。

通过标准：
1. 新基线有数据证据。
2. 阈值变更有影响分析。

失败判据：
1. 基线由主观估计给出。
2. 阈值调整未说明风险。

### 4. 固化 RC 准入门槛
1. 定义 RC 必须满足项：测试通过、门禁通过、文档对齐、回滚可用。
2. 定义阻断项列表与解除条件。
3. 明确 RC 观察窗口与退出条件。

通过标准：
1. 准入门槛量化且可判定。
2. 阻断项解除路径明确。

失败判据：
1. RC 标准模糊不可执行。
2. 阻断项无解除条件。

### 5. 验证稳定窗口
1. 运行稳定窗口观察（>=7 天，强制）。
2. 校验 Sev-0/Sev-1 事故为 0。
3. 对超阈值事件执行修复并复验。

通过标准：
1. 稳定窗口无 Sev-0/Sev-1。
2. 超阈值事件闭环完成。

失败判据：
1. 观察窗口不足即宣告稳定。
2. 重大事件未闭环就进入下一阶段。

### 6. 执行自审与准入判定
1. 汇总稳定化证据与 RC 结论。
2. 判定是否准入 I2。
3. 输出 PASS/FAIL。

通过标准：
1. RC 结论可追溯。
2. P0 阻断项为 0。

失败判据：
1. 仍有 P0 阻断项。
2. 自审记录不完整。

## Prompt 模板（至少3个：执行、反证审查、准入判定）
### Prompt-A（执行）
```text
请执行 I1“稳定化冲刺”，输入：
- Project_Docs/Contracts/*.md
- Project_Docs/Architecture/*.md
- Project_Docs/Manuals/RELEASE_CHECKLIST.md

要求：
1) 输出遗留问题清单、三方一致性报告、RC准入清单、更新基线、自审日志。
2) 每个问题必须有优先级、修复动作、通过标准、失败判据。
3) 单人维护模式，仅自审+自记录。
```

### Prompt-B（反证审查）
```text
请反证审查 I1 产物：
1) 找出 10 个“看似稳定但实际脆弱”的点。
2) 给出 5 个“合同-实现-手册漂移”高风险场景。
3) 为每个场景给出补强措施与验证命令。
4) 输出阻断 RC 的 P0 清单。
```

### Prompt-C（准入判定）
```text
请判定 I1 是否可准入 I2：
检查对象：
- 01_Stabilization_Backlog.md
- 02_Contract_Impl_Manual_Consistency_Report.md
- 03_RC_Readiness_Checklist.md
- 04_Updated_Baseline_And_Thresholds.md
- 05_Solo_Review_Log.md

判定规则：
1) P0 问题是否清零。
2) 三方一致性是否达标。
3) RC 门槛是否全部满足。
4) 稳定窗口是否无 Sev-0/Sev-1。

输出：Verdict / Blocking Issues / Fix Plan。
```

## 建议命名与目录
```text
Project_Docs/
  Re_Step/
    RE_STEP_20_I1_稳定化冲刺.md
    Artifacts/
      Step20/
        01_Stabilization_Backlog.md
        02_Contract_Impl_Manual_Consistency_Report.md
        03_RC_Readiness_Checklist.md
        04_Updated_Baseline_And_Thresholds.md
        05_Solo_Review_Log.md
```

## 前置产物硬门禁
硬规则：前置产物缺失 => Verdict=FAIL => 禁止进入下一步。

前置产物文件清单（Step19，强制全部存在且非空）：
1. `Project_Docs/Re_Step/Artifacts/Step19/01_Gray_Rollout_Plan.md`
2. `Project_Docs/Re_Step/Artifacts/Step19/02_Gate_Threshold_And_Actions.md`
3. `Project_Docs/Re_Step/Artifacts/Step19/03_Phase_Decision_Log.md`
4. `Project_Docs/Re_Step/Artifacts/Step19/04_Rollback_And_RCA_Record.md`
5. `Project_Docs/Re_Step/Artifacts/Step19/05_Solo_Review_Log.md`

机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
STEP_DIR="Project_Docs/Re_Step/Artifacts/Step19"
test -d "$STEP_DIR"
test -s "$STEP_DIR/01_Gray_Rollout_Plan.md"
test -s "$STEP_DIR/02_Gate_Threshold_And_Actions.md"
test -s "$STEP_DIR/03_Phase_Decision_Log.md"
test -s "$STEP_DIR/04_Rollback_And_RCA_Record.md"
test -s "$STEP_DIR/05_Solo_Review_Log.md"
ls -1 "$STEP_DIR"/*.md >/dev/null
```

通过阈值：
1. Step19 前置目录存在。
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
count=$(find "Project_Docs/Re_Step/Artifacts/Step19" -maxdepth 1 -type f -name '*.md' | wc -l)
test "$count" -ge 1
```
- 通过阈值：前置步骤产物文件数 >= 1。
- 阻断动作：命令失败立即 Verdict=FAIL，禁止进入下一步，并在本步骤自审+自记录中登记阻断项。

### Gate-2 固定章节完整性
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
file="Project_Docs/Re_Step/RE_STEP_20_I1_稳定化冲刺.md"
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
rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_20_I1_稳定化冲刺.md"
```
- 通过阈值：命中文本“自审+自记录” >= 1。
- 阻断动作：命令失败立即 Verdict=FAIL，退回文档修订。

### Gate-4 弱化语义清零
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
! rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_20_I1_稳定化冲刺.md" >/dev/null
```
- 通过阈值：弱化语义命中数 = 0。
- 阻断动作：命令失败立即 Verdict=FAIL，禁止进入下一步。

### 误判防护
| 场景ID | 类型 | 场景说明 | 检测补偿动作 |
| --- | --- | --- | --- |
| FP-1 | 假阳性 | Gate-1 因旧文件残留通过，但前置产物与当前版本不匹配 | 补跑版本一致性校验：`rg -q "Last Updated|Version" Project_Docs/Re_Step/Artifacts/Step19/*.md`，不匹配即 FAIL |
| FP-2 | 假阳性 | Gate-2 章节标题存在但正文为空，导致形式通过 | 追加正文非空校验：`test "$(wc -l < "Project_Docs/Re_Step/RE_STEP_20_I1_稳定化冲刺.md")" -ge 120`，不足阈值即 FAIL |
| FN-1 | 假阴性 | Gate-3 因字符编码或转义差异误判术语缺失 | 补跑宽松匹配：`rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_20_I1_稳定化冲刺.md"`，命中后判为通过并记录修复动作 |
| FN-2 | 假阴性 | Gate-4 在极端 shell 环境下因 `!` 语法差异误判失败 | 补跑等价判定：`test "$(rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_20_I1_稳定化冲刺.md" | wc -l)" -eq 0` |

### 门禁证据留存格式
- 证据目录规范：`Project_Docs/Re_Step/Artifacts/Step20/GateEvidence/<UTC时间戳>/`。
- 文件命名规范：`gate1.log`、`gate1.exit`、`gate2.log`、`gate2.exit`、`gate3.log`、`gate3.exit`、`gate4.log`、`gate4.exit`、`summary.json`。
- 留存命令模板（非交互、可重复）：
```bash
set -euo pipefail
STAMP=$(date -u +%Y%m%dT%H%M%SZ)
DIR="Project_Docs/Re_Step/Artifacts/Step20/GateEvidence/${STAMP}"
mkdir -p "$DIR"
( set -euo pipefail; count=$(find "Project_Docs/Re_Step/Artifacts/Step19" -maxdepth 1 -type f -name '*.md' | wc -l); test "$count" -ge 1 ) >"$DIR/gate1.log" 2>&1; echo $? >"$DIR/gate1.exit"
( set -euo pipefail; file="Project_Docs/Re_Step/RE_STEP_20_I1_稳定化冲刺.md"; for h in "文档元数据" "步骤目标" "为什么此步骤在此顺序" "输入材料（强制）" "输出物定义（强制）" "详细执行步骤（编号化）" "Prompt 模板（至少3个：执行、反证审查、准入判定）" "建议命名与目录" "投产门禁（Go/No-Go）" "残余风险与挂起条件" "本步骤完成判据（最终）"; do test "$(rg -c "^## $h$" "$file")" -eq 1; done ) >"$DIR/gate2.log" 2>&1; echo $? >"$DIR/gate2.exit"
( set -euo pipefail; rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_20_I1_稳定化冲刺.md" ) >"$DIR/gate3.log" 2>&1; echo $? >"$DIR/gate3.exit"
( set -euo pipefail; ! rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_20_I1_稳定化冲刺.md" >/dev/null ) >"$DIR/gate4.log" 2>&1; echo $? >"$DIR/gate4.exit"
jq -n --arg step "20" --arg file "Project_Docs/Re_Step/RE_STEP_20_I1_稳定化冲刺.md" --arg dir "$DIR" '{step:$step,file:$file,evidence_dir:$dir}' >"$DIR/summary.json"
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
1. 灰度遗留问题完成分级与闭环计划。
2. 合同-实现-手册三方差异已收敛到可接受范围。
3. RC 准入标准全部满足且可复验。
4. 稳定窗口无 Sev-0/Sev-1 事故。
5. 自审通过并准入 I2。
