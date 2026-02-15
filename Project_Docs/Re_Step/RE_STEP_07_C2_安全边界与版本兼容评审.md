# Re_Step-07：C2 安全边界与版本兼容评审（单人维护版）

## 文档元数据
| 字段 | 值 |
| --- | --- |
| StepID | RE-STEP-07 |
| Version | 1.0.0 |
| Status | Ready |
| Owner | Solo Maintainer（你） |
| Last Updated | 2026-02-15 |
| 关联主计划 | `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`（阶段C/C2） |
| 证据来源 | `SECURITY_BOUNDARY.md` + `VERSIONING_AND_COMPATIBILITY.md` |
| 前置步骤 | `RE_STEP_06_C1_线程模型与故障模型评审.md` |
| 准入后继 | `RE_STEP_08_D1_Bridge通道核心重构.md` |

## 步骤目标
完成“安全控制面”与“版本演进面”的双评审，建立可执行的密钥治理、鉴权边界、协议兼容与弃用门禁。

核心目标：
1. 安全边界清晰：匿名与鉴权路径、端口暴露、token 生命周期、轮换回滚点明确。
2. 版本边界清晰：`protocol_version`、兼容窗口、弃用窗口、灰度回滚规则明确。
3. 安全与版本联动：任何不兼容改动必须有安全与回滚计划。

## 为什么此步骤在此顺序
C2 是 D 阶段代码重构的“上线约束层”。若先改代码再评审边界，极易出现协议升级失配、密钥策略失效或灰度不可回滚。

先做 C2 能把 D 阶段改造限制在“可发布、可回滚、可审计”的轨道内。

## 输入材料（强制）
1. `Project_Docs/Architecture/SECURITY_BOUNDARY.md`
2. `Project_Docs/Architecture/VERSIONING_AND_COMPATIBILITY.md`
3. `Project_Docs/Architecture/SYSTEM_CONTEXT.md`
4. `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`
5. `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`
6. `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
7. `Project_Docs/Manuals/RELEASE_CHECKLIST.md`
8. `Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md`
9. `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`

## 输出物定义（强制）
目录（强制）：`Project_Docs/Re_Step/Artifacts/Step07/`

1. `01_Security_Boundary_Review.md`
- 鉴权边界、暴露面、AuthN/AuthZ 规则、关键风险。

2. `02_Token_Rotation_and_Rollback_Blueprint.md`
- 轮换五阶段、回滚点、中断预算、审计字段。

3. `03_Protocol_Version_Governance.md`
- SemVer 规则、兼容矩阵、feature flag 策略。

4. `04_Deprecation_and_GrayRelease_Gates.md`
- N/N+1/N+2 窗口、遥测指标、自动回滚触发。

5. `05_C2_Risk_Register.md`
- 安全与版本联合风险表（含截止日期与阻断条件）。

6. `06_Solo_Review_Log_C2.md`
- 自审+自记录与准入判定。

## 详细执行步骤（编号化）
1. 评审鉴权与暴露边界。
- 操作：核对匿名可达路径与必须鉴权路径，验证控制面不得匿名访问。
- 通过标准：受保护页面匿名 200 命中目标为 0，且规则可复验。
- 失败判据：存在匿名直达管理页或 `/ws` 未鉴权。

2. 评审 token 生命周期与治理。
- 操作：定义生成、存储、轮换、吊销、审计全流程。
- 通过标准：每阶段有回滚点和中断预算。
- 失败判据：只给轮换流程，不给失败回滚动作。

3. 评审传输与接口安全。
- 操作：确认公网管理接口 TLS/WSS、CORS 最小白名单、敏感字段脱敏。
- 通过标准：无明文管理通道，无通配 CORS 暴露。
- 失败判据：允许公网 HTTP/WS 管理入口长期存在。

4. 评审版本协商与兼容门禁。
- 操作：定义 `protocol_version` 强制策略、同 MAJOR 兼容要求、未知字段容错。
- 通过标准：兼容矩阵可执行，门禁超阈值可回滚。
- 失败判据：新旧混部无量化门槛或无回滚条件。

5. 评审弃用窗口与灰度策略。
- 操作：按 N/N+1/N+2 固化字段/命令/错误码退场机制。
- 通过标准：每个弃用项有遥测指标、迁移指引、最早移除日期。
- 失败判据：弃用仅写“未来移除”，没有窗口与证据要求。

6. 合并安全与版本风险清单。
- 操作：形成联合风险表，标注 High 项阻断 D 阶段条件。
- 通过标准：每个 High 风险有明确截止和阻断动作。
- 失败判据：高风险只有说明，没有时间与处置动作。

7. 准入判定（进入 D1）。
- 操作：C2 门禁 PASS/FAIL，决定是否允许进入底层重构。
- 通过标准：安全与版本双域无未处置 High 阻断项。
- 失败判据：关键边界未收口即进入代码重构阶段。

## Prompt 模板（至少3个：执行、反证审查、准入判定）
### Prompt-A（执行）
```text
你是安全与版本治理评审助手。请完成 Step-07（C2）产物：
输入：
- SECURITY_BOUNDARY.md
- VERSIONING_AND_COMPATIBILITY.md
- RELEASE_CHECKLIST.md
- UPGRADE_MIGRATION_GUIDE.md

要求：
1) 输出安全边界评审、密钥轮换蓝图、版本治理、弃用灰度门禁、风险清单、自审+自记录日志。
2) 每条规则必须有“验证方式 + 通过标准 + 失败判据”。
3) 单人维护模式：自审+自记录。
```

### Prompt-B（反证审查）
```text
请对 Step-07 产物做反证审查：
1) 找出 10 个“最可能导致安全事故或版本失配”的脆弱点。
2) 必查反例：
   - 旧 token 吊销后关键客户端不可用
   - 协议 MAJOR 不兼容但仍放量
   - 弃用窗口不足 60 天就移除
   - 灰度超阈值后未回滚
3) 输出每条反例的触发条件、影响级别、修复动作。
```

### Prompt-C（准入判定）
```text
请执行 Step-07 准入判定：
检查对象：
- 01_Security_Boundary_Review.md
- 02_Token_Rotation_and_Rollback_Blueprint.md
- 03_Protocol_Version_Governance.md
- 04_Deprecation_and_GrayRelease_Gates.md
- 05_C2_Risk_Register.md
- 06_Solo_Review_Log_C2.md

判定规则：
1) 控制面鉴权边界是否收口。
2) token 轮换是否具备回滚点与预算。
3) 版本兼容与灰度回滚门禁是否可执行。
4) High 风险是否全部闭环。

输出：Verdict/Blocking Issues/Fix Actions。
```

## 建议命名与目录
```text
Project_Docs/
  Re_Step/
    RE_STEP_07_C2_安全边界与版本兼容评审.md
    Artifacts/
      Step07/
        01_Security_Boundary_Review.md
        02_Token_Rotation_and_Rollback_Blueprint.md
        03_Protocol_Version_Governance.md
        04_Deprecation_and_GrayRelease_Gates.md
        05_C2_Risk_Register.md
        06_Solo_Review_Log_C2.md
```

## 前置产物硬门禁
1. 本步骤执行前必须完成并固化上一步产物；任一缺失直接判定 Verdict=FAIL，禁止进入下一步。
2. 上一步产物清单（全部强制存在）：
- `Project_Docs/Re_Step/Artifacts/Step06/01_Thread_Owner_Matrix_Review.md`
- `Project_Docs/Re_Step/Artifacts/Step06/02_ForbiddenPattern_Scan_Report.md`
- `Project_Docs/Re_Step/Artifacts/Step06/03_Failure_StateMachine_Review.md`
- `Project_Docs/Re_Step/Artifacts/Step06/04_Pending_Lifecycle_and_Compensation.md`
- `Project_Docs/Re_Step/Artifacts/Step06/05_Chaos_and_Stress_Review_Plan.md`
- `Project_Docs/Re_Step/Artifacts/Step06/06_Solo_Review_Log_C1.md`
3. 机检命令（前置产物存在性）：
```bash
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step06/01_Thread_Owner_Matrix_Review.md \
  Project_Docs/Re_Step/Artifacts/Step06/02_ForbiddenPattern_Scan_Report.md \
  Project_Docs/Re_Step/Artifacts/Step06/03_Failure_StateMachine_Review.md \
  Project_Docs/Re_Step/Artifacts/Step06/04_Pending_Lifecycle_and_Compensation.md \
  Project_Docs/Re_Step/Artifacts/Step06/05_Chaos_and_Stress_Review_Plan.md \
  Project_Docs/Re_Step/Artifacts/Step06/06_Solo_Review_Log_C1.md >/dev/null
```
4. 门禁判定：
- 通过阈值：上一步产物存在率=100%，可读率=100%，命名与路径与上一步定义完全一致。
- 阻断动作：机检失败后立即 No-Go；仅允许补齐缺失产物并完成一次自审+自记录后重新判定。

## 投产门禁（Go/No-Go）
1. 机检命令（必须逐条执行并留证据）：
```bash
set -euo pipefail
DOC_PATH="/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_07_C2_安全边界与版本兼容评审.md"
PREV_EXPECTED=6
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step06/01_Thread_Owner_Matrix_Review.md \
  Project_Docs/Re_Step/Artifacts/Step06/02_ForbiddenPattern_Scan_Report.md \
  Project_Docs/Re_Step/Artifacts/Step06/03_Failure_StateMachine_Review.md \
  Project_Docs/Re_Step/Artifacts/Step06/04_Pending_Lifecycle_and_Compensation.md \
  Project_Docs/Re_Step/Artifacts/Step06/05_Chaos_and_Stress_Review_Plan.md \
  Project_Docs/Re_Step/Artifacts/Step06/06_Solo_Review_Log_C1.md \
  >/dev/null
PREV_COUNT=$(ls -1 \
  Project_Docs/Re_Step/Artifacts/Step06/01_Thread_Owner_Matrix_Review.md \
  Project_Docs/Re_Step/Artifacts/Step06/02_ForbiddenPattern_Scan_Report.md \
  Project_Docs/Re_Step/Artifacts/Step06/03_Failure_StateMachine_Review.md \
  Project_Docs/Re_Step/Artifacts/Step06/04_Pending_Lifecycle_and_Compensation.md \
  Project_Docs/Re_Step/Artifacts/Step06/05_Chaos_and_Stress_Review_Plan.md \
  Project_Docs/Re_Step/Artifacts/Step06/06_Solo_Review_Log_C1.md \
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
1. 保存路径规范：`Project_Docs/Re_Step/Evidence/Step07/{RUN_ID}/`。
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
EVID_DIR="Project_Docs/Re_Step/Evidence/Step07/${RUN_ID}"
mkdir -p "$EVID_DIR"
{
  echo "step=Step07"
  echo "doc_path=/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_07_C2_安全边界与版本兼容评审.md"
  echo "run_id=$RUN_ID"
  date -u
} | tee "$EVID_DIR/00_context.txt"
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step06/01_Thread_Owner_Matrix_Review.md \
  Project_Docs/Re_Step/Artifacts/Step06/02_ForbiddenPattern_Scan_Report.md \
  Project_Docs/Re_Step/Artifacts/Step06/03_Failure_StateMachine_Review.md \
  Project_Docs/Re_Step/Artifacts/Step06/04_Pending_Lifecycle_and_Compensation.md \
  Project_Docs/Re_Step/Artifacts/Step06/05_Chaos_and_Stress_Review_Plan.md \
  Project_Docs/Re_Step/Artifacts/Step06/06_Solo_Review_Log_C1.md \
  | tee "$EVID_DIR/01_prev_gate.txt"
rg -n "^## (文档元数据|步骤目标|为什么此步骤在此顺序|输入材料（强制）|输出物定义（强制）|详细执行步骤（编号化）|Prompt 模板（至少3个：执行、反证审查、准入判定）|建议命名与目录|前置产物硬门禁|投产门禁（Go/No-Go）|残余风险与挂起条件|本步骤完成判据（最终）)$" "/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_07_C2_安全边界与版本兼容评审.md" | tee "$EVID_DIR/02_section_gate.txt"
wc -l "/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_07_C2_安全边界与版本兼容评审.md" | tee "$EVID_DIR/03_line_gate.txt"
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
1. 安全边界、密钥治理、暴露面策略可执行可审计。
2. 版本协商、兼容窗口、弃用与灰度门禁可复验。
3. 安全和版本的联合高风险项清零或有正式挂起依据。
4. 准入结论明确，可直接进入 D1。
5. 自审+自记录完整并可追溯。
