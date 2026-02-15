---
doc_id: MANUAL-RELEASE-001
title: RELEASE_CHECKLIST
owner: Release Manager
status: active
version: 1.0.0
last_updated: 2026-02-14
review_cycle: per-release
audience: release-manager, maintainer, oncall
summary: 发布门禁清单，覆盖文档、测试、配置、审计、回滚五大维度。
---

# RELEASE_CHECKLIST

## 1. 目标与范围
- 目标：将发布决策从“经验判断”转为“门禁判断”。
- 范围：所有进入共享环境或生产环境的版本发布。

## 2. 发布角色与职责
| 角色 | 职责 |
|---|---|
| Release Manager | 最终 Go/No-Go 决策 |
| Tech Owner | 变更正确性背书 |
| QA/Verifier | 测试与验收证据确认 |
| On-Call | 发布窗口值守与应急响应 |
| Auditor | 审计材料完整性检查 |

## 3. 发布门禁总览（强制）
### 3.1 硬门禁（未通过即 No-Go）
| 门禁项 | 通过标准 | 可验证命令或证据 | 未通过处置 |
|---|---|---|---|
| 文档完备性 | 发布说明、升级说明、回滚说明均更新且可访问 | `rg -n \"RELEASE_CHECKLIST|UPGRADE_MIGRATION_GUIDE|INCIDENT_RESPONSE_PLAYBOOK\" Project_Docs/README.md Project_Docs/INDEX.md`；MR/PR 文档变更记录 | 立即阻断发布 |
| 自动化测试 | 单元/集成测试通过，构建成功 | `./gradlew clean test`；`./gradlew build`；CI 绿灯截图/链接 | 立即阻断发布 |
| 核心冒烟 | 核心命令与主链路全部通过 | 冒烟执行记录（命令清单 + 输出日志）；验收签字 | 立即阻断发布 |
| 配置审查 | 配置差异已复核，敏感项双人审批 | `git diff -- Project_Docs COMMAND_PERMISSION_MATRIX_V1.md`（文档层）；配置变更单与双人审批记录 | 立即阻断发布 |
| 回滚就绪 | 回滚步骤、触发阈值、责任人明确且已演练 | 回滚演练记录；`UPGRADE_MIGRATION_GUIDE` 中回滚步骤引用 | 立即阻断发布 |
| 审计可追溯 | 发布编号、审批链、时间线、执行人完整 | 发布审计模板填写完成；工单/流水链接 | 立即阻断发布 |

### 3.2 软门禁（未通过可带风险发布，需豁免）
| 门禁项 | 通过标准 | 可验证命令或证据 | 未通过处置 |
|---|---|---|---|
| 覆盖率改进 | 覆盖率不低于上一版本，或下降有说明 | 覆盖率报告链接；`./gradlew test jacocoTestReport`（若启用） | 记录风险并由 Tech Owner 豁免 |
| 性能回归评估 | P95/P99 无明显回归，或有降级预案 | 压测报告、监控对比截图 | 记录风险并增强监控 |
| 告警噪声治理 | 新增告警有 owner 和阈值说明 | 告警规则变更单 | 记录风险并设观察窗口 |
| 文档可读性复核 | 非责任人能按文档完成一次演练 | 演练签到 + 反馈单 | 下个迭代强制补齐 |

## 4. 发布检查清单（强制）
### 4.1 发布前（T-1 ~ T-0）
- [ ] 已确认发布范围（版本、模块、影响面）。
- [ ] `UPGRADE_MIGRATION_GUIDE` 相关步骤已准备。
- [ ] 变更文档已更新（含操作手册与术语影响）。
- [ ] 自动化测试通过（单元、集成、关键端到端）。
- [ ] 冒烟用例清单已签字确认。
- [ ] 配置变更已完成双人复核。
- [ ] 审计字段准备完毕（发布人、审批人、时间窗、版本号）。
- [ ] 回滚预案在当前版本下可执行。

### 4.2 发布中（T+0）
- [ ] 发布窗口开始前执行环境健康检查。
- [ ] 按灰度阶段发布并逐阶段验收。
- [ ] 关键指标实时监控（错误率、延迟、告警、核心命令成功率）。
- [ ] 每阶段发布结论已记录（Go/No-Go）。

### 4.3 发布后（T+30min ~ T+24h）
- [ ] 核心功能冒烟全部通过。
- [ ] 审计日志完整且可检索。
- [ ] 问题清单已更新并分配 Owner。
- [ ] 发布总结与证据归档完成。

## 5. 阻断条件与升级路径（强制）
- 任一门禁未通过：立即 `No-Go`，不得“先发后补”。
- 关键指标越过阈值：暂停发布并进入事故响应流程。
- 升级路径：Release Manager -> Tech Owner -> 项目负责人。

## 6. 回滚要求（强制）
- 回滚触发条件必须在发布前定义，且可量化。
- 回滚演练至少每个大版本执行一次。
- 回滚后必须执行最小冒烟集并记录恢复时间。
- 参考：`Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md`、`Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md`。

## 7. 验收标准
- 五类门禁均有“已通过”证据。
- 发布过程每个阶段有明确责任人和时间戳。
- 能在 5 分钟内定位到发布记录与回滚记录。

## 8. 审计记录模板
```markdown
# Release Audit: <Version>
- Window:
- Release Manager:
- Tech Owner:
- Approver:

## Gate Results
- Documentation: Pass/Fail
- Testing: Pass/Fail
- Configuration: Pass/Fail
- Audit Trail: Pass/Fail
- Rollback Readiness: Pass/Fail

## Timeline
- T-30:
- T+00:
- T+15:
- T+60:

## Final Decision
- Go/No-Go:
- Notes:
```

## 9. 附录
- 版本命名建议：`major.minor.patch`。
- 发布编号建议：`REL-YYYYMMDD-序号`。

## 10. 误判案例与自动化防呆（强制）
### 10.1 误判案例（看起来通过，实际不通过）
| 案例 | 表面现象（误判） | 实际问题（不通过原因） | 自动化防呆规则 |
|---|---|---|---|
| Case-1: 测试绿灯误判 | CI `test` 通过，认为可发布 | 只跑了单元测试，集成/冒烟未执行 | 发布流水线必须同时存在 `unit + integration + smoke` 三类结果，缺一即 `No-Go` |
| Case-2: 文档已改误判 | README 有改动，认为文档门禁通过 | 关键手册未更新或索引未挂接，入口断链 | 校验变更集必须命中 `README/INDEX/对应Manual` 三联动，否则失败 |
| Case-3: 回滚已写误判 | 回滚步骤写在文档里，认为回滚就绪 | 未验证可执行性，回滚脚本参数失效 | 发布前强制执行一次回滚演练 dry-run，未产生成功记录则失败 |

### 10.2 自动化防呆规则清单
1. 测试完整性守卫：流水线校验 `unit/integration/smoke` 三个工件是否全部存在且状态成功。
2. 文档联动守卫：检测本次发布若涉及运行逻辑变更，则 `Project_Docs/README.md`、`Project_Docs/INDEX.md` 与至少一份 `Project_Docs/Manuals/*.md` 必须同时变更。
3. 索引可达守卫：解析 `INDEX.md` 中关键链接，验证目标文件存在且非空。
4. 术语漂移守卫：检测新增高频术语是否同步进入 `GLOSSARY.md`。
5. 回滚演练守卫：检查最近一次演练记录时间是否在允许窗口内（建议 30 天），超期则 `No-Go`。

### 10.3 建议实现命令（示例）
```bash
# 1) 校验关键文档是否存在并已挂接
rg -n "INCIDENT_RESPONSE_PLAYBOOK|UPGRADE_MIGRATION_GUIDE|RELEASE_CHECKLIST" Project_Docs/README.md Project_Docs/INDEX.md

# 2) 校验三联动（示例：在 CI 中检查 diff）
git diff --name-only origin/main...HEAD | rg "^Project_Docs/(README.md|INDEX.md|Manuals/.+\\.md)$"

# 3) 校验索引引用目标存在
rg -o "Project_Docs/[A-Za-z0-9_./-]+\\.md" Project_Docs/INDEX.md | sort -u | xargs -I{} test -s "{}"
```
