# Re_Step-21：I2 开源治理落地（单人维护版）

## 文档元数据
| 字段 | 值 |
| --- | --- |
| StepID | RE-STEP-21 |
| Version | 1.0.0 |
| Status | Ready |
| Owner | Solo Maintainer（你） |
| Last Updated | 2026-02-15 |
| 关联主计划 | `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`（阶段I/I2） |
| 证据来源 | `Project_Docs/SYSTEM_CRITIQUE_AND_REFACTOR_PAPER_V1.md`、`Project_Docs/Manuals/RELEASE_CHECKLIST.md`、`Project_Docs/Architecture/VERSIONING_AND_COMPATIBILITY.md` |

## 步骤目标
将项目从“可运行”提升到“可协作、可复用、可公开发布”：完成许可证与社区治理基线、去敏示例、对外构建运行指引，并验证外部贡献者可独立完成构建测试运行。

## 为什么此步骤在此顺序
I1 稳定化后才能对外暴露；若在系统不稳时推进开源治理，会把内部不一致放大为外部协作成本与信誉风险。

## 输入材料（强制）
1. `Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`
2. `Project_Docs/SYSTEM_CRITIQUE_AND_REFACTOR_PAPER_V1.md`
3. `Project_Docs/Manuals/RELEASE_CHECKLIST.md`
4. `Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md`
5. `Project_Docs/Architecture/VERSIONING_AND_COMPATIBILITY.md`
6. `Project_Docs/Contracts/PRIVACY_POLICY.md`
7. 上一步产物（强制存在，缺失则 Verdict=FAIL 并禁止进入下一步）：`Project_Docs/Re_Step/Artifacts/Step20/*`

## 输出物定义（强制）
目录：`Project_Docs/Re_Step/Artifacts/Step21/`

1. `01_OpenSource_Governance_Checklist.md`
- 覆盖 LICENSE/CONTRIBUTING/SECURITY/CODE_OF_CONDUCT 等治理文件门槛。

2. `02_Sanitized_Example_Config_Spec.md`
- 定义对外示例配置去敏规则与验证方法。

3. `03_External_Contributor_Onboarding_Test.md`
- 记录外部贡献者视角的构建-测试-运行路径验证。

4. `04_Release_Artifact_Layout_Spec.md`
- 定义对外发布包目录、校验和、文档入口。

5. `05_Solo_Review_Log.md`
- 自审结论与准入判定。

## 详细执行步骤（编号化）
### 1. 建立治理文件基线门槛
1. 固化治理文件最低集合与必填章节。
2. 定义每个治理文件的版本与更新责任。
3. 定义缺失时的发布阻断规则。

通过标准：
1. 治理文件集合完整。
2. 每份文档可被索引与访问。

失败判据：
1. 缺关键治理文件仍允许发布。
2. 治理文件无版本与更新记录。

### 2. 固化去敏配置规则
1. 定义 token、密钥、账号、路径等敏感项去敏策略。
2. 建立自动扫描规则防止明文泄露。
3. 提供可运行的最小示例配置。

通过标准：
1. 示例配置可运行且无敏感泄露。
2. 扫描规则可阻断明文提交。

失败判据：
1. 示例配置不可运行。
2. 发现明文 token/密钥未阻断。

### 3. 构建外部贡献者路径验证
1. 用“干净环境”验证 clone -> build -> test -> run。
2. 记录遇到的阻塞点与修复。
3. 输出最小上手时间与必要前置条件。

通过标准：
1. 外部路径一次可走通。
2. 文档可指导非项目成员独立完成流程。

失败判据：
1. 依赖隐式知识才能完成。
2. 关键步骤缺失或顺序错误。

### 4. 固化发布产物结构与入口
1. 定义发布包内容（二进制、校验、文档链接、变更摘要）。
2. 固化版本命名与兼容声明。
3. 校验发布入口可达与可检索。

通过标准：
1. 发布产物结构固定且可机检。
2. 文档入口完整无断链。

失败判据：
1. 发布包缺核心工件。
2. 版本与兼容声明缺失。

### 5. 执行安全与合规检查
1. 执行敏感信息扫描、依赖许可扫描、文档合规核验。
2. 对风险项输出修复计划与上线门禁。
3. 形成可审计检查报告。

通过标准：
1. 高风险泄露项为 0。
2. 合规风险均有处置路径。

失败判据：
1. 存在高风险泄露仍放行。
2. 合规风险无负责人和截止日期。

### 6. 执行自审与准入判定
1. 汇总治理、示例、上手验证、发布结构结果。
2. 判定是否准入 J1。
3. 输出 PASS/FAIL。

通过标准：
1. 外部协作闭环可执行。
2. 阻断项清零或有挂起依据。

失败判据：
1. 关键治理项缺失。
2. 自审记录不完整。

## Prompt 模板（至少3个：执行、反证审查、准入判定）
### Prompt-A（执行）
```text
请执行 I2“开源治理落地”，输入：
- Project_Docs/SYSTEM_CRITIQUE_AND_REFACTOR_PAPER_V1.md
- Project_Docs/Manuals/RELEASE_CHECKLIST.md
- Project_Docs/Architecture/VERSIONING_AND_COMPATIBILITY.md

要求：
1) 输出治理清单、去敏示例规范、外部上手验证、发布产物规范、自审日志。
2) 每项都包含通过标准、失败判据、机检方式。
3) 单人维护模式，仅自审+自记录。
```

### Prompt-B（反证审查）
```text
请反证审查 I2 产物：
1) 找出 8 个“可开源但不可协作”的隐患。
2) 找出 5 个“示例可跑但存在泄露风险”的场景。
3) 给出每个场景的修复动作与阻断规则。
4) 输出 P0 风险列表。
```

### Prompt-C（准入判定）
```text
请对 I2 进行准入判定（PASS/FAIL）：
检查对象：
- 01_OpenSource_Governance_Checklist.md
- 02_Sanitized_Example_Config_Spec.md
- 03_External_Contributor_Onboarding_Test.md
- 04_Release_Artifact_Layout_Spec.md
- 05_Solo_Review_Log.md

判定规则：
1) 治理文件是否完整可达。
2) 示例配置是否去敏且可运行。
3) 外部贡献者路径是否可复现。
4) 发布产物结构是否固定并可机检。

输出：Verdict / Blocking Issues / Fix Plan。
```

## 建议命名与目录
```text
Project_Docs/
  Re_Step/
    RE_STEP_21_I2_开源治理落地.md
    Artifacts/
      Step21/
        01_OpenSource_Governance_Checklist.md
        02_Sanitized_Example_Config_Spec.md
        03_External_Contributor_Onboarding_Test.md
        04_Release_Artifact_Layout_Spec.md
        05_Solo_Review_Log.md
```

## 前置产物硬门禁
硬规则：前置产物缺失 => Verdict=FAIL => 禁止进入下一步。

前置产物文件清单（Step20，强制全部存在且非空）：
1. `Project_Docs/Re_Step/Artifacts/Step20/01_Stabilization_Backlog.md`
2. `Project_Docs/Re_Step/Artifacts/Step20/02_Contract_Impl_Manual_Consistency_Report.md`
3. `Project_Docs/Re_Step/Artifacts/Step20/03_RC_Readiness_Checklist.md`
4. `Project_Docs/Re_Step/Artifacts/Step20/04_Updated_Baseline_And_Thresholds.md`
5. `Project_Docs/Re_Step/Artifacts/Step20/05_Solo_Review_Log.md`

机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
STEP_DIR="Project_Docs/Re_Step/Artifacts/Step20"
test -d "$STEP_DIR"
test -s "$STEP_DIR/01_Stabilization_Backlog.md"
test -s "$STEP_DIR/02_Contract_Impl_Manual_Consistency_Report.md"
test -s "$STEP_DIR/03_RC_Readiness_Checklist.md"
test -s "$STEP_DIR/04_Updated_Baseline_And_Thresholds.md"
test -s "$STEP_DIR/05_Solo_Review_Log.md"
ls -1 "$STEP_DIR"/*.md >/dev/null
```

通过阈值：
1. Step20 前置目录存在。
2. 指定 5 个前置产物文件全部存在且非空。
3. 目录下可枚举到 `*.md` 产物文件。

阻断动作：
1. 任一检查失败立即 Verdict=FAIL。
2. 立即禁止进入下一步与禁止投产。
3. 在本步骤自审+自记录中登记阻断项、修复动作与复检结果。

## 投产门禁（Go/No-Go）
### Gate-1 前置产物存在性
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
count=$(find "Project_Docs/Re_Step/Artifacts/Step20" -maxdepth 1 -type f -name '*.md' | wc -l)
test "$count" -ge 1
```
- 通过阈值：前置步骤产物文件数 >= 1。
- 阻断动作：命令失败立即 Verdict=FAIL，禁止进入下一步，并在本步骤自审+自记录中登记阻断项。

### Gate-2 固定章节完整性
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
file="Project_Docs/Re_Step/RE_STEP_21_I2_开源治理落地.md"
for h in   "文档元数据"   "步骤目标"   "为什么此步骤在此顺序"   "输入材料（强制）"   "输出物定义（强制）"   "详细执行步骤（编号化）"   "Prompt 模板（至少3个：执行、反证审查、准入判定）"   "建议命名与目录"   "投产门禁（Go/No-Go）"   "残余风险与挂起条件"   "本步骤完成判据（最终）"; do
  test "$(rg -c "^## $h$" "$file")" -eq 1
done
```
- 通过阈值：11 个固定章节均存在且仅出现 1 次。
- 阻断动作：命令失败立即 Verdict=FAIL，禁止投产。

### Gate-3 术语一致性检查（自审+自记录）
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_21_I2_开源治理落地.md"
```
- 通过阈值：命中文本“自审+自记录” >= 1。
- 阻断动作：命令失败立即 Verdict=FAIL，退回文档修订。

### Gate-4 弱化语义清零
- 机检命令（非交互、可重复、失败返回非0）：
```bash
set -euo pipefail
! rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_21_I2_开源治理落地.md" >/dev/null
```
- 通过阈值：弱化语义命中数 = 0。
- 阻断动作：命令失败立即 Verdict=FAIL，禁止进入下一步。

### 误判防护
| 场景ID | 类型 | 场景说明 | 检测补偿动作 |
| --- | --- | --- | --- |
| FP-1 | 假阳性 | Gate-1 因旧文件残留通过，但前置产物与当前版本不匹配 | 补跑版本一致性校验：`rg -q "Last Updated|Version" Project_Docs/Re_Step/Artifacts/Step20/*.md`，不匹配即 FAIL |
| FP-2 | 假阳性 | Gate-2 章节标题存在但正文为空，导致形式通过 | 追加正文非空校验：`test "$(wc -l < "Project_Docs/Re_Step/RE_STEP_21_I2_开源治理落地.md")" -ge 120`，不足阈值即 FAIL |
| FN-1 | 假阴性 | Gate-3 因字符编码或转义差异误判术语缺失 | 补跑宽松匹配：`rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_21_I2_开源治理落地.md"`，命中后判为通过并记录修复动作 |
| FN-2 | 假阴性 | Gate-4 在极端 shell 环境下因 `!` 语法差异误判失败 | 补跑等价判定：`test "$(rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_21_I2_开源治理落地.md" | wc -l)" -eq 0` |

### 门禁证据留存格式
- 证据目录规范：`Project_Docs/Re_Step/Artifacts/Step21/GateEvidence/<UTC时间戳>/`。
- 文件命名规范：`gate1.log`、`gate1.exit`、`gate2.log`、`gate2.exit`、`gate3.log`、`gate3.exit`、`gate4.log`、`gate4.exit`、`summary.json`。
- 留存命令模板（非交互、可重复）：
```bash
set -euo pipefail
STAMP=$(date -u +%Y%m%dT%H%M%SZ)
DIR="Project_Docs/Re_Step/Artifacts/Step21/GateEvidence/${STAMP}"
mkdir -p "$DIR"
( set -euo pipefail; count=$(find "Project_Docs/Re_Step/Artifacts/Step20" -maxdepth 1 -type f -name '*.md' | wc -l); test "$count" -ge 1 ) >"$DIR/gate1.log" 2>&1; echo $? >"$DIR/gate1.exit"
( set -euo pipefail; file="Project_Docs/Re_Step/RE_STEP_21_I2_开源治理落地.md"; for h in "文档元数据" "步骤目标" "为什么此步骤在此顺序" "输入材料（强制）" "输出物定义（强制）" "详细执行步骤（编号化）" "Prompt 模板（至少3个：执行、反证审查、准入判定）" "建议命名与目录" "投产门禁（Go/No-Go）" "残余风险与挂起条件" "本步骤完成判据（最终）"; do test "$(rg -c "^## $h$" "$file")" -eq 1; done ) >"$DIR/gate2.log" 2>&1; echo $? >"$DIR/gate2.exit"
( set -euo pipefail; rg -Fq "自审+自记录" "Project_Docs/Re_Step/RE_STEP_21_I2_开源治理落地.md" ) >"$DIR/gate3.log" 2>&1; echo $? >"$DIR/gate3.exit"
( set -euo pipefail; ! rg -n "若已存在|可选、建议但不影响准入" "Project_Docs/Re_Step/RE_STEP_21_I2_开源治理落地.md" >/dev/null ) >"$DIR/gate4.log" 2>&1; echo $? >"$DIR/gate4.exit"
jq -n --arg step "21" --arg file "Project_Docs/Re_Step/RE_STEP_21_I2_开源治理落地.md" --arg dir "$DIR" '{step:$step,file:$file,evidence_dir:$dir}' >"$DIR/summary.json"
```
- 判定方式：`gate*.exit` 全为 `0` 才可 Go；任一非 `0` 必须 No-Go。
## 残余风险与挂起条件
| 风险ID | 风险描述 | 触发条件 | 挂起条件 | 解除条件 |
| --- | --- | --- | --- | --- |
| R1 | 前置产物缺失导致链路断裂 | Gate-1 输出值 < 1 | 本步骤产出全部标记挂起，Verdict=FAIL | 补齐前置产物并复跑 Gate-1 达标 |
| R2 | 固定章节缺失导致审计不可复验 | Gate-2 输出值 != 11 | 禁止进入下一步与投产评审 | 补齐章节并复跑 Gate-2 达标 |
| R3 | 术语不一致导致执行口径漂移 | Gate-3 输出值 < 1 | 挂起准入判定，不允许 PASS | 全文统一为“自审+自记录”并复检通过 |
| R4 | 弱化语义残留导致门禁失效 | Gate-4 输出值 > 0 | 挂起投产决策并强制返工 | 清零弱化语义并复跑 Gate-4 为 0 |
| R5 | 证据链缺失导致故障不可追踪 | 自审记录中缺少证据路径或命令结果 | 挂起发布，按阻断项处理 | 补齐证据路径并完成二次自审+自记录 |

## 本步骤完成判据（最终）
1. 开源治理基础文档与流程完整。
2. 示例配置去敏且可运行。
3. 外部贡献者可按文档完成构建测试运行。
4. 发布产物结构与入口固定可检。
5. 自审通过并准入 J1。
