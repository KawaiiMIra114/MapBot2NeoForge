# Step-02 A2 Baseline Metrics Report

## MTR-001 BridgeHandlers 超时配置分位（秒）
- 采样口径：从 Reforged/Alpha 代码中提取 `BridgeHandlers` 相关超时常量样本，统计秒级分位。
- 采样命令/查询：
```bash
cat Project_Docs/Re_Step/Evidence/Step02/20260215_140711/metric_bridge_handler_timeout_samples_sec.txt
cat Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_metrics_quantiles.log
```
- 样本数：9
- 结果值（含分位数）：`count=9 p50=5 p90=8 p95=8 min=3 max=8`
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_metrics_quantiles.log`
- 来源依据：`Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`，`Project_Docs/Architecture/FAILURE_MODEL.md`

## MTR-002 BridgeProxy 同步请求超时分位（秒）
- 采样口径：提取 `BridgeProxy` 同步请求超时配置样本，统计秒级分位。
- 采样命令/查询：
```bash
cat Project_Docs/Re_Step/Evidence/Step02/20260215_140711/metric_bridge_proxy_timeout_samples_sec.txt
cat Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_metrics_quantiles.log
```
- 样本数：2
- 结果值（含分位数）：`count=2 p50=10 p90=10 p95=10 min=10 max=10`
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_metrics_quantiles.log`
- 来源依据：`Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`

## MTR-003 Runbook 阈值样本分位（秒）
- 采样口径：从双 Runbook 抽取时间阈值样本并统一为秒，统计分位。
- 采样命令/查询：
```bash
cat Project_Docs/Re_Step/Evidence/Step02/20260215_140711/metric_runbook_threshold_samples_sec.txt
cat Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_metrics_quantiles.log
```
- 样本数：7
- 结果值（含分位数）：`count=7 p50=180 p90=900 p95=900 min=10 max=900`
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_metrics_quantiles.log`
- 来源依据：`Project_Docs/Manuals/OPERATIONS_RUNBOOK.md`，`Project_Docs/Manuals/DEPLOYMENT_RUNBOOK.md`

## MTR-004 五条关键命令链路命中分位
- 采样口径：对 `#bind/#unbind/#status/#list/#reload` 统计输入/处理/输出/日志四段命中数分位。
- 采样命令/查询：
```bash
cat Project_Docs/Re_Step/Evidence/Step02/20260215_140711/metric_command_chain_counts.csv
cat Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_metrics_command_quantiles.log
```
- 样本数：5（命令）
- 结果值（含分位数）：
  - `input_hits: count=5 p50=4 p90=10 p95=10 min=2 max=10`
  - `process_hits: count=5 p50=5 p90=7 p95=7 min=4 max=7`
  - `output_hits: count=5 p50=10 p90=22 p95=22 min=2 max=22`
  - `log_hits: count=5 p50=41 p90=41 p95=41 min=41 max=41`
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/metric_command_chain_counts.csv`，`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_metrics_command_quantiles.log`
- 来源依据：`Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`，`Project_Docs/Re_Step/Artifacts/Step01/02_DoD_Checklist.md`

## MTR-005 模块 Java 文件规模分位
- 采样口径：统计 Alpha/Reforged 两个模块 Java 文件数量并计算分位。
- 采样命令/查询：
```bash
cat Project_Docs/Re_Step/Evidence/Step02/20260215_140711/module_java_file_counts.txt
cat Project_Docs/Re_Step/Evidence/Step02/20260215_140711/module_java_file_counts.quantiles
```
- 样本数：2（模块）
- 结果值（含分位数）：`count=2 p50=43 p95=52 min=43 max=52`
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/module_java_file_counts.quantiles`
- 来源依据：`Project_Docs/Architecture/MODULE_BOUNDARY.md`，`Project_Docs/Architecture/SYSTEM_CONTEXT.md`
