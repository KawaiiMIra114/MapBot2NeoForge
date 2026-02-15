# Re_Step-05：B3 一致性与SLO契约映射（单人维护版）

## 文档元数据
| 字段 | 值 |
| --- | --- |
| StepID | RE-STEP-05 |
| Version | 1.0.0 |
| Status | Ready |
| Owner | Solo Maintainer（你） |
| Last Updated | 2026-02-15 |
| 关联主计划 | `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`（阶段B/B3） |
| 证据来源 | `DATA_CONSISTENCY_CONTRACT.md` + `OBSERVABILITY_SLO_CONTRACT.md` |
| 前置步骤 | `RE_STEP_04_B2_权限与配置契约映射.md` |
| 准入后继 | `RE_STEP_06_C1_线程模型与故障模型评审.md` |

## 步骤目标
把“数据一致性规则”与“可观测SLO规则”映射到可实施位点，确保冲突可判定、恢复可验证、告警可执行。

关键目标：
1. 事实源与版本语义统一：写入必须走 `entity_version` 与 CAS。
2. 冲突与恢复统一：冲突返回 `CONSISTENCY-409`，恢复满足 `RTO<=10分钟/RPO<=60秒`。
3. 指标与告警统一：合同指标有采集点、阈值、责任链与最小证据集。

## 为什么此步骤在此顺序
B3 需要建立“正确性底座”。在进入 C/D 的架构与代码重构前，先把一致性与SLO合同投射到实现面，可避免后续改造出现“功能可用但数据不可信/不可观测”的隐患。

## 输入材料（强制）
1. `Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`
2. `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
3. `Project_Docs/Architecture/DATA_FLOW_AND_STATE.md`
4. `Project_Docs/Architecture/FAILURE_MODEL.md`
5. `Project_Docs/Architecture/SYSTEM_CONTEXT.md`
6. `Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md`
7. `Project_Docs/Manuals/OPERATIONS_RUNBOOK.md`
8. `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`

## 输出物定义（强制）
目录（强制）：`Project_Docs/Re_Step/Artifacts/Step05/`

1. `01_SourceOfTruth_and_Versioning_Map.md`
- 实体、事实源、版本字段、写入约束。

2. `02_CAS_and_Conflict_StateMachine.md`
- CAS 冲突判定、`CONSISTENCY-409` 返回路径、重试策略。

3. `03_Recovery_Path_Profile.md`
- `snapshot + event_log` 恢复流程与 RTO/RPO 验收证据。

4. `04_SLI_SLO_Metric_Dictionary.md`
- 指标定义、标签、采样点、统计窗口、SLO 阈值。

5. `05_Alert_Severity_and_Evidence_Set.md`
- Warning/Critical 规则、分级响应、最小证据集字段。

6. `06_Solo_Review_Log_B3.md`
- 自审+自记录、阻断项、准入结论。

## 详细执行步骤（编号化）
1. 建立事实源与实体版本映射。
- 操作：列出授权策略、配置、绑定、白名单等实体及唯一事实源。
- 通过标准：每个实体都有“事实源+版本字段+写入入口”。
- 失败判据：存在多写入口且无主从优先级定义。

2. 固化 CAS 冲突与返回语义。
- 操作：定义读写时序、版本递增规则、冲突返回 `CONSISTENCY-409`。
- 通过标准：冲突不会静默覆盖，且冲突事件可审计检索。
- 失败判据：同一资源并发写仍可能“后写覆盖先写”且无冲突记录。

3. 设计恢复链路与目标阈值。
- 操作：固化恢复顺序：加载快照 -> checksum 校验 -> 回放日志 -> 对账收敛。
- 通过标准：恢复报告包含耗时与结果，满足 RTO/RPO 合同阈值。
- 失败判据：恢复只能依赖人工猜测，无法复现恢复边界点。

4. 映射 SLI/SLO 到采集位点。
- 操作：对齐 `auth_decision_total`、`config_reload_total`、`consistency_conflict_total` 等指标的代码采样入口。
- 通过标准：每个合同指标都能定位采集点与标签定义。
- 失败判据：指标存在但标签缺失，导致不可分组定位。

5. 建立告警与分级响应规则。
- 操作：把阈值映射为 Warning/Critical，绑定 S1/S2 响应时限。
- 通过标准：每条告警有触发条件、升级路径、恢复判据。
- 失败判据：告警只有阈值，没有处置动作或责任人语义。

6. 执行故障注入验证。
- 操作：注入版本冲突、恢复失败、配置热重载失败，验证指标/告警/日志一致。
- 通过标准：观测三件套（指标+日志+告警）结论一致。
- 失败判据：告警触发但日志无证据，或日志有错但指标不变。

7. 准入判定（进入 C1）。
- 操作：判定一致性闭环与SLO闭环是否可审计复验。
- 通过标准：无 High 阻断项，恢复与冲突流程可回放。
- 失败判据：存在“冲突语义不统一”或“恢复目标无量化证明”。

## Prompt 模板（至少3个：执行、反证审查、准入判定）
### Prompt-A（执行）
```text
你是一致性与SLO契约落地助手。请完成 Step-05（B3）产物：
输入：
- DATA_CONSISTENCY_CONTRACT.md
- OBSERVABILITY_SLO_CONTRACT.md
- DATA_FLOW_AND_STATE.md
- FAILURE_MODEL.md

要求：
1) 输出事实源映射、CAS状态机、恢复画像、指标字典、告警与证据集、自审+自记录日志。
2) 每个条目必须给出：依据路径、通过标准、失败判据、验证方法。
3) 单人维护模式：仅自审+自记录。
```

### Prompt-B（反证审查）
```text
请对 Step-05 产物执行反证审查：
1) 找出 10 个“最可能导致数据不一致或SLO误判”的脆弱点。
2) 必查反例：
   - 并发写冲突被静默覆盖
   - 恢复回放重复执行
   - 告警触发但缺最小证据集
   - RTO/RPO 统计窗口定义错误
3) 输出每个反例的检测信号与修复动作。
```

### Prompt-C（准入判定）
```text
请作为 Step-05 门禁检查器，输出 PASS/FAIL：
检查对象：
- 01_SourceOfTruth_and_Versioning_Map.md
- 02_CAS_and_Conflict_StateMachine.md
- 03_Recovery_Path_Profile.md
- 04_SLI_SLO_Metric_Dictionary.md
- 05_Alert_Severity_and_Evidence_Set.md
- 06_Solo_Review_Log_B3.md

判定规则：
1) 冲突是否统一返回 CONSISTENCY-409。
2) 恢复是否可证明满足 RTO/RPO。
3) 每个 SLO 指标是否有采集点与告警阈值。
4) 最小证据集是否完整。

输出：Verdict/Blocking Issues/Fix Actions。
```

## 建议命名与目录
```text
Project_Docs/
  Re_Step/
    RE_STEP_05_B3_一致性与SLO契约映射.md
    Artifacts/
      Step05/
        01_SourceOfTruth_and_Versioning_Map.md
        02_CAS_and_Conflict_StateMachine.md
        03_Recovery_Path_Profile.md
        04_SLI_SLO_Metric_Dictionary.md
        05_Alert_Severity_and_Evidence_Set.md
        06_Solo_Review_Log_B3.md
```

## 前置产物硬门禁
1. 本步骤执行前必须完成并固化上一步产物；任一缺失直接判定 Verdict=FAIL，禁止进入下一步。
2. 上一步产物清单（全部强制存在）：
- `Project_Docs/Re_Step/Artifacts/Step04/01_Command_Authorization_Matrix.md`
- `Project_Docs/Re_Step/Artifacts/Step04/02_Role_Normalization_and_Migration.md`
- `Project_Docs/Re_Step/Artifacts/Step04/03_Config_Schema_Validation_Profile.md`
- `Project_Docs/Re_Step/Artifacts/Step04/04_HotReload_Rollback_Audit_Flow.md`
- `Project_Docs/Re_Step/Artifacts/Step04/05_B2_Negative_Test_Cases.md`
- `Project_Docs/Re_Step/Artifacts/Step04/06_Solo_Review_Log_B2.md`
3. 机检命令（前置产物存在性）：
```bash
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step04/01_Command_Authorization_Matrix.md \
  Project_Docs/Re_Step/Artifacts/Step04/02_Role_Normalization_and_Migration.md \
  Project_Docs/Re_Step/Artifacts/Step04/03_Config_Schema_Validation_Profile.md \
  Project_Docs/Re_Step/Artifacts/Step04/04_HotReload_Rollback_Audit_Flow.md \
  Project_Docs/Re_Step/Artifacts/Step04/05_B2_Negative_Test_Cases.md \
  Project_Docs/Re_Step/Artifacts/Step04/06_Solo_Review_Log_B2.md >/dev/null
```
4. 门禁判定：
- 通过阈值：上一步产物存在率=100%，可读率=100%，命名与路径与上一步定义完全一致。
- 阻断动作：机检失败后立即 No-Go；仅允许补齐缺失产物并完成一次自审+自记录后重新判定。

## 投产门禁（Go/No-Go）
1. 机检命令（必须逐条执行并留证据）：
```bash
set -euo pipefail
DOC_PATH="/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_05_B3_一致性与SLO契约映射.md"
PREV_EXPECTED=6
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step04/01_Command_Authorization_Matrix.md \
  Project_Docs/Re_Step/Artifacts/Step04/02_Role_Normalization_and_Migration.md \
  Project_Docs/Re_Step/Artifacts/Step04/03_Config_Schema_Validation_Profile.md \
  Project_Docs/Re_Step/Artifacts/Step04/04_HotReload_Rollback_Audit_Flow.md \
  Project_Docs/Re_Step/Artifacts/Step04/05_B2_Negative_Test_Cases.md \
  Project_Docs/Re_Step/Artifacts/Step04/06_Solo_Review_Log_B2.md \
  >/dev/null
PREV_COUNT=$(ls -1 \
  Project_Docs/Re_Step/Artifacts/Step04/01_Command_Authorization_Matrix.md \
  Project_Docs/Re_Step/Artifacts/Step04/02_Role_Normalization_and_Migration.md \
  Project_Docs/Re_Step/Artifacts/Step04/03_Config_Schema_Validation_Profile.md \
  Project_Docs/Re_Step/Artifacts/Step04/04_HotReload_Rollback_Audit_Flow.md \
  Project_Docs/Re_Step/Artifacts/Step04/05_B2_Negative_Test_Cases.md \
  Project_Docs/Re_Step/Artifacts/Step04/06_Solo_Review_Log_B2.md \
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
1. 保存路径规范：`Project_Docs/Re_Step/Evidence/Step05/{RUN_ID}/`。
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
EVID_DIR="Project_Docs/Re_Step/Evidence/Step05/${RUN_ID}"
mkdir -p "$EVID_DIR"
{
  echo "step=Step05"
  echo "doc_path=/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_05_B3_一致性与SLO契约映射.md"
  echo "run_id=$RUN_ID"
  date -u
} | tee "$EVID_DIR/00_context.txt"
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step04/01_Command_Authorization_Matrix.md \
  Project_Docs/Re_Step/Artifacts/Step04/02_Role_Normalization_and_Migration.md \
  Project_Docs/Re_Step/Artifacts/Step04/03_Config_Schema_Validation_Profile.md \
  Project_Docs/Re_Step/Artifacts/Step04/04_HotReload_Rollback_Audit_Flow.md \
  Project_Docs/Re_Step/Artifacts/Step04/05_B2_Negative_Test_Cases.md \
  Project_Docs/Re_Step/Artifacts/Step04/06_Solo_Review_Log_B2.md \
  | tee "$EVID_DIR/01_prev_gate.txt"
rg -n "^## (文档元数据|步骤目标|为什么此步骤在此顺序|输入材料（强制）|输出物定义（强制）|详细执行步骤（编号化）|Prompt 模板（至少3个：执行、反证审查、准入判定）|建议命名与目录|前置产物硬门禁|投产门禁（Go/No-Go）|残余风险与挂起条件|本步骤完成判据（最终）)$" "/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_05_B3_一致性与SLO契约映射.md" | tee "$EVID_DIR/02_section_gate.txt"
wc -l "/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_05_B3_一致性与SLO契约映射.md" | tee "$EVID_DIR/03_line_gate.txt"
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
1. 事实源与版本语义统一且可机检。
2. 冲突处理、恢复流程、补偿逻辑均可回放验证。
3. 合同级指标与告警实现位点完整映射。
4. 最小证据集在演练中可稳定产出。
5. 自审完成并给出进入 C1 的明确结论。
