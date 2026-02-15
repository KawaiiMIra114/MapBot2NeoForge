# Step-02 A2 Baseline Environment Manifest

## 项目熟悉结论
1. 主链路为 `OneBot -> Mapbot-Alpha -> Bridge -> MapBot_Reforged`，命令入口在 `InboundHandler`，跨服与数据侧关键处理在 `BridgeProxy/BridgeHandlers/DataManager`。
2. `#bind #unbind #status #list #reload` 五条链路均已定位到“输入-处理-输出-日志”四段证据，可追溯到代码行与证据日志。
3. Step01 的冻结约束已继承：合同优先、失败可回滚、反向测试必选、单人自审与证据闭环。
4. 当前 A2 采用静态可复核采样口径，目标是建立“可比对基线”，不替代 B 阶段运行态压测与联调。

## ENV-001 操作系统与时间基线
- 采样口径：执行主机 OS 内核、发行版、采样时间戳单点快照。
- 采样命令/查询：
```bash
uname -a
cat /etc/os-release
date -Is
```
- 样本数：1
- 结果值（含分位数）：
  - `kernel=Linux 6.6.87.2-microsoft-standard-WSL2`
  - `distro=Ubuntu 24.04.3 LTS`
  - 分位数：`p50=p95=min=max=单样本值`
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_env_repo.log`
- 来源依据：`Project_Docs/Manuals/OPERATIONS_RUNBOOK.md`，`Project_Docs/Manuals/DEPLOYMENT_RUNBOOK.md`

## ENV-002 运行时工具链基线
- 采样口径：Java 与 Node 主版本静态快照。
- 采样命令/查询：
```bash
java -version
node -v
```
- 样本数：2（Java/Node 各 1）
- 结果值（含分位数）：
  - `java=21.0.10`
  - `node=v24.13.0`
  - 分位数：`p50=p95=min=max=单样本值（按每个工具单独统计）`
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_env_repo.log`
- 来源依据：`Project_Docs/Manuals/OPERATIONS_RUNBOOK.md`（F02），`Project_Docs/Manuals/DEPLOYMENT_RUNBOOK.md`（F02）

## ENV-003 依赖命令完整性基线（Runbook F01）
- 采样口径：Runbook 规定依赖命令存在性检查（`java tmux curl jq ss rg`）。
- 采样命令/查询：
```bash
for c in java tmux curl jq ss rg; do command -v "$c" >/dev/null || echo "MISS:$c"; done
```
- 样本数：6（命令项）
- 结果值（含分位数）：
  - `HAS: java, tmux, curl, jq, ss, rg`
  - 缺失计数分位：`count=6 p50=1 p95=1 min=1 max=1`（按“每项存在=1”编码）
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_runbook_f01_f02.log`
- 来源依据：`Project_Docs/Manuals/OPERATIONS_RUNBOOK.md`（F01），`Project_Docs/Manuals/DEPLOYMENT_RUNBOOK.md`（F01）

## ENV-004 仓库版本与工作树状态基线
- 采样口径：`git` 提交点、分支、工作树变更规模（modified/untracked/error）单次快照。
- 采样命令/查询：
```bash
git rev-parse --short=12 HEAD
git branch --show-current
git status --short
```
- 样本数：1
- 结果值（含分位数）：
  - `commit=31a50b5a2537`
  - `branch=main`
  - `dirty_summary: modified=17, untracked=27, index_error=1, total=45`
  - 分位数：`p50=p95=min=max=单样本值`
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_env_repo.log`
- 来源依据：`Project_Docs/Architecture/VERSIONING_AND_COMPATIBILITY.md`

## ENV-005 强制输入材料覆盖基线
- 采样口径：A2 强制输入文件清单完整性与行数统计。
- 采样命令/查询：
```bash
cat Project_Docs/Re_Step/Evidence/Step02/20260215_140711/input_manifest.txt
cat Project_Docs/Re_Step/Evidence/Step02/20260215_140711/input_line_counts.txt
sha256sum <强制输入集合>
```
- 样本数：37（文件项）
- 结果值（含分位数）：
  - `required_inputs=37/37`
  - `input_total_lines=4466`
  - 行数分位：见分组统计（Contracts=1690, Architecture=1171, Manuals=1021, Step01=316, Step01 Evidence=268）
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/input_manifest.txt`，`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/input_line_counts.txt`，`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_input_checksums.log`
- 来源依据：`Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`，`Project_Docs/Re_Step/Artifacts/Step01/03_Change_Gate_Rules.md`
