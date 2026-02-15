# Re_Step-02：A2 基线快照与基线对比体系（单人维护版）

## 文档元数据
| 字段 | 值 |
| --- | --- |
| StepID | RE-STEP-02 |
| Version | 1.0.0 |
| Status | Ready |
| Owner | Solo Maintainer（你） |
| Last Updated | 2026-02-15 |
| 关联主计划 | `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`（阶段A/A2） |
| 证据来源 | `Project_Docs/Contracts/*` + `Project_Docs/Architecture/*` + `Project_Docs/Manuals/*` |
| 前置步骤 | `RE_STEP_01_A1_立项冻结与重构门禁.md` |
| 准入后继 | `RE_STEP_03_B1_Bridge消息与错误契约映射.md` |

## 步骤目标
建立可复用的“重构前后对比基线”，使后续每一步都能量化证明是否变好、变差或漂移。

本步骤必须交付三类基线：
1. 配置基线：Alpha/Reforged/Redis/OneBot 的生效配置快照与哈希。
2. 指标基线：延迟、超时率、重试率、注册失败率、关键命令成功率。
3. 行为基线：`#bind/#unbind/#status/#list/#reload` 的输入-输出-日志证据链。

## 为什么此步骤在此顺序
A1 已冻结范围与门禁，但尚未有“可比较基线”。如果不先做快照，B/C/D/E 的改造将失去判定标准，出现“改了很多但无法证明收益”的审计缺口。

A2 放在契约映射前，能让 B1/B2/B3 在改造时直接对照当前真实行为，而不是凭记忆判断现状。

## 输入材料（强制）
1. `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`
2. `Project_Docs/SYSTEM_CRITIQUE_AND_REFACTOR_PAPER_V1.md`
3. `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`
4. `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`
5. `Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md`
6. `Project_Docs/Contracts/CONFIG_SCHEMA_CONTRACT.md`
7. `Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`
8. `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
9. `Project_Docs/Architecture/SYSTEM_CONTEXT.md`
10. `Project_Docs/Architecture/DATA_FLOW_AND_STATE.md`
11. `Project_Docs/Architecture/THREADING_MODEL.md`
12. `Project_Docs/Manuals/OPERATIONS_RUNBOOK.md`
13. `Project_Docs/Manuals/DEPLOYMENT_RUNBOOK.md`

## 输出物定义（强制）
目录（强制）：`Project_Docs/Re_Step/Artifacts/Step02/`

1. `01_Baseline_Environment_Manifest.md`
- 环境信息、版本、端口、依赖与快照时间点。

2. `02_Baseline_Config_Snapshots.md`
- 配置快照路径、哈希、差异说明、敏感字段脱敏说明。

3. `03_Baseline_Metrics_Report.md`
- 指标定义、快照口径、统计值（可含分位）、失败率。

4. `04_Baseline_Behavior_Trace.md`
- 五条关键命令的请求-回执-日志-结论证据链。

5. `05_Baseline_Comparator_Spec.md`
- 重构后对比规则、容忍阈值、判定优先级（PASS/WARN/FAIL）。

6. `06_Solo_Review_Log_A2.md`
- 自审+自记录：风险、偏差、已知缺口、下一步阻断项。

## 详细执行步骤（编号化）
1. 固定快照范围与证据边界。
- 操作：确定一次可复验快照（时间点、时区、样本来源、排除条件）。
- 通过标准：快照时间、时区、样本来源、排除条件全部落文档。
- 失败判据：存在“临时截取片段”但无时间边界与证据路径说明。

2. 采集环境与配置基线。
- 操作：记录 Alpha/Reforged/Redis/OneBot 版本、端口、配置快照与哈希，敏感值脱敏。
- 通过标准：每份配置都有“原始路径+快照路径+哈希值+脱敏规则”。
- 失败判据：配置快照缺任一关键组件，或哈希不可复验。

3. 建立指标基线字典并快照。
- 操作：按合同快照 `auth_decision_latency_ms`、`config_reload_total`、`consistency_conflict_total`、Bridge 超时率、注册失败率。
- 通过标准：每个指标有快照命令/查询语句、统计口径、基线值。
- 失败判据：指标定义与合同术语不一致，或关键指标缺失。

4. 执行关键行为基线回放。
- 操作：执行 `#bind/#unbind/#status/#list/#reload`，记录请求体、返回文本、错误码、日志片段、耗时。
- 通过标准：五条链路均形成“输入-处理-输出-日志”四联证据。
- 失败判据：任一命令只记录结果不记录日志，或无法关联 requestId。

5. 建立“重构后对比”判定器。
- 操作：定义同口径对比规则：语义一致性优先于性能，性能阈值优先于日志文案。
- 通过标准：每个指标/行为均有 PASS/WARN/FAIL 阈值及解释。
- 失败判据：只有均值无分位数，或阈值未写明来源。

6. 执行偏差审查与自记录。
- 操作：列出快照盲区（例如低流量时段、离线子服），写出影响与补录计划。
- 通过标准：至少 3 条偏差项，且每条有“影响等级+补救动作”。
- 失败判据：只写“后续关注”但无具体补救动作。

7. 准入判定（进入 B1）。
- 操作：按 A2 清单做 `PASS / FAIL` 判断，阻断项必须闭环或挂起说明。
- 通过标准：输出明确 Verdict，阻断项为 0 或均有可追溯挂起理由。
- 失败判据：结论为“基本通过/大体可用”等不可执行语句。

## Prompt 模板（至少3个：执行、反证审查、准入判定）
### Prompt-A（执行）
```text
你是该项目的基线快照执行助手。请依据以下输入材料生成 Step-02 的全部产物：
- SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST
- SYSTEM_CRITIQUE_AND_REFACTOR_PAPER_V1
- Contracts/*.md
- Architecture/*.md
- Manuals/OPERATIONS_RUNBOOK.md

要求：
1) 输出环境、配置、指标、行为、对比器、自审 6 份文档。
2) 每项基线必须给出“快照口径 + 命令/查询 + 结果值 + 证据路径”。
3) 单人维护模式：仅“自审+自记录”，不出现多人审批语义。
4) 每份输出都必须包含通过标准与失败判据。
```

### Prompt-B（反证审查）
```text
请对当前 Step-02 基线包执行反证审查：
1) 找出 10 个可能导致“对比失真”的风险点。
2) 对每个风险给出：触发条件、影响范围、误判类型（假阳/假阴）、补采动作。
3) 强制检查以下场景是否覆盖：
   - 子服离线时 #reload
   - Bridge 短时断连
   - 高峰时段 #status/#list 延迟抖动
4) 产出阻断清单（按 High/Medium/Low）。
```

### Prompt-C（准入判定）
```text
请作为 Step-02 门禁检查器，仅输出 PASS/FAIL：
检查对象：
- 01_Baseline_Environment_Manifest.md
- 02_Baseline_Config_Snapshots.md
- 03_Baseline_Metrics_Report.md
- 04_Baseline_Behavior_Trace.md
- 05_Baseline_Comparator_Spec.md
- 06_Solo_Review_Log_A2.md

判定规则：
1) 是否可复验（命令/查询可复跑）。
2) 是否可比较（有同口径阈值）。
3) 是否可审计（证据路径完整）。
4) 是否存在阻断项未处置。

输出：
- Verdict: PASS/FAIL
- Blocking Issues: path + 原因
- Fix Actions: 每项阻断的修复动作
```

## 建议命名与目录
```text
Project_Docs/
  Re_Step/
    RE_STEP_02_A2_基线采样与基线对比体系.md
    Artifacts/
      Step02/
        01_Baseline_Environment_Manifest.md
        02_Baseline_Config_Snapshots.md
        03_Baseline_Metrics_Report.md
        04_Baseline_Behavior_Trace.md
        05_Baseline_Comparator_Spec.md
        06_Solo_Review_Log_A2.md
```

## 前置产物硬门禁
1. 本步骤执行前必须完成并固化上一步产物；任一缺失直接判定 Verdict=FAIL，禁止进入下一步。
2. 上一步产物清单（全部强制存在）：
- `Project_Docs/Re_Step/Artifacts/Step01/01_Refactor_Charter.md`
- `Project_Docs/Re_Step/Artifacts/Step01/02_DoD_Checklist.md`
- `Project_Docs/Re_Step/Artifacts/Step01/03_Change_Gate_Rules.md`
- `Project_Docs/Re_Step/Artifacts/Step01/04_Solo_Review_Log.md`
3. 机检命令（前置产物存在性）：
```bash
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step01/01_Refactor_Charter.md \
  Project_Docs/Re_Step/Artifacts/Step01/02_DoD_Checklist.md \
  Project_Docs/Re_Step/Artifacts/Step01/03_Change_Gate_Rules.md \
  Project_Docs/Re_Step/Artifacts/Step01/04_Solo_Review_Log.md >/dev/null
```
4. 门禁判定：
- 通过阈值：上一步产物存在率=100%，可读率=100%，命名与路径与上一步定义完全一致。
- 阻断动作：机检失败后立即 No-Go；仅允许补齐缺失产物并完成一次自审+自记录后重新判定。

## 投产门禁（Go/No-Go）
1. 机检命令（必须逐条执行并留证据）：
```bash
set -euo pipefail
DOC_PATH="/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_02_A2_基线采样与基线对比体系.md"
PREV_EXPECTED=4
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step01/01_Refactor_Charter.md \
  Project_Docs/Re_Step/Artifacts/Step01/02_DoD_Checklist.md \
  Project_Docs/Re_Step/Artifacts/Step01/03_Change_Gate_Rules.md \
  Project_Docs/Re_Step/Artifacts/Step01/04_Solo_Review_Log.md \
  >/dev/null
PREV_COUNT=$(ls -1 \
  Project_Docs/Re_Step/Artifacts/Step01/01_Refactor_Charter.md \
  Project_Docs/Re_Step/Artifacts/Step01/02_DoD_Checklist.md \
  Project_Docs/Re_Step/Artifacts/Step01/03_Change_Gate_Rules.md \
  Project_Docs/Re_Step/Artifacts/Step01/04_Solo_Review_Log.md \
  | wc -l)
[ "$PREV_COUNT" -eq "$PREV_EXPECTED" ]
SECTION_COUNT=$(rg -n "^## (文档元数据|步骤目标|为什么此步骤在此顺序|输入材料（强制）|输出物定义（强制）|详细执行步骤（编号化）|Prompt 模板（至少3个：执行、反证审查、准入判定）|建议命名与目录|前置产物硬门禁|投产门禁（Go/No-Go）|残余风险与挂起条件|本步骤完成判据（最终）)$" "$DOC_PATH" | wc -l)
[ "$SECTION_COUNT" -eq 12 ]
DOC_LINES=$(wc -l < "$DOC_PATH")
[ "$DOC_LINES" -ge 170 ]
```
2. 通过阈值：
- 前置产物文件数必须等于 4，且全部可读。
- 强制章节命中数必须=12（缺任一即失败）。
- 当前文档行数必须 >= 170。
- 产物文档与证据文件必须齐全且可读。
3. 阻断动作：
- 任一机检命令失败立即 No-Go，冻结下一步执行。
- 立即记录失败命令、退出码、时间戳、修复动作到自审+自记录日志。
- 修复后必须全量重跑机检命令，不允许跳项或人工口头放行。

### 误判防护
| 类型 | 场景 | 误判表现 | 检测补偿动作 |
| --- | --- | --- | --- |
| 假阳性 FP-1 | 命中历史日志导致本次门禁被判通过 | 机检显示通过，但问题属于旧批次 | 在门禁前写入 RUN_MARKER，所有日志检索必须限定 marker 之后时间窗；不满足则判失败并重跑。 |
| 假阳性 FP-2 | 前置产物文件存在但内容为空或损坏 | ls 通过但产物不可用 | 对前置产物追加 wc -c 与 rg 关键字段机检，任一文件为空或缺关键字段立即 No-Go。 |
| 假阴性 FN-1 | 文档路径引用旧批次证据 | 当前结论与最新证据不一致 | 读取 `LATEST_RUN_ID` 并对关键证据做路径一致性机检，不一致则判 No-Go。 |
| 假阴性 FN-2 | 文件系统瞬时锁导致 rg 偶发失败 | 文档有效但扫描命令偶发非0 | 同命令连续执行 2 轮交叉比对；首轮失败次轮通过时补跑第 3 轮确认并留存证据。 |

### 门禁证据留存格式
1. 保存路径规范：`Project_Docs/Re_Step/Evidence/Step02/{RUN_ID}/`。
2. RUN_ID 规范：UTC 时间戳，格式 `YYYYMMDDTHHMMSSZ`。
3. 证据文件规范：
- `00_context.txt`：执行人、Step、DOC_PATH、时间。
- `01_prev_gate.txt`：前置产物存在性与计数机检输出。
- `02_section_gate.txt`：章节机检输出。
- `03_line_gate.txt`：行数阈值机检输出。
- `04_api_gate.txt`：API/JQ 可选机检输出（无 token 可标记 SKIP）。
- `05_final_verdict.txt`：Go/No-Go 结论与阻断项。
4. 证据采集命令模板：
```bash
set -euo pipefail
RUN_ID="$(date -u +%Y%m%dT%H%M%SZ)"
EVID_DIR="Project_Docs/Re_Step/Evidence/Step02/${RUN_ID}"
mkdir -p "$EVID_DIR"
{
  echo "step=Step02"
  echo "doc_path=/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_02_A2_基线采样与基线对比体系.md"
  echo "run_id=$RUN_ID"
  date -u
} | tee "$EVID_DIR/00_context.txt"
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step01/01_Refactor_Charter.md \
  Project_Docs/Re_Step/Artifacts/Step01/02_DoD_Checklist.md \
  Project_Docs/Re_Step/Artifacts/Step01/03_Change_Gate_Rules.md \
  Project_Docs/Re_Step/Artifacts/Step01/04_Solo_Review_Log.md \
  | tee "$EVID_DIR/01_prev_gate.txt"
rg -n "^## (文档元数据|步骤目标|为什么此步骤在此顺序|输入材料（强制）|输出物定义（强制）|详细执行步骤（编号化）|Prompt 模板（至少3个：执行、反证审查、准入判定）|建议命名与目录|前置产物硬门禁|投产门禁（Go/No-Go）|残余风险与挂起条件|本步骤完成判据（最终）)$" "/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_02_A2_基线采样与基线对比体系.md" | tee "$EVID_DIR/02_section_gate.txt"
wc -l "/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_02_A2_基线采样与基线对比体系.md" | tee "$EVID_DIR/03_line_gate.txt"
if [ -n "${ALPHA_TOKEN:-}" ]; then
  curl -fsS http://127.0.0.1:25560/api/status -H "Authorization: Bearer ${ALPHA_TOKEN}" | jq -e . | tee "$EVID_DIR/04_api_gate.txt" >/dev/null
else
  echo "api_status_check=SKIP_NO_TOKEN" | tee "$EVID_DIR/04_api_gate.txt"
fi
echo "verdict=GO" | tee "$EVID_DIR/05_final_verdict.txt"
```

## 残余风险与挂起条件
| 风险ID | 风险描述 | 触发条件 | 挂起条件 | 解除条件 |
| --- | --- | --- | --- | --- |
| R1 | 上一步产物漂移或缺失 | 前置文件路径变更、文件为空或不可读 | 前置门禁任一项失败 | 补齐并通过前置门禁全量复检 |
| R2 | 合同更新与本步骤产物失配 | Contracts/Architecture 版本更新后未回刷本步骤 | 出现术语或阈值冲突且无法即时修复 | 完成差异回刷并通过术语一致性机检 |
| R3 | 机检脚本误报或漏报 | rg/ls/wc/curl/jq 输出与人工结论冲突 | 关键结论无法由机检稳定复现 | 修正机检脚本并连续两次复跑一致 |
| R4 | 外部依赖不可用导致可选证据缺失 | ALPHA_TOKEN 不可用或接口不可达 | 可选证据长期缺失且无说明 | 记录 SKIP 原因并在后续阶段补录一次可选证据 |
| R5 | 回滚路径未被验证 | 存在变更但无可执行回滚证据 | 回滚命令未演练或演练失败 | 补齐回滚演练记录并通过一次完整演练 |
## 本步骤完成判据（最终）
全部满足才算完成：
1. 三类基线（配置/指标/行为）齐全且可复验。
2. 对比器已定义 PASS/WARN/FAIL 阈值，来源可追溯到合同或手册。
3. 关键链路五命令均有证据链且可回放。
4. 自审+自记录完成，阻断项清零或均有挂起依据。
5. 给出明确准入结论，可直接进入 B1。
