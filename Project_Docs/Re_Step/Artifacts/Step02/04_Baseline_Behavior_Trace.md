# Step-02 A2 Baseline Behavior Trace

## 行为链路覆盖结论
- 已覆盖：`#bind #unbind #status #list #reload`
- 每条命令均具备“输入(Input)-处理(Processing)-输出(Output)-日志(Log)”四联证据。
- 四联覆盖抽样统计：`count=20 p50=1 p95=1 min=1 max=1`（20=5 命令 x 4 段）。
- 覆盖证据：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/sample_behavior_chain_coverage.log`，`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/behavior_chain_section_quantiles.log`

## BCH-001 #bind
- 采样口径：抽取 `#bind/#id` 入口注册、绑定处理流程、成功/失败返回、日志行为。
- 采样命令/查询：
```bash
sed -n '/## 输入/,/## 处理/p' Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_bind.log
sed -n '/## 处理/,/## 输出/p' Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_bind.log
sed -n '/## 输出/,/## 日志/p' Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_bind.log
```
- 样本数：4（输入/处理/输出/日志）
- 结果值（含分位数）：`bind,count=4 p50=10 p95=41 min=4 max=41`
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_bind.log`，`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/metric_behavior_link_quantiles.log`
- 来源依据：`Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`，`Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`

## BCH-002 #unbind
- 采样口径：抽取 `#unbind` 入口、解绑处理、返回语义、日志行为。
- 采样命令/查询：
```bash
sed -n '/## 输入/,/## 处理/p' Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_unbind.log
sed -n '/## 处理/,/## 输出/p' Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_unbind.log
sed -n '/## 输出/,/## 日志/p' Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_unbind.log
```
- 样本数：4（输入/处理/输出/日志）
- 结果值（含分位数）：`unbind,count=4 p50=4 p95=41 min=2 max=41`
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_unbind.log`，`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/metric_behavior_link_quantiles.log`
- 来源依据：`Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md`，`Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`

## BCH-003 #status
- 采样口径：抽取 `#status` 入口、状态获取处理、回包语义、日志行为。
- 采样命令/查询：
```bash
sed -n '/## 输入/,/## 处理/p' Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_status.log
sed -n '/## 处理/,/## 输出/p' Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_status.log
sed -n '/## 输出/,/## 日志/p' Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_status.log
```
- 样本数：4（输入/处理/输出/日志）
- 结果值（含分位数）：`status,count=4 p50=7 p95=41 min=5 max=41`
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_status.log`，`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/metric_behavior_link_quantiles.log`
- 来源依据：`Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`，`Project_Docs/Manuals/OPERATIONS_RUNBOOK.md`

## BCH-004 #list
- 采样口径：抽取 `#list` 入口、玩家列表处理、回包语义、日志行为。
- 采样命令/查询：
```bash
sed -n '/## 输入/,/## 处理/p' Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_list.log
sed -n '/## 处理/,/## 输出/p' Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_list.log
sed -n '/## 输出/,/## 日志/p' Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_list.log
```
- 样本数：4（输入/处理/输出/日志）
- 结果值（含分位数）：`list,count=4 p50=3 p95=41 min=3 max=41`
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_list.log`，`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/metric_behavior_link_quantiles.log`
- 来源依据：`Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`，`Project_Docs/Manuals/OPERATIONS_RUNBOOK.md`

## BCH-005 #reload
- 采样口径：抽取 `#reload` 入口、Alpha/子服重载处理、回包语义、日志行为。
- 采样命令/查询：
```bash
sed -n '/## 输入/,/## 处理/p' Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_reload.log
sed -n '/## 处理/,/## 输出/p' Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_reload.log
sed -n '/## 输出/,/## 日志/p' Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_reload.log
```
- 样本数：4（输入/处理/输出/日志）
- 结果值（含分位数）：`reload,count=4 p50=5 p95=41 min=2 max=41`
- 证据路径：`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/trace_reload.log`，`Project_Docs/Re_Step/Evidence/Step02/20260215_140711/metric_behavior_link_quantiles.log`
- 来源依据：`Project_Docs/Contracts/CONFIG_SCHEMA_CONTRACT.md`，`Project_Docs/Manuals/DEPLOYMENT_RUNBOOK.md`
