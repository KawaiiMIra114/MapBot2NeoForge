# Re_Step-11：E1 命令语义统一重构（单人维护版）

## 文档元数据
| 字段 | 值 |
| --- | --- |
| StepID | RE-STEP-11 |
| Version | 1.0.0 |
| Status | Ready |
| Owner | Solo Maintainer（你） |
| Last Updated | 2026-02-15 |
| 关联主计划 | `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`（阶段E/E1） |
| 证据来源 | 权限/配置/协议/一致性阶段产物 |
| 前置步骤 | `RE_STEP_10_D3_数据一致性与恢复重构.md` |
| 准入后继 | `RE_STEP_12_E2_关键业务链路重构.md` |

## 步骤目标
统一命令系统语义，消除多入口行为分叉，确保 QQ 命令、控制面 API、内部命令执行口径一致。

核心目标：
1. 命令权限判定链路统一，拒绝语义一致。
2. 命令重复语义清理，兼容别名可控可退场。
3. 帮助与可见性规则按场景统一输出。
4. 命令回执语义与错误码统一，不再“同命令不同结果表达”。

## 为什么此步骤在此顺序
E1 依赖 D 阶段底层能力稳定后再做。若在 D 前统一命令语义，会被后续协议/线程/一致性改造打断并重新漂移。

## 输入材料（强制）
1. `Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md`
2. `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`
3. `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
4. `Project_Docs/Architecture/MODULE_BOUNDARY.md`
5. `Project_Docs/Manuals/OPERATIONS_RUNBOOK.md`
6. `Project_Docs/Re_Step/RE_STEP_04_B2_权限与配置契约映射.md`
7. `Project_Docs/Re_Step/RE_STEP_08_D1_Bridge通道核心重构.md`
8. `Project_Docs/Re_Step/RE_STEP_10_D3_数据一致性与恢复重构.md`

## 输出物定义（强制）
目录（强制）：`Project_Docs/Re_Step/Artifacts/Step11/`

1. `01_Command_Semantics_Inventory.md`
- 命令清单、分类、入口、当前语义差异。

2. `02_Authorization_and_Visibility_Unification.md`
- 鉴权判定链、可见性策略、拒绝语义。

3. `03_Command_Alias_and_Deprecation_Plan.md`
- 重复命令收敛、别名保留窗口、退场规则。

4. `04_Reply_and_Error_Semantics_Standard.md`
- 成功/失败/超时/待确认回执模板与错误码映射。

5. `05_E1_Regression_and_Negative_Test_Report.md`
- 跨入口一致性回归和反向测试结果。

6. `06_Solo_Review_Log_E1.md`
- 自审+自记录（含准入结论）。

## 详细执行步骤（编号化）
1. 建立命令语义盘点表。
- 操作：盘点所有命令入口与实现语义，标出冲突项。
- 通过标准：每条命令都有唯一 `command_id` 与语义定义。
- 失败判据：存在同名命令多语义且无优先口径。

2. 统一鉴权与可见性链路。
- 操作：按 `user/admin/owner` 统一判定与帮助展示范围。
- 通过标准：无权限命令不暴露细节且拒绝码一致。
- 失败判据：不同入口对同一角色给出不同判定结果。

3. 收敛重复命令并设计兼容别名。
- 操作：明确主命令、别名映射、弃用窗口与迁移提示。
- 通过标准：别名有清晰过渡窗口和回退策略。
- 失败判据：直接删除旧命令导致调用方瞬断。

4. 统一回执与错误码语义。
- 操作：定义命令回执分类（成功/失败/超时/待确认）与标准错误码。
- 通过标准：QQ/API/内部入口对同一失败原因返回同语义。
- 失败判据：入口不同导致“同错不同码”或“同码不同义”。

5. 场景化统一帮助与文案。
- 操作：按玩家群/管理群/私聊输出差异化帮助，但语义不冲突。
- 通过标准：同一命令在不同场景仅展示差异，不改变执行语义。
- 失败判据：帮助文案与实际权限判定不一致。

6. 执行回归与反向测试。
- 操作：覆盖权限拒绝、离线子服、超时、别名调用、兼容窗口等场景。
- 通过标准：关键命令跨入口结果一致率 100%。
- 失败判据：存在入口差异回执且无例外说明。

7. 准入判定（进入 E2）。
- 操作：按语义一致性与兼容风险做 PASS/FAIL。
- 通过标准：无 High 语义冲突项。
- 失败判据：关键命令仍存在多语义并存。

## Prompt 模板（至少3个：执行、反证审查、准入判定）
### Prompt-A（执行）
```text
你是命令语义统一重构助手。请完成 Step-11（E1）产物：
输入：
- COMMAND_AUTHORIZATION_CONTRACT.md
- BRIDGE_ERROR_CODE_CONTRACT.md
- Step04/Step08/Step10 产物

要求：
1) 输出命令盘点、鉴权可见性统一、别名退场、回执语义标准、测试报告、自审+自记录日志。
2) 每项必须给通过标准、失败判据、验证方法。
3) 单人维护模式：自审+自记录。
```

### Prompt-B（反证审查）
```text
请对 Step-11 产物执行反证审查：
1) 找出 10 个“最可能导致命令语义冲突”的脆弱点。
2) 必查反例：
   - QQ 与 API 对同命令权限判定不一致
   - 别名迁移导致错误命令被放行
   - 文案显示可执行但实际拒绝
   - 超时被误报成功
3) 输出阻断项与补强动作。
```

### Prompt-C（准入判定）
```text
请执行 Step-11 准入判定：
检查对象：
- 01_Command_Semantics_Inventory.md
- 02_Authorization_and_Visibility_Unification.md
- 03_Command_Alias_and_Deprecation_Plan.md
- 04_Reply_and_Error_Semantics_Standard.md
- 05_E1_Regression_and_Negative_Test_Report.md
- 06_Solo_Review_Log_E1.md

判定规则：
1) 命令语义是否一命令一口径。
2) 跨入口权限判定是否一致。
3) 兼容别名是否有窗口与退场机制。
4) 关键回归是否全通过。

输出：Verdict/Blocking Issues/Fix Actions。
```

## 建议命名与目录
```text
Project_Docs/
  Re_Step/
    RE_STEP_11_E1_命令语义统一重构.md
    Artifacts/
      Step11/
        01_Command_Semantics_Inventory.md
        02_Authorization_and_Visibility_Unification.md
        03_Command_Alias_and_Deprecation_Plan.md
        04_Reply_and_Error_Semantics_Standard.md
        05_E1_Regression_and_Negative_Test_Report.md
        06_Solo_Review_Log_E1.md
```

## 前置产物硬门禁
1. 本步骤执行前必须完成并固化上一步产物；任一缺失直接判定 Verdict=FAIL，禁止进入下一步。
2. 上一步产物清单（全部强制存在）：
- `Project_Docs/Re_Step/Artifacts/Step10/01_D3_CAS_WritePath_Design.md`
- `Project_Docs/Re_Step/Artifacts/Step10/02_Snapshot_EventLog_Recovery_Design.md`
- `Project_Docs/Re_Step/Artifacts/Step10/03_Atomic_Persistence_Standard.md`
- `Project_Docs/Re_Step/Artifacts/Step10/04_Compensation_and_Replay_Closure.md`
- `Project_Docs/Re_Step/Artifacts/Step10/05_D3_FaultInjection_Test_Report.md`
- `Project_Docs/Re_Step/Artifacts/Step10/06_Solo_Review_Log_D3.md`
3. 机检命令（前置产物存在性）：
```bash
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step10/01_D3_CAS_WritePath_Design.md \
  Project_Docs/Re_Step/Artifacts/Step10/02_Snapshot_EventLog_Recovery_Design.md \
  Project_Docs/Re_Step/Artifacts/Step10/03_Atomic_Persistence_Standard.md \
  Project_Docs/Re_Step/Artifacts/Step10/04_Compensation_and_Replay_Closure.md \
  Project_Docs/Re_Step/Artifacts/Step10/05_D3_FaultInjection_Test_Report.md \
  Project_Docs/Re_Step/Artifacts/Step10/06_Solo_Review_Log_D3.md >/dev/null
```
4. 门禁判定：
- 通过阈值：上一步产物存在率=100%，可读率=100%，命名与路径与上一步定义完全一致。
- 阻断动作：机检失败后立即 No-Go；仅允许补齐缺失产物并完成一次自审+自记录后重新判定。

## 投产门禁（Go/No-Go）
1. 机检命令（必须逐条执行并留证据）：
```bash
set -euo pipefail
DOC_PATH="/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_11_E1_命令语义统一重构.md"
PREV_EXPECTED=6
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step10/01_D3_CAS_WritePath_Design.md \
  Project_Docs/Re_Step/Artifacts/Step10/02_Snapshot_EventLog_Recovery_Design.md \
  Project_Docs/Re_Step/Artifacts/Step10/03_Atomic_Persistence_Standard.md \
  Project_Docs/Re_Step/Artifacts/Step10/04_Compensation_and_Replay_Closure.md \
  Project_Docs/Re_Step/Artifacts/Step10/05_D3_FaultInjection_Test_Report.md \
  Project_Docs/Re_Step/Artifacts/Step10/06_Solo_Review_Log_D3.md \
  >/dev/null
PREV_COUNT=$(ls -1 \
  Project_Docs/Re_Step/Artifacts/Step10/01_D3_CAS_WritePath_Design.md \
  Project_Docs/Re_Step/Artifacts/Step10/02_Snapshot_EventLog_Recovery_Design.md \
  Project_Docs/Re_Step/Artifacts/Step10/03_Atomic_Persistence_Standard.md \
  Project_Docs/Re_Step/Artifacts/Step10/04_Compensation_and_Replay_Closure.md \
  Project_Docs/Re_Step/Artifacts/Step10/05_D3_FaultInjection_Test_Report.md \
  Project_Docs/Re_Step/Artifacts/Step10/06_Solo_Review_Log_D3.md \
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
1. 保存路径规范：`Project_Docs/Re_Step/Evidence/Step11/{RUN_ID}/`。
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
EVID_DIR="Project_Docs/Re_Step/Evidence/Step11/${RUN_ID}"
mkdir -p "$EVID_DIR"
{
  echo "step=Step11"
  echo "doc_path=/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_11_E1_命令语义统一重构.md"
  echo "run_id=$RUN_ID"
  date -u
} | tee "$EVID_DIR/00_context.txt"
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step10/01_D3_CAS_WritePath_Design.md \
  Project_Docs/Re_Step/Artifacts/Step10/02_Snapshot_EventLog_Recovery_Design.md \
  Project_Docs/Re_Step/Artifacts/Step10/03_Atomic_Persistence_Standard.md \
  Project_Docs/Re_Step/Artifacts/Step10/04_Compensation_and_Replay_Closure.md \
  Project_Docs/Re_Step/Artifacts/Step10/05_D3_FaultInjection_Test_Report.md \
  Project_Docs/Re_Step/Artifacts/Step10/06_Solo_Review_Log_D3.md \
  | tee "$EVID_DIR/01_prev_gate.txt"
rg -n "^## (文档元数据|步骤目标|为什么此步骤在此顺序|输入材料（强制）|输出物定义（强制）|详细执行步骤（编号化）|Prompt 模板（至少3个：执行、反证审查、准入判定）|建议命名与目录|前置产物硬门禁|投产门禁（Go/No-Go）|残余风险与挂起条件|本步骤完成判据（最终）)$" "/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_11_E1_命令语义统一重构.md" | tee "$EVID_DIR/02_section_gate.txt"
wc -l "/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_11_E1_命令语义统一重构.md" | tee "$EVID_DIR/03_line_gate.txt"
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
1. 命令语义、权限判定、回执语义跨入口一致。
2. 重复命令收敛并具备兼容退场策略。
3. 帮助文案与执行行为一致，无误导项。
4. 回归与反向测试通过且证据完整。
5. 自审完成并给出进入 E2 的明确结论。
