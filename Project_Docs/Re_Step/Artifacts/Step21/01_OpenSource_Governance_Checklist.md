# 01 开源治理文件清单 (OpenSource Governance Checklist)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-21 |
| Artifact | 01/05 |
| RUN_ID | 20260216T182100Z |

## 治理文件最低集合
| # | 文件 | 路径 | 状态 | 必填章节 | 更新责任 |
|---|---|---|---|---|---|
| G-01 | LICENSE | `/LICENSE` | ✅ 本次创建 (MIT) | 许可证全文 | 版本发布时复审 |
| G-02 | CONTRIBUTING.md | `/CONTRIBUTING.md` | ✅ 本次创建 | 行为准则引用/开发环境/提交规范/PR流程/Issue模板 | 每季度复审 |
| G-03 | SECURITY.md | `/SECURITY.md` | ✅ 本次创建 | 支持版本/报告方式/响应SLA/披露策略 | 每次安全事件后 |
| G-04 | CODE_OF_CONDUCT.md | `/CODE_OF_CONDUCT.md` | ✅ 本次创建 (Contributor Covenant 2.1) | 承诺/标准/执行/适用范围 | 社区投诉后 |
| G-05 | README.md | `/README.md` | ⚠️ 不存在 (I2 不创建根 README, 留 J1) | 项目简介/快速开始/架构/许可 | 每次发布 |

## 缺失时发布阻断规则
| 文件 | 缺失时是否阻断发布 |
|---|---|
| LICENSE | **是** (法律合规, P0 阻断) |
| CONTRIBUTING.md | **是** (开源协作基础, P0 阻断) |
| SECURITY.md | **是** (安全披露合规, P0 阻断) |
| CODE_OF_CONDUCT.md | **是** (社区治理基础, P1 阻断) |
| README.md | 否 (J1 创建, I2 不阻断) |

## 版本与更新记录
| 文件 | 初始版本 | 创建日期 | 最后更新 |
|---|---|---|---|
| LICENSE | MIT | 2026-02-16 | 2026-02-16 |
| CONTRIBUTING.md | 1.0.0 | 2026-02-16 | 2026-02-16 |
| SECURITY.md | 1.0.0 | 2026-02-16 | 2026-02-16 |
| CODE_OF_CONDUCT.md | 2.1 (CC) | 2026-02-16 | 2026-02-16 |

## 差距
| ID | 差距 | 严重度 |
|---|---|---|
| GOV-01 | 根 README.md 尚未创建 (计划 J1) | Medium |
| GOV-02 | .github/ISSUE_TEMPLATE 目录未创建 | Low |
