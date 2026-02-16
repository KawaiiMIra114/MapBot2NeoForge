# 04_Reusable_Playbook_Summary — 可复用流程模板汇总

## 文档元数据
| 字段 | 值 |
|---|---|
| DocID | PLAYBOOK-J1-001 |
| Version | 1.0.0 |
| Owner | Solo Maintainer |
| Last Updated | 2026-02-16 |

## 1. 门禁检查模板

**模板名**: Gate Check Template
**触发条件**: 每个 Step 执行前/后
**最小输入**: RE_STEP 文件, Evidence 目录, CURRENT_STATE
**禁用条件**: 纯文档讨论、无代码变更的 Step

### 流程
1. gate01: 前置产物存在性检查
2. gate02: RE_STEP 章节完整性 (Contains check)
3. gate03: 术语一致性 ("自审+自记录")
4. gate04: 弱化语义清零 (含 FP 补偿)
5. validate_delivery.py --phase precommit
6. 提交 → gate10 回填
7. validate_delivery.py --phase postcommit

### 已知陷阱
- PowerShell `-match` 对 CJK 不可靠, 使用 `Contains()`
- validate_delivery.py 不输出 stdout, 需从 gate 文件汇总
- 相对路径在 CWD 漂移时失效, 使用绝对路径

---

## 2. 事故复盘模板

**模板名**: Postmortem Template
**触发条件**: Sev-0/Sev-1 事故后
**最小输入**: 事故时间线, 影响范围, 根因
**禁用条件**: 非生产环境事故

### 流程
1. 时间线重建 (发现→止血→诊断→修复→验证)
2. 根因分析 (5-Why 或鱼骨图)
3. 防再发行动 (可执行, 有责任人+截止日)
4. 证据归档 → `Project_Docs/Reports/`

---

## 3. 迁移演练模板

**模板名**: Migration Drill Template
**触发条件**: 版本迁移前, 协议变更前
**最小输入**: UPGRADE_MIGRATION_GUIDE, 目标版本
**禁用条件**: 热修复 (patch-only)

### 流程
1. 预演 (staging 环境)
2. 数据备份验证
3. 执行迁移
4. 冒烟测试
5. 回滚测试 (验证回滚路径可达)
6. 记录 → Evidence

---

## 4. 灰度评估模板

**模板名**: Canary Evaluation Template
**触发条件**: 发布前, 关键链路改造后
**最小输入**: 基线指标, 变更范围, 灰度比例
**禁用条件**: 非生产部署

### 流程
1. 暗发布 → 小流量 (10%) → 中流量 (50%) → 全量
2. 每阶段: 对照基线, 差异超阈值则回滚
3. 观察窗口: 每阶段至少 1 小时
4. Go/No-Go 决策记录

---

## 5. 模板边界汇总

| 模板 | 可用场景 | 不可用场景 | 最小输入 |
|---|---|---|---|
| 门禁检查 | 每步交付 | 纯讨论 | RE_STEP + Evidence |
| 事故复盘 | 生产事故 | 非生产 | 事故时间线+影响 |
| 迁移演练 | 版本/协议迁移 | 热修复 | 迁移指南+目标版本 |
| 灰度评估 | 生产发布 | 非生产 | 基线+变更范围 |
