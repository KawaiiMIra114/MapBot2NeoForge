# Re_Step-04：B2 权限与配置契约映射（单人维护版）

## 文档元数据
| 字段 | 值 |
| --- | --- |
| StepID | RE-STEP-04 |
| Version | 1.0.0 |
| Status | Ready |
| Owner | Solo Maintainer（你） |
| Last Updated | 2026-02-15 |
| 关联主计划 | `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`（阶段B/B2） |
| 证据来源 | `COMMAND_AUTHORIZATION_CONTRACT.md` + `CONFIG_SCHEMA_CONTRACT.md` |
| 前置步骤 | `RE_STEP_03_B1_Bridge消息与错误契约映射.md` |
| 准入后继 | `RE_STEP_05_B3_一致性与SLO契约映射.md` |

## 步骤目标
把“权限语义”和“配置语义”从分散实现收口为合同一致模型，确保命令鉴权、配置热重载、回滚与审计行为统一。

重点目标：
1. 角色统一：仅 `user/admin/owner` 有效，未知角色必须拒绝。
2. 鉴权统一：拒绝错误码固定 `AUTH-403`，不进入业务副作用。
3. 配置统一：类型/范围/热重载/回滚/审计字段完整可验证。

## 为什么此步骤在此顺序
B1 已稳定协议层，B2 负责“谁可做什么”和“配置如何生效”。如果跳过 B2，后续 D/E 改造会把错误权限与错误配置放大到核心链路。

先固化权限和配置，才能保证后续一致性、线程、安全改造不会建立在错误治理基线上。

## 输入材料（强制）
1. `Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md`
2. `Project_Docs/Contracts/CONFIG_SCHEMA_CONTRACT.md`
3. `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
4. `Project_Docs/Architecture/MODULE_BOUNDARY.md`
5. `Project_Docs/Architecture/SECURITY_BOUNDARY.md`
6. `Project_Docs/Manuals/OPERATIONS_RUNBOOK.md`
7. `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`
8. `Project_Docs/Re_Step/RE_STEP_03_B1_Bridge消息与错误契约映射.md`

## 输出物定义（强制）
目录（强制）：`Project_Docs/Re_Step/Artifacts/Step04/`

1. `01_Command_Authorization_Matrix.md`
- 命令分类、最小角色阈值、可见性、拒绝语义。

2. `02_Role_Normalization_and_Migration.md`
- 旧角色到三角色映射、未知角色拒绝策略、迁移风险。

3. `03_Config_Schema_Validation_Profile.md`
- 类型、默认值、范围、未知键 fail-closed、热重载能力表。

4. `04_HotReload_Rollback_Audit_Flow.md`
- `解析 -> 校验 -> 原子替换 -> 审计落盘 -> 失败回滚` 流程证据。

5. `05_B2_Negative_Test_Cases.md`
- 越权、未知角色、非法配置、热重载失败回滚等反向用例。

6. `06_Solo_Review_Log_B2.md`
- 自审、阻断、准入结论。

## 详细执行步骤（编号化）
1. 盘点命令并分类映射。
- 操作：按合同分类 `query/operation/admin/governance`，标注最小角色阈值。
- 通过标准：所有命令均可映射且不存在未分类命令。
- 失败判据：出现“临时命令/内部命令”未纳入权限表。

2. 统一角色集合与比较语义。
- 操作：明确定义并固化 `user/admin/owner` 排序与比较规则。
- 通过标准：未知角色 100% 拒绝且落审计。
- 失败判据：任意入口对未知角色默认降级为 `user`。

3. 固化越权处理链路。
- 操作：拒绝码固定 `AUTH-403`，拒绝请求不得触发副作用。
- 通过标准：拒绝路径可在日志/审计中证明“未执行业务动作”。
- 失败判据：出现“先执行后拒绝”或拒绝码不一致。

4. 建立配置 schema 校验与热重载流程。
- 操作：定义键类型、默认值、范围、热重载标记、重载失败回滚。
- 通过标准：任一配置非法时整体回滚到上一个有效版本。
- 失败判据：部分键已生效、部分键未生效的半提交状态。

5. 对齐安全约束与审计字段。
- 操作：确认明文 secret 禁止、未知键 fail-closed、审计字段最小集完整。
- 通过标准：配置变更审计记录包含操作者、角色、前后版本、结果。
- 失败判据：变更有结果但缺审计字段，无法追溯责任。

6. 执行反向测试并形成缺口队列。
- 操作：至少覆盖 N01~N05 类反例（越权/离线/token 不一致/配置非法）。
- 通过标准：每个反例有“预期结果+实际结果+结论”。
- 失败判据：只记录通过用例，不记录失败用例行为。

7. 准入判定（进入 B3）。
- 操作：按权限、配置、审计三域门禁做 PASS/FAIL。
- 通过标准：无 High 阻断项，且回滚机制已可复验。
- 失败判据：存在“高危命令权限不清”或“非法配置无法回滚”。

## Prompt 模板（至少3个：执行、反证审查、准入判定）
### Prompt-A（执行）
```text
你是权限与配置契约落地助手。请完成 Step-04（B2）全部产物：
输入：
- COMMAND_AUTHORIZATION_CONTRACT.md
- CONFIG_SCHEMA_CONTRACT.md
- OBSERVABILITY_SLO_CONTRACT.md
- MODULE_BOUNDARY.md / SECURITY_BOUNDARY.md

要求：
1) 输出命令权限矩阵、角色迁移、配置校验画像、热重载回滚流程、反向用例、自审+自记录日志。
2) 每项必须给出通过标准与失败判据。
3) 严格单人维护语义：自审+自记录。
4) 所有结论附依据路径。
```

### Prompt-B（反证审查）
```text
请对 Step-04 产物执行反证审查：
1) 找出 10 个“最可能导致越权或错配生效”的脆弱点。
2) 必查反例：
   - 非三角色输入
   - owner 集合为空
   - 热重载中途失败
   - 未知配置键注入
3) 每条反例输出：触发方式、预期拒绝、实际偏差、修复动作。
4) 输出阻断清单（必须阻断进入 B3）。
```

### Prompt-C（准入判定）
```text
请对 Step-04 做准入判定：
检查对象：
- 01_Command_Authorization_Matrix.md
- 02_Role_Normalization_and_Migration.md
- 03_Config_Schema_Validation_Profile.md
- 04_HotReload_Rollback_Audit_Flow.md
- 05_B2_Negative_Test_Cases.md
- 06_Solo_Review_Log_B2.md

判定规则：
1) 是否仅存在 user/admin/owner 三角色。
2) 是否固定 AUTH-403 并阻断副作用。
3) 非法配置是否 100% 回滚。
4) 审计字段是否完整可检索。

输出：Verdict/Blocking Issues/Fix Actions。
```

## 建议命名与目录
```text
Project_Docs/
  Re_Step/
    RE_STEP_04_B2_权限与配置契约映射.md
    Artifacts/
      Step04/
        01_Command_Authorization_Matrix.md
        02_Role_Normalization_and_Migration.md
        03_Config_Schema_Validation_Profile.md
        04_HotReload_Rollback_Audit_Flow.md
        05_B2_Negative_Test_Cases.md
        06_Solo_Review_Log_B2.md
```

## 前置产物硬门禁
1. 本步骤执行前必须完成并固化上一步产物；任一缺失直接判定 Verdict=FAIL，禁止进入下一步。
2. 上一步产物清单（全部强制存在）：
- `Project_Docs/Re_Step/Artifacts/Step03/01_Message_Field_Mapping_Table.md`
- `Project_Docs/Re_Step/Artifacts/Step03/02_Message_Type_Execution_Matrix.md`
- `Project_Docs/Re_Step/Artifacts/Step03/03_Error_Code_DualStack_Mapping.md`
- `Project_Docs/Re_Step/Artifacts/Step03/04_FrameSize_Gate_and_Precheck.md`
- `Project_Docs/Re_Step/Artifacts/Step03/05_B1_Gap_Backlog.md`
- `Project_Docs/Re_Step/Artifacts/Step03/06_Solo_Review_Log_B1.md`
3. 机检命令（前置产物存在性）：
```bash
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step03/01_Message_Field_Mapping_Table.md \
  Project_Docs/Re_Step/Artifacts/Step03/02_Message_Type_Execution_Matrix.md \
  Project_Docs/Re_Step/Artifacts/Step03/03_Error_Code_DualStack_Mapping.md \
  Project_Docs/Re_Step/Artifacts/Step03/04_FrameSize_Gate_and_Precheck.md \
  Project_Docs/Re_Step/Artifacts/Step03/05_B1_Gap_Backlog.md \
  Project_Docs/Re_Step/Artifacts/Step03/06_Solo_Review_Log_B1.md >/dev/null
```
4. 门禁判定：
- 通过阈值：上一步产物存在率=100%，可读率=100%，命名与路径与上一步定义完全一致。
- 阻断动作：机检失败后立即 No-Go；仅允许补齐缺失产物并完成一次自审+自记录后重新判定。

## 投产门禁（Go/No-Go）
1. 机检命令（必须逐条执行并留证据）：
```bash
set -euo pipefail
DOC_PATH="/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_04_B2_权限与配置契约映射.md"
PREV_EXPECTED=6
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step03/01_Message_Field_Mapping_Table.md \
  Project_Docs/Re_Step/Artifacts/Step03/02_Message_Type_Execution_Matrix.md \
  Project_Docs/Re_Step/Artifacts/Step03/03_Error_Code_DualStack_Mapping.md \
  Project_Docs/Re_Step/Artifacts/Step03/04_FrameSize_Gate_and_Precheck.md \
  Project_Docs/Re_Step/Artifacts/Step03/05_B1_Gap_Backlog.md \
  Project_Docs/Re_Step/Artifacts/Step03/06_Solo_Review_Log_B1.md \
  >/dev/null
PREV_COUNT=$(ls -1 \
  Project_Docs/Re_Step/Artifacts/Step03/01_Message_Field_Mapping_Table.md \
  Project_Docs/Re_Step/Artifacts/Step03/02_Message_Type_Execution_Matrix.md \
  Project_Docs/Re_Step/Artifacts/Step03/03_Error_Code_DualStack_Mapping.md \
  Project_Docs/Re_Step/Artifacts/Step03/04_FrameSize_Gate_and_Precheck.md \
  Project_Docs/Re_Step/Artifacts/Step03/05_B1_Gap_Backlog.md \
  Project_Docs/Re_Step/Artifacts/Step03/06_Solo_Review_Log_B1.md \
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
1. 保存路径规范：`Project_Docs/Re_Step/Evidence/Step04/{RUN_ID}/`。
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
EVID_DIR="Project_Docs/Re_Step/Evidence/Step04/${RUN_ID}"
mkdir -p "$EVID_DIR"
{
  echo "step=Step04"
  echo "doc_path=/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_04_B2_权限与配置契约映射.md"
  echo "run_id=$RUN_ID"
  date -u
} | tee "$EVID_DIR/00_context.txt"
ls -1 \
  Project_Docs/Re_Step/Artifacts/Step03/01_Message_Field_Mapping_Table.md \
  Project_Docs/Re_Step/Artifacts/Step03/02_Message_Type_Execution_Matrix.md \
  Project_Docs/Re_Step/Artifacts/Step03/03_Error_Code_DualStack_Mapping.md \
  Project_Docs/Re_Step/Artifacts/Step03/04_FrameSize_Gate_and_Precheck.md \
  Project_Docs/Re_Step/Artifacts/Step03/05_B1_Gap_Backlog.md \
  Project_Docs/Re_Step/Artifacts/Step03/06_Solo_Review_Log_B1.md \
  | tee "$EVID_DIR/01_prev_gate.txt"
rg -n "^## (文档元数据|步骤目标|为什么此步骤在此顺序|输入材料（强制）|输出物定义（强制）|详细执行步骤（编号化）|Prompt 模板（至少3个：执行、反证审查、准入判定）|建议命名与目录|前置产物硬门禁|投产门禁（Go/No-Go）|残余风险与挂起条件|本步骤完成判据（最终）)$" "/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_04_B2_权限与配置契约映射.md" | tee "$EVID_DIR/02_section_gate.txt"
wc -l "/mnt/d/axm/mcs/MapBot2NeoForge/Project_Docs/Re_Step/RE_STEP_04_B2_权限与配置契约映射.md" | tee "$EVID_DIR/03_line_gate.txt"
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
1. 命令权限矩阵完整，未知角色拒绝策略生效。
2. 越权固定 `AUTH-403`，且无副作用执行证据。
3. 配置 schema 校验、热重载、失败回滚链路闭环。
4. 反向测试覆盖核心反例并有证据记录。
5. 自审完成并给出进入 B3 的明确结论。
