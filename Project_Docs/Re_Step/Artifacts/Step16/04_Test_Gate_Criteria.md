# 04 测试门禁标准 (Test Gate Criteria)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-16 |
| Artifact | 04/05 |
| RUN_ID | 20260216T005500Z |

## 三层门禁定义

### Local 门禁 (开发者本地)
| 项目 | 标准 | 阻断 | 命令 |
|---|---|---|---|
| 编译通过 | exit=0 | ✅ 阻断合并 | `./gradlew compileJava` |
| CT 核心用例 | CT-01,04,10,11,15 通过 | ✅ 阻断合并 | `./gradlew test --tests CT*` |
| 代码扫描 | 0 new TODO/FIXME | ⚠️ 警告 | `grep -rn "TODO\|FIXME"` |
| 配置校验 | JSON valid | ✅ 阻断合并 | `python -m json.tool config.json` |

### CI 门禁 (自动集成)
| 项目 | 标准 | 阻断 | 命令 |
|---|---|---|---|
| 全量编译 | Alpha + Reforged exit=0 | ✅ 阻断合并 | 双端 `compileJava` |
| CT 全量 | CT-01~CT-18 通过 | ✅ 阻断合并 | `./gradlew test` |
| E2E 阻断级 | 绑定/解绑/重载 成功+失败 | ✅ 阻断合并 | E2E test suite |
| 错误码覆盖 | BRG_* 映射 100% | ✅ 阻断合并 | contract test |
| 权限反向 | user→admin 拒绝 | ✅ 阻断合并 | permission test |

### Preprod 门禁 (预发布)
| 项目 | 标准 | 阻断 | 命令 |
|---|---|---|---|
| CT-01~CT-18 | 100% 通过 | ✅ 阻断发布 | full contract suite |
| E2E 四类场景 | 成功/失败/超时/重试 覆盖 | ✅ 阻断发布 | full E2E suite |
| 故障注入 | FI-01~FI-05 收敛 | ✅ 阻断发布 | fault injection suite |
| 回滚验证 | ≤15 分钟 | ✅ 阻断发布 | rollback drill |
| 性能基线 | P95 ≤ 50ms | ⚠️ 警告 | perf benchmark |

## Fail-Fast 策略
1. Local: 编译失败 → 立即停止, 不运行测试
2. CI: 阻断级用例失败 → 立即标红, 不继续非阻断用例
3. Preprod: 任一阻断项失败 → 放行按钮禁用

## 门禁升级规则
| 场景 | 当前级别 | 升级到 | 条件 |
|---|---|---|---|
| CT 新增阻断用例 | CI 警告 | CI 阻断 | 经评审确认为关键路径 |
| 性能回归 | Preprod 警告 | Preprod 阻断 | 连续 3 次回归超 20% |
| 安全漏洞 | 无 | 全层阻断 | CVE 评分 ≥ 7.0 |

## 通过标准
1. 三层门禁规则明确且可自动执行
2. 未通过门禁可阻断合并或发布
3. Fail-fast 策略已定义

## 失败判据
1. 门禁只提醒不阻断
2. Preprod 未全覆盖即放行

## 可机检命令
```bash
# 验证三层均有定义
for layer in "Local" "CI" "Preprod"; do
  grep -c "$layer" 04_Test_Gate_Criteria.md
done
# 预期: 每层 >= 3
```
