# Re_Step-10：D3 数据一致性与恢复重构（单人维护版）

## 文档元数据
| 字段 | 值 |
| --- | --- |
| StepID | RE-STEP-10 |
| Version | 1.0.0 |
| Status | Ready |
| Owner | Solo Maintainer（你） |
| Last Updated | 2026-02-15 |
| 关联主计划 | `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`（阶段D/D3） |
| 证据来源 | `DATA_CONSISTENCY_CONTRACT.md` + `FAILURE_MODEL.md` + B3/D2 产物 |
| 前置步骤 | `RE_STEP_09_D2_线程与执行模型重构.md` |
| 准入后继 | `RE_STEP_11_E1_命令语义统一重构.md` |

## 步骤目标
实现一致性与恢复能力重构，确保并发写不静默覆盖、故障恢复可重放、持久化原子性一致。

核心目标：
1. 写入统一 CAS：实体版本单调递增，冲突返回 `CONSISTENCY-409`。
2. 恢复统一：`snapshot + append-only event_log` 可回放。
3. 持久化统一：`tmp -> fsync -> atomic replace -> backup` 原子策略。
4. 补偿统一：半成功有可追踪补偿终态。

## 为什么此步骤在此顺序
D2 已把线程与执行模型稳定，D3 才能在可控执行环境下改一致性内核。若先改 D3 再改线程，可能因竞态导致一致性机制假通过。

## 输入材料（强制）
1. `Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`
2. `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
3. `Project_Docs/Architecture/FAILURE_MODEL.md`
4. `Project_Docs/Architecture/DATA_FLOW_AND_STATE.md`
5. `Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md`
6. `Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md`
7. `Project_Docs/Re_Step/RE_STEP_05_B3_一致性与SLO契约映射.md`
8. `Project_Docs/Re_Step/RE_STEP_09_D2_线程与执行模型重构.md`

## 输出物定义（强制）
目录（强制）：`Project_Docs/Re_Step/Artifacts/Step10/`

1. `01_D3_CAS_WritePath_Design.md`
- 版本控制、冲突判定、错误码路径。

2. `02_Snapshot_EventLog_Recovery_Design.md`
- 快照生成、日志回放、校验与终态判据。

3. `03_Atomic_Persistence_Standard.md`
- 原子写流程、失败回退、损坏检测。

4. `04_Compensation_and_Replay_Closure.md`
- 半成功补偿状态机与去重策略。

5. `05_D3_FaultInjection_Test_Report.md`
- 冲突注入、损坏快照、重启恢复、回放幂等结果。

6. `06_Solo_Review_Log_D3.md`
- 自审+自记录（含准入结论）。

## 详细执行步骤（编号化）
1. 固化 CAS 写入协议。
- 操作：统一写入前读版本、提交时比较版本、冲突返回 `CONSISTENCY-409`。
- 通过标准：并发冲突不再静默覆盖，冲突事件可审计。
- 失败判据：仍有“最后写入覆盖”且无冲突返回。

2. 实施版本化恢复链路。
- 操作：快照含版本与校验，重启时回放增量日志到一致终态。
- 通过标准：恢复过程可重复执行且结果一致。
- 失败判据：回放二次执行产生额外副作用。

3. 统一原子持久化策略。
- 操作：写临时文件、校验、原子替换、保留备份、失败回滚。
- 通过标准：断电/中断后可检测并恢复，不产生半文件。
- 失败判据：出现损坏文件且无自动回退路径。

4. 建立补偿与重放闭环。
- 操作：对半成功请求执行补偿任务并记录终态。
- 通过标准：终态收敛到 `COMMITTED` 或 `COMPENSATED`。
- 失败判据：存在长期 `PENDING` 无补偿或人工不可追踪。

5. 增加一致性观测与告警。
- 操作：采集冲突率、恢复耗时、补偿积压、回放失败率。
- 通过标准：关键指标可按请求类型和资源类型分组。
- 失败判据：指标缺标签，无法定位具体故障域。

6. 执行故障注入验证。
- 操作：注入并发冲突、快照损坏、日志乱序、重启风暴。
- 通过标准：恢复阈值满足合同，且无重复副作用。
- 失败判据：RTO/RPO 不达标或回放后状态不一致。

7. 准入判定（进入 E1）。
- 操作：判定一致性核心能力是否达发布门槛。
- 通过标准：无 High 阻断项，回滚路径完整可执行。
- 失败判据：关键一致性缺口挂起却进入业务层重构。

## Prompt 模板（至少3个：执行、反证审查、准入判定）
### Prompt-A（执行）
```text
你是数据一致性与恢复重构助手。请完成 Step-10（D3）产物：
输入：
- DATA_CONSISTENCY_CONTRACT.md
- FAILURE_MODEL.md
- OBSERVABILITY_SLO_CONTRACT.md
- Step05/Step09 产物

要求：
1) 输出 CAS 写入设计、恢复设计、原子持久化标准、补偿闭环、故障注入报告、自审+自记录日志。
2) 每项必须包含通过标准、失败判据、验证方法、回滚动作。
3) 单人维护模式：自审+自记录。
```

### Prompt-B（反证审查）
```text
请对 Step-10 产物做反证审查：
1) 找出 10 个最脆弱一致性假设。
2) 必查反例：
   - 并发写冲突被覆盖
   - 快照损坏后无法恢复
   - 回放重复导致重复副作用
   - 原子替换失败后主数据损坏
3) 输出阻断项和补强计划。
```

### Prompt-C（准入判定）
```text
请执行 Step-10 准入判定：
检查对象：
- 01_D3_CAS_WritePath_Design.md
- 02_Snapshot_EventLog_Recovery_Design.md
- 03_Atomic_Persistence_Standard.md
- 04_Compensation_and_Replay_Closure.md
- 05_D3_FaultInjection_Test_Report.md
- 06_Solo_Review_Log_D3.md

判定规则：
1) CAS 冲突是否统一为 CONSISTENCY-409。
2) 恢复链路是否可重放且满足 RTO/RPO。
3) 原子持久化是否可抗中断损坏。
4) 补偿终态是否可收敛。

输出：Verdict/Blocking Issues/Fix Actions。
```

## 建议命名与目录
```text
Project_Docs/
  Re_Step/
    RE_STEP_10_D3_数据一致性与恢复重构.md
    Artifacts/
      Step10/
        01_D3_CAS_WritePath_Design.md
        02_Snapshot_EventLog_Recovery_Design.md
        03_Atomic_Persistence_Standard.md
        04_Compensation_and_Replay_Closure.md
        05_D3_FaultInjection_Test_Report.md
        06_Solo_Review_Log_D3.md
```

## 前置产物硬门禁
1. 本步骤执行前必须完成并固化上一步产物；任一缺失直接判定 Verdict=FAIL，禁止进入下一步。
2. 上一步产物清单（全部强制存在）：
- `Project_Docs/Re_Step/Artifacts/Step09/01_D2_Threading_Refactor_Scope.md`
- `Project_Docs/Re_Step/Artifacts/Step09/02_IO_to_MainThread_Route_Plan.md`
- `Project_Docs/Re_Step/Artifacts/Step09/03_Blocking_Call_Removal_List.md`
- `Project_Docs/Re_Step/Artifacts/Step09/04_Snapshot_Read_and_Scheduler_Shutdown.md`
- `Project_Docs/Re_Step/Artifacts/Step09/05_D2_Stress_and_Boundary_Test_Report.md`
- `Project_Docs/Re_Step/Artifacts/Step09/06_Solo_Review_Log_D2.md`
3. 机检命令（前置产物存在性）：
```bash
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step09/01_D2_Threading_Refactor_Scope.md \
  Project_Docs/Re_Step/Artifacts/Step09/02_IO_to_MainThread_Route_Plan.md \
  Project_Docs/Re_Step/Artifacts/Step09/03_Blocking_Call_Removal_List.md \
  Project_Docs/Re_Step/Artifacts/Step09/04_Snapshot_Read_and_Scheduler_Shutdown.md \
  Project_Docs/Re_Step/Artifacts/Step09/05_D2_Stress_and_Boundary_Test_Report.md \
  Project_Docs/Re_Step/Artifacts/Step09/06_Solo_Review_Log_D2.md >/dev/null
```
4. 门禁判定：
- 通过阈值：上一步产物存在率=100%，可读率=100%，命名与路径与上一步定义完全一致。
- 阻断动作：机检失败后立即 No-Go；仅允许补齐缺失产物并完成一次自审+自记录后重新判定。

## 投产门禁（Go/No-Go）
1. 机检命令（必须逐条执行并留证据）：
```bash
set -euo pipefail
DOC_PATH="/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_10_D3_数据一致性与恢复重构.md"
PREV_EXPECTED=6
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step09/01_D2_Threading_Refactor_Scope.md \
  Project_Docs/Re_Step/Artifacts/Step09/02_IO_to_MainThread_Route_Plan.md \
  Project_Docs/Re_Step/Artifacts/Step09/03_Blocking_Call_Removal_List.md \
  Project_Docs/Re_Step/Artifacts/Step09/04_Snapshot_Read_and_Scheduler_Shutdown.md \
  Project_Docs/Re_Step/Artifacts/Step09/05_D2_Stress_and_Boundary_Test_Report.md \
  Project_Docs/Re_Step/Artifacts/Step09/06_Solo_Review_Log_D2.md \
  >/dev/null
PREV_COUNT=$(ls -1 \
  Project_Docs/Re_Step/Artifacts/Step09/01_D2_Threading_Refactor_Scope.md \
  Project_Docs/Re_Step/Artifacts/Step09/02_IO_to_MainThread_Route_Plan.md \
  Project_Docs/Re_Step/Artifacts/Step09/03_Blocking_Call_Removal_List.md \
  Project_Docs/Re_Step/Artifacts/Step09/04_Snapshot_Read_and_Scheduler_Shutdown.md \
  Project_Docs/Re_Step/Artifacts/Step09/05_D2_Stress_and_Boundary_Test_Report.md \
  Project_Docs/Re_Step/Artifacts/Step09/06_Solo_Review_Log_D2.md \
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
1. 保存路径规范：`Project_Docs/Re_Step/Evidence/Step10/{RUN_ID}/`。
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
EVID_DIR="Project_Docs/Re_Step/Evidence/Step10/${RUN_ID}"
mkdir -p "$EVID_DIR"
{
  echo "step=Step10"
  echo "doc_path=/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_10_D3_数据一致性与恢复重构.md"
  echo "run_id=$RUN_ID"
  date -u
} | tee "$EVID_DIR/00_context.txt"
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step09/01_D2_Threading_Refactor_Scope.md \
  Project_Docs/Re_Step/Artifacts/Step09/02_IO_to_MainThread_Route_Plan.md \
  Project_Docs/Re_Step/Artifacts/Step09/03_Blocking_Call_Removal_List.md \
  Project_Docs/Re_Step/Artifacts/Step09/04_Snapshot_Read_and_Scheduler_Shutdown.md \
  Project_Docs/Re_Step/Artifacts/Step09/05_D2_Stress_and_Boundary_Test_Report.md \
  Project_Docs/Re_Step/Artifacts/Step09/06_Solo_Review_Log_D2.md \
  | tee "$EVID_DIR/01_prev_gate.txt"
rg -n "^## (文档元数据|步骤目标|为什么此步骤在此顺序|输入材料（强制）|输出物定义（强制）|详细执行步骤（编号化）|Prompt 模板（至少3个：执行、反证审查、准入判定）|建议命名与目录|前置产物硬门禁|投产门禁（Go/No-Go）|残余风险与挂起条件|本步骤完成判据（最终）)$" "/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_10_D3_数据一致性与恢复重构.md" | tee "$EVID_DIR/02_section_gate.txt"
wc -l "/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_10_D3_数据一致性与恢复重构.md" | tee "$EVID_DIR/03_line_gate.txt"
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
1. CAS 写入与冲突语义统一且可审计。
2. 恢复链路可回放并满足合同阈值。
3. 原子持久化策略统一并可抗故障中断。
4. 补偿与重放闭环可收敛到明确终态。
5. 自审完成并给出进入 E1 的明确结论。
