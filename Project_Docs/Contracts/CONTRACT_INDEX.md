# 合同索引（Contract Index）

## Metadata
| Field | Value |
| --- | --- |
| DocID | MB2N-CONTRACT-INDEX |
| Version | 1.1.0 |
| Status | Active |
| Owner | A1 合同-协议层（知识库搭建） |
| Reviewers | Alpha Core Maintainer, Reforged Maintainer, Bridge QA |
| Source of Truth | `Project_Docs/Contracts/CONTRACT_INDEX.md` |
| Last Updated | 2026-02-14 |
| Related Docs | `Project_Docs/CURRENT_STATUS.md`, `Project_Docs/Reports/025_P0_P4_Followup_Report.md`, `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md`, `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md` |
| Change Impact | 建立并持续维护 Bridge 协议治理基线；不直接变更运行时代码；后续协议改动必须先改本目录合同再改实现。 |

## Purpose
建立 Alpha + Reforged + Bridge 的合同入口，明确协议与错误语义的唯一索引、版本关系和验证门禁，避免“代码先行、语义漂移”。

## Scope
- 适用对象：`Mapbot-Alpha-V1` 与 `MapBot_Reforged` 之间的 Bridge 控制面消息。
- 适用文档：本索引与以下子合同。
- 非适用对象：OneBot v11 对外协议细节、Web 控制台 UI 文案、业务命令说明书。

## Definitions
- 合同（Contract）：可执行、可验证、可审计的规范文档，定义跨模块交互语义。
- 规范性规则（Normative Rule）：使用 `MUST/SHOULD/MAY` 的约束语句。
- SoT（Source of Truth）：冲突时优先级最高的定义来源。
- 兼容窗口：旧字段/旧行为允许并存的版本区间。

## Normative Rules (MUST/SHOULD/MAY)
- `MUST` 所有 Bridge 相关协议变更先更新本索引与对应子合同，再提交代码实现。
- `MUST` 每个合同文档包含统一元数据字段：`DocID/Version/Status/Owner/Reviewers/Source of Truth/Last Updated/Related Docs/Change Impact`。
- `MUST` 每个合同文档包含固定章节：`Purpose/Scope/Definitions/Normative Rules/Non-goals/Risks and Failure Modes/Verification and Acceptance Criteria/Change Log`。
- `MUST` 任何新增消息类型或错误码在发布前进入合同注册表并分配版本。
- `SHOULD` 合同版本采用语义化版本：破坏性变更升主版本、向后兼容新增升次版本、文字澄清升补丁版本。
- `SHOULD` 合同规则与代码常量保持可追溯（可通过 `rg` 定位到实现）。
- `MAY` 在不破坏既有行为的前提下增加扩展字段，但必须在合同中声明兼容策略。

### 合同注册表
| DocID | Document | Version | Status | SoT |
| --- | --- | --- | --- | --- |
| MB2N-CONTRACT-INDEX | `Project_Docs/Contracts/CONTRACT_INDEX.md` | 1.1.0 | Active | 本文件 |
| MB2N-BRIDGE-MESSAGE | `Project_Docs/Contracts/BRIDGE_MESSAGE_CONTRACT.md` | 1.1.0 | Active | 消息包、状态机、超时/重试/幂等规范 |
| MB2N-BRIDGE-ERROR-CODE | `Project_Docs/Contracts/BRIDGE_ERROR_CODE_CONTRACT.md` | 1.1.0 | Active | 错误分层、命名、用户文案、审计字段 |

## Non-goals
- 不替代业务功能设计文档（如签到/发奖业务流程）。
- 不定义非 Bridge 通道（如 OneBot WS）的底层协议标准。
- 不在本索引内维护代码级参数（具体常量由子合同引用代码 SoT）。

## Risks and Failure Modes
- 合同与代码脱节：规则更新未同步到实现，导致“文档正确、系统错误”。
- 多代理并行改动：未对齐合同版本，造成消息语义分叉。
- 口径冲突：历史文档仍被引用，覆盖当前 Alpha + Reforged 双端事实。

## Verification and Acceptance Criteria
- 验证项 CI-1（结构完整性）：
  - 对本目录合同文件执行章节与元数据检查，缺项即失败。
  - 建议命令：`rg -n "^## (Purpose|Scope|Definitions|Normative Rules \(MUST/SHOULD/MAY\)|Non-goals|Risks and Failure Modes|Verification and Acceptance Criteria|Change Log)$" Project_Docs/Contracts/*CONTRACT*.md`。
- 验证项 CI-2（注册一致性）：
  - 注册表中每个文档路径必须存在，且 `DocID` 唯一。
- 验证项 CI-3（代码可追溯）：
  - `BRIDGE_MESSAGE_CONTRACT` 与 `BRIDGE_ERROR_CODE_CONTRACT` 中声明的关键规则可在 `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge` 与 `MapBot_Reforged/src/main/java/com/mapbot/network` 中定位实现或定位缺口。
- 验收标准：三项全部通过，且评审人确认无冲突 SoT。

## Change Log
| Date | Version | Change | Author |
| --- | --- | --- | --- |
| 2026-02-14 | 1.1.0 | 同步子合同升级到 v1.1.0（字段实现状态、大小合同、错误双栈退场计划）。 | A1 合同-协议层 |
| 2026-02-14 | 1.0.0 | 首次建立合同索引与注册表。 | A1 合同-协议层 |
