# Re_Step-08：D1 Bridge通道核心重构（单人维护版）

## 文档元数据
| 字段 | 值 |
| --- | --- |
| StepID | RE-STEP-08 |
| Version | 1.0.0 |
| Status | Ready |
| Owner | Solo Maintainer（你） |
| Last Updated | 2026-02-15 |
| 关联主计划 | `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`（阶段D/D1） |
| 证据来源 | B1/B3/C2 产物 + Bridge 相关合同 |
| 前置步骤 | `RE_STEP_07_C2_安全边界与版本兼容评审.md` |
| 准入后继 | `RE_STEP_09_D2_线程与执行模型重构.md` |

## 步骤目标
完成 Bridge 通道的核心能力重构，使其具备版本协商、幂等去重、断连快失败、结构化错误与大小门禁五项基础能力。

本步骤目标：
1. 协议协商：请求携带 `protocol_version` 并执行兼容门禁。
2. 幂等去重：变更型请求以 `idempotencyKey/requestId` 去重。
3. 断连快失败：pending 请求统一失败并回收，不雪崩堆积。
4. 错误统一：标准错误码优先，双栈兼容可观测。
5. 帧大小统一：64KiB 单帧和 46KiB base64 安全上限执行。

## 为什么此步骤在此顺序
D1 是 D2/D3 的通信底盘。若 Bridge 基础能力不先稳定，线程重构与一致性重构会在不稳定通道上反复返工，且故障难以定位。

## 输入材料（强制）
1. `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`
2. `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`
3. `Project_Docs/Architecture/FAILURE_MODEL.md`
4. `Project_Docs/Architecture/VERSIONING_AND_COMPATIBILITY.md`
5. `Project_Docs/Re_Step/RE_STEP_03_B1_Bridge消息与错误契约映射.md`
6. `Project_Docs/Re_Step/RE_STEP_05_B3_一致性与SLO契约映射.md`
7. `Project_Docs/Re_Step/RE_STEP_07_C2_安全边界与版本兼容评审.md`

## 输出物定义（强制）
目录（强制）：`Project_Docs/Re_Step/Artifacts/Step08/`

1. `01_D1_Change_Scope_and_Gates.md`
- 变更范围、禁止项、代码门禁、回滚触发条件。

2. `02_ProtocolVersion_and_Capability_Design.md`
- 协商流程、兼容矩阵、拒绝语义与审计字段。

3. `03_Idempotency_Dedup_Design.md`
- key 结构、TTL、冲突语义、缓存失效策略。

4. `04_Disconnect_FastFail_and_Pending_Reclaim.md`
- 断连后状态迁移、失败码、清理策略。

5. `05_D1_Contract_Test_and_Chaos_Result.md`
- CT 用例与断连/超限/重放实验结果。

6. `06_Solo_Review_Log_D1.md`
- 自审+自记录（含准入结论）。

## 详细执行步骤（编号化）
1. 冻结 D1 变更范围与门禁。
- 操作：限定只改 Bridge 核心链路，不并行引入业务语义变更。
- 通过标准：范围、禁止项、回滚触发写入门禁文档。
- 失败判据：一次提交同时混入命令语义或权限模型改造。

2. 实施协议版本协商。
- 操作：注册/请求链路携带 `protocol_version` 与能力位，执行 MAJOR 兼容判定。
- 通过标准：不兼容版本可稳定拒绝并返回可诊断错误。
- 失败判据：缺失版本请求仍默认为成功路径且无告警。

3. 实施幂等去重缓存。
- 操作：对变更型请求启用 `idempotencyKey` 去重，设 TTL 与冲突返回策略。
- 通过标准：重复请求不产生重复副作用，且可给出一致回执。
- 失败判据：重试风暴下出现重复发放/重复写入。

4. 实施断连快失败与 pending 回收。
- 操作：断连事件触发 pending 统一迁移到 `FAILED_DISCONNECT` 并清理。
- 通过标准：无请求无限等待，回收动作有结构化日志。
- 失败判据：断连后仍长期堆积 pending 或超时雪崩。

5. 实施错误码与帧大小门禁。
- 操作：优先输出结构化 `errorCode`，超限统一 `BRG_VALIDATION_205`。
- 通过标准：所有错误回包可机器解析，超限前置失败。
- 失败判据：同类错误返回多种非合同语义字符串。

6. 执行契约测试与混沌验证。
- 操作：覆盖 register、超时、断连、超限、重放、冲突映射等核心 case。
- 通过标准：关键契约用例全通过，混沌验证无重复副作用。
- 失败判据：关键 case 通过率 < 100% 或结果不可复验。

7. 准入判定（进入 D2）。
- 操作：按协议稳定性、幂等正确性、断连恢复性做 PASS/FAIL。
- 通过标准：无 High 阻断项且回滚路径可执行。
- 失败判据：存在不可回滚的协议变更或幂等误判。

## Prompt 模板（至少3个：执行、反证审查、准入判定）
### Prompt-A（执行）
```text
你是 Bridge 核心重构执行助手。请完成 Step-08（D1）产物：
输入：
- BRIDGE_MESSAGE_CONTRACT.md
- BRIDGE_ERROR_CODE_CONTRACT.md
- FAILURE_MODEL.md
- VERSIONING_AND_COMPATIBILITY.md

要求：
1) 输出 D1 范围门禁、协议协商设计、幂等去重、断连回收、测试结果、自审+自记录日志。
2) 每项必须包含通过标准、失败判据、验证方法与回滚动作。
3) 单人维护模式：自审+自记录。
```

### Prompt-B（反证审查）
```text
请对 Step-08 产物执行反证审查：
1) 找出 10 个最可能导致“半成功/重试污染/协议失配”的脆弱点。
2) 必查反例：
   - 同一 idempotencyKey 重放
   - 断连后 pending 未回收
   - protocol_version 缺失/不兼容
   - payload 超限但未被拒绝
3) 输出阻断项与修复动作。
```

### Prompt-C（准入判定）
```text
请对 Step-08 做准入判定：
检查对象：
- 01_D1_Change_Scope_and_Gates.md
- 02_ProtocolVersion_and_Capability_Design.md
- 03_Idempotency_Dedup_Design.md
- 04_Disconnect_FastFail_and_Pending_Reclaim.md
- 05_D1_Contract_Test_and_Chaos_Result.md
- 06_Solo_Review_Log_D1.md

判定规则：
1) 协议协商是否可阻断不兼容请求。
2) 幂等去重是否防重复副作用。
3) 断连是否快失败并回收 pending。
4) 高风险改动是否有回滚路径。

输出：Verdict/Blocking Issues/Fix Actions。
```

## 建议命名与目录
```text
Project_Docs/
  Re_Step/
    RE_STEP_08_D1_Bridge通道核心重构.md
    Artifacts/
      Step08/
        01_D1_Change_Scope_and_Gates.md
        02_ProtocolVersion_and_Capability_Design.md
        03_Idempotency_Dedup_Design.md
        04_Disconnect_FastFail_and_Pending_Reclaim.md
        05_D1_Contract_Test_and_Chaos_Result.md
        06_Solo_Review_Log_D1.md
```

## 前置产物硬门禁
1. 本步骤执行前必须完成并固化上一步产物；任一缺失直接判定 Verdict=FAIL，禁止进入下一步。
2. 上一步产物清单（全部强制存在）：
- `Project_Docs/Re_Step/Artifacts/Step07/01_Security_Boundary_Review.md`
- `Project_Docs/Re_Step/Artifacts/Step07/02_Token_Rotation_and_Rollback_Blueprint.md`
- `Project_Docs/Re_Step/Artifacts/Step07/03_Protocol_Version_Governance.md`
- `Project_Docs/Re_Step/Artifacts/Step07/04_Deprecation_and_GrayRelease_Gates.md`
- `Project_Docs/Re_Step/Artifacts/Step07/05_C2_Risk_Register.md`
- `Project_Docs/Re_Step/Artifacts/Step07/06_Solo_Review_Log_C2.md`
3. 机检命令（前置产物存在性）：
```bash
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step07/01_Security_Boundary_Review.md \
  Project_Docs/Re_Step/Artifacts/Step07/02_Token_Rotation_and_Rollback_Blueprint.md \
  Project_Docs/Re_Step/Artifacts/Step07/03_Protocol_Version_Governance.md \
  Project_Docs/Re_Step/Artifacts/Step07/04_Deprecation_and_GrayRelease_Gates.md \
  Project_Docs/Re_Step/Artifacts/Step07/05_C2_Risk_Register.md \
  Project_Docs/Re_Step/Artifacts/Step07/06_Solo_Review_Log_C2.md >/dev/null
```
4. 门禁判定：
- 通过阈值：上一步产物存在率=100%，可读率=100%，命名与路径与上一步定义完全一致。
- 阻断动作：机检失败后立即 No-Go；仅允许补齐缺失产物并完成一次自审+自记录后重新判定。

## 投产门禁（Go/No-Go）
1. 机检命令（必须逐条执行并留证据）：
```bash
set -euo pipefail
DOC_PATH="/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_08_D1_Bridge通道核心重构.md"
PREV_EXPECTED=6
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step07/01_Security_Boundary_Review.md \
  Project_Docs/Re_Step/Artifacts/Step07/02_Token_Rotation_and_Rollback_Blueprint.md \
  Project_Docs/Re_Step/Artifacts/Step07/03_Protocol_Version_Governance.md \
  Project_Docs/Re_Step/Artifacts/Step07/04_Deprecation_and_GrayRelease_Gates.md \
  Project_Docs/Re_Step/Artifacts/Step07/05_C2_Risk_Register.md \
  Project_Docs/Re_Step/Artifacts/Step07/06_Solo_Review_Log_C2.md \
  >/dev/null
PREV_COUNT=$(ls -1 \
  Project_Docs/Re_Step/Artifacts/Step07/01_Security_Boundary_Review.md \
  Project_Docs/Re_Step/Artifacts/Step07/02_Token_Rotation_and_Rollback_Blueprint.md \
  Project_Docs/Re_Step/Artifacts/Step07/03_Protocol_Version_Governance.md \
  Project_Docs/Re_Step/Artifacts/Step07/04_Deprecation_and_GrayRelease_Gates.md \
  Project_Docs/Re_Step/Artifacts/Step07/05_C2_Risk_Register.md \
  Project_Docs/Re_Step/Artifacts/Step07/06_Solo_Review_Log_C2.md \
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
1. 保存路径规范：`Project_Docs/Re_Step/Evidence/Step08/{RUN_ID}/`。
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
EVID_DIR="Project_Docs/Re_Step/Evidence/Step08/${RUN_ID}"
mkdir -p "$EVID_DIR"
{
  echo "step=Step08"
  echo "doc_path=/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_08_D1_Bridge通道核心重构.md"
  echo "run_id=$RUN_ID"
  date -u
} | tee "$EVID_DIR/00_context.txt"
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step07/01_Security_Boundary_Review.md \
  Project_Docs/Re_Step/Artifacts/Step07/02_Token_Rotation_and_Rollback_Blueprint.md \
  Project_Docs/Re_Step/Artifacts/Step07/03_Protocol_Version_Governance.md \
  Project_Docs/Re_Step/Artifacts/Step07/04_Deprecation_and_GrayRelease_Gates.md \
  Project_Docs/Re_Step/Artifacts/Step07/05_C2_Risk_Register.md \
  Project_Docs/Re_Step/Artifacts/Step07/06_Solo_Review_Log_C2.md \
  | tee "$EVID_DIR/01_prev_gate.txt"
rg -n "^## (文档元数据|步骤目标|为什么此步骤在此顺序|输入材料（强制）|输出物定义（强制）|详细执行步骤（编号化）|Prompt 模板（至少3个：执行、反证审查、准入判定）|建议命名与目录|前置产物硬门禁|投产门禁（Go/No-Go）|残余风险与挂起条件|本步骤完成判据（最终）)$" "/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_08_D1_Bridge通道核心重构.md" | tee "$EVID_DIR/02_section_gate.txt"
wc -l "/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_08_D1_Bridge通道核心重构.md" | tee "$EVID_DIR/03_line_gate.txt"
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
1. 协议版本协商与能力位门禁生效。
2. 幂等去重在重放场景下无重复副作用。
3. 断连后 pending 统一快失败且可审计回收。
4. 错误码与帧大小门禁符合合同。
5. 自审完成并给出进入 D2 的明确结论。
