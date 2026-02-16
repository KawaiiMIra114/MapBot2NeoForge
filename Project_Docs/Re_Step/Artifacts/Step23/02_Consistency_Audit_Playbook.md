# 02_Consistency_Audit_Playbook — 合同一致性巡检手册

## 文档元数据
| 字段 | 值 |
|---|---|
| DocID | AUDIT-J2-001 |
| Version | 1.0.0 |
| Owner | Solo Maintainer |
| Last Updated | 2026-02-16 |

## 1. 巡检范围

### 1.1 三角对账模型
```
合同 (Contracts/*.md)
   ↕
实现 (src/**/*.java)
   ↕
手册 (Manuals/*.md)
```

每次巡检必须覆盖：合同 ↔ 实现、实现 ↔ 手册、合同 ↔ 手册。

### 1.2 巡检对象

| 合同 | 实现入口 | 关联手册 |
|---|---|---|
| COMMAND_AUTHORIZATION_CONTRACT | CommandRegistry, Permission checks | RELEASE_CHECKLIST |
| CONFIG_SCHEMA_CONTRACT | config.json schema validation | UPGRADE_MIGRATION_GUIDE |
| DATA_CONSISTENCY_CONTRACT | DataManager, CAS logic | INCIDENT_RESPONSE_PLAYBOOK |
| OBSERVABILITY_SLO_CONTRACT | Metrics, Alerts | INCIDENT_RESPONSE_PLAYBOOK |
| PRIVACY_POLICY | Data handling, logging | SECURITY.md |

## 2. 巡检流程

### 2.1 触发条件
- EVT-01: 重构步骤执行中 (每步)
- EVT-02: 功能变更后 (合并到 main)
- EVT-06: 定期巡检 (每月)

### 2.2 执行步骤
1. **读取合同** → 提取强制规则列表
2. **扫描实现** → 检查每条规则的代码覆盖
3. **扫描手册** → 检查操作手册与合同的一致性
4. **生成差异报告** → 标注漂移项 + 风险分级

### 2.3 输出格式

| 漂移项 | 被检对象 | 合同条款 | 实际状态 | 风险级别 | 修复期限 |
|---|---|---|---|---|---|
| D-xxx | 文件名 | 条款引用 | 偏离描述 | P0/P1/P2 | 截止日 |

### 2.4 阻断规则
- **P0 漂移**: 立即阻断发布，必须在当前迭代修复
- **P1 漂移**: 标记为 Fix Action，下一迭代修复
- **P2 漂移**: 记录为 Gap，纳入 backlog

## 3. 机检辅助命令

```bash
# 检查合同文件是否被引用
rg -l "CONTRACT" Project_Docs/Contracts/*.md

# 搜索实现中的合同引用
rg -rn "Authorization|Schema|Consistency|SLO" src/ --include="*.java"

# 验证手册链接可达性
for f in Project_Docs/Manuals/*.md; do test -s "$f" || echo "MISSING: $f"; done
```

## 4. 闭环机制
1. 漂移发现 → 创建 Fix Action (带截止日)
2. Fix Action 到期 → 复检 → 通过/升级
3. 连续 2 次未修复 → 升级为 P0 阻断
