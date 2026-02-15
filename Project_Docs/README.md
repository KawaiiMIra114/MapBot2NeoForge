---
doc_id: DOC-HUB-README-001
title: Project_Docs README
owner: Knowledge Base Maintainer
status: active
version: 1.0.0
last_updated: 2026-02-14
review_cycle: monthly
audience: newcomer, maintainer, release-manager
summary: Project_Docs 总入口，提供 10 分钟上手路径与文档治理规范。
---

# Project_Docs README

## 1. 10 分钟上手路径（新人必读）
1. 第 1-2 分钟：阅读 `Project_Docs/INDEX.md`，理解文档分层与入口。
2. 第 3-5 分钟：阅读 `Project_Docs/GLOSSARY.md`，统一术语和缩写。
3. 第 6-8 分钟：按你的职责阅读对应手册。  
   - 值班/稳定性：`Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md`  
   - 升级负责人：`Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md`  
   - 发布负责人：`Project_Docs/Manuals/RELEASE_CHECKLIST.md`
4. 第 9-10 分钟：查看 `Project_Docs/DECISIONS/ADR-000-template.md`，了解决策记录标准。

### 1.1 为什么不会迷路
- 路径是单向的：`INDEX -> GLOSSARY -> 对应手册 -> ADR`，每一步都只给下一跳。
- `INDEX` 按“场景”而不是“目录名”导航，减少新人对文件树的猜测成本。
- `GLOSSARY` 先统一词义，再读手册可避免术语误解导致的走偏。

## 2. 文档分层
| 层级 | 目录 | 用途 |
|---|---|---|
| L0 入口层 | `README.md`、`INDEX.md`、`GLOSSARY.md` | 快速导航、术语统一 |
| L1 操作层 | `Manuals/` | 可执行流程与检查单 |
| L2 架构层 | `Architecture/` | 系统设计与迁移方案 |
| L3 决策层 | `DECISIONS/` | ADR 决策与权衡记录 |
| L4 记录层 | `Reports/` | 执行报告与历史追溯 |

## 3. 统一元数据规范（强制）
所有核心文档必须包含 YAML 元数据，字段如下：
| 字段 | 含义 | 示例 |
|---|---|---|
| `doc_id` | 文档唯一编号 | `MANUAL-IR-001` |
| `title` | 文档标题 | `INCIDENT_RESPONSE_PLAYBOOK` |
| `owner` | 文档责任角色 | `Release Manager` |
| `status` | 状态（active/draft/deprecated） | `active` |
| `version` | 文档版本 | `1.0.0` |
| `last_updated` | 最后更新日期 | `2026-02-14` |
| `review_cycle` | 复审周期 | `monthly` |
| `audience` | 目标读者 | `newcomer, maintainer` |
| `summary` | 一句话摘要 | `发布门禁清单` |

## 4. 强制章节规范（强制）
### 4.1 操作手册类文档（Manuals）
- `1. 目标与范围`
- `2. 角色与职责`
- `3. 强制流程主体`
- `4. 回滚与应急`
- `5. 验收标准`
- `6. 记录与审计`

### 4.2 入口类文档（README/INDEX/GLOSSARY）
- `1. 使用目标`
- `2. 如何快速定位`
- `3. 维护规则`

### 4.3 ADR 文档
- 使用 ADR 标准结构（Status、Context、Decision、Consequences、Alternatives）。

## 5. 维护规则
- 变更了流程或责任边界，必须同步更新对应手册与索引。
- 新增术语时，必须同步更新 `GLOSSARY.md`。
- 关键技术决策必须新增 ADR，不得只留在聊天或口头结论中。
- 文档更新与代码发布应在同一迭代完成，避免知识漂移。

## 6. 下一步入口
- 总索引：`Project_Docs/INDEX.md`
- 术语：`Project_Docs/GLOSSARY.md`
- 事故手册：`Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md`
- 升级手册：`Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md`
- 发布清单：`Project_Docs/Manuals/RELEASE_CHECKLIST.md`
- ADR 模板：`Project_Docs/DECISIONS/ADR-000-template.md`

## 7. 一致性检查脚本思路（防术语与索引漂移）
目标：
- 防止 `INDEX` 引用失效。
- 防止 `README` 入口声明与 `INDEX` 场景映射不一致。
- 防止手册新增术语未进入 `GLOSSARY`。

建议脚本形态：
- 文件：`Project_Docs/scripts/docs_consistency_check.sh`（建议新增）。
- 触发：`pre-commit` + CI（PR 必跑）。
- 输出：失败时打印缺失项并返回非零退出码。

建议检查项：
1. 链接存在性：提取 `README.md` 与 `INDEX.md` 内的 `Project_Docs/*.md` 路径并校验文件存在。
2. 入口对齐：`README` 的“下一步入口”必须都能在 `INDEX` 找到对应记录。
3. 术语覆盖：`Manuals/*.md` 中出现的核心术语（Incident/Severity/Rollback/ADR 等）必须在 `GLOSSARY.md` 存在定义。
4. 章节完整性：关键文档必须包含约定强制章节标题。

示例命令骨架：
```bash
# links check
rg -o "Project_Docs/[A-Za-z0-9_./-]+\\.md" Project_Docs/README.md Project_Docs/INDEX.md | sort -u | xargs -I{} test -s "{}"

# glossary check (示例词可扩展)
for t in Incident Severity Rollback ADR; do
  rg -q "$t" Project_Docs/GLOSSARY.md || { echo "Missing term: $t"; exit 1; }
done
```
