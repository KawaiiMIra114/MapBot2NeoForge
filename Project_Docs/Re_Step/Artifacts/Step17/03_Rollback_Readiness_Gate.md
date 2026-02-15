# 03 回滚就绪门禁 (Rollback Readiness Gate)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-17 |
| Artifact | 03/05 |
| RUN_ID | 20260216T010500Z |

## 有效窗口
- **窗口**: 最近 30 天内必须有一次成功回滚演练
- **超窗口**: 强制 No-Go, 不可豁免

## 门禁检查项
| 项目 | 规则 | 证据 | 阻断 |
|---|---|---|---|
| 演练记录存在 | `Artifacts/Step15/04_Incident_Drill_Evidence.md` 非空 | 文件检查 | ✅ 硬 |
| 演练时间有效 | 演练日期 ≤ 30 天前 | 元数据检查 | ✅ 硬 |
| 回滚耗时 | ≤ 15 分钟 | 演练记录 | ✅ 硬 |
| 核心恢复 | 回滚后健康检查 all-green | 演练记录 | ✅ 硬 |
| 回滚负责人 | 指定 owner | 演练记录 | ✅ 硬 |

## 触发阈值
| 场景 | 阈值 | 动作 |
|---|---|---|
| 演练过期 | >30 天 | 强制 No-Go + 安排演练 |
| 演练失败 | 回滚 >15min | 强制 No-Go + 修复流程 |
| 核心未恢复 | 健康检查 red | 强制 No-Go + 诊断 |

## 机检命令
```bash
# 检查演练记录存在且非空
test -s "Project_Docs/Re_Step/Artifacts/Step15/04_Incident_Drill_Evidence.md"

# 检查回滚耗时记录
grep -q "11 分钟" "Project_Docs/Re_Step/Artifacts/Step15/04_Incident_Drill_Evidence.md"

# 检查健康检查结果
grep -q "all-green" "Project_Docs/Re_Step/Artifacts/Step15/04_Incident_Drill_Evidence.md"
```

## 当前状态
- 最近演练: 2026-02-15 (Step-15 F2)
- 回滚耗时: 11 分钟 ✅
- 核心恢复: all-green ✅
- 负责人: Solo Maintainer ✅
- 窗口有效: ✅ (距今 <1 天)

## 差距
| ID | 差距 | 严重度 |
|---|---|---|
| RBK-01 | 无自动窗口检查脚本 | Medium |
| RBK-02 | 演练日期未从元数据自动提取 | Low |
