# 02 自动化检查规则 (Automated Checks Spec)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-17 |
| Artifact | 02/05 |
| RUN_ID | 20260216T010500Z |

## 测试完整性守卫
| 工件 | 检查方式 | 阻断 |
|---|---|---|
| Unit tests | `./gradlew test` exit=0 | ✅ 硬 |
| Integration tests | `./gradlew integrationTest` exit=0 | ✅ 硬 |
| Smoke tests | 核心命令 (#help,#bind,#reload) 响应 | ✅ 硬 |
| 三工件齐全 | unit+integration+smoke 报告存在 | ✅ 硬 |

## 文档联动守卫
| 检查项 | 规则 | 命令 | 阻断 |
|---|---|---|---|
| README 更新 | 版本号与 build.gradle 一致 | `grep version README.md` vs `gradle properties` | ⚠️ 软 |
| INDEX 可达 | 所有链接返回 200/文件存在 | `grep -oP '\[.*?\]\(.*?\)' INDEX.md` + 检查 | ⚠️ 软 |
| Manuals 入口 | 升级指南+事故手册入口存在 | `test -s UPGRADE_MIGRATION_GUIDE.md` | ✅ 硬 |

## 索引可达守卫
```bash
# 检查所有 .md 内部链接可达
find Project_Docs -name "*.md" -exec grep -oP '\]\(([^)]+)\)' {} \; | \
  sed 's/](\(.*\))/\1/' | while read link; do
    test -e "$link" || echo "BROKEN: $link"
  done
```
- 阻断: 关键文档断链 → 硬阻断; 非关键 → 警告

## 术语漂移守卫
| 术语 | 正确值 | 错误值 | 检查命令 |
|---|---|---|---|
| 权限角色 | user/admin/owner | member/manager/op | `grep -rn "member\|manager\|/op " *.md` |
| 错误码前缀 | BRG_ | ERR_/ERROR_ | `grep -rn "ERR_\|ERROR_" *.java` |
| 证据术语 | 自审+自记录 | 自检/自查 | `grep -rn "自检\|自查" *.md` |
| 阻断术语 | No-Go | 暂停/待定 | `grep -rn "暂停\|待定" *.md` (排除风险记录) |

## 安全扫描守卫
| 检查项 | 工具 | 阈值 | 阻断 |
|---|---|---|---|
| 依赖漏洞 | `dependencyCheck` | CVE ≥ 7.0 → No-Go | ✅ 硬 |
| 敏感信息泄露 | `grep -rn "password\|token\|secret"` | 明文凭据 → No-Go | ✅ 硬 |
| 过期依赖 | `./gradlew dependencyUpdates` | 主版本落后2+ → 警告 | ⚠️ 软 |

## 错误码一致性守卫
```bash
# 检查 BRG_* 代码 vs 合同定义
code_errors=$(grep -roh "BRG_[A-Z_]*_[0-9]*" Mapbot-Alpha-V1/src/ MapBot_Reforged/src/ | sort -u)
contract_errors=$(grep -oh "BRG_[A-Z_]*_[0-9]*" Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md | sort -u)
diff <(echo "$code_errors") <(echo "$contract_errors")
# 差异 → 阻断 + 修复清单
```

## 差距
| ID | 差距 | 严重度 |
|---|---|---|
| ACK-01 | 自动化检查脚本未编码 | High |
| ACK-02 | CI 集成未配置 | Medium |
| ACK-03 | dependencyCheck 插件未引入 | Medium |
