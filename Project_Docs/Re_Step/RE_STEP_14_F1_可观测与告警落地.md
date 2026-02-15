# Re_Step-14：F1 可观测与告警落地（单人维护版）

## 文档元数据
| 字段 | 值 |
| --- | --- |
| StepID | RE-STEP-14 |
| Version | 1.0.0 |
| Status | Ready |
| Owner | Solo Maintainer（你） |
| Last Updated | 2026-02-15 |
| 关联主计划 | `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`（阶段F/F1） |
| 证据来源 | `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`、`Project_Docs/Architecture/FAILURE_MODEL.md`、`Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md` |

## 步骤目标
按 SLO 合同落地指标、告警、证据采集三件套，保证故障出现后 10 分钟内可定位到模块级根因范围，并满足 Critical 事件可复盘可审计。

## 为什么此步骤在此顺序
E3 先统一语义后，监控才有稳定口径。若语义先天不一致，告警只会放大噪音；因此 F1 必须在 E3 之后、F2 运维联调之前。

## 输入材料（强制）
1. `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`
2. `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
3. `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`
4. `Project_Docs/Architecture/FAILURE_MODEL.md`
5. `Project_Docs/Architecture/THREADING_MODEL.md`
6. `Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md`
7. `Project_Docs/Manuals/OPERATIONS_RUNBOOK.md`
8. 上一步产物（强制存在，缺失则 Verdict=FAIL 并禁止进入下一步）：`Project_Docs/Re_Step/Artifacts/Step13/*`

## 输出物定义（强制）
目录：`Project_Docs/Re_Step/Artifacts/Step14/`

1. `01_SLI_SLO_Dashboard_Spec.md`
- 定义核心指标、标签、聚合窗口与看板分层。

2. `02_Alert_Rules_Warning_Critical.md`
- 定义告警阈值、持续时长、抑制与升级规则。

3. `03_Minimum_Evidence_Bundle_Template.md`
- 定义排障最小证据集与采集脚本接口。

4. `04_Incident_Severity_Mapping.md`
- 映射 S1/S2/S3/S4 判定与响应时限。

5. `05_Solo_Review_Log.md`
- 自审记录、问题清单与门禁结论。

## 详细执行步骤（编号化）
### 1. 固化指标字典与采集边界
1. 按合同落地 6 个核心指标：`auth_decision_total`、`auth_decision_latency_ms`、`config_reload_total`、`consistency_conflict_total`、`replay_lag_seconds`、`audit_log_write_total`。
2. 统一关键标签（如 `decision/role/command_category`）。
3. 为每个指标定义采样周期与缺失值处理策略。

通过标准：
1. 合同指标覆盖率 100%。
2. 指标名、标签名、单位与合同一致。

失败判据：
1. 任一合同指标缺采集点。
2. 标签口径不一致导致同指标不可聚合。

### 2. 落地 SLO 与窗口计算
1. 固化 SLO：可用率 `>=99.95%`、`p95<=50ms`、热重载成功率 `>=99.90%`、一致性恢复达标率 `>=99.00%` 且单次 `<=10分钟`、审计落盘 `>=99.99%`。
2. 固化统计窗口（7天/30天滚动）。
3. 输出日常对账脚本与失败自动标注规则。

通过标准：
1. 所有 SLO 可由自动任务计算。
2. 每个 SLO 都有“数据源-公式-窗口”说明。

失败判据：
1. SLO 仅文字描述、无法计算。
2. 窗口定义缺失导致同指标多口径。

### 3. 配置 Warning/Critical 告警
1. 按合同阈值配置双级告警（如 p95>40ms 10分钟 Warning，>50ms 5分钟 Critical）。
2. 配置主备两条独立告警通道。
3. 配置告警去抖与抑制策略，避免重复轰炸。

通过标准：
1. 告警规则全量覆盖合同阈值。
2. 主备通道任一失效 5 分钟内触发升级告警。

失败判据：
1. Critical 告警仅单通道。
2. 阈值或持续时长与合同不一致。

### 4. 建立最小证据集自动采集
1. 为每次告警自动采集 `incident_id`、时间窗、`request_id` 样本、版本字段（`policy_version/schema.version/snapshot_version`）。
2. 采集至少 15 分钟窗口指标快照。
3. 输出证据路径规范（可检索、可归档）。

通过标准：
1. Critical 告警证据集完整率 100%。
2. 证据可在 5 分钟内按 `incident_id` 检索。

失败判据：
1. 关键字段缺失导致无法复盘。
2. 告警已触发但无证据包。

### 5. 进行故障注入与阈值校准
1. 注入鉴权延迟、热重载失败、回放滞后场景。
2. 验证 Warning/Critical 触发时长与分级一致。
3. 若误报/漏报，记录并修正阈值或采样逻辑。

通过标准：
1. 注入场景触发对应等级告警。
2. 恢复后告警可自动清除并形成闭环记录。

失败判据：
1. 漏报关键故障或误报正常场景。
2. 告警恢复后未触发 RCA 流程。

### 6. 执行自审与准入判定
1. 完成“规则一致性、证据完整性、告警可执行性”自审。
2. 阻断项分级处理：阻断发布/限时修复/后续观察。
3. 形成阶段 PASS/FAIL 结论。

通过标准：
1. 阻断项均有负责人（你）与截止时间。
2. 无阻断项时方可进入 F2。

失败判据：
1. 仅有监控图，无阈值与处理动作。
2. 自审结论无法追溯到证据。

## Prompt 模板（至少3个：执行、反证审查、准入判定）
### Prompt-A（执行）
```text
请依据以下文档落地 F1“可观测与告警”：
- Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md
- Project_Docs/Architecture/FAILURE_MODEL.md
- Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md

输出要求：
1) 指标字典、SLO计算规则、Warning/Critical 规则、最小证据集模板、自审日志。
2) 每条规则必须可执行、可机检、可审计。
3) 单人维护模式，仅“自审+自记录”。
4) 每项产物提供通过标准与失败判据。
```

### Prompt-B（反证审查）
```text
请反证审查 F1 产物：
1) 找出 10 个可能导致漏报或误报的配置点。
2) 指出 5 个“看板正常但实际故障”的盲区。
3) 给出每个盲区的补强措施（指标、阈值、证据字段）。
4) 输出最小修复集（优先级 P0/P1/P2）。
```

### Prompt-C（准入判定）
```text
请对 F1 做门禁判定（PASS/FAIL）：
检查对象：
- 01_SLI_SLO_Dashboard_Spec.md
- 02_Alert_Rules_Warning_Critical.md
- 03_Minimum_Evidence_Bundle_Template.md
- 04_Incident_Severity_Mapping.md
- 05_Solo_Review_Log.md

判定规则：
1) 合同 SLO/阈值是否完整落地。
2) Critical 是否具备主备告警通道。
3) 证据集字段是否齐全且可检索。
4) 是否存在不可执行的告警处置步骤。

输出：Verdict / Blocking Issues / Fix Plan。
```

## 建议命名与目录
```text
Project_Docs/
  Re_Step/
    RE_STEP_14_F1_可观测与告警落地.md
    Artifacts/
      Step14/
        01_SLI_SLO_Dashboard_Spec.md
        02_Alert_Rules_Warning_Critical.md
        03_Minimum_Evidence_Bundle_Template.md
        04_Incident_Severity_Mapping.md
        05_Solo_Review_Log.md
```

## 前置产物硬门禁
硬规则：前置产物缺失 => Verdict=FAIL => 禁止进入下一步。

前置产物文件清单（Step13，强制全部存在且非空）：
1. `Project_Docs/Re_Step/Artifacts/Step13/01_API_Command_Semantics_Matrix.md`
2. `Project_Docs/Re_Step/Artifacts/Step13/02_Ack_State_Model.md`
3. `Project_Docs/Re_Step/Artifacts/Step13/03_ErrorCode_And_HTTP_Mapping.md`
4. `Project_Docs/Re_Step/Artifacts/Step13/04_Compatibility_And_Deprecation_Plan.md`
5. `Project_Docs/Re_Step/Artifacts/Step13/05_Solo_Review_Log.md`

机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
STEP_DIR="Project_Docs/Re_Step/Artifacts/Step13"
test -d "$STEP_DIR"
test -s "$STEP_DIR/01_API_Command_Semantics_Matrix.md"
test -s "$STEP_DIR/02_Ack_State_Model.md"
test -s "$STEP_DIR/03_ErrorCode_And_HTTP_Mapping.md"
test -s "$STEP_DIR/04_Compatibility_And_Deprecation_Plan.md"
test -s "$STEP_DIR/05_Solo_Review_Log.md"
ls -1 "$STEP_DIR"/*.md >/dev/null
```

通过阈值：
1. Step13 前置目录存在。
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
count=$(find "Project_Docs/Re_Step/Artifacts/Step13" -maxdepth 1 -type f -name '*.md' | wc -l)
test "$count" -ge 1
```
- 通过阈值：前置步骤产物文件数 >= 1。
- 阻断动作：命令失败立即 Verdict=FAIL，禁止进入下一步，并在本步骤自审+自记录中登记阻断项。

### Gate-2 固定章节完整性
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
file="Project_Docs/Re_Step/RE_STEP_14_F1_可观测与告警落地.md"
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
rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_14_F1_可观测与告警落地.md"
```
- 通过阈值：命中文本“自审+自记录” >= 1。
- 阻断动作：命令失败立即 Verdict=FAIL，退回文档修订。

### Gate-4 弱化语义清零
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
! rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_14_F1_可观测与告警落地.md" >/dev/null
```
- 通过阈值：弱化语义命中数 = 0。
- 阻断动作：命令失败立即 Verdict=FAIL，禁止进入下一步。

### 误判防护
| 场景ID | 类型 | 场景说明 | 检测补偿动作 |
| --- | --- | --- | --- |
| FP-1 | 假阳性 | Gate-1 因旧文件残留通过，但前置产物与当前版本不匹配 | 补跑版本一致性校验：`rg -q "Last Updated|Version" Project_Docs/Re_Step/Artifacts/Step13/*.md`，不匹配即 FAIL |
| FP-2 | 假阳性 | Gate-2 章节标题存在但正文为空，导致形式通过 | 追加正文非空校验：`test "$(wc -l < "Project_Docs/Re_Step/RE_STEP_14_F1_可观测与告警落地.md")" -ge 120`，不足阈值即 FAIL |
| FN-1 | 假阴性 | Gate-3 因字符编码或转义差异误判术语缺失 | 补跑宽松匹配：`rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_14_F1_可观测与告警落地.md"`，命中后判为通过并记录修复动作 |
| FN-2 | 假阴性 | Gate-4 在极端 shell 环境下因 `!` 语法差异误判失败 | 补跑等价判定：`test "$(rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_14_F1_可观测与告警落地.md" | wc -l)" -eq 0` |

### 门禁证据留存格式
- 证据目录规范：`Project_Docs/Re_Step/Artifacts/Step14/GateEvidence/<UTC时间戳>/`。
- 文件命名规范：`gate1.log`、`gate1.exit`、`gate2.log`、`gate2.exit`、`gate3.log`、`gate3.exit`、`gate4.log`、`gate4.exit`、`summary.json`。
- 留存命令模板（非交互、可重复）：
```bash
set -euo pipefail
STAMP=$(date -u +%Y%m%dT%H%M%SZ)
DIR="Project_Docs/Re_Step/Artifacts/Step14/GateEvidence/${STAMP}"
mkdir -p "$DIR"
( set -euo pipefail; count=$(find "Project_Docs/Re_Step/Artifacts/Step13" -maxdepth 1 -type f -name '*.md' | wc -l); test "$count" -ge 1 ) >"$DIR/gate1.log" 2>&1; echo $? >"$DIR/gate1.exit"
( set -euo pipefail; file="Project_Docs/Re_Step/RE_STEP_14_F1_可观测与告警落地.md"; for h in "文档元数据" "步骤目标" "为什么此步骤在此顺序" "输入材料（强制）" "输出物定义（强制）" "详细执行步骤（编号化）" "Prompt 模板（至少3个：执行、反证审查、准入判定）" "建议命名与目录" "投产门禁（Go/No-Go）" "残余风险与挂起条件" "本步骤完成判据（最终）"; do test "$(rg -c "^## $h$" "$file")" -eq 1; done ) >"$DIR/gate2.log" 2>&1; echo $? >"$DIR/gate2.exit"
( set -euo pipefail; rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_14_F1_可观测与告警落地.md" ) >"$DIR/gate3.log" 2>&1; echo $? >"$DIR/gate3.exit"
( set -euo pipefail; ! rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_14_F1_可观测与告警落地.md" >/dev/null ) >"$DIR/gate4.log" 2>&1; echo $? >"$DIR/gate4.exit"
jq -n --arg step "14" --arg file "Project_Docs/Re_Step/RE_STEP_14_F1_可观测与告警落地.md" --arg dir "$DIR" '{step:$step,file:$file,evidence_dir:$dir}' >"$DIR/summary.json"
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
1. 合同定义的 SLI/SLO 与告警阈值全部落地且可机检。
2. Critical 告警具备主备通道与 RCA 触发链路。
3. 最小证据集自动采集可用，关键字段完整率 100%。
4. 注入实验通过且误报/漏报已在可接受范围内。
5. 自审完成并给出明确 PASS/FAIL 结论。
