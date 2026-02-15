# Contract Quick Index

用途：为 Step04/B2 交接提供“合同文档 -> 作用 -> 实现域 -> 证据入口”的快速对照。

## 核心合同总览

| 合同文档 | 主要用途 | 对应实现域（代码/流程） | 证据建议路径 |
| --- | --- | --- | --- |
| `Project_Docs/Contracts/CONTRACT_INDEX.md` | 合同主索引与变更门禁，定义合同优先规则与注册表 | 文档治理与发布门禁；`Re_Step` 阶段 Gate | `Project_Docs/Re_Step/Artifacts/Step01/03_Change_Gate_Rules.md` |
| `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md` | 约束 Bridge 消息字段、状态机、重试与大小上限 | Alpha Bridge 通道、Reforged 网络层 | `Project_Docs/Re_Step/Artifacts/Step03/01_Message_Field_Mapping_Table.md` |
| `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md` | 统一错误码分层、语义与双栈映射 | Bridge 错误返回链路、客户端/服务端错误处理 | `Project_Docs/Re_Step/Artifacts/Step03/03_Error_Code_DualStack_Mapping.md` |
| `Project_Docs/Contracts/COMMAND_AUTHORIZATION_CONTRACT.md` | 统一命令授权模型（三角色、最小权限、越权拒绝、审计） | 聊天命令注册与分发、HTTP API 授权、权限策略管理 | `Project_Docs/Re_Step/Artifacts/Step04/01_Command_Authorization_Matrix.md` |
| `Project_Docs/Contracts/CONFIG_SCHEMA_CONTRACT.md` | 约束配置 schema、类型范围、热重载、回滚、迁移 | `AlphaConfig` 加载/校验、`ReloadCommand`、配置持久化与审计 | `Project_Docs/Re_Step/Artifacts/Step04/03_Config_Schema_Validation_Profile.md` |
| `Project_Docs/Contracts/DATA_CONSISTENCY_CONTRACT.md` | 约束权限/配置/快照在写入、回放、恢复的一致性边界 | 快照与回放、数据存储一致性、恢复流程 | `Project_Docs/Re_Step/RE_STEP_05_B3_一致性与SLO契约映射.md` |
| `Project_Docs/Contracts/OBSERVABILITY_SLO_CONTRACT.md` | 约束指标、SLO、告警阈值、RCA 与最小证据集 | 指标采集、告警规则、事件响应与RCA流程 | `Project_Docs/Re_Step/RE_STEP_14_F1_可观测与告警落地.md` |

## Step04 重点合同（B2 范围）

| 合同 | Step04 关注点 | 关键实现域 | 证据建议路径 |
| --- | --- | --- | --- |
| `COMMAND_AUTHORIZATION_CONTRACT.md` | 角色收敛到 `user/admin/owner`；越权固定 `AUTH-403`；拒绝无副作用 | `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/command/`、`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/network/HttpRequestDispatcher.java` | `Project_Docs/Re_Step/Artifacts/Step04/01_Command_Authorization_Matrix.md` |
| `CONFIG_SCHEMA_CONTRACT.md` | `schema.version`、未知键 fail-closed、热重载失败整体回滚、最小审计字段 | `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/config/AlphaConfig.java`、`Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/command/impl/ReloadCommand.java` | `Project_Docs/Re_Step/Artifacts/Step04/03_Config_Schema_Validation_Profile.md` |
| `OBSERVABILITY_SLO_CONTRACT.md` | 指标口径与阈值可机检；Critical 事件RCA闭环；排障最小证据集可回放 | `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/metrics/`、运维告警与手册流程 | `Project_Docs/Manuals/OPERATIONS_RUNBOOK.md` |

## 交接执行顺序（建议）

1. 先读 `Project_Docs/Contracts/CONTRACT_INDEX.md`，确认 SoT 与版本口径。
2. 按 B2 三合同顺序核对：授权 -> 配置 -> 可观测。
3. 每一条核对项都回填证据到 `Project_Docs/Memory_KB/04_Evidence/EVIDENCE_MAP.md` 的最新批次目录。
