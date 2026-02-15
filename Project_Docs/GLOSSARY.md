---
doc_id: DOC-HUB-GLOSSARY-001
title: Project_Docs GLOSSARY
owner: Knowledge Base Maintainer
status: active
version: 1.0.0
last_updated: 2026-02-14
review_cycle: monthly
audience: newcomer, maintainer, release-manager
summary: 核心术语与缩写统一词典，减少跨团队沟通歧义。
---

# Project_Docs GLOSSARY

## 1. 使用目标
- 统一团队语言，降低“同词不同义”带来的协作成本。
- 新成员先读术语，再读手册，可显著降低理解门槛。

## 2. 新人 10 分钟路径中的术语最小集
按顺序掌握以下 10 个词，再进入手册不会迷路：
1. Incident
2. Severity
3. Containment
4. Diagnosis
5. Postmortem
6. Release Gate
7. Rollback
8. Baseline
9. Go/No-Go
10. ADR

## 3. 为什么不会迷路
- 新人先学最小术语集，再读手册时不会卡在缩写或角色定义上。
- 本文术语与 `README/INDEX/Manuals` 保持同名，检索词一致。
- 当看到陌生词时，可回跳本页定位定义，不需要跨目录猜测。

## 4. 核心术语
| 术语 | 英文/缩写 | 定义 |
|---|---|---|
| 事故 | Incident | 对可用性、正确性或安全性产生实际影响的异常事件。 |
| 分级 | Severity | 事故影响级别，本文档采用 Sev-0 到 Sev-3。 |
| 止血 | Containment | 在未彻底修复前，先控制影响面并恢复基本服务。 |
| 排障 | Diagnosis | 通过证据和实验定位问题根因的过程。 |
| 复盘 | Postmortem | 事故后的结构化回顾，输出可执行防再发行动。 |
| 升级 | Upgrade | 版本或依赖向前迁移。 |
| 迁移 | Migration | 数据、配置、协议等结构性变化过程。 |
| 回滚 | Rollback | 将系统恢复到最近稳定状态的动作。 |
| 门禁 | Release Gate | 发布前必须满足的硬性通过条件。 |
| 灰度 | Canary/Rollout | 分批放量发布，逐步扩大影响范围。 |
| 冒烟测试 | Smoke Test | 快速验证核心功能是否可用的最小测试集。 |
| 基线 | Baseline | 对比用的稳定指标、配置或版本状态。 |
| 观察窗口 | Observation Window | 发布/修复后的稳定性监控时段。 |
| 审计 | Audit | 对关键操作进行可追溯记录和核验。 |
| 变更冻结 | Change Freeze | 指定窗口内限制新增变更，降低发布风险。 |
| Go/No-Go | Go/No-Go | 发布或升级是否继续执行的决策点。 |
| MTTR | Mean Time To Recovery | 从故障发生到恢复服务的平均时间。 |
| RCA | Root Cause Analysis | 根因分析方法与产出。 |
| Runbook | Runbook | 面向操作执行的步骤化手册。 |
| ADR | Architecture Decision Record | 架构决策记录，用于沉淀背景、决策与后果。 |
| Owner | Owner | 对某文档或动作最终负责的角色。 |
| On-Call | On-Call | 值班响应角色。 |
| Feature Flag | Feature Flag | 可在运行时开关特性的控制机制。 |
| SLO | Service Level Objective | 服务等级目标，如可用性与时延目标。 |
| SLA | Service Level Agreement | 对响应或服务能力的约定时限。 |

## 5. 常用缩写
| 缩写 | 全称 | 常见语境 |
|---|---|---|
| IC | Incident Commander | 事故指挥角色 |
| QA | Quality Assurance | 测试与验收 |
| RCA | Root Cause Analysis | 事故复盘 |
| MTTR | Mean Time To Recovery | 稳定性指标 |
| SLO | Service Level Objective | 指标目标 |
| SLA | Service Level Agreement | 响应承诺 |
| ADR | Architecture Decision Record | 技术决策沉淀 |

## 6. 维护规则
- 新引入术语必须在此登记后再进入手册或报告。
- 如术语定义变化，需在同一迭代内更新引用文档。
- 对同义词采用“主词条 + 别名”方式，避免重复定义。

## 7. 一致性检查契约（供脚本校验）
- 核心手册中的高频术语必须能在本词典检索到（至少含 Incident、Severity、Rollback、Release Gate、ADR）。
- 缩写必须在“常用缩写”或“核心术语”至少出现一次定义。
- 同一术语不允许在不同文档出现冲突定义；如变更定义，需同迭代更新 README/INDEX 的相关描述。
