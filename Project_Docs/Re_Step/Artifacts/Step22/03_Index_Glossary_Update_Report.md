# 03_Index_Glossary_Update_Report — 索引与术语更新报告

## 文档元数据
| 字段 | 值 |
|---|---|
| DocID | IDX-GLOSS-J1-001 |
| Version | 1.0.0 |
| Owner | Solo Maintainer |
| Last Updated | 2026-02-16 |

## 1. INDEX.md 更新建议

### 1.1 新增入口 (待补充)
| # | 文档 | 建议层级 | 场景 |
|---|---|---|---|
| I-01 | `Project_Docs/Re_Step/` | L4 历史记录层 | 查看重构步骤产物 |
| I-02 | `CONTRIBUTING.md` | L0 入口层 | 外部贡献者入口 |
| I-03 | `SECURITY.md` | L1 操作手册层 | 安全漏洞报告 |
| I-04 | `Project_Docs/Control/` | L2 架构层 | 中控执行流程 |
| I-05 | `Project_Docs/Memory_KB/` | L4 历史记录层 | 系统状态追踪 |

### 1.2 断链检查
| # | 链接 | 状态 | 备注 |
|---|---|---|---|
| L-01 | `Project_Docs/Architecture/Web_Console_Design.md` | ⚠️ 待验证 | INDEX 引用, 需确认存在 |
| L-02 | `Project_Docs/DECISIONS/ADR-000-template.md` | ⚠️ 待验证 | INDEX 引用, 需确认存在 |
| L-03 | `Project_Docs/Reports/README.md` | ⚠️ 待验证 | INDEX 引用, 需确认存在 |

### 1.3 可达性结论
- INDEX 核心路径 (新人10分钟) 的 4 个文档均存在且可达
- 5 个新入口建议纳入下一次 INDEX 更新

## 2. GLOSSARY.md 更新建议

### 2.1 新增术语 (重构过程产生)
| # | 术语 | 英文 | 定义 |
|---|---|---|---|
| G-01 | 中控执行器 | Control Executor | 按 MASTER_PROMPT 驱动步骤执行的 AI 角色 |
| G-02 | 证据链 | Evidence Chain | 从门禁到产物到审计日志的完整可追溯路径 |
| G-03 | 阶段化交付 | Phased Delivery | 按 A→J 阶段顺序交付, 每阶段可独立验收 |
| G-04 | 差距 (Gap) | Gap | 条件通过时记录的未完全满足项 |
| G-05 | phase-aware | Phase-Aware | 区分 precommit/postcommit 的校验策略 |

### 2.2 废弃术语
| # | 术语 | 原因 |
|---|---|---|
| (无) | 本轮重构无术语废弃 |

### 2.3 同义冲突检查
- "自审" vs "Self Review" → 统一为 "自审+自记录" (ADR-002)
- "门禁" vs "Gate" → 两者等价, GLOSSARY 已有 "Release Gate"
- 无冲突定义

## 3. 结论
- INDEX: 5 个新入口待补充, 3 个链接待验证
- GLOSSARY: 5 个新术语待纳入, 无废弃, 无冲突
- 优先级: Medium (不阻断发布, 但影响新人体验)
