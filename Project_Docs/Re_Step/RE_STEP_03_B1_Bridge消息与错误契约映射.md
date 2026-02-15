# Re_Step-03：B1 Bridge消息与错误契约映射（单人维护版）

## 文档元数据
| 字段 | 值 |
| --- | --- |
| StepID | RE-STEP-03 |
| Version | 1.0.0 |
| Status | Ready |
| Owner | Solo Maintainer（你） |
| Last Updated | 2026-02-15 |
| 关联主计划 | `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`（阶段B/B1） |
| 证据来源 | `BRIDGE_MESSAGE_CONTRACT.md` + `BRIDGE_ERROR_CODE_CONTRACT.md` + 相关架构文档 |
| 前置步骤 | `RE_STEP_02_A2_基线采样与基线对比体系.md` |
| 准入后继 | `RE_STEP_04_B2_权限与配置契约映射.md` |

## 步骤目标
把 Bridge 协议“合同条款”逐条映射到“现有实现位点或缺口项”，形成可改造、可验收的协议落地清单。

本步骤必须解决三件事：
1. 消息字段映射：`implemented/planned/deprecated` 与代码位点一一对应。
2. 错误语义映射：结构化错误码与字符串错误双栈优先级一致。
3. 门禁映射：64KiB 单帧、46KiB base64 安全上限与超限错误码闭环。

## 为什么此步骤在此顺序
B1 是 B2/B3 的协议基础。若消息/错误语义未统一，后续权限、一致性与 SLO 指标会建立在漂移协议上，导致“上层正确、底层不一致”。

先做 B1 可将协议口径稳定为唯一真相，再推进权限和数据层映射，避免返工。

## 输入材料（强制）
1. `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`
2. `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`
3. `Project_Docs/Contracts/CONTRACT_INDEX.md`
4. `Project_Docs/Architecture/SYSTEM_CONTEXT.md`
5. `Project_Docs/Architecture/DATA_FLOW_AND_STATE.md`
6. `Project_Docs/Architecture/FAILURE_MODEL.md`
7. `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`
8. `Project_Docs/Re_Step/RE_STEP_02_A2_基线采样与基线对比体系.md`

## 输出物定义（强制）
目录（强制）：`Project_Docs/Re_Step/Artifacts/Step03/`

1. `01_Message_Field_Mapping_Table.md`
- 合同字段、规范级别、实现状态、代码位点、缺口说明。

2. `02_Message_Type_Execution_Matrix.md`
- request/response/event 类型与状态机行为映射。

3. `03_Error_Code_DualStack_Mapping.md`
- `errorCode` 优先级、字符串兜底、冲突标记 `mappingConflict` 规则。

4. `04_FrameSize_Gate_and_Precheck.md`
- 64KiB/46KiB 上限检查与 `BRG_VALIDATION_205` 回包路径。

5. `05_B1_Gap_Backlog.md`
- P0/P1 缺口、风险等级、修复顺序、验证方法。

6. `06_Solo_Review_Log_B1.md`
- 自审结论与准入判定记录。

## 详细执行步骤（编号化）
1. 建立消息字段总清单。
- 操作：从合同抽取 `type/requestId/serverId/payload/result/error/success/protocolVersion` 等字段，标注 MUST/SHOULD/MAY。
- 通过标准：字段清单 100% 来自合同，且每项有规范级别。
- 失败判据：字段来源混杂代码猜测，未区分合同等级。

2. 映射消息类型与状态机。
- 操作：对齐 `register/heartbeat/proxy_request/proxy_response/file_*` 的请求-响应-事件语义。
- 通过标准：每个 `type` 都能定位“输入条件+期望输出+异常输出”。
- 失败判据：存在“只入不出”或“仅日志无协议回包”的类型。

3. 落地错误码双栈优先级。
- 操作：按合同固定优先级：结构化 `errorCode` > 字符串映射 > `BRG_INTERNAL_999`。
- 通过标准：冲突场景统一写 `mappingConflict=true` 且可审计。
- 失败判据：同一错误在不同入口映射到不同标准码。

4. 建立消息体大小门禁。
- 操作：定义发送前预检、接收端拒绝、超限统一 `BRG_VALIDATION_205`。
- 通过标准：超限路径有明确拒绝动作与日志字段。
- 失败判据：仍存在“发送后被截断”或“超限静默失败”。

5. 构建兼容窗口与退场计划。
- 操作：按 `v1.1.x -> v1.2.x -> v2.0.0+` 约束写明双栈保留与移除条件。
- 通过标准：每个阶段有“允许行为/禁止行为/观测指标”。
- 失败判据：只写目标版本，不写迁移期间约束。

6. 形成缺口整改队列（P0/P1）。
- 操作：输出字段缺口、错误码缺口、测试缺口，附责任、截止、验证命令。
- 通过标准：每个缺口均可被“一个验证动作”证明修复。
- 失败判据：缺口仅描述现象，无修复入口和验收条件。

7. 准入判定（进入 B2）。
- 操作：核查映射覆盖率、错误码一致性、门禁完整性。
- 通过标准：无 P0 未闭环项，P1 有明确计划与挂起理由。
- 失败判据：存在字段/错误码无归属状态（未知/待观察）。

## Prompt 模板（至少3个：执行、反证审查、准入判定）
### Prompt-A（执行）
```text
你是 Bridge 契约落地执行助手。请完成 Step-03（B1）全部产物：
输入：
- BRIDGE_MESSAGE_CONTRACT.md
- BRIDGE_ERROR_CODE_CONTRACT.md
- CONTRACT_INDEX.md
- SYSTEM_CONTEXT.md / DATA_FLOW_AND_STATE.md / FAILURE_MODEL.md

要求：
1) 生成字段映射、消息执行矩阵、错误码双栈映射、帧大小门禁、缺口队列、自审+自记录日志。
2) 每项必须包含“合同条款 -> 当前实现 -> 差距 -> 修复动作 -> 验收方式”。
3) 单人维护模式：统一自审+自记录。
4) 每个结论给证据路径。
```

### Prompt-B（反证审查）
```text
请对 Step-03 产物做反证审查：
1) 枚举 10 个“最可能导致协议漂移”的漏洞。
2) 强制审查以下反例：
   - 首帧不是 register
   - errorCode 与 rawError 映射冲突
   - payload 超 64KiB
   - 变更型请求被隐式自动重试
3) 对每个反例给出：触发条件、影响、检测信号、修补动作。
4) 输出阻断项（必须阻断进入 B2）。
```

### Prompt-C（准入判定）
```text
请执行 Step-03 门禁判定：
检查对象：
- 01_Message_Field_Mapping_Table.md
- 02_Message_Type_Execution_Matrix.md
- 03_Error_Code_DualStack_Mapping.md
- 04_FrameSize_Gate_and_Precheck.md
- 05_B1_Gap_Backlog.md
- 06_Solo_Review_Log_B1.md

判定规则：
1) 字段映射覆盖率是否 100%。
2) 错误码优先级是否唯一且一致。
3) 帧大小门禁是否可执行可验证。
4) P0 缺口是否清零。

输出：Verdict/Blocking Issues/Fix Actions。
```

## 建议命名与目录
```text
Project_Docs/
  Re_Step/
    RE_STEP_03_B1_Bridge消息与错误契约映射.md
    Artifacts/
      Step03/
        01_Message_Field_Mapping_Table.md
        02_Message_Type_Execution_Matrix.md
        03_Error_Code_DualStack_Mapping.md
        04_FrameSize_Gate_and_Precheck.md
        05_B1_Gap_Backlog.md
        06_Solo_Review_Log_B1.md
```

## 前置产物硬门禁
1. 本步骤执行前必须完成并固化上一步产物；任一缺失直接判定 Verdict=FAIL，禁止进入下一步。
2. 上一步产物清单（全部强制存在）：
- `Project_Docs/Re_Step/Artifacts/Step02/01_Baseline_Environment_Manifest.md`
- `Project_Docs/Re_Step/Artifacts/Step02/02_Baseline_Config_Snapshots.md`
- `Project_Docs/Re_Step/Artifacts/Step02/03_Baseline_Metrics_Report.md`
- `Project_Docs/Re_Step/Artifacts/Step02/04_Baseline_Behavior_Trace.md`
- `Project_Docs/Re_Step/Artifacts/Step02/05_Baseline_Comparator_Spec.md`
- `Project_Docs/Re_Step/Artifacts/Step02/06_Solo_Review_Log_A2.md`
3. 机检命令（前置产物存在性）：
```bash
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step02/01_Baseline_Environment_Manifest.md \
  Project_Docs/Re_Step/Artifacts/Step02/02_Baseline_Config_Snapshots.md \
  Project_Docs/Re_Step/Artifacts/Step02/03_Baseline_Metrics_Report.md \
  Project_Docs/Re_Step/Artifacts/Step02/04_Baseline_Behavior_Trace.md \
  Project_Docs/Re_Step/Artifacts/Step02/05_Baseline_Comparator_Spec.md \
  Project_Docs/Re_Step/Artifacts/Step02/06_Solo_Review_Log_A2.md >/dev/null
```
4. 门禁判定：
- 通过阈值：上一步产物存在率=100%，可读率=100%，命名与路径与上一步定义完全一致。
- 阻断动作：机检失败后立即 No-Go；仅允许补齐缺失产物并完成一次自审+自记录后重新判定。

## 投产门禁（Go/No-Go）
1. 机检命令（必须逐条执行并留证据）：
```bash
set -euo pipefail
DOC_PATH="/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_03_B1_Bridge消息与错误契约映射.md"
PREV_EXPECTED=6
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step02/01_Baseline_Environment_Manifest.md \
  Project_Docs/Re_Step/Artifacts/Step02/02_Baseline_Config_Snapshots.md \
  Project_Docs/Re_Step/Artifacts/Step02/03_Baseline_Metrics_Report.md \
  Project_Docs/Re_Step/Artifacts/Step02/04_Baseline_Behavior_Trace.md \
  Project_Docs/Re_Step/Artifacts/Step02/05_Baseline_Comparator_Spec.md \
  Project_Docs/Re_Step/Artifacts/Step02/06_Solo_Review_Log_A2.md \
  >/dev/null
PREV_COUNT=$(ls -1 \
  Project_Docs/Re_Step/Artifacts/Step02/01_Baseline_Environment_Manifest.md \
  Project_Docs/Re_Step/Artifacts/Step02/02_Baseline_Config_Snapshots.md \
  Project_Docs/Re_Step/Artifacts/Step02/03_Baseline_Metrics_Report.md \
  Project_Docs/Re_Step/Artifacts/Step02/04_Baseline_Behavior_Trace.md \
  Project_Docs/Re_Step/Artifacts/Step02/05_Baseline_Comparator_Spec.md \
  Project_Docs/Re_Step/Artifacts/Step02/06_Solo_Review_Log_A2.md \
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
1. 保存路径规范：`Project_Docs/Re_Step/Evidence/Step03/{RUN_ID}/`。
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
EVID_DIR="Project_Docs/Re_Step/Evidence/Step03/${RUN_ID}"
mkdir -p "$EVID_DIR"
{
  echo "step=Step03"
  echo "doc_path=/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_03_B1_Bridge消息与错误契约映射.md"
  echo "run_id=$RUN_ID"
  date -u
} | tee "$EVID_DIR/00_context.txt"
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step02/01_Baseline_Environment_Manifest.md \
  Project_Docs/Re_Step/Artifacts/Step02/02_Baseline_Config_Snapshots.md \
  Project_Docs/Re_Step/Artifacts/Step02/03_Baseline_Metrics_Report.md \
  Project_Docs/Re_Step/Artifacts/Step02/04_Baseline_Behavior_Trace.md \
  Project_Docs/Re_Step/Artifacts/Step02/05_Baseline_Comparator_Spec.md \
  Project_Docs/Re_Step/Artifacts/Step02/06_Solo_Review_Log_A2.md \
  | tee "$EVID_DIR/01_prev_gate.txt"
rg -n "^## (文档元数据|步骤目标|为什么此步骤在此顺序|输入材料（强制）|输出物定义（强制）|详细执行步骤（编号化）|Prompt 模板（至少3个：执行、反证审查、准入判定）|建议命名与目录|前置产物硬门禁|投产门禁（Go/No-Go）|残余风险与挂起条件|本步骤完成判据（最终）)$" "/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_03_B1_Bridge消息与错误契约映射.md" | tee "$EVID_DIR/02_section_gate.txt"
wc -l "/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_03_B1_Bridge消息与错误契约映射.md" | tee "$EVID_DIR/03_line_gate.txt"
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
1. Bridge 字段、类型、状态机映射完整无空洞。
2. 错误码双栈优先级固定且冲突可审计。
3. 消息大小门禁可复验，超限行为统一。
4. P0 缺口全部闭环，P1 缺口有时限计划。
5. 形成自审+自记录并给出进入 B2 的明确结论。
