# Step-01 A1 Refactor Charter

## 1. 目标
在任何代码重构前冻结范围、非目标、DoD 与门禁规则，建立可审计、可机检、可追溯基线；执行语义固定为单人维护（自审+自记录）。

## 2. 需求编号（REQ-ID）
- REQ-001：协议合同优先（Bridge 消息、错误码、兼容窗口）。
- REQ-002：权限三角色与越权拒绝规则固定。
- REQ-003：线程边界与跨线程访问禁止模式固定。
- REQ-004：安全边界、token 治理、TLS 与轮换回滚点固定。
- REQ-005：一致性（版本、冲突、回放、恢复）规则固定。
- REQ-006：可观测指标、SLO、告警与 RCA 义务固定。
- REQ-007：运维配置（schema、热重载、迁移）失败回滚固定。
- REQ-008：版本兼容、弃用窗口、灰度回滚规则固定。
- REQ-009：测试与反向测试门禁固定（含断连/超时/重放/冲突）。

## 3. 范围冻结矩阵
| REQ-ID | 条目 | 冻结决策 | 理由 | 重纳入触发条件 |
| --- | --- | --- | --- | --- |
| REQ-001 | Bridge 消息结构、状态机、超时/重试、幂等键、错误码分层 | 纳入 | 后续实现与回滚的协议基线 | `BRIDGE_MESSAGE_CONTRACT` 或 `BRIDGE_ERROR_CODE_CONTRACT` 版本变更 |
| REQ-002 | 权限角色、最小授权、越权拒绝与审计 | 纳入 | 安全与越权风险核心边界 | `COMMAND_AUTHORIZATION_CONTRACT` 角色模型变更 |
| REQ-003 | 线程归属、跨线程约束、黑名单模式 | 纳入 | 竞态/死锁/越界写入高风险项 | `THREADING_MODEL.md` 强制规则更新 |
| REQ-004 | token 治理、TLS、轮换 SOP 与回滚预算 | 纳入 | 直接影响发布安全 | `SECURITY_BOUNDARY.md` 轮换流程或预算变更 |
| REQ-005 | entity_version、冲突码、回放与快照恢复 | 纳入 | 保证数据可恢复与可信 | `DATA_CONSISTENCY_CONTRACT` 一致性条款变更 |
| REQ-006 | 指标、SLO、告警阈值、RCA | 纳入 | Gate 判定依赖统一可观测口径 | `OBSERVABILITY_SLO_CONTRACT` 阈值或分级变更 |
| REQ-007 | schema.version、热重载、迁移、失败回滚 | 纳入 | 运维改动高频且风险集中 | `CONFIG_SCHEMA_CONTRACT` 关键键或迁移规则变更 |
| REQ-008 | semver、弃用窗口、灰度与回滚门禁 | 纳入 | 升级/回退可执行性基础 | `VERSIONING_AND_COMPATIBILITY.md` 兼容策略变更 |
| REQ-009 | 契约测试、故障注入、反向测试基线 | 纳入 | 防止“仅正向通过”的假阳性 | `SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md` 测试门禁变更 |
| NREQ-001 | 新业务功能开发 | 不纳入 | A1 只冻结治理基线 | 进入 E 阶段且 A1~C2 完成 |
| NREQ-002 | UI/交互重做 | 不纳入 | 非 A1 必要路径 | F 阶段开始并补充设计任务单 |
| NREQ-003 | 超 SLO 的极限性能优化 | 延后 | 当前优先正确性与回滚 | G 阶段测试体系稳定后 |
| NREQ-004 | 多方签核流程引入 | 不纳入 | 当前约束为单人维护 | 发布治理模型升级文档后 |

## 4. 非目标清单（>=5）
1. 不修改生产代码。
2. 不新增业务命令。
3. 不替换存储中间件或事实源实现。
4. 不变更生产网络拓扑与端口映射。
5. 不进行跨仓依赖升级。
6. 不引入多方签核工作流。

## 5. 禁止行为清单（>=5，可验证）
1. 禁止“多方签核/待外部签核”语义。验证：`rg -n "多方签核|待外部签核" Project_Docs/Re_Step/Artifacts/Step01/0*.md` 无命中。
2. 禁止模糊词（尽量/基本/适当）出现在 DoD 与 Gate。验证：`rg -n "尽量|基本|适当" Project_Docs/Re_Step/Artifacts/Step01/0*.md` 无命中。
3. 禁止 DoD 缺少“验证方法+失败判据+依据路径”。验证：`02_DoD_Checklist.md` 每条三字段齐全。
4. 禁止 Gate 缺少“检查命令+阻断动作+恢复动作”。验证：`03_Change_Gate_Rules.md` 每个 Gate 三字段齐全。
5. 禁止跳过输入门禁。验证：`Gate_Evidence/*/gate_inputs.exit` 存在且值为 `0`。
6. 禁止修复后只重跑单个 Gate。验证：`03_Change_Gate_Rules.md` 明确“修复后重跑 Gate-1~Gate-4”。

## 6. 假设
1. 强制输入 13 份文档为当前基线。
2. A1 仅定义治理与门禁，不替代代码实现验证。
3. 合同升级将触发 Step-01 重新冻结。

## 7. 约束
1. 写入范围仅限 Step01 四个主文件与 Gate_Evidence。
2. 判定逻辑使用硬门槛交集：`PASS = 全部 BLOCKER 通过`。
3. 自审机制固定为单人闭环，不设多方签核节点。

## 8. 退出条件（Exit Criteria）
1. 四份文档齐备并通过产物门禁。
2. 反证审查 >=3 轮且风险闭环可量化、可验证、可追溯。
3. 门禁 stdout/stderr 与 exit code 全部落盘。
4. 自审日志至少 1 条并含日期、变更原因、证据路径、自审结论、待决问题、风险级别。

## 9. 依据路径
- 依据路径：`Project_Docs/SYSTEM_CRITIQUE_AND_REFACTOR_PAPER_V1.md`
- 依据路径：`Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`
- 依据路径：`Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`
- 依据路径：`Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`
- 依据路径：`Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md`
- 依据路径：`Project_Docs/Contracts/CONFIG_SCHEMA_CONTRACT.md`
- 依据路径：`Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`
- 依据路径：`Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
- 依据路径：`Project_Docs/Architecture/SYSTEM_CONTEXT.md`
- 依据路径：`Project_Docs/Architecture/THREADING_MODEL.md`
- 依据路径：`Project_Docs/Architecture/FAILURE_MODEL.md`
- 依据路径：`Project_Docs/Architecture/SECURITY_BOUNDARY.md`
- 依据路径：`Project_Docs/Architecture/VERSIONING_AND_COMPATIBILITY.md`
