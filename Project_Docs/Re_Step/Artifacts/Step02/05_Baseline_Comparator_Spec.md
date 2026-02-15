# Step-02 A2 Baseline Comparator Spec

## 判定总则
- 总判定：`PASS` 当且仅当所有条目状态均不为 `FAIL`。
- 预警策略：存在 `WARN` 时允许进入 B1，但必须在 `06_Solo_Review_Log_A2.md` 记录风险闭环与补录动作。
- 阻断策略：任一硬阻断项 `FAIL`（如关键链路证据缺失）则 `NO-GO B1`，修复后重跑 A2 门禁。

## CMP-001 Java 主版本一致性
- 采样口径：Runbook F02 主版本检查。
- 采样命令/查询：`java -version`
- 样本数：1
- 结果值（含分位数）：`java_major=21`，`p50=p95=min=max=21`
- PASS/WARN/FAIL 阈值：
  - PASS：`java_major=21`
  - WARN：无
  - FAIL：`java_major!=21`
- 当前判定：PASS
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_env_repo.log`，`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_runbook_f01_f02.log`
- 来源依据：`Project_Docs/Manuals/OPERATIONS_RUNBOOK.md`（F02），`Project_Docs/Manuals/DEPLOYMENT_RUNBOOK.md`（F02）

## CMP-002 依赖命令完整性
- 采样口径：Runbook F01 依赖命令存在性检查。
- 采样命令/查询：`for c in java tmux curl jq ss rg; do command -v "$c" >/dev/null || echo MISS:$c; done`
- 样本数：6
- 结果值（含分位数）：`present=6/6`，`missing=0`，`p50=p95=min=max=1（按每项存在编码）`
- PASS/WARN/FAIL 阈值：
  - PASS：`missing=0`
  - WARN：无
  - FAIL：`missing>0`
- 当前判定：PASS
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_runbook_f01_f02.log`
- 来源依据：`Project_Docs/Manuals/OPERATIONS_RUNBOOK.md`（F01），`Project_Docs/Manuals/DEPLOYMENT_RUNBOOK.md`（F01）

## CMP-003 BridgeProxy 超时阈值
- 采样口径：`BridgeProxy` 同步请求超时分位与合同 10 秒阈值比较。
- 采样命令/查询：`cat metric_bridge_proxy_timeout_samples_sec.txt && cat sample_metrics_quantiles.log`
- 样本数：2
- 结果值（含分位数）：`p95=10s, p50=10s, min=10s, max=10s`
- PASS/WARN/FAIL 阈值：
  - PASS：`p95 <= 10s`
  - WARN：`10s < p95 <= 12s`
  - FAIL：`p95 > 12s`
- 当前判定：PASS
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_metrics_quantiles.log`
- 来源依据：`Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`（Alpha 同步请求 10 秒）

## CMP-004 BridgeHandlers 超时安全窗
- 采样口径：`BridgeHandlers` 超时分位与故障模型文件类超时上限比较。
- 采样命令/查询：`cat metric_bridge_handler_timeout_samples_sec.txt && cat sample_metrics_quantiles.log`
- 样本数：9
- 结果值（含分位数）：`p95=8s, p50=5s, min=3s, max=8s`
- PASS/WARN/FAIL 阈值：
  - PASS：`p95 <= 30s`
  - WARN：`30s < p95 <= 120s`
  - FAIL：`p95 > 120s`
- 当前判定：PASS
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_metrics_quantiles.log`
- 来源依据：`Project_Docs/Architecture/FAILURE_MODEL.md`（文件类 30 秒、变更类 120 秒）

## CMP-005 Runbook 全局时间阈值包络
- 采样口径：Runbook 时间阈值样本分位与最大回滚演练窗口比较。
- 采样命令/查询：`cat metric_runbook_threshold_samples_sec.txt && cat sample_metrics_quantiles.log`
- 样本数：7
- 结果值（含分位数）：`p95=900s, p50=180s, min=10s, max=900s`
- PASS/WARN/FAIL 阈值：
  - PASS：`p95 <= 900s`
  - WARN：`900s < p95 <= 1080s`
  - FAIL：`p95 > 1080s`
- 当前判定：PASS
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_metrics_quantiles.log`
- 来源依据：`Project_Docs/Manuals/OPERATIONS_RUNBOOK.md`（F18=15 分钟），`Project_Docs/Manuals/DEPLOYMENT_RUNBOOK.md`（F18=15 分钟）

## CMP-006 五条命令四联证据完整性
- 采样口径：`#bind/#unbind/#status/#list/#reload` 每条命令必须同时具备 Input/Processing/Output/Log 段。
- 采样命令/查询：
```bash
cat Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_behavior_chain_coverage.log
cat Project_Docs/Re_Step/Evidence/Step02/20260215_140711/behavior_chain_section_quantiles.log
```
- 样本数：20（5 命令 x 4 段）
- 结果值（含分位数）：`count=20 p50=1 p95=1 min=1 max=1`
- PASS/WARN/FAIL 阈值：
  - PASS：每命令四段计数均 `>=1`
  - WARN：无
  - FAIL：任一命令任一段计数 `=0`
- 当前判定：PASS
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_behavior_chain_coverage.log`，`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_bind.log`，`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_unbind.log`，`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_status.log`，`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_list.log`，`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_reload.log`
- 来源依据：`Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`，`Project_Docs/Re_Step/Artifacts/Step01/02_DoD_Checklist.md`

## CMP-007 指标样本充足性（反证审查闭环条目）
- 采样口径：比较器输入分位日志中的最小样本数，用于判定基线稳定度。
- 采样命令/查询：
```bash
rg -n 'count=' Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_metrics_quantiles.log
rg -n 'count=' Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_metrics_command_quantiles.log
```
- 样本数：7（3 项时延分位 + 4 项命中分位）
- 结果值（含分位数）：`min_count=2, p50_count=5, p95_count=9`
- PASS/WARN/FAIL 阈值：
  - PASS：`min_count >= 2`
  - WARN：`min_count = 1`
  - FAIL：`min_count = 0`
- 当前判定：PASS
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_metrics_quantiles.log`，`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_metrics_command_quantiles.log`，`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/subagent_rounds.md`
- 来源依据：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/subagent_rounds.md`（第2/3轮）

## CMP-008 工作树洁净度（证据重放风险条目）
- 采样口径：采样时仓库脏状态规模，用于评估“基线重放一致性”风险。
- 采样命令/查询：`rg -n '^( M |\?\? |error:)' sample_env_repo.log`
- 样本数：1
- 结果值（含分位数）：`dirty_total=45 (modified=17, untracked=27, errors=1)`，`p50=p95=min=max=45`
- PASS/WARN/FAIL 阈值：
  - PASS：`dirty_total <= 20`
  - WARN：`20 < dirty_total <= 60`
  - FAIL：`dirty_total > 60`
- 当前判定：WARN
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_env_repo.log`，`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/subagent_rounds.md`
- 来源依据：`Project_Docs/Architecture/VERSIONING_AND_COMPATIBILITY.md`，`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/subagent_rounds.md`（第2/3轮）
