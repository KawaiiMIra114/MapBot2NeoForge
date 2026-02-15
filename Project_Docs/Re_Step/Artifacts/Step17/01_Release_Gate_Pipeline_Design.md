# 01 发布门禁流水线设计 (Release Gate Pipeline Design)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-17 |
| Artifact | 01/05 |
| RUN_ID | 20260216T010500Z |

## 流水线阶段定义

### Stage 1: Pre-Build 门禁
| 门禁 | 类型 | 输入工件 | 阻断规则 | 命令 |
|---|---|---|---|---|
| PB-01 配置完整性 | 硬 | alpha.json, config.json | JSON 无效 → No-Go | `python -m json.tool` |
| PB-02 依赖版本锁定 | 硬 | build.gradle | 依赖变更未审批 → No-Go | `./gradlew dependencies --check` |
| PB-03 分支保护 | 硬 | Git branch | 非 main 分支 → No-Go | `git branch --show-current` |

### Stage 2: Build 门禁
| 门禁 | 类型 | 输入工件 | 阻断规则 | 命令 |
|---|---|---|---|---|
| BD-01 Alpha 编译 | 硬 | 源码 | exit≠0 → No-Go | `./gradlew compileJava` |
| BD-02 Reforged 编译 | 硬 | 源码 | exit≠0 → No-Go | `./gradlew compileJava` |

### Stage 3: Test 门禁
| 门禁 | 类型 | 输入工件 | 阻断规则 | 命令 |
|---|---|---|---|---|
| TS-01 契约测试 | 硬 | CT-01~CT-18 | 任一失败 → No-Go | `./gradlew contractTest` |
| TS-02 集成测试 | 硬 | E2E suite | 关键链路失败 → No-Go | `./gradlew integrationTest` |
| TS-03 冒烟测试 | 硬 | smoke suite | 核心命令失败 → No-Go | `./gradlew smokeTest` |
| TS-04 覆盖率 | 软 | coverage report | <80% → 警告记录 | `./gradlew jacocoReport` |

### Stage 4: Quality 门禁
| 门禁 | 类型 | 输入工件 | 阻断规则 | 命令 |
|---|---|---|---|---|
| QA-01 代码扫描 | 硬 | 源码 | 新增 TODO/FIXME → No-Go | `grep -rn "TODO\|FIXME"` |
| QA-02 安全扫描 | 硬 | dependencies | CVE ≥7.0 → No-Go | `./gradlew dependencyCheck` |
| QA-03 术语漂移 | 硬 | 全文档 | 术语不一致 → No-Go | `grep` 对照检查 |
| QA-04 文档联动 | 软 | README/INDEX | 断链 → 警告记录 | 链接可达检查 |

### Stage 5: Release 门禁
| 门禁 | 类型 | 输入工件 | 阻断规则 | 命令 |
|---|---|---|---|---|
| RL-01 回滚就绪 | 硬 | 演练记录 | 无有效演练 → No-Go | 30 天窗口检查 |
| RL-02 审计可追溯 | 硬 | 证据链 | 缺证据 → No-Go | 目录完整性检查 |
| RL-03 Go/No-Go 决策 | 硬 | 全部门禁 | 任一硬门禁失败 → No-Go | 汇总判定 |

## 硬门禁 vs 软门禁
- **硬门禁** (12个): PB-01~03, BD-01~02, TS-01~03, QA-01~03, RL-01~03
  - 未通过 = No-Go, 不可跳过, 无口头豁免
- **软门禁** (2个): TS-04, QA-04
  - 未通过 = 警告 + 风险记录, 可带风险继续
