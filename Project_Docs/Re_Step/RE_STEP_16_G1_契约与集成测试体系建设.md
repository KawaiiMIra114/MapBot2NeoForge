# Re_Step-16：G1 契约与集成测试体系建设（单人维护版）

## 文档元数据
| 字段 | 值 |
| --- | --- |
| StepID | RE-STEP-16 |
| Version | 1.0.0 |
| Status | Ready |
| Owner | Solo Maintainer（你） |
| Last Updated | 2026-02-15 |
| 关联主计划 | `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`（阶段G/G1） |
| 证据来源 | `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`、`Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`、`Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md`、`Project_Docs/Architecture/FAILURE_MODEL.md` |

## 步骤目标
建立“合同即测试”的体系：Bridge 契约、权限契约、一致性故障注入、端到端链路全部自动化，确保关键链路最少覆盖成功/失败/超时/重试四类场景。

## 为什么此步骤在此顺序
F 阶段已具备可观测与运维联调基础，G1 才能把“可观察结果”固化为自动化质量资产；若跳过 G1 直接发布，回归风险不可控。

## 输入材料（强制）
1. `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`
2. `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`
3. `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`
4. `Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md`
5. `Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`
6. `Project_Docs/Architecture/FAILURE_MODEL.md`
7. `Project_Docs/Architecture/THREADING_MODEL.md`
8. `Project_Docs/Manuals/OPERATIONS_RUNBOOK.md`
9. 上一步产物（强制存在，缺失则 Verdict=FAIL 并禁止进入下一步）：`Project_Docs/Re_Step/Artifacts/Step15/*`

## 输出物定义（强制）
目录：`Project_Docs/Re_Step/Artifacts/Step16/`

1. `01_Contract_Test_Catalog.md`
- 定义 CT-01~CT-18 用例映射与自动化状态。

2. `02_Integration_E2E_TestPlan.md`
- 定义 Alpha-Reforged 端到端成功/失败/超时/重试矩阵。

3. `03_Fault_Injection_TestPlan.md`
- 定义断连、乱序、超时、重放场景注入方法与阈值。

4. `04_Test_Gate_Criteria.md`
- 定义 Local/CI/Preprod 三层通过门槛。

5. `05_Solo_Review_Log.md`
- 自审记录与缺口清单。

## 详细执行步骤（编号化）
### 1. 固化契约测试目录
1. 将 `BRIDGE_MESSAGE_CONTRACT` 的 CT-01~CT-18 映射为可执行测试用例。
2. 标注每条用例的环境要求（Local/CI/Preprod）。
3. 对未落地用例给出实现截止时间。

通过标准：
1. CT-01~CT-18 覆盖率 100%。
2. 每条用例有唯一 ID 与预期结果。

失败判据：
1. 存在无 ID 或重复 ID 用例。
2. 关键用例（CT-13/14/15/16）缺失。

### 2. 建立错误码与权限反向测试
1. 按错误码合同构造双栈冲突、未知错误、字段缺失场景。
2. 按权限合同构造角色越权、未定义角色、最小权限边界场景。
3. 校验输出是否符合 `BRG_*` 与 `user/admin/owner` 约束。

通过标准：
1. 高频错误映射覆盖率 100%。
2. 未定义角色拒绝率 100%。

失败判据：
1. 错误码回退策略失效。
2. 任一越权用例可执行成功。

### 3. 建立端到端集成测试
1. 构建关键链路 E2E：绑定、解绑、跨服、重载、执行命令。
2. 每条链路必须包含成功/失败/超时/重试四类断言。
3. 输出 requestId 级追踪证据。

通过标准：
1. 关键链路四类场景覆盖率 100%。
2. 每次失败可定位到链路节点。

失败判据：
1. 仅有成功路径测试。
2. 失败场景无可诊断断言。

### 4. 落地故障注入测试
1. 注入断连、乱序、超时、重放，验证状态机收敛。
2. 对照 FAILURE_MODEL 验证 `PENDING -> COMMITTED/COMPENSATED`。
3. 验证重试污染防护（同 request_id 不重复副作用）。

通过标准：
1. 状态迁移可审计且终态收敛率 >= 99%。
2. 重复副作用数为 0。

失败判据：
1. pending 超期滞留未处理。
2. 同 request_id 产生重复副作用。

### 5. 固化三层测试门禁
1. Local 至少覆盖合同矩阵要求。
2. CI 覆盖所有阻断级用例并 fail-fast。
3. Preprod 全覆盖 CT-01~CT-18 后才允许发布。

通过标准：
1. 三层门禁规则明确且可自动执行。
2. 未通过门禁可阻断合并或发布。

失败判据：
1. 门禁只提醒不阻断。
2. Preprod 未全覆盖即放行。

### 6. 执行自审与准入判定
1. 汇总覆盖率、失败率、未闭环缺口。
2. 对 P0 缺口给出修复计划与截止时间。
3. 输出 PASS/FAIL。

通过标准：
1. P0 缺口为 0。
2. 证据可回放。

失败判据：
1. 存在 P0 缺口仍判 PASS。
2. 自审记录与测试事实不一致。

## Prompt 模板（至少3个：执行、反证审查、准入判定）
### Prompt-A（执行）
```text
请建立 G1“契约与集成测试体系”，输入：
- Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md
- Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md
- Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md
- Project_Docs/Architecture/FAILURE_MODEL.md

输出要求：
1) 契约测试目录、E2E计划、故障注入计划、三层门禁标准、自审日志。
2) 关键链路必须覆盖成功/失败/超时/重试。
3) 每项包含通过标准、失败判据、可机检命令。
4) 单人维护模式，仅自审+自记录。
```

### Prompt-B（反证审查）
```text
请反证审查 G1 产物：
1) 找出 10 个“看似覆盖实则漏测”的场景。
2) 指出 5 个最可能的假阳性测试。
3) 为每个问题给出最小修复测试与阻断级别。
4) 输出若直接发布最可能引发事故的3条链路。
```

### Prompt-C（准入判定）
```text
请判定 G1 是否准入下一步：
检查对象：
- 01_Contract_Test_Catalog.md
- 02_Integration_E2E_TestPlan.md
- 03_Fault_Injection_TestPlan.md
- 04_Test_Gate_Criteria.md
- 05_Solo_Review_Log.md

判定规则：
1) CT-01~CT-18 覆盖是否完整。
2) 四类场景（成功/失败/超时/重试）是否覆盖关键链路。
3) 门禁是否具备阻断能力。
4) P0 缺口是否清零。

输出：Verdict / Blocking Issues / Fix Plan。
```

## 建议命名与目录
```text
Project_Docs/
  Re_Step/
    RE_STEP_16_G1_契约与集成测试体系建设.md
    Artifacts/
      Step16/
        01_Contract_Test_Catalog.md
        02_Integration_E2E_TestPlan.md
        03_Fault_Injection_TestPlan.md
        04_Test_Gate_Criteria.md
        05_Solo_Review_Log.md
```

## 前置产物硬门禁
硬规则：前置产物缺失 => Verdict=FAIL => 禁止进入下一步。

前置产物文件清单（Step15，强制全部存在且非空）：
1. `Project_Docs/Re_Step/Artifacts/Step15/01_Runbook_E2E_Rehearsal_Record.md`
2. `Project_Docs/Re_Step/Artifacts/Step15/02_Threshold_Validation_Report.md`
3. `Project_Docs/Re_Step/Artifacts/Step15/03_Reload_Positive_Negative_Test_Report.md`
4. `Project_Docs/Re_Step/Artifacts/Step15/04_Incident_Drill_Evidence.md`
5. `Project_Docs/Re_Step/Artifacts/Step15/05_Solo_Review_Log.md`

机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
STEP_DIR="Project_Docs/Re_Step/Artifacts/Step15"
test -d "$STEP_DIR"
test -s "$STEP_DIR/01_Runbook_E2E_Rehearsal_Record.md"
test -s "$STEP_DIR/02_Threshold_Validation_Report.md"
test -s "$STEP_DIR/03_Reload_Positive_Negative_Test_Report.md"
test -s "$STEP_DIR/04_Incident_Drill_Evidence.md"
test -s "$STEP_DIR/05_Solo_Review_Log.md"
ls -1 "$STEP_DIR"/*.md >/dev/null
```

通过阈值：
1. Step15 前置目录存在。
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
count=$(find "Project_Docs/Re_Step/Artifacts/Step15" -maxdepth 1 -type f -name '*.md' | wc -l)
test "$count" -ge 1
```
- 通过阈值：前置步骤产物文件数 >= 1。
- 阻断动作：命令失败立即 Verdict=FAIL，禁止进入下一步，并在本步骤自审+自记录中登记阻断项。

### Gate-2 固定章节完整性
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
file="Project_Docs/Re_Step/RE_STEP_16_G1_契约与集成测试体系建设.md"
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
rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_16_G1_契约与集成测试体系建设.md"
```
- 通过阈值：命中文本“自审+自记录” >= 1。
- 阻断动作：命令失败立即 Verdict=FAIL，退回文档修订。

### Gate-4 弱化语义清零
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
! rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_16_G1_契约与集成测试体系建设.md" >/dev/null
```
- 通过阈值：弱化语义命中数 = 0。
- 阻断动作：命令失败立即 Verdict=FAIL，禁止进入下一步。

### 误判防护
| 场景ID | 类型 | 场景说明 | 检测补偿动作 |
| --- | --- | --- | --- |
| FP-1 | 假阳性 | Gate-1 因旧文件残留通过，但前置产物与当前版本不匹配 | 补跑版本一致性校验：`rg -q "Last Updated|Version" Project_Docs/Re_Step/Artifacts/Step15/*.md`，不匹配即 FAIL |
| FP-2 | 假阳性 | Gate-2 章节标题存在但正文为空，导致形式通过 | 追加正文非空校验：`test "$(wc -l < "Project_Docs/Re_Step/RE_STEP_16_G1_契约与集成测试体系建设.md")" -ge 120`，不足阈值即 FAIL |
| FN-1 | 假阴性 | Gate-3 因字符编码或转义差异误判术语缺失 | 补跑宽松匹配：`rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_16_G1_契约与集成测试体系建设.md"`，命中后判为通过并记录修复动作 |
| FN-2 | 假阴性 | Gate-4 在极端 shell 环境下因 `!` 语法差异误判失败 | 补跑等价判定：`test "$(rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_16_G1_契约与集成测试体系建设.md" | wc -l)" -eq 0` |

### 门禁证据留存格式
- 证据目录规范：`Project_Docs/Re_Step/Artifacts/Step16/GateEvidence/<UTC时间戳>/`。
- 文件命名规范：`gate1.log`、`gate1.exit`、`gate2.log`、`gate2.exit`、`gate3.log`、`gate3.exit`、`gate4.log`、`gate4.exit`、`summary.json`。
- 留存命令模板（非交互、可重复）：
```bash
set -euo pipefail
STAMP=$(date -u +%Y%m%dT%H%M%SZ)
DIR="Project_Docs/Re_Step/Artifacts/Step16/GateEvidence/${STAMP}"
mkdir -p "$DIR"
( set -euo pipefail; count=$(find "Project_Docs/Re_Step/Artifacts/Step15" -maxdepth 1 -type f -name '*.md' | wc -l); test "$count" -ge 1 ) >"$DIR/gate1.log" 2>&1; echo $? >"$DIR/gate1.exit"
( set -euo pipefail; file="Project_Docs/Re_Step/RE_STEP_16_G1_契约与集成测试体系建设.md"; for h in "文档元数据" "步骤目标" "为什么此步骤在此顺序" "输入材料（强制）" "输出物定义（强制）" "详细执行步骤（编号化）" "Prompt 模板（至少3个：执行、反证审查、准入判定）" "建议命名与目录" "投产门禁（Go/No-Go）" "残余风险与挂起条件" "本步骤完成判据（最终）"; do test "$(rg -c "^## $h$" "$file")" -eq 1; done ) >"$DIR/gate2.log" 2>&1; echo $? >"$DIR/gate2.exit"
( set -euo pipefail; rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_16_G1_契约与集成测试体系建设.md" ) >"$DIR/gate3.log" 2>&1; echo $? >"$DIR/gate3.exit"
( set -euo pipefail; ! rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_16_G1_契约与集成测试体系建设.md" >/dev/null ) >"$DIR/gate4.log" 2>&1; echo $? >"$DIR/gate4.exit"
jq -n --arg step "16" --arg file "Project_Docs/Re_Step/RE_STEP_16_G1_契约与集成测试体系建设.md" --arg dir "$DIR" '{step:$step,file:$file,evidence_dir:$dir}' >"$DIR/summary.json"
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
1. 合同测试目录完整覆盖 CT-01~CT-18。
2. 关键链路具备四类场景测试且可自动回归。
3. 故障注入验证通过，状态收敛与幂等防护满足阈值。
4. 三层门禁可阻断不合格变更。
5. 自审通过且 P0 缺口为零。
