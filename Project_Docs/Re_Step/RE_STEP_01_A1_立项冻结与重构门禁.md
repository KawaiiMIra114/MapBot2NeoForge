# Re_Step-01：A1 立项冻结与重构门禁（单人维护版）

## 文档元数据
| 字段 | 值 |
| --- | --- |
| StepID | RE-STEP-01 |
| Version | 1.0.0 |
| Status | Ready |
| Owner | Solo Maintainer（你） |
| Last Updated | 2026-02-15 |
| 关联主计划 | `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`（阶段A/A1） |
| 证据来源 | `Project_Docs/SYSTEM_CRITIQUE_AND_REFACTOR_PAPER_V1.md` + `Project_Docs/Contracts/*` + `Project_Docs/Architecture/*` |

## 1. 步骤目标
本步骤只做一件事：在开始任何代码重构前，冻结“范围、非目标、完成定义、变更门禁”，确保后续实现不再语义漂移。

完成本步骤后，你将得到：
1. 一份可执行的重构 Charter（边界清晰，禁止项清晰）。
2. 一份量化 DoD（可判定“做完/没做完”）。
3. 一份“先改合同再改代码”的门禁规则。
4. 一份单人维护条件下的自审记录模板。

## 2. 为什么第一步必须做这个
依据研究报告，当前系统最大风险不是“缺功能”，而是“结构性漂移”（权限、协议、一致性、线程、安全口径不统一）。  
如果不先冻结规则，后续每一轮修复都可能互相覆盖，导致返工和新故障。

本步骤本质是先建立“约束系统”，再做“实现系统”。

## 3. 输入材料（强制）
执行前必须准备并读完：
1. `Project_Docs/SYSTEM_CRITIQUE_AND_REFACTOR_PAPER_V1.md`
2. `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`
3. `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`
4. `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`
5. `Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md`
6. `Project_Docs/Contracts/CONFIG_SCHEMA_CONTRACT.md`
7. `Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`
8. `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
9. `Project_Docs/Architecture/SYSTEM_CONTEXT.md`
10. `Project_Docs/Architecture/THREADING_MODEL.md`
11. `Project_Docs/Architecture/FAILURE_MODEL.md`
12. `Project_Docs/Architecture/SECURITY_BOUNDARY.md`
13. `Project_Docs/Architecture/VERSIONING_AND_COMPATIBILITY.md`

## 4. 输出物定义（本步骤必须产出）
建议统一放到：`Project_Docs/Re_Step/Artifacts/Step01/`

1. `01_Refactor_Charter.md`
- 必含：目标、范围、非目标、假设、约束、退出条件。

2. `02_DoD_Checklist.md`
- 必含：协议、权限、线程、安全、一致性、可观测、运维、回滚、测试等量化条目。

3. `03_Change_Gate_Rules.md`
- 必含：合同优先原则、禁止直改高风险逻辑、提交前验证项。

4. `04_Solo_Review_Log.md`
- 必含：日期、变更原因、证据路径、自审结论、待决问题。

## 5. 详细执行步骤（严谨版）

### Step 5.1 建立“范围冻结矩阵”
1. 从主计划中抽取阶段A~J。
2. 逐项标记“本轮重构纳入/不纳入/延后”。
3. 对每个“不纳入”给出理由（资源不足、依赖未就绪、风险过高等）。

通过标准：
1. 每个模块都有归类，不存在“待定空白项”。
2. 每个延后项都写了触发条件（何时重新纳入）。

### Step 5.2 建立“非目标清单”
1. 明确本轮不做的事情（例如 UI 美化、非关键功能扩展、跨平台额外适配）。
2. 明确禁止的危险行为（例如未改合同先改协议语义）。

通过标准：
1. 非目标至少 5 条且可验证。
2. 禁止行为至少 5 条且可审计。

### Step 5.3 建立“DoD 量化清单”
DoD 必须是量化条件，禁止模糊描述。

示例（可直接采用）：
1. 协议：变更型请求 100% 具备幂等语义。
2. 权限：只允许 `user/admin/owner`，未知角色 100% 拒绝。
3. 一致性：冲突写入 100% 返回 `CONSISTENCY-409`，无静默覆盖。
4. 线程：主线程路径 0 个阻塞调用（`join/get/sleep`）。
5. 安全：敏感 token 0 明文提交。
6. 可观测：关键链路都能产出 request 级追踪证据。

通过标准：
1. 每条 DoD 有“验证方法”。
2. 每条 DoD 有“失败判据”。

### Step 5.4 建立“变更门禁（Gate）”
门禁规则建议：
1. Gate-1：先改合同，后改代码。
2. Gate-2：高风险改动必须同时提交回滚路径。
3. Gate-3：涉及协议/权限/安全，必须追加反向测试。
4. Gate-4：未更新手册与索引，不允许进入下一阶段。

通过标准：
1. 每条 Gate 对应到具体检查命令或清单项。
2. Gate 失败时有统一处理动作（阻断/回滚/补文档）。

### Step 5.5 建立“单人自审机制”
1. 固化自审模板（变更前、变更后、风险、证据）。
2. 固化触发条件：
- 重构执行中：每个子阶段结束必须自审。
- 功能制作中：每次跨模块改动必须自审。
- 重大安全事件后：24 小时内必须自审并补文档。

通过标准：
1. 有模板可直接填写。
2. 有触发规则且不依赖固定周期。

### Step 5.6 完成“阶段准入判定”
只有以下全部通过，才允许进入下一步（A2 基线采样）：
1. Charter 完成。
2. DoD 完成。
3. Gate 完成。
4. 自审模板完成。
5. 关键冲突条目（协议/权限/安全）已明确优先级。

## 6. Prompt 模板（可直接复制）

### Prompt-A：执行本步骤（产出草案）
```text
你是该项目的重构执行助手。请严格依据以下文档，完成“立项冻结与重构门禁”产物：
1) Project_Docs/SYSTEM_CRITIQUE_AND_REFACTOR_PAPER_V1.md
2) Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md
3) Project_Docs/Contracts/*.md
4) Project_Docs/Architecture/*.md

任务要求：
- 输出 4 份文档：
  - 01_Refactor_Charter.md
  - 02_DoD_Checklist.md
  - 03_Change_Gate_Rules.md
  - 04_Solo_Review_Log.md
- 所有产出必须落盘到 `D:\axm\mcs\MapBot2NeoForge\Project_Docs\Re_Step\Artifacts\Step01\`，并创建/覆盖上述 4 个文件。
- 允许创建多个子代理协作完成任务；主代理必须负责最终汇总、交叉审查与一致性裁决。
- 在开始执行前，必须先完整阅读全部输入材料，再阅读相关代码并形成项目熟悉结论；未完成该前置动作不得进入产出阶段。
- 必须量化、可验证、可审计，禁止空话。
- 每个结论必须附“依据路径”。
- 单人维护模式：不使用多人审批流程，统一为自审与记录。
- 输出时标注每份文档的“通过条件”和“失败判据”。
```

### Prompt-B：反证审查（查缺补漏）
```text
请对当前 Step-01 产物做反证审查：
1) 找出 10 个最脆弱假设（逐条写失效条件、后果、缓释策略）。
2) 找出 5 个可能的互相冲突点（DoD vs Gate、合同 vs 实现）。
3) 逐条给出修正建议，并标注应修改的文件与段落。
4) 输出“若今天进入下一阶段，最可能失败的3个原因”。
```

### Prompt-C：准入判定（是否进入A2）
```text
请作为质量门禁检查器，仅做“通过/不通过”判定：
检查对象：
- 01_Refactor_Charter.md
- 02_DoD_Checklist.md
- 03_Change_Gate_Rules.md
- 04_Solo_Review_Log.md

判定规则：
1) 是否存在未量化条目。
2) 是否存在无证据路径条目。
3) 是否存在与合同冲突条目。
4) 是否存在无法执行的门禁条目。

输出格式：
- Verdict: PASS / FAIL
- Blocking Issues: 列出阻断项（path + 行号 + 原因）
- Fix Plan: 每个阻断项的修复动作
```

## 7. 建议命名与目录
```text
Project_Docs/
  Re_Step/
    RE_STEP_01_A1_立项冻结与重构门禁.md
    Artifacts/
      Step01/
        01_Refactor_Charter.md
        02_DoD_Checklist.md
        03_Change_Gate_Rules.md
        04_Solo_Review_Log.md
```

## 前置产物硬门禁
Step-01 为起始步骤，无上一步产物；但输入材料是强制门禁，缺任一输入即判定 `Verdict=FAIL`，禁止进入 A2。

强制输入机检命令：
```bash
set -euo pipefail
ls -1 \
  Project_Docs/SYSTEM_CRITIQUE_AND_REFACTOR_PAPER_V1.md \
  Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md \
  Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md \
  Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md \
  Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md \
  Project_Docs/Contracts/CONFIG_SCHEMA_CONTRACT.md \
  Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md \
  Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md \
  Project_Docs/Architecture/SYSTEM_CONTEXT.md \
  Project_Docs/Architecture/THREADING_MODEL.md \
  Project_Docs/Architecture/FAILURE_MODEL.md \
  Project_Docs/Architecture/SECURITY_BOUNDARY.md \
  Project_Docs/Architecture/VERSIONING_AND_COMPATIBILITY.md >/dev/null
```

通过阈值：
1. 强制输入存在率 = 100%。
2. 强制输入可读率 = 100%。

阻断动作：
1. 任何缺失直接 No-Go。
2. 仅允许补齐缺失并完成一次自审+自记录后重判。

## 投产门禁（Go/No-Go）
机检命令：
```bash
set -euo pipefail
DOC_PATH="Project_Docs/Re_Step/RE_STEP_01_A1_立项冻结与重构门禁.md"
rg -n "^## (文档元数据|1\\. 步骤目标|2\\. 为什么此步骤在此顺序|3\\. 输入材料（强制）|4\\. 输出物定义（强制）|5\\. 详细执行步骤（严谨版）|6\\. Prompt 模板（可直接复制）|7\\. 建议命名与目录|前置产物硬门禁|投产门禁（Go/No-Go）|残余风险与挂起条件|本步骤完成判据（最终）)$" "$DOC_PATH" >/dev/null
test "$(rg -n "### Prompt-" "$DOC_PATH" | wc -l)" -ge 3
test "$(wc -l < "$DOC_PATH")" -ge 220
! rg -n "若已存在|可选、建议但不影响准入" "$DOC_PATH" >/dev/null
```

通过阈值：
1. 强制章节命中完整。
2. Prompt 数 >= 3。
3. 文档行数 >= 220。
4. 不包含弱化准入语句。

阻断动作：
1. 任一门禁失败立即 No-Go。
2. 修复后必须全量重跑门禁，不允许人工豁免。

### 误判防护
| 类型 | 场景 | 检测补偿动作 |
| --- | --- | --- |
| 假阳性 | 章节标题被误命中（注释/代码块） | 用 `^## ` 锚定 + 人工抽样 5 行上下文复核 |
| 假阳性 | 行数满足但关键内容缺失 | 增加 `rg -n "通过阈值|失败判据|阻断动作"` 三联命中校验 |
| 假阴性 | shell 环境差异导致 `! rg` 判定异常 | 补跑等价命令：`test "$(rg -n '若已存在|可选、建议但不影响准入' "$DOC_PATH" | wc -l)" -eq 0` |
| 假阴性 | 文档路径含空格导致命令失败 | 强制路径变量加双引号并重跑全量机检 |

### 门禁证据留存格式
证据目录建议：`Project_Docs/Re_Step/Artifacts/Step01/Gate_Evidence/`

命名规范：
1. `gate1_inputs.log` / `gate1_inputs.exit`
2. `gate2_sections.log` / `gate2_sections.exit`
3. `gate3_prompts.log` / `gate3_prompts.exit`
4. `gate4_weakphrase.log` / `gate4_weakphrase.exit`

示例：
```bash
set -euo pipefail
DIR="Project_Docs/Re_Step/Artifacts/Step01/Gate_Evidence/$(date +%Y%m%d_%H%M%S)"
mkdir -p "$DIR"
( ls -1 Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md >/dev/null ) >"$DIR/gate1_inputs.log" 2>&1; echo $? >"$DIR/gate1_inputs.exit"
( rg -n "^## " "Project_Docs/Re_Step/RE_STEP_01_A1_立项冻结与重构门禁.md" ) >"$DIR/gate2_sections.log" 2>&1; echo $? >"$DIR/gate2_sections.exit"
```

## 残余风险与挂起条件
| 风险ID | 风险描述 | 触发条件 | 挂起条件 | 解除条件 |
| --- | --- | --- | --- | --- |
| R1 | 输入文档版本漂移 | 合同/架构更新但 Step01 未同步 | 出现术语冲突且无法当日修复 | 完成同步修订并重跑门禁 |
| R2 | DoD 量化不足 | 产物仍有“模糊条目” | 任一条目不可机检 | 补齐量化阈值与验证命令 |
| R3 | 门禁脚本误报/漏报 | 自动判定与人工结论冲突 | 无法稳定复现门禁结果 | 修正脚本并连续两次一致通过 |
| R4 | 自审记录不完整 | 缺日期/证据/结论 | 自审字段缺失任一关键项 | 补齐字段并附证据路径 |
| R5 | 进入A2前阻断未清零 | 仍有高风险挂起项 | 未给挂起理由与截止时间 | 阻断清零或形成可审计挂起单 |

## 本步骤完成判据（最终）
全部满足才算完成：
1. 四份产物文档齐全。
2. 每条关键规则可追溯到至少一份合同或架构文档。
3. 所有条目都有可执行验证方式。
4. 反证审查已执行，阻断项已清零或明确挂起理由。
5. 形成一次自审记录。
