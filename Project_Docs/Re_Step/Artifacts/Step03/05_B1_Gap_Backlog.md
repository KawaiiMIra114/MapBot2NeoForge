# Step-03 B1 Gap Backlog (After Fix)

- RunID: `20260215T080030Z`
- P0 未闭环项：0

| Backlog ID | 状态 | 合同条款 | 当前实现位点 | 差距 | 修复动作 | 验收方式 | 证据路径 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| GAP-001 | CLOSED | BRIDGE_ERROR_CODE_CONTRACT 4（双栈优先级） | Alpha/Reforged `BridgeErrorMapper` + 回包构造点 | 历史无统一结构化错误字段 | 已统一输出 `errorCode/rawError/retryable/mappingConflict` | `gate01_structured_fields.exit=0` 且关键链路命中 | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate01_structured_fields.log` |
| GAP-002 | CLOSED | BRIDGE_MESSAGE_CONTRACT 9（64KiB/46KiB） | Alpha `BridgeProxy/BridgeFileProxy/BridgeServer`; Reforged `BridgeClient/BridgeHandlers` | 历史门禁口径不一致 | 已统一 64KiB 单帧+46KiB base64 原始载荷，发送前预检+接收拒绝 | `gate03_required_codes.exit=0` 且 `BRG_VALIDATION_205` 路径完整 | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate03_required_codes.log` |
| GAP-003 | CLOSED | BRIDGE_MESSAGE_CONTRACT 9.3（超限错误码） | Alpha/Reforged 关键拒绝路径 | 历史超限主返回语义分散 | 主语义统一为 `BRG_VALIDATION_205` | `gate02_legacy_size_literals.exit=1`（关键链路无旧字符串主返回） | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate02_legacy_size_literals.log` |
| GAP-004 | CLOSED | BRIDGE_MESSAGE_CONTRACT 4 + BRIDGE_ERROR_CODE_CONTRACT 3 | Alpha `BridgeServer.java` | register 失败结构化字段不足 | 首帧非 register -> `BRG_VALIDATION_201`; unauthorized -> `BRG_AUTH_101` | `gate03_required_codes.exit=0` | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate03_required_codes.log` |
| GAP-005 | OPEN-P1 | BRIDGE_MESSAGE_CONTRACT 7（idempotencyKey） | 双端 mutation 去重缓存 | 合同预留项未在 B1 落地 | 列入 B3 实施，不阻断 B2 | B3 增加去重缓存与反向测试 | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate03_required_codes.log` |

## 准入结论
- 阻断级缺口（P0）：0
- B1 准入建议：GO B2
