# Re_Step-06：C1 线程模型与故障模型评审（单人维护版）

## 文档元数据
| 字段 | 值 |
| --- | --- |
| StepID | RE-STEP-06 |
| Version | 1.0.0 |
| Status | Ready |
| Owner | Solo Maintainer（你） |
| Last Updated | 2026-02-15 |
| 关联主计划 | `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`（阶段C/C1） |
| 证据来源 | `THREADING_MODEL.md` + `FAILURE_MODEL.md` |
| 前置步骤 | `RE_STEP_05_B3_一致性与SLO契约映射.md` |
| 准入后继 | `RE_STEP_07_C2_安全边界与版本兼容评审.md` |

## 步骤目标
在代码大改前完成线程边界与故障状态机的审计式评审，确定“哪些行为必须禁止、哪些故障必须可恢复”。

核心目标：
1. 线程归属明确：主线程/IO/Worker/Scheduler 访问边界可验证。
2. 故障状态统一：timeout/disconnect/half-success/retry 污染统一分类。
3. pending 生命周期清晰：滞留上限、失败上报、补偿动作可复验。

## 为什么此步骤在此顺序
C1 是 D1/D2/D3 的风险前置。若不先评审线程与故障模型，D 阶段重构容易在高并发下引入新竞态和雪崩风险，导致“功能完成但不可上线”。

## 输入材料（强制）
1. `Project_Docs/Architecture/THREADING_MODEL.md`
2. `Project_Docs/Architecture/FAILURE_MODEL.md`
3. `Project_Docs/Architecture/MODULE_BOUNDARY.md`
4. `Project_Docs/Architecture/DATA_FLOW_AND_STATE.md`
5. `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`
6. `Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`
7. `Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md`
8. `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`

## 输出物定义（强制）
目录（强制）：`Project_Docs/Re_Step/Artifacts/Step06/`

1. `01_Thread_Owner_Matrix_Review.md`
- 资源归属、跨线程传递规则、黑白名单命中情况。

2. `02_ForbiddenPattern_Scan_Report.md`
- `get/join/sleep/new Thread` 与越界访问扫描结果。

3. `03_Failure_StateMachine_Review.md`
- 状态迁移、失败分类、对外错误语义一致性。

4. `04_Pending_Lifecycle_and_Compensation.md`
- pending 上限、失败码、补偿策略、审计字段。

5. `05_Chaos_and_Stress_Review_Plan.md`
- T/F 系列实验计划与通过阈值。

6. `06_Solo_Review_Log_C1.md`
- 自审+自记录（含准入判定）。

## 详细执行步骤（编号化）
1. 审核线程归属矩阵。
- 操作：按资源类型复核 owner thread，标记跨线程访问路径。
- 通过标准：所有核心资源均有唯一 owner 线程定义。
- 失败判据：同一资源在多个线程存在直接写操作。

2. 扫描禁止模式与越界路径。
- 操作：扫描 `IO 线程调用 MC API`、主线程阻塞调用、匿名线程创建。
- 通过标准：违规项全部定位到文件/行并分级。
- 失败判据：存在“疑似问题”但无证据位点。

3. 审核故障状态机一致性。
- 操作：统一 `PENDING/PREPARED/COMMITTED/COMPENSATED/FAILED_*` 迁移语义。
- 通过标准：每个迁移有触发条件、日志字段、对外回执语义。
- 失败判据：同一故障在不同链路返回不同分类或错误码。

4. 评审 pending 生命周期与补偿闭环。
- 操作：核查查询/变更/文件/控制请求的滞留阈值和到期动作。
- 通过标准：超时后必有失败上报与终态收敛路径。
- 失败判据：pending 可无限滞留或被静默丢弃。

5. 制定混沌与压测评审清单。
- 操作：规划线程越界、断连风暴、重试污染等实验，绑定阈值。
- 通过标准：每个实验有输入、窗口、阈值、失败处置动作。
- 失败判据：实验只有“执行”无“通过阈值”。

6. 生成整改顺序与冻结点。
- 操作：按 High/Medium/Low 输出整改优先级，定义冻结条件。
- 通过标准：High 项有明确截止与阻断策略。
- 失败判据：高风险项未设置阻断门槛。

7. 准入判定（进入 C2）。
- 操作：评审报告 PASS/FAIL，明确是否允许进入安全与版本评审。
- 通过标准：线程越界与pending黑洞无未处置High项。
- 失败判据：存在高风险未闭环却进入下一阶段。

## Prompt 模板（至少3个：执行、反证审查、准入判定）
### Prompt-A（执行）
```text
你是线程与故障模型评审助手。请完成 Step-06（C1）产物：
输入：
- THREADING_MODEL.md
- FAILURE_MODEL.md
- MODULE_BOUNDARY.md
- DATA_FLOW_AND_STATE.md

要求：
1) 输出线程归属评审、禁止模式扫描、故障状态机评审、pending与补偿、实验计划、自审+自记录日志。
2) 每项必须有通过标准和失败判据。
3) 输出必须可审计（文件/行号/证据路径）。
4) 单人维护：自审+自记录。
```

### Prompt-B（反证审查）
```text
请对 Step-06 产物做反证审查：
1) 找出 10 个最脆弱的并发/故障假设。
2) 必查反例：
   - 非主线程写世界状态
   - 主线程 join/get/sleep
   - 断连后 pending 未及时失败
   - 半成功误报成功
3) 每条输出：触发、后果、检测、缓释、截止时间。
```

### Prompt-C（准入判定）
```text
请执行 Step-06 准入判定：
检查对象：
- 01_Thread_Owner_Matrix_Review.md
- 02_ForbiddenPattern_Scan_Report.md
- 03_Failure_StateMachine_Review.md
- 04_Pending_Lifecycle_and_Compensation.md
- 05_Chaos_and_Stress_Review_Plan.md
- 06_Solo_Review_Log_C1.md

判定规则：
1) 线程越界 High 项是否清零。
2) 主线程阻塞路径是否清零。
3) pending 生命周期是否闭环。
4) 实验计划是否有量化阈值。

输出：Verdict/Blocking Issues/Fix Actions。
```

## 建议命名与目录
```text
Project_Docs/
  Re_Step/
    RE_STEP_06_C1_线程模型与故障模型评审.md
    Artifacts/
      Step06/
        01_Thread_Owner_Matrix_Review.md
        02_ForbiddenPattern_Scan_Report.md
        03_Failure_StateMachine_Review.md
        04_Pending_Lifecycle_and_Compensation.md
        05_Chaos_and_Stress_Review_Plan.md
        06_Solo_Review_Log_C1.md
```

## 前置产物硬门禁
1. 本步骤执行前必须完成并固化上一步产物；任一缺失直接判定 Verdict=FAIL，禁止进入下一步。
2. 上一步产物清单（全部强制存在）：
- `Project_Docs/Re_Step/Artifacts/Step05/01_SourceOfTruth_and_Versioning_Map.md`
- `Project_Docs/Re_Step/Artifacts/Step05/02_CAS_and_Conflict_StateMachine.md`
- `Project_Docs/Re_Step/Artifacts/Step05/03_Recovery_Path_Profile.md`
- `Project_Docs/Re_Step/Artifacts/Step05/04_SLI_SLO_Metric_Dictionary.md`
- `Project_Docs/Re_Step/Artifacts/Step05/05_Alert_Severity_and_Evidence_Set.md`
- `Project_Docs/Re_Step/Artifacts/Step05/06_Solo_Review_Log_B3.md`
3. 机检命令（前置产物存在性）：
```bash
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step05/01_SourceOfTruth_and_Versioning_Map.md \
  Project_Docs/Re_Step/Artifacts/Step05/02_CAS_and_Conflict_StateMachine.md \
  Project_Docs/Re_Step/Artifacts/Step05/03_Recovery_Path_Profile.md \
  Project_Docs/Re_Step/Artifacts/Step05/04_SLI_SLO_Metric_Dictionary.md \
  Project_Docs/Re_Step/Artifacts/Step05/05_Alert_Severity_and_Evidence_Set.md \
  Project_Docs/Re_Step/Artifacts/Step05/06_Solo_Review_Log_B3.md >/dev/null
```
4. 门禁判定：
- 通过阈值：上一步产物存在率=100%，可读率=100%，命名与路径与上一步定义完全一致。
- 阻断动作：机检失败后立即 No-Go；仅允许补齐缺失产物并完成一次自审+自记录后重新判定。

## 投产门禁（Go/No-Go）
1. 机检命令（必须逐条执行并留证据）：
```bash
set -euo pipefail
DOC_PATH="/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_06_C1_线程模型与故障模型评审.md"
PREV_EXPECTED=6
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step05/01_SourceOfTruth_and_Versioning_Map.md \
  Project_Docs/Re_Step/Artifacts/Step05/02_CAS_and_Conflict_StateMachine.md \
  Project_Docs/Re_Step/Artifacts/Step05/03_Recovery_Path_Profile.md \
  Project_Docs/Re_Step/Artifacts/Step05/04_SLI_SLO_Metric_Dictionary.md \
  Project_Docs/Re_Step/Artifacts/Step05/05_Alert_Severity_and_Evidence_Set.md \
  Project_Docs/Re_Step/Artifacts/Step05/06_Solo_Review_Log_B3.md \
  >/dev/null
PREV_COUNT=$(ls -1 \
  Project_Docs/Re_Step/Artifacts/Step05/01_SourceOfTruth_and_Versioning_Map.md \
  Project_Docs/Re_Step/Artifacts/Step05/02_CAS_and_Conflict_StateMachine.md \
  Project_Docs/Re_Step/Artifacts/Step05/03_Recovery_Path_Profile.md \
  Project_Docs/Re_Step/Artifacts/Step05/04_SLI_SLO_Metric_Dictionary.md \
  Project_Docs/Re_Step/Artifacts/Step05/05_Alert_Severity_and_Evidence_Set.md \
  Project_Docs/Re_Step/Artifacts/Step05/06_Solo_Review_Log_B3.md \
  | wc -l)
[ "$PREV_COUNT" -eq "$PREV_EXPECTED" ]
SECTION_COUNT=$(rg -n "^## (文档元数据|步骤目标|为什么此步骤在此顺序|输入材料（强制）|输出物定义（强制）|详细执行步骤（编号化）|Prompt 模板（至少3个：执行、反证审查、准入判定）|建议命名与目录|前置产物硬门禁|投产门禁（Go/No-Go）|残余风险与挂起条件|本步骤完成判据（最终）)$" "$DOC_PATH" | wc -l)
[ "$SECTION_COUNT" -eq 12 ]
DOC_LINES=$(wc -l < "$DOC_PATH")
[ "$DOC_LINES" -ge 170 ]
curl -fsS http://127.0.0.1:25560/api/status -H "Authorization: Bearer ${ALPHA_TOKEN:?ALPHA_TOKEN required}" | jq -e . >/dev/null
```
2. 通过阈值：
- 前置产物文件数必须等于 6，且全部可读。
- 强制章节命中数必须=12（缺任一即失败）。
- 当前文档行数必须 >= 170。
- api/status 必须返回可被 jq -e 解析的有效 JSON。
3. 阻断动作：
- 任一机检命令失败立即 No-Go，冻结下一步执行。
- 立即记录失败命令、退出码、时间戳、修复动作到自审+自记录日志。
- 修复后必须全量重跑机检命令，不允许跳项或人工口头放行。

### 误判防护
| 类型 | 场景 | 误判表现 | 检测补偿动作 |
| --- | --- | --- | --- |
| 假阳性 FP-1 | 命中历史日志导致本次门禁被判通过 | 机检显示通过，但问题属于旧批次 | 在门禁前写入 RUN_MARKER，所有日志检索必须限定 marker 之后时间窗；不满足则判失败并重跑。 |
| 假阳性 FP-2 | 前置产物文件存在但内容为空或损坏 | ls 通过但产物不可用 | 对前置产物追加 wc -c 与 rg 关键字段机检，任一文件为空或缺关键字段立即 No-Go。 |
| 假阴性 FN-1 | API 短时抖动导致偶发失败 | 实际可用但单次 curl 失败 | 增加固定 3 次重试窗口（间隔 2 秒），仅当连续 3 次失败才判 No-Go，并留存三次输出证据。 |
| 假阴性 FN-2 | 文件系统瞬时锁导致 rg 偶发失败 | 文档有效但扫描命令偶发非0 | 同命令连续执行 2 轮交叉比对；首轮失败次轮通过时补跑第 3 轮确认并留存证据。 |

### 门禁证据留存格式
1. 保存路径规范：`Project_Docs/Re_Step/Evidence/Step06/{RUN_ID}/`。
2. RUN_ID 规范：UTC 时间戳，格式 `YYYYMMDDTHHMMSSZ`。
3. 证据文件规范：
- `00_context.txt`：执行人、Step、DOC_PATH、时间。
- `01_prev_gate.txt`：前置产物存在性与计数机检输出。
- `02_section_gate.txt`：章节机检输出。
- `03_line_gate.txt`：行数阈值机检输出。
- `04_api_gate.txt`：API/JQ 机检输出。
- `05_final_verdict.txt`：Go/No-Go 结论与阻断项。
4. 证据采集命令模板：
```bash
set -euo pipefail
RUN_ID="$(date -u +%Y%m%dT%H%M%SZ)"
EVID_DIR="Project_Docs/Re_Step/Evidence/Step06/${RUN_ID}"
mkdir -p "$EVID_DIR"
{
  echo "step=Step06"
  echo "doc_path=/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_06_C1_线程模型与故障模型评审.md"
  echo "run_id=$RUN_ID"
  date -u
} | tee "$EVID_DIR/00_context.txt"
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step05/01_SourceOfTruth_and_Versioning_Map.md \
  Project_Docs/Re_Step/Artifacts/Step05/02_CAS_and_Conflict_StateMachine.md \
  Project_Docs/Re_Step/Artifacts/Step05/03_Recovery_Path_Profile.md \
  Project_Docs/Re_Step/Artifacts/Step05/04_SLI_SLO_Metric_Dictionary.md \
  Project_Docs/Re_Step/Artifacts/Step05/05_Alert_Severity_and_Evidence_Set.md \
  Project_Docs/Re_Step/Artifacts/Step05/06_Solo_Review_Log_B3.md \
  | tee "$EVID_DIR/01_prev_gate.txt"
rg -n "^## (文档元数据|步骤目标|为什么此步骤在此顺序|输入材料（强制）|输出物定义（强制）|详细执行步骤（编号化）|Prompt 模板（至少3个：执行、反证审查、准入判定）|建议命名与目录|前置产物硬门禁|投产门禁（Go/No-Go）|残余风险与挂起条件|本步骤完成判据（最终）)$" "/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_06_C1_线程模型与故障模型评审.md" | tee "$EVID_DIR/02_section_gate.txt"
wc -l "/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_06_C1_线程模型与故障模型评审.md" | tee "$EVID_DIR/03_line_gate.txt"
curl -fsS http://127.0.0.1:25560/api/status -H "Authorization: Bearer ${ALPHA_TOKEN:?ALPHA_TOKEN required}" | jq -e . | tee "$EVID_DIR/04_api_gate.txt" >/dev/null
echo "verdict=GO" | tee "$EVID_DIR/05_final_verdict.txt"
```

## 残余风险与挂起条件
| 风险ID | 风险描述 | 触发条件 | 挂起条件 | 解除条件 |
| --- | --- | --- | --- | --- |
| R1 | 上一步产物漂移或缺失 | 前置文件路径变更、文件为空或不可读 | 前置门禁任一项失败 | 补齐并通过前置门禁全量复检 |
| R2 | 合同更新与本步骤产物失配 | Contracts/Architecture 版本更新后未回刷本步骤 | 出现术语或阈值冲突且无法即时修复 | 完成差异回刷并通过术语一致性机检 |
| R3 | 机检脚本误报或漏报 | rg/ls/wc/curl/jq 输出与人工结论冲突 | 关键结论无法由机检稳定复现 | 修正机检脚本并连续两次复跑一致 |
| R4 | 运行时环境导致门禁不稳定 | api/status 间歇失败或依赖服务不可达 | 关键门禁无法在稳定窗口内连续通过 | 恢复依赖并在30分钟内连续两次门禁通过 |
| R5 | 回滚路径未被验证 | 存在变更但无可执行回滚证据 | 回滚命令未演练或演练失败 | 补齐回滚演练记录并通过一次完整演练 |
## 本步骤完成判据（最终）
全部满足才算完成：
1. 线程归属、跨线程约束、禁止模式三类评审完成。
2. 故障状态机和 pending 处置规则无冲突且可回放。
3. 高风险项均有整改动作与阻断条件。
4. 混沌/压测计划可执行且阈值明确。
5. 自审完成并给出进入 C2 的明确结论。
