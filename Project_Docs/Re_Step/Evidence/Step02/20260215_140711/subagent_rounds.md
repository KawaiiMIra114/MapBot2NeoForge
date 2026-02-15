# Step-02 A2 反证审查记录

- 主代理：Step-02 执行主代理
- 子代理：019c5ff1-0afc-7891-ae84-a405a8fd4129（explorer）
- 审查模式：单人维护（主代理自审+自记录，子代理仅反证）
- 审查日期：2026-02-15

## 第1轮
- 主代理问题：
  - 在不运行新测试的前提下，仅靠静态采样证据，最可能导致 A2 基线失真的前三个风险是什么？每个风险给出：触发条件、可观测信号、可机检修补措施。
- 子代理回答（摘要）：
  - 风险1：环境变量/依赖未同步导致基线偏移。
  - 风险2：指标采集配置变动导致分位失真。
  - 风险3：阈值来源未冻结导致比较器使用漂移阈值。
  - 每项均给出触发条件、信号、自动比对或恢复措施。
- 主代理复核：
  - 结论：方向正确，但偏运行态，尚未直接绑定 Step02 现有证据文件与量化阈值，无法直接转为门禁。
- 追加问题（进入第2轮）：
  - 请收敛为“静态证据本身会误导比较器”的风险，必须给出 PASS/WARN/FAIL 阈值并绑定到 `sample_metrics_quantiles.log`、`trace_*.log`、`threshold_sources.log` 等已有证据。

## 第2轮
- 主代理问题：
  - 收敛为静态证据误导风险，并量化阈值（PASS/WARN/FAIL）且绑定现有证据文件。
- 子代理回答（摘要）：
  - 风险1：`sample_metrics_quantiles.log` 样本数过小（count=9/2/7）导致分位不稳。
    - 建议阈值：PASS `count>=30`，WARN `20<=count<30`，FAIL `count<20`。
  - 风险2：`sample_metrics_command_quantiles.log` 仅 5 样本导致命令链路比较敏感。
    - 建议阈值：PASS `count>=30`，WARN `10<=count<30`，FAIL `count<10`。
  - 风险3：`sample_env_repo.log` 显示工作树脏状态，误导“已冻结”判断。
    - 建议阈值：PASS 0 行变更，WARN 1~5 行，FAIL >5 行。
- 主代理复核：
  - 结论：风险已可量化，但仍缺少“可执行门禁命令+阻断动作+修复后复检范围”。
- 追加问题（进入第3轮）：
  - 请将三项风险转为可落地 Gate 条目，每条必须有 `检查命令 / Block 条件 / Fix Action / 复检要求`，并绑定 Step02 证据目录文件。

## 第3轮
- 主代理问题：
  - 将三项风险转为可执行 Gate 条目（检查命令、Block、Fix、复检）并绑定 Step02 证据文件。
- 子代理回答（摘要）：
  - Gate-A：`sample_metrics_quantiles.log` 中任一 `count<20` 则阻断；修复为补采样并重生 `metric_*samples*` 与 quantiles。
  - Gate-B：`sample_metrics_command_quantiles.log` 中任一 `count<10` 则阻断；修复为补采命令样本并重生 `metric_*hits*` 与 quantiles。
  - Gate-C：`sample_env_repo.log` 脏条目 >5 则阻断；修复为清理/收敛工作树并重采环境快照。
- 主代理复核：
  - 结论：三项均达到“可量化+可验证+可追溯”标准，可直接写入 `05_Baseline_Comparator_Spec.md` 与 `06_Solo_Review_Log_A2.md`。
  - 闭环状态：闭合（Closed）。

## 主代理采纳清单
1. 将 Gate-A/B/C 作为 A2 比较器稳定性条目写入 `05_Baseline_Comparator_Spec.md`。
2. 在 `06_Solo_Review_Log_A2.md` 记录三轮追问与闭环判定。
3. 将“修复后全量重跑 A2 门禁”写入最终裁决与门禁脚本说明。
