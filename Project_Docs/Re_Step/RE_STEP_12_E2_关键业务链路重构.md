# Re_Step-12：E2 关键业务链路重构（单人维护版）

## 文档元数据
| 字段 | 值 |
| --- | --- |
| StepID | RE-STEP-12 |
| Version | 1.0.0 |
| Status | Ready |
| Owner | Solo Maintainer（你） |
| Last Updated | 2026-02-15 |
| 关联主计划 | `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`（阶段E/E2） |
| 证据来源 | E1 + D1/D3 + 一致性/故障/可观测合同 |
| 前置步骤 | `RE_STEP_11_E1_命令语义统一重构.md` |
| 准入后继 | 后续 E3/F 阶段 |

## 步骤目标
重构关键业务链路（绑定、解绑、切服等），确保端到端行为具备强回执、可观测、可补偿、可回滚。

核心目标：
1. 绑定链路：`解析身份 -> 写权威 -> fan-out 子服 -> 回执聚合` 全链路可追踪。
2. 解绑链路：全服白名单移除闭环，无残留授权。
3. 切服链路：强回执语义，不允许“发送即成功”。
4. 失败分类：超时/断连/冲突/执行失败可区分并可补偿。

## 为什么此步骤在此顺序
E2 必须在 E1 命令语义统一后执行，才能避免“入口语义统一前先改业务链路”导致回执与权限再次分叉。

D 阶段底座完成后再改 E2，才能保证关键链路建立在稳定协议、线程和一致性能力上。

## 输入材料（强制）
1. `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`
2. `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`
3. `Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`
4. `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
5. `Project_Docs/Architecture/DATA_FLOW_AND_STATE.md`
6. `Project_Docs/Architecture/FAILURE_MODEL.md`
7. `Project_Docs/Manuals/OPERATIONS_RUNBOOK.md`
8. `Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md`
9. `Project_Docs/Re_Step/RE_STEP_11_E1_命令语义统一重构.md`

## 输出物定义（强制）
目录（强制）：`Project_Docs/Re_Step/Artifacts/Step12/`

1. `01_E2_CriticalFlow_StateModel.md`
- bind/unbind/switch_server 端到端状态机与回执点。

2. `02_BindFlow_AuthoritativeWrite_and_Fanout.md`
- 权威写入、子服分发、回执聚合、幂等策略。

3. `03_UnbindFlow_GlobalCleanup_Closure.md`
- 全服白名单清理、失败补偿、最终一致性判据。

4. `04_SwitchServer_StrongAck_Design.md`
- 强回执、pending 管理、禁止假成功规则。

5. `05_E2_Observability_and_Compensation_Report.md`
- 关键链路指标、失败分类、补偿演练证据。

6. `06_Solo_Review_Log_E2.md`
- 自审+自记录与阶段结论。

## 详细执行步骤（编号化）
1. 建立关键链路统一状态模型。
- 操作：为 bind/unbind/switch_server 定义统一状态迁移与终态条件。
- 通过标准：每个链路有唯一终态定义（成功/失败/补偿完成）。
- 失败判据：链路只记录“是否发送”，不记录执行终态。

2. 重构绑定链路为“权威先行”。
- 操作：先写权威数据，再 fan-out 到子服，最后聚合回执。
- 通过标准：任何子服失败都可在聚合回执中被准确识别。
- 失败判据：权威未落盘就回包成功，或子服失败被吞。

3. 重构解绑链路为“全服闭环”。
- 操作：确保解绑触发全服白名单移除与残留检测。
- 通过标准：解绑后全服残留授权计数为 0。
- 失败判据：部分子服残留白名单且无补偿任务。

4. 重构切服链路为强回执语义。
- 操作：`switch_server` 返回执行回执或 `pending`，禁止“已发送即成功”。
- 通过标准：调用方可区分已完成与待确认状态。
- 失败判据：超时/断连仍返回成功文本。

5. 补齐失败分类与补偿流程。
- 操作：区分 timeout/disconnect/conflict/execution，并绑定补偿动作。
- 通过标准：失败分类准确且补偿任务可追踪到终态。
- 失败判据：失败统一为泛化错误，无法驱动补偿。

6. 建立关键链路观测与演练。
- 操作：为每一步打点 requestId、阶段耗时、失败码；执行失败注入演练。
- 通过标准：链路每一步都有指标和日志证据，演练结果可复验。
- 失败判据：只有总结果，没有阶段证据，无法定位瓶颈。

7. 准入判定（进入后续 E3/F）。
- 操作：按“强回执、可补偿、可观测、可回滚”四维判定 PASS/FAIL。
- 通过标准：关键链路无 High 风险残留。
- 失败判据：仍存在假成功或不可追踪失败链路。

## Prompt 模板（至少3个：执行、反证审查、准入判定）
### Prompt-A（执行）
```text
你是关键业务链路重构助手。请完成 Step-12（E2）产物：
输入：
- BRIDGE_MESSAGE_CONTRACT.md
- BRIDGE_ERROR_CODE_CONTRACT.md
- DATA_CONSISTENCY_CONTRACT.md
- OBSERVABILITY_SLO_CONTRACT.md
- FAILURE_MODEL.md
- Step11 产物

要求：
1) 输出关键链路状态模型、绑定链路设计、解绑闭环、切服强回执、观测与补偿报告、自审+自记录日志。
2) 每个步骤必须含通过标准/失败判据/验证动作/回滚动作。
3) 单人维护模式：自审+自记录。
```

### Prompt-B（反证审查）
```text
请对 Step-12 产物执行反证审查：
1) 找出 10 个最可能导致关键链路“假成功或脏状态”的脆弱点。
2) 必查反例：
   - bind 半成功后无补偿
   - unbind 后子服白名单残留
   - switch_server 超时却返回成功
   - fan-out 聚合遗漏失败子项
3) 输出阻断项、影响级别、补强动作。
```

### Prompt-C（准入判定）
```text
请执行 Step-12 准入判定：
检查对象：
- 01_E2_CriticalFlow_StateModel.md
- 02_BindFlow_AuthoritativeWrite_and_Fanout.md
- 03_UnbindFlow_GlobalCleanup_Closure.md
- 04_SwitchServer_StrongAck_Design.md
- 05_E2_Observability_and_Compensation_Report.md
- 06_Solo_Review_Log_E2.md

判定规则：
1) 关键链路是否具备强回执语义。
2) 半成功是否有补偿闭环。
3) 失败分类是否可驱动运维处置。
4) 全链路是否具备 requestId 级可观测证据。

输出：Verdict/Blocking Issues/Fix Actions。
```

## 建议命名与目录
```text
Project_Docs/
  Re_Step/
    RE_STEP_12_E2_关键业务链路重构.md
    Artifacts/
      Step12/
        01_E2_CriticalFlow_StateModel.md
        02_BindFlow_AuthoritativeWrite_and_Fanout.md
        03_UnbindFlow_GlobalCleanup_Closure.md
        04_SwitchServer_StrongAck_Design.md
        05_E2_Observability_and_Compensation_Report.md
        06_Solo_Review_Log_E2.md
```

## 前置产物硬门禁
1. 本步骤执行前必须完成并固化上一步产物；任一缺失直接判定 Verdict=FAIL，禁止进入下一步。
2. 上一步产物清单（全部强制存在）：
- `Project_Docs/Re_Step/Artifacts/Step11/01_Command_Semantics_Inventory.md`
- `Project_Docs/Re_Step/Artifacts/Step11/02_Authorization_and_Visibility_Unification.md`
- `Project_Docs/Re_Step/Artifacts/Step11/03_Command_Alias_and_Deprecation_Plan.md`
- `Project_Docs/Re_Step/Artifacts/Step11/04_Reply_and_Error_Semantics_Standard.md`
- `Project_Docs/Re_Step/Artifacts/Step11/05_E1_Regression_and_Negative_Test_Report.md`
- `Project_Docs/Re_Step/Artifacts/Step11/06_Solo_Review_Log_E1.md`
3. 机检命令（前置产物存在性）：
```bash
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step11/01_Command_Semantics_Inventory.md \
  Project_Docs/Re_Step/Artifacts/Step11/02_Authorization_and_Visibility_Unification.md \
  Project_Docs/Re_Step/Artifacts/Step11/03_Command_Alias_and_Deprecation_Plan.md \
  Project_Docs/Re_Step/Artifacts/Step11/04_Reply_and_Error_Semantics_Standard.md \
  Project_Docs/Re_Step/Artifacts/Step11/05_E1_Regression_and_Negative_Test_Report.md \
  Project_Docs/Re_Step/Artifacts/Step11/06_Solo_Review_Log_E1.md >/dev/null
```
4. 门禁判定：
- 通过阈值：上一步产物存在率=100%，可读率=100%，命名与路径与上一步定义完全一致。
- 阻断动作：机检失败后立即 No-Go；仅允许补齐缺失产物并完成一次自审+自记录后重新判定。

## 投产门禁（Go/No-Go）
1. 机检命令（必须逐条执行并留证据）：
```bash
set -euo pipefail
DOC_PATH="/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_12_E2_关键业务链路重构.md"
PREV_EXPECTED=6
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step11/01_Command_Semantics_Inventory.md \
  Project_Docs/Re_Step/Artifacts/Step11/02_Authorization_and_Visibility_Unification.md \
  Project_Docs/Re_Step/Artifacts/Step11/03_Command_Alias_and_Deprecation_Plan.md \
  Project_Docs/Re_Step/Artifacts/Step11/04_Reply_and_Error_Semantics_Standard.md \
  Project_Docs/Re_Step/Artifacts/Step11/05_E1_Regression_and_Negative_Test_Report.md \
  Project_Docs/Re_Step/Artifacts/Step11/06_Solo_Review_Log_E1.md \
  >/dev/null
PREV_COUNT=$(ls -1 \
  Project_Docs/Re_Step/Artifacts/Step11/01_Command_Semantics_Inventory.md \
  Project_Docs/Re_Step/Artifacts/Step11/02_Authorization_and_Visibility_Unification.md \
  Project_Docs/Re_Step/Artifacts/Step11/03_Command_Alias_and_Deprecation_Plan.md \
  Project_Docs/Re_Step/Artifacts/Step11/04_Reply_and_Error_Semantics_Standard.md \
  Project_Docs/Re_Step/Artifacts/Step11/05_E1_Regression_and_Negative_Test_Report.md \
  Project_Docs/Re_Step/Artifacts/Step11/06_Solo_Review_Log_E1.md \
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
1. 保存路径规范：`Project_Docs/Re_Step/Evidence/Step12/{RUN_ID}/`。
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
EVID_DIR="Project_Docs/Re_Step/Evidence/Step12/${RUN_ID}"
mkdir -p "$EVID_DIR"
{
  echo "step=Step12"
  echo "doc_path=/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_12_E2_关键业务链路重构.md"
  echo "run_id=$RUN_ID"
  date -u
} | tee "$EVID_DIR/00_context.txt"
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step11/01_Command_Semantics_Inventory.md \
  Project_Docs/Re_Step/Artifacts/Step11/02_Authorization_and_Visibility_Unification.md \
  Project_Docs/Re_Step/Artifacts/Step11/03_Command_Alias_and_Deprecation_Plan.md \
  Project_Docs/Re_Step/Artifacts/Step11/04_Reply_and_Error_Semantics_Standard.md \
  Project_Docs/Re_Step/Artifacts/Step11/05_E1_Regression_and_Negative_Test_Report.md \
  Project_Docs/Re_Step/Artifacts/Step11/06_Solo_Review_Log_E1.md \
  | tee "$EVID_DIR/01_prev_gate.txt"
rg -n "^## (文档元数据|步骤目标|为什么此步骤在此顺序|输入材料（强制）|输出物定义（强制）|详细执行步骤（编号化）|Prompt 模板（至少3个：执行、反证审查、准入判定）|建议命名与目录|前置产物硬门禁|投产门禁（Go/No-Go）|残余风险与挂起条件|本步骤完成判据（最终）)$" "/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_12_E2_关键业务链路重构.md" | tee "$EVID_DIR/02_section_gate.txt"
wc -l "/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_12_E2_关键业务链路重构.md" | tee "$EVID_DIR/03_line_gate.txt"
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
1. bind/unbind/switch_server 三条关键链路均具备明确终态与强回执。
2. 失败分类、补偿流程、回滚动作闭环可执行。
3. 全链路 requestId 级证据可追溯、可审计、可复验。
4. 高风险项清零或有正式挂起依据与截止时间。
5. 自审完成并形成阶段结论，可进入后续阶段。
