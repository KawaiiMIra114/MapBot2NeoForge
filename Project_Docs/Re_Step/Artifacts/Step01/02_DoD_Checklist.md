# Step-01 A1 DoD Checklist

## 判定公式
- `PASS = 所有 DOD 条目判定 = PASS（AND）`
- `FAIL = 任一 DOD 条目判定 = FAIL`

## 证据绑定规则（防伪与防过期）
- 每条 DoD 证据必须绑定：`commit SHA`、`CI Run ID/URL`、`artifact hash (SHA256)`、`timestamp`。
- 若 `commit SHA` 变化，则历史证据自动失效，必须重跑全部 Gate 并重采证据。

## DOD-001
- DOD-ID：DOD-001
- REQ-ID：REQ-001
- 维度：协议
- 验证方法：`rg -n "MUST|requestId|idempotencyKey|64 KiB|46 KiB|register" Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`
- 失败判据：任一关键词缺失，或无法定位状态机/超时/幂等条款。
- 依据路径：`Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`，`Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md`
- 期望结果：命令返回 `0` 且命中关键条款。
- 证据路径：`Project_Docs/Re_Step/Artifacts/Step01/Gate_Evidence/20260215_134455/gate_outputs.log`
- 证据元数据：`commit SHA=31a50b5a25371d4237d7fd51c67b4e319c875c00`，`CI Run ID/URL=N/A (local-run)`，`artifact hash=d9c297f21f45439f6978bc031634c23eacfd90c44516c432b51b73dfbd4b30ef`，`timestamp=2026-02-15T14:04:50+08:00`
- 判定：PASS

## DOD-002
- DOD-ID：DOD-002
- REQ-ID：REQ-002
- 维度：权限
- 验证方法：`rg -n "user|admin|owner|AUTH-403|最小授权|不得被解释为有效权限角色" Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md`
- 失败判据：角色集合非三角色闭集，或越权错误码不为 `AUTH-403`。
- 依据路径：`Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md`，`Project_Docs/Architecture/SECURITY_BOUNDARY.md`
- 期望结果：命令返回 `0`，命中角色闭集与越权规则条款。
- 证据路径：`Project_Docs/Re_Step/Artifacts/Step01/Gate_Evidence/20260215_134455/gate_outputs.log`
- 证据元数据：`commit SHA=31a50b5a25371d4237d7fd51c67b4e319c875c00`，`CI Run ID/URL=N/A (local-run)`，`artifact hash=d9c297f21f45439f6978bc031634c23eacfd90c44516c432b51b73dfbd4b30ef`，`timestamp=2026-02-15T14:04:50+08:00`
- 判定：PASS

## DOD-003
- DOD-ID：DOD-003
- REQ-ID：REQ-003
- 维度：线程
- 验证方法：`rg -n "Game Main Thread|server.execute|禁止在 IO Thread|Future#get|join|锁顺序" Project_Docs/Architecture/THREADING_MODEL.md`
- 失败判据：不存在主线程回切约束、阻塞禁令或锁顺序规则。
- 依据路径：`Project_Docs/Architecture/THREADING_MODEL.md`，`Project_Docs/Architecture/SYSTEM_CONTEXT.md`
- 期望结果：命令返回 `0`，并命中线程边界强制规则。
- 证据路径：`Project_Docs/Re_Step/Artifacts/Step01/Gate_Evidence/20260215_134455/gate_outputs.log`
- 证据元数据：`commit SHA=31a50b5a25371d4237d7fd51c67b4e319c875c00`，`CI Run ID/URL=N/A (local-run)`，`artifact hash=d9c297f21f45439f6978bc031634c23eacfd90c44516c432b51b73dfbd4b30ef`，`timestamp=2026-02-15T14:04:50+08:00`
- 判定：PASS

## DOD-004
- DOD-ID：DOD-004
- REQ-ID：REQ-004
- 维度：安全
- 验证方法：`rg -n "TLS|WSS|token|密钥轮换|回滚|禁止硬编码|最小暴露面" Project_Docs/Architecture/SECURITY_BOUNDARY.md`
- 失败判据：缺少 token 治理、传输安全或轮换回滚预算任一条款。
- 依据路径：`Project_Docs/Architecture/SECURITY_BOUNDARY.md`，`Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md`
- 期望结果：命令返回 `0` 且命中关键安全条款。
- 证据路径：`Project_Docs/Re_Step/Artifacts/Step01/Gate_Evidence/20260215_134455/gate_outputs.log`
- 证据元数据：`commit SHA=31a50b5a25371d4237d7fd51c67b4e319c875c00`，`CI Run ID/URL=N/A (local-run)`，`artifact hash=d9c297f21f45439f6978bc031634c23eacfd90c44516c432b51b73dfbd4b30ef`，`timestamp=2026-02-15T14:04:50+08:00`
- 判定：PASS

## DOD-005
- DOD-ID：DOD-005
- REQ-ID：REQ-005
- 维度：一致性
- 验证方法：`rg -n "entity_version|CONSISTENCY-409|回放|快照|自动回退" Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`
- 失败判据：冲突码不固定、回放隔离缺失或快照失败无自动回退。
- 依据路径：`Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md`，`Project_Docs/Architecture/FAILURE_MODEL.md`
- 期望结果：命令返回 `0` 且命中版本/冲突/恢复条款。
- 证据路径：`Project_Docs/Re_Step/Artifacts/Step01/Gate_Evidence/20260215_134455/gate_outputs.log`
- 证据元数据：`commit SHA=31a50b5a25371d4237d7fd51c67b4e319c875c00`，`CI Run ID/URL=N/A (local-run)`，`artifact hash=d9c297f21f45439f6978bc031634c23eacfd90c44516c432b51b73dfbd4b30ef`，`timestamp=2026-02-15T14:04:50+08:00`
- 判定：PASS

## DOD-006
- DOD-ID：DOD-006
- REQ-ID：REQ-006
- 维度：可观测
- 验证方法：`rg -n "SLO|Warning|Critical|RCA|auth_decision_latency_ms|config_reload_total" Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`
- 失败判据：缺少 SLO、告警阈值分级或 Critical RCA 条款。
- 依据路径：`Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md`，`Project_Docs/SYSTEM_CRITIQUE_AND_REFACTOR_PAPER_V1.md`
- 期望结果：命令返回 `0` 且命中指标与分级条款。
- 证据路径：`Project_Docs/Re_Step/Artifacts/Step01/Gate_Evidence/20260215_134455/gate_outputs.log`
- 证据元数据：`commit SHA=31a50b5a25371d4237d7fd51c67b4e319c875c00`，`CI Run ID/URL=N/A (local-run)`，`artifact hash=d9c297f21f45439f6978bc031634c23eacfd90c44516c432b51b73dfbd4b30ef`，`timestamp=2026-02-15T14:04:50+08:00`
- 判定：PASS

## DOD-007
- DOD-ID：DOD-007
- REQ-ID：REQ-007
- 维度：运维
- 验证方法：`rg -n "热重载|原子替换|整体回滚|schema.version|dry-run" Project_Docs/Contracts/CONFIG_SCHEMA_CONTRACT.md`
- 失败判据：配置变更无法实现失败整体回滚，或迁移不可回放验证。
- 依据路径：`Project_Docs/Contracts/CONFIG_SCHEMA_CONTRACT.md`，`Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`
- 期望结果：命令返回 `0` 并命中热重载回滚条款。
- 证据路径：`Project_Docs/Re_Step/Artifacts/Step01/Gate_Evidence/20260215_134455/gate_outputs.log`
- 证据元数据：`commit SHA=31a50b5a25371d4237d7fd51c67b4e319c875c00`，`CI Run ID/URL=N/A (local-run)`，`artifact hash=d9c297f21f45439f6978bc031634c23eacfd90c44516c432b51b73dfbd4b30ef`，`timestamp=2026-02-15T14:04:50+08:00`
- 判定：PASS

## DOD-008
- DOD-ID：DOD-008
- REQ-ID：REQ-008
- 维度：回滚
- 验证方法：`rg -n "回滚|灰度|N\+1|N\+2|2 分钟内完成回滚|恢复" Project_Docs/Architecture/VERSIONING_AND_COMPATIBILITY.md Project_Docs/Architecture/FAILURE_MODEL.md`
- 失败判据：不存在回滚触发阈值、回退动作或恢复完成判据。
- 依据路径：`Project_Docs/Architecture/VERSIONING_AND_COMPATIBILITY.md`，`Project_Docs/Architecture/FAILURE_MODEL.md`
- 期望结果：命令返回 `0` 且命中灰度/回滚阈值条款。
- 证据路径：`Project_Docs/Re_Step/Artifacts/Step01/Gate_Evidence/20260215_134455/gate_outputs.log`
- 证据元数据：`commit SHA=31a50b5a25371d4237d7fd51c67b4e319c875c00`，`CI Run ID/URL=N/A (local-run)`，`artifact hash=d9c297f21f45439f6978bc031634c23eacfd90c44516c432b51b73dfbd4b30ef`，`timestamp=2026-02-15T14:04:50+08:00`
- 判定：PASS

## DOD-009
- DOD-ID：DOD-009
- REQ-ID：REQ-009
- 维度：测试（含反向测试）
- 验证方法：`rg -n "契约测试|故障注入|断连|乱序|超时|重放|发布门禁" Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`
- 失败判据：缺少契约测试矩阵或缺少断连/超时/重放/冲突等负向场景。
- 依据路径：`Project_Docs/SYSTEM_REFACTOR_DEVELOPMENT_TASKLIST.md`，`Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`
- 期望结果：命令返回 `0` 且命中测试矩阵与负向场景条款。
- 证据路径：`Project_Docs/Re_Step/Artifacts/Step01/Gate_Evidence/20260215_134455/gate_outputs.log`
- 证据元数据：`commit SHA=31a50b5a25371d4237d7fd51c67b4e319c875c00`，`CI Run ID/URL=N/A (local-run)`，`artifact hash=d9c297f21f45439f6978bc031634c23eacfd90c44516c432b51b73dfbd4b30ef`，`timestamp=2026-02-15T14:04:50+08:00`
- 判定：PASS
