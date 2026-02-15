---
doc_id: DOC-HUB-INDEX-001
title: Project_Docs INDEX
owner: Knowledge Base Maintainer
status: active
version: 1.0.0
last_updated: 2026-02-14
review_cycle: monthly
audience: newcomer, maintainer, reviewer
summary: 按场景和层级组织的文档索引，支持 10 分钟内完成定位。
---

# Project_Docs INDEX

## 1. 使用目标
- 你不知道从哪里看时，先看这里。
- 按“我要做什么”而不是“文件夹名称”来定位文档。

## 2. 新人 10 分钟路径（固定顺序）
1. 第 1-2 分钟：`Project_Docs/README.md`
2. 第 3-5 分钟：`Project_Docs/GLOSSARY.md`
3. 第 6-8 分钟：按职责进入以下其一：  
   - 值班：`Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md`  
   - 升级：`Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md`  
   - 发布：`Project_Docs/Manuals/RELEASE_CHECKLIST.md`
4. 第 9-10 分钟：`Project_Docs/DECISIONS/ADR-000-template.md`

## 3. 为什么不会迷路
- 入口只有一层：新人先看 README，再由 INDEX 分发到场景手册。
- 场景表固定“先读/再读”，避免在 Architecture/Reports 里盲搜。
- 每个关键文档都回链到其它关键文档（发布、升级、事故、ADR）。

## 4. 按场景快速定位
| 场景 | 先读 | 再读 |
|---|---|---|
| 我是新成员，想快速了解体系 | `Project_Docs/README.md` | `Project_Docs/GLOSSARY.md` |
| 线上故障，需要快速处理 | `Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md` | `Project_Docs/Reports/README.md` |
| 计划升级版本 | `Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md` | `Project_Docs/Architecture/Migration_1.21.md` |
| 准备发布上线 | `Project_Docs/Manuals/RELEASE_CHECKLIST.md` | `Project_Docs/COMMAND_PERMISSION_MATRIX_V1.md` |
| 要记录技术决策 | `Project_Docs/DECISIONS/ADR-000-template.md` | `Project_Docs/Architecture/README.md` |

## 5. 分层索引
### 5.1 L0 入口层
- `Project_Docs/README.md`：总入口与治理规则。
- `Project_Docs/INDEX.md`：场景索引。
- `Project_Docs/GLOSSARY.md`：术语与缩写。

### 5.2 L1 操作手册层（Manuals）
- `Project_Docs/Manuals/INCIDENT_RESPONSE_PLAYBOOK.md`：事故分级、止血、排障、复盘。
- `Project_Docs/Manuals/UPGRADE_MIGRATION_GUIDE.md`：升级前/中/后校验与回滚。
- `Project_Docs/Manuals/RELEASE_CHECKLIST.md`：发布门禁与审计记录。

### 5.3 L2 架构层（Architecture）
- `Project_Docs/Architecture/README.md`
- `Project_Docs/Architecture/Migration_1.21.md`
- `Project_Docs/Architecture/Web_Console_Design.md`

### 5.4 L3 决策层（Decisions）
- `Project_Docs/DECISIONS/ADR-000-template.md`：ADR 模板起点。

### 5.5 L4 历史记录层（Reports）
- `Project_Docs/Reports/README.md`：历史报告入口。
- `Project_Docs/Reports/`：任务执行报告和复盘沉淀。

## 6. 维护规则
- 新增核心文档时，必须同步更新本索引。
- 入口文档（README/INDEX/GLOSSARY）任何一项过期都视为 P1 文档债务。
- 不允许只新增文件而不挂入口，避免“隐形文档”。

## 7. 一致性检查契约（供脚本校验）
- 本文档中的 `Project_Docs/*.md` 链接必须可达且非空文件。
- “新人 10 分钟路径”中的四步文档必须全部存在于本索引。
- “按场景快速定位”每个场景的“先读”文档必须在 README 的入口列表可追溯。
- 若新增或删除手册，必须同步更新本索引与 README，不允许单边更新。
