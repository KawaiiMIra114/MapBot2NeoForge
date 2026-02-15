# Re_Step-09：D2 线程与执行模型重构（单人维护版）

## 文档元数据
| 字段 | 值 |
| --- | --- |
| StepID | RE-STEP-09 |
| Version | 1.0.0 |
| Status | Ready |
| Owner | Solo Maintainer（你） |
| Last Updated | 2026-02-15 |
| 关联主计划 | `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`（阶段D/D2） |
| 证据来源 | `THREADING_MODEL.md` + C1/C2/D1 产物 |
| 前置步骤 | `RE_STEP_08_D1_Bridge通道核心重构.md` |
| 准入后继 | `RE_STEP_10_D3_数据一致性与恢复重构.md` |

## 步骤目标
完成线程与执行模型改造，确保网络线程不触达游戏副作用、主线程无阻塞等待、执行路径可审计。

核心目标：
1. IO 线程只做收发与解析，不直接调用游戏对象写路径。
2. 所有副作用统一回主线程提交。
3. 主线程路径移除 `get/join/sleep` 等阻塞点。
4. 调度器和后台线程可有序停机、无悬挂。

## 为什么此步骤在此顺序
D1 先稳定通信语义后，D2 才能重构执行模型而不引入协议噪声。若反过来，线程重构与协议缺陷会相互放大，排障成本陡增。

## 输入材料（强制）
1. `Project_Docs/Architecture/THREADING_MODEL.md`
2. `Project_Docs/Architecture/MODULE_BOUNDARY.md`
3. `Project_Docs/Architecture/FAILURE_MODEL.md`
4. `Project_Docs/Re_Step/RE_STEP_06_C1_线程模型与故障模型评审.md`
5. `Project_Docs/Re_Step/RE_STEP_08_D1_Bridge通道核心重构.md`
6. `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`
7. `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`

## 输出物定义（强制）
目录（强制）：`Project_Docs/Re_Step/Artifacts/Step09/`

1. `01_D2_Threading_Refactor_Scope.md`
- 线程改造范围、风险点、冻结规则。

2. `02_IO_to_MainThread_Route_Plan.md`
- IO->DTO->队列->主线程提交路径规范。

3. `03_Blocking_Call_Removal_List.md`
- 阻塞调用清单、替代方案、复测结果。

4. `04_Snapshot_Read_and_Scheduler_Shutdown.md`
- 快照读策略、调度器生命周期、退出流程。

5. `05_D2_Stress_and_Boundary_Test_Report.md`
- 越界探测、死锁检查、主线程预算测试结果。

6. `06_Solo_Review_Log_D2.md`
- 自审+自记录（含准入结论）。

## 详细执行步骤（编号化）
1. 固化 D2 改造范围与回滚边界。
- 操作：限定只改线程归属和执行路径，不改变业务语义。
- 通过标准：范围、回滚点、冻结条件文档化。
- 失败判据：线程改造同时引入业务逻辑变更且不可分离。

2. 重构 IO 到主线程提交链路。
- 操作：网络回调仅构建不可变 DTO，副作用统一 `server.execute` 回切。
- 通过标准：游戏对象写操作主线程命中率 100%。
- 失败判据：出现非主线程写世界状态路径。

3. 清除主线程阻塞调用。
- 操作：移除或替换 `Future#get/join/sleep` 与阻塞网络调用。
- 通过标准：扫描命中 0，且压测下主线程预算达标。
- 失败判据：主线程仍可被外部 I/O 或等待阻塞。

4. 建立快照读与并发容器规范。
- 操作：后台线程仅读主线程快照，不直接持有实体引用。
- 通过标准：跨线程共享对象均为不可变值或并发安全容器。
- 失败判据：保留 `Player/ItemStack/ServerLevel` 跨线程延后使用。

5. 统一调度器生命周期。
- 操作：停止匿名线程，统一调度器命名、启停、异常处理和停服回收。
- 通过标准：停服后无悬挂线程，调度器可有序关闭。
- 失败判据：仍存在 `new Thread(...).start()` 核心流程线程。

6. 执行边界与压力验证。
- 操作：执行线程越界探测、死锁回归、主线程预算压测。
- 通过标准：越界=0、死锁=0、主线程 P95/P99 达标。
- 失败判据：任一关键阈值不达标且无降级或回滚动作。

7. 准入判定（进入 D3）。
- 操作：对线程安全性、执行可控性、运维可恢复性做 PASS/FAIL。
- 通过标准：高风险线程问题清零，回滚路径可执行。
- 失败判据：存在已知线程越界仍继续推进数据层重构。

## Prompt 模板（至少3个：执行、反证审查、准入判定）
### Prompt-A（执行）
```text
你是线程执行模型重构助手。请完成 Step-09（D2）产物：
输入：
- THREADING_MODEL.md
- MODULE_BOUNDARY.md
- FAILURE_MODEL.md
- Step06/Step08 产物

要求：
1) 输出范围文档、IO到主线程路由、阻塞调用清单、快照与调度器策略、测试报告、自审+自记录日志。
2) 每个步骤必须给通过标准/失败判据/回滚动作。
3) 单人维护模式：自审+自记录。
```

### Prompt-B（反证审查）
```text
请对 Step-09 产物做反证审查：
1) 找出 10 个最可能导致竞态或主线程卡顿的脆弱点。
2) 必查反例：
   - IO线程直接调用 MC API
   - 主线程 join/get/sleep
   - 匿名线程无生命周期管理
   - 快照过期导致错误判定
3) 输出阻断项与补强动作。
```

### Prompt-C（准入判定）
```text
请执行 Step-09 准入判定：
检查对象：
- 01_D2_Threading_Refactor_Scope.md
- 02_IO_to_MainThread_Route_Plan.md
- 03_Blocking_Call_Removal_List.md
- 04_Snapshot_Read_and_Scheduler_Shutdown.md
- 05_D2_Stress_and_Boundary_Test_Report.md
- 06_Solo_Review_Log_D2.md

判定规则：
1) 主线程阻塞调用是否清零。
2) 非主线程副作用写是否清零。
3) 停服后线程是否可有序退出。
4) 压测阈值是否达标。

输出：Verdict/Blocking Issues/Fix Actions。
```

## 建议命名与目录
```text
Project_Docs/
  Re_Step/
    RE_STEP_09_D2_线程与执行模型重构.md
    Artifacts/
      Step09/
        01_D2_Threading_Refactor_Scope.md
        02_IO_to_MainThread_Route_Plan.md
        03_Blocking_Call_Removal_List.md
        04_Snapshot_Read_and_Scheduler_Shutdown.md
        05_D2_Stress_and_Boundary_Test_Report.md
        06_Solo_Review_Log_D2.md
```

## 前置产物硬门禁
1. 本步骤执行前必须完成并固化上一步产物；任一缺失直接判定 Verdict=FAIL，禁止进入下一步。
2. 上一步产物清单（全部强制存在）：
- `Project_Docs/Re_Step/Artifacts/Step08/01_D1_Change_Scope_and_Gates.md`
- `Project_Docs/Re_Step/Artifacts/Step08/02_ProtocolVersion_and_Capability_Design.md`
- `Project_Docs/Re_Step/Artifacts/Step08/03_Idempotency_Dedup_Design.md`
- `Project_Docs/Re_Step/Artifacts/Step08/04_Disconnect_FastFail_and_Pending_Reclaim.md`
- `Project_Docs/Re_Step/Artifacts/Step08/05_D1_Contract_Test_and_Chaos_Result.md`
- `Project_Docs/Re_Step/Artifacts/Step08/06_Solo_Review_Log_D1.md`
3. 机检命令（前置产物存在性）：
```bash
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step08/01_D1_Change_Scope_and_Gates.md \
  Project_Docs/Re_Step/Artifacts/Step08/02_ProtocolVersion_and_Capability_Design.md \
  Project_Docs/Re_Step/Artifacts/Step08/03_Idempotency_Dedup_Design.md \
  Project_Docs/Re_Step/Artifacts/Step08/04_Disconnect_FastFail_and_Pending_Reclaim.md \
  Project_Docs/Re_Step/Artifacts/Step08/05_D1_Contract_Test_and_Chaos_Result.md \
  Project_Docs/Re_Step/Artifacts/Step08/06_Solo_Review_Log_D1.md >/dev/null
```
4. 门禁判定：
- 通过阈值：上一步产物存在率=100%，可读率=100%，命名与路径与上一步定义完全一致。
- 阻断动作：机检失败后立即 No-Go；仅允许补齐缺失产物并完成一次自审+自记录后重新判定。

## 投产门禁（Go/No-Go）
1. 机检命令（必须逐条执行并留证据）：
```bash
set -euo pipefail
DOC_PATH="/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_09_D2_线程与执行模型重构.md"
PREV_EXPECTED=6
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step08/01_D1_Change_Scope_and_Gates.md \
  Project_Docs/Re_Step/Artifacts/Step08/02_ProtocolVersion_and_Capability_Design.md \
  Project_Docs/Re_Step/Artifacts/Step08/03_Idempotency_Dedup_Design.md \
  Project_Docs/Re_Step/Artifacts/Step08/04_Disconnect_FastFail_and_Pending_Reclaim.md \
  Project_Docs/Re_Step/Artifacts/Step08/05_D1_Contract_Test_and_Chaos_Result.md \
  Project_Docs/Re_Step/Artifacts/Step08/06_Solo_Review_Log_D1.md \
  >/dev/null
PREV_COUNT=$(ls -1 \
  Project_Docs/Re_Step/Artifacts/Step08/01_D1_Change_Scope_and_Gates.md \
  Project_Docs/Re_Step/Artifacts/Step08/02_ProtocolVersion_and_Capability_Design.md \
  Project_Docs/Re_Step/Artifacts/Step08/03_Idempotency_Dedup_Design.md \
  Project_Docs/Re_Step/Artifacts/Step08/04_Disconnect_FastFail_and_Pending_Reclaim.md \
  Project_Docs/Re_Step/Artifacts/Step08/05_D1_Contract_Test_and_Chaos_Result.md \
  Project_Docs/Re_Step/Artifacts/Step08/06_Solo_Review_Log_D1.md \
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
1. 保存路径规范：`Project_Docs/Re_Step/Evidence/Step09/{RUN_ID}/`。
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
EVID_DIR="Project_Docs/Re_Step/Evidence/Step09/${RUN_ID}"
mkdir -p "$EVID_DIR"
{
  echo "step=Step09"
  echo "doc_path=/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_09_D2_线程与执行模型重构.md"
  echo "run_id=$RUN_ID"
  date -u
} | tee "$EVID_DIR/00_context.txt"
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step08/01_D1_Change_Scope_and_Gates.md \
  Project_Docs/Re_Step/Artifacts/Step08/02_ProtocolVersion_and_Capability_Design.md \
  Project_Docs/Re_Step/Artifacts/Step08/03_Idempotency_Dedup_Design.md \
  Project_Docs/Re_Step/Artifacts/Step08/04_Disconnect_FastFail_and_Pending_Reclaim.md \
  Project_Docs/Re_Step/Artifacts/Step08/05_D1_Contract_Test_and_Chaos_Result.md \
  Project_Docs/Re_Step/Artifacts/Step08/06_Solo_Review_Log_D1.md \
  | tee "$EVID_DIR/01_prev_gate.txt"
rg -n "^## (文档元数据|步骤目标|为什么此步骤在此顺序|输入材料（强制）|输出物定义（强制）|详细执行步骤（编号化）|Prompt 模板（至少3个：执行、反证审查、准入判定）|建议命名与目录|前置产物硬门禁|投产门禁（Go/No-Go）|残余风险与挂起条件|本步骤完成判据（最终）)$" "/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_09_D2_线程与执行模型重构.md" | tee "$EVID_DIR/02_section_gate.txt"
wc -l "/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_09_D2_线程与执行模型重构.md" | tee "$EVID_DIR/03_line_gate.txt"
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
1. 线程归属与执行路径符合架构合同。
2. 主线程阻塞点清零并通过预算压测。
3. IO/Worker 不再直接触达游戏副作用对象。
4. 调度器生命周期可控，停服无悬挂线程。
5. 自审完成并给出进入 D3 的明确结论。
