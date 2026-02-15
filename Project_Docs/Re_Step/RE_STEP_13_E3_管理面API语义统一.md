# Re_Step-13：E3 管理面API语义统一（单人维护版）

## 文档元数据
| 字段 | 值 |
| --- | --- |
| StepID | RE-STEP-13 |
| Version | 1.0.0 |
| Status | Ready |
| Owner | Solo Maintainer（你） |
| Last Updated | 2026-02-15 |
| 关联主计划 | `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`（阶段E/E3） |
| 证据来源 | `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`、`Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`、`Project_Docs/Architecture/DATA_FLOW_AND_STATE.md`、`Project_Docs/Architecture/FAILURE_MODEL.md` |

## 步骤目标
将管理面 HTTP API、QQ 命令入口、Bridge 回执链路统一为同一语义模型：同一动作必须返回同类状态（成功、失败、pending）与同一错误码口径，杜绝“API 假成功、机器人真失败”。

## 为什么此步骤在此顺序
阶段 D 已完成底层协议与一致性改造，阶段 E 先收敛业务语义再进入观测与运维（F）才可产生稳定指标。若先做监控而语义未统一，会把接口口径差异误判为系统故障。

## 输入材料（强制）
1. `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`
2. `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`
3. `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`
4. `Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md`
5. `Project_Docs/Architecture/DATA_FLOW_AND_STATE.md`
6. `Project_Docs/Architecture/FAILURE_MODEL.md`
7. `Project_Docs/Architecture/VERSIONING_AND_COMPATIBILITY.md`
8. 上一步产物（强制存在，缺失则 Verdict=FAIL 并禁止进入下一步）：`Project_Docs/Re_Step/Artifacts/Step12/*`

## 输出物定义（强制）
目录：`Project_Docs/Re_Step/Artifacts/Step13/`

1. `01_API_Command_Semantics_Matrix.md`
- 定义 API 与 QQ 命令的一一映射、入参/出参、状态语义、兼容说明。

2. `02_Ack_State_Model.md`
- 定义 `SUCCESS/FAILED/PENDING` 状态机、超时转移、补偿动作。

3. `03_ErrorCode_And_HTTP_Mapping.md`
- 定义 `BRG_*` 与 HTTP 状态码映射，含冲突优先级。

4. `04_Compatibility_And_Deprecation_Plan.md`
- 定义旧返回字段退场窗口与客户端迁移策略。

5. `05_Solo_Review_Log.md`
- 记录自审结论、证据路径、未决风险与处置计划。

## 详细执行步骤（编号化）
### 1. 建立“动作-语义”基线矩阵
1. 枚举高风险动作：`bind/unbind/switch_server/reload/execute_command`。
2. 为每个动作定义统一结果枚举：`SUCCESS`、`FAILED`、`PENDING`。
3. 对照 API 与 QQ 入口，补齐缺失返回字段（至少含 `requestId`、`errorCode`、`state`）。

通过标准：
1. 高风险动作覆盖率 100%。
2. 每个动作的 API 与 QQ 入口语义一致。

失败判据：
1. 任一动作在两个入口出现不同成功定义。
2. 任一入口缺少 `requestId` 或 `state` 字段。

### 2. 统一回执等待与 pending 规则
1. 以 `BRIDGE_MESSAGE_CONTRACT` 为准，定义同步等待窗口与超时后处理。
2. 明确 API 行为：未拿到执行回执时只能返回 `pending`，禁止返回已成功。
3. 明确 `pending -> failed_timeout/committed` 的最终归并策略。

通过标准：
1. API 不再存在“发送即成功”语义。
2. `pending` 状态都可追踪最终结果。

失败判据：
1. 仍存在 `202 accepted` 但无后续查询机制。
2. `pending` 超时后未落审计事件。

### 3. 统一错误码与 HTTP 映射
1. 将 `register_required/unauthorized/timeout/FAIL:*` 映射到 `BRG_*`。
2. 定义 HTTP 映射规则（示例：鉴权失败=401，参数非法=400，执行失败=409/500）。
3. 落地“结构化错误优先，字符串错误兜底”双栈策略。

通过标准：
1. 高频错误映射覆盖率 100%。
2. 管理端返回包含 `errorCode` 与可重试指引。

失败判据：
1. 同一错误在 API 与 QQ 中落不同错误码。
2. 未命中错误映射时未回退 `BRG_INTERNAL_999`。

### 4. 统一权限语义与失败文案
1. 对齐 `user/admin/owner` 三角色，不得使用未定义角色。
2. 权限拒绝语义统一为“明确拒绝+下一步动作”。
3. 管理群入口仅作通道，不作为授权依据。

通过标准：
1. 未定义角色请求拒绝率 100%。
2. API/QQ 权限拒绝文案一致且不泄露敏感信息。

失败判据：
1. 出现“在管理群即可越权执行”的路径。
2. 用户可见文案包含 token、路径、堆栈等敏感信息。

### 5. 完成兼容窗口与退场计划
1. 为旧字段、旧文案、旧状态定义 N/N+1/N+2 退场路径。
2. 定义客户端迁移门禁：新客户端优先解析 `errorCode/state`。
3. 设定“移除旧语义”的最早版本与回退开关。

通过标准：
1. 退场计划有明确日期与触发条件。
2. 回退开关可在故障时恢复兼容口径。

失败判据：
1. 退场计划无时限或无回退机制。
2. 兼容期内移除导致旧客户端不可用。

### 6. 执行自审与准入判定
1. 使用统一模板记录“结论-证据-风险”。
2. 对阻断项给出修复动作与截止时间。
3. 仅在阻断项清零或挂起理由完整时准入下一步。

通过标准：
1. 自审日志字段完整（日期、证据、结论、待办）。
2. 准入结论明确为 PASS/FAIL。

失败判据：
1. 存在“通过但无证据路径”的结论。
2. 阻断项未闭环即进入下一阶段。

## Prompt 模板（至少3个：执行、反证审查、准入判定）
### Prompt-A（执行）
```text
你是 MapBot2NeoForge 重构执行助手。请完成 E3“管理面API语义统一”产物，输入仅限：
- Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md
- Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md
- Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md
- Project_Docs/Architecture/DATA_FLOW_AND_STATE.md
- Project_Docs/Architecture/FAILURE_MODEL.md

要求：
1) 输出动作语义矩阵、回执状态模型、错误码映射、兼容退场计划、自审日志。
2) 单人维护模式，仅使用“自审+自记录”，不使用多人审批语义。
3) 每条规则必须给出验证方法、通过标准、失败判据与证据路径。
```

### Prompt-B（反证审查）
```text
请对 E3 产物做反证审查：
1) 找出 10 个语义冲突风险（API vs QQ vs Bridge）。
2) 找出 5 个“假成功”风险路径，并给出最小复现实验。
3) 对每个风险输出：失效条件、影响、修复动作、应改文件。
4) 给出“若今天上线最可能失败的3个点”。
```

### Prompt-C（准入判定）
```text
你是 E3 阶段门禁检查器，只输出 PASS 或 FAIL。
检查对象：
- 01_API_Command_Semantics_Matrix.md
- 02_Ack_State_Model.md
- 03_ErrorCode_And_HTTP_Mapping.md
- 04_Compatibility_And_Deprecation_Plan.md
- 05_Solo_Review_Log.md

判定规则：
1) 同动作在 API/QQ 是否语义一致。
2) 是否存在 pending 无归并路径。
3) BRG_* 映射是否完整且可机检。
4) 是否存在越权入口或敏感信息泄露。

输出：
- Verdict: PASS/FAIL
- Blocking Issues: path + 行号 + 原因
- Fix Plan: 每条阻断项的修复动作
```

## 建议命名与目录
```text
Project_Docs/
  Re_Step/
    RE_STEP_13_E3_管理面API语义统一.md
    Artifacts/
      Step13/
        01_API_Command_Semantics_Matrix.md
        02_Ack_State_Model.md
        03_ErrorCode_And_HTTP_Mapping.md
        04_Compatibility_And_Deprecation_Plan.md
        05_Solo_Review_Log.md
```

## 前置产物硬门禁
硬规则：前置产物缺失 => Verdict=FAIL => 禁止进入下一步。

前置产物文件清单（Step12，强制全部存在且非空）：
1. `Project_Docs/Re_Step/Artifacts/Step12/01_E2_CriticalFlow_StateModel.md`
2. `Project_Docs/Re_Step/Artifacts/Step12/02_BindFlow_AuthoritativeWrite_and_Fanout.md`
3. `Project_Docs/Re_Step/Artifacts/Step12/03_UnbindFlow_GlobalCleanup_Closure.md`
4. `Project_Docs/Re_Step/Artifacts/Step12/04_SwitchServer_StrongAck_Design.md`
5. `Project_Docs/Re_Step/Artifacts/Step12/05_E2_Observability_and_Compensation_Report.md`

机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
STEP_DIR="Project_Docs/Re_Step/Artifacts/Step12"
test -d "$STEP_DIR"
test -s "$STEP_DIR/01_E2_CriticalFlow_StateModel.md"
test -s "$STEP_DIR/02_BindFlow_AuthoritativeWrite_and_Fanout.md"
test -s "$STEP_DIR/03_UnbindFlow_GlobalCleanup_Closure.md"
test -s "$STEP_DIR/04_SwitchServer_StrongAck_Design.md"
test -s "$STEP_DIR/05_E2_Observability_and_Compensation_Report.md"
ls -1 "$STEP_DIR"/*.md >/dev/null
```

通过阈值：
1. Step12 前置目录存在。
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
count=$(find "Project_Docs/Re_Step/Artifacts/Step12" -maxdepth 1 -type f -name '*.md' | wc -l)
test "$count" -ge 1
```
- 通过阈值：前置步骤产物文件数 >= 1。
- 阻断动作：命令失败立即 Verdict=FAIL，禁止进入下一步，并在本步骤自审+自记录中登记阻断项。

### Gate-2 固定章节完整性
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
file="Project_Docs/Re_Step/RE_STEP_13_E3_管理面API语义统一.md"
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
rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_13_E3_管理面API语义统一.md"
```
- 通过阈值：命中文本“自审+自记录” >= 1。
- 阻断动作：命令失败立即 Verdict=FAIL，退回文档修订。

### Gate-4 弱化语义清零
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
! rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_13_E3_管理面API语义统一.md" >/dev/null
```
- 通过阈值：弱化语义命中数 = 0。
- 阻断动作：命令失败立即 Verdict=FAIL，禁止进入下一步。

### 误判防护
| 场景ID | 类型 | 场景说明 | 检测补偿动作 |
| --- | --- | --- | --- |
| FP-1 | 假阳性 | Gate-1 因旧文件残留通过，但前置产物与当前版本不匹配 | 补跑版本一致性校验：`rg -q "Last Updated|Version" Project_Docs/Re_Step/Artifacts/Step12/*.md`，不匹配即 FAIL |
| FP-2 | 假阳性 | Gate-2 章节标题存在但正文为空，导致形式通过 | 追加正文非空校验：`test "$(wc -l < "Project_Docs/Re_Step/RE_STEP_13_E3_管理面API语义统一.md")" -ge 120`，不足阈值即 FAIL |
| FN-1 | 假阴性 | Gate-3 因字符编码或转义差异误判术语缺失 | 补跑宽松匹配：`rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_13_E3_管理面API语义统一.md"`，命中后判为通过并记录修复动作 |
| FN-2 | 假阴性 | Gate-4 在极端 shell 环境下因 `!` 语法差异误判失败 | 补跑等价判定：`test "$(rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_13_E3_管理面API语义统一.md" | wc -l)" -eq 0` |

### 门禁证据留存格式
- 证据目录规范：`Project_Docs/Re_Step/Artifacts/Step13/GateEvidence/<UTC时间戳>/`。
- 文件命名规范：`gate1.log`、`gate1.exit`、`gate2.log`、`gate2.exit`、`gate3.log`、`gate3.exit`、`gate4.log`、`gate4.exit`、`summary.json`。
- 留存命令模板（非交互、可重复）：
```bash
set -euo pipefail
STAMP=$(date -u +%Y%m%dT%H%M%SZ)
DIR="Project_Docs/Re_Step/Artifacts/Step13/GateEvidence/${STAMP}"
mkdir -p "$DIR"
( set -euo pipefail; count=$(find "Project_Docs/Re_Step/Artifacts/Step12" -maxdepth 1 -type f -name '*.md' | wc -l); test "$count" -ge 1 ) >"$DIR/gate1.log" 2>&1; echo $? >"$DIR/gate1.exit"
( set -euo pipefail; file="Project_Docs/Re_Step/RE_STEP_13_E3_管理面API语义统一.md"; for h in "文档元数据" "步骤目标" "为什么此步骤在此顺序" "输入材料（强制）" "输出物定义（强制）" "详细执行步骤（编号化）" "Prompt 模板（至少3个：执行、反证审查、准入判定）" "建议命名与目录" "投产门禁（Go/No-Go）" "残余风险与挂起条件" "本步骤完成判据（最终）"; do test "$(rg -c "^## $h$" "$file")" -eq 1; done ) >"$DIR/gate2.log" 2>&1; echo $? >"$DIR/gate2.exit"
( set -euo pipefail; rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_13_E3_管理面API语义统一.md" ) >"$DIR/gate3.log" 2>&1; echo $? >"$DIR/gate3.exit"
( set -euo pipefail; ! rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_13_E3_管理面API语义统一.md" >/dev/null ) >"$DIR/gate4.log" 2>&1; echo $? >"$DIR/gate4.exit"
jq -n --arg step "13" --arg file "Project_Docs/Re_Step/RE_STEP_13_E3_管理面API语义统一.md" --arg dir "$DIR" '{step:$step,file:$file,evidence_dir:$dir}' >"$DIR/summary.json"
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
1. API 与 QQ 对同一动作返回同类状态与同一错误码口径。
2. 不存在“发送即成功”路径，`pending` 全部可追踪最终状态。
3. 高频错误映射覆盖率达到 100%，并提供可机检规则。
4. 兼容退场计划具备时间窗、回退开关、迁移说明。
5. 自审记录完成且阻断项为零（或有明确挂起依据）。
