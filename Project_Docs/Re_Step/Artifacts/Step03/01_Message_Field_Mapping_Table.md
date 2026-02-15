# Step-03 B1 Message Field Mapping Table

- RunID: `20260215T080030Z`
- 格式：每条结论均包含“合同条款 -> 当前实现位点 -> 差距 -> 修复动作 -> 验收方式 -> 证据路径”。

| 结论ID | 合同条款 | 当前实现位点 | 差距 | 修复动作 | 验收方式 | 证据路径 |
| --- | --- | --- | --- | --- | --- | --- |
| FLD-001 | BRIDGE_MESSAGE_CONTRACT 2/3：`proxy_response` 兼容 `result`，并行结构化错误字段 | `MapBot_Reforged/src/main/java/com/mapbot/network/BridgeClient.java` `sendProxyResponse` | 原实现仅 `result`，缺少 `errorCode/rawError/retryable/mappingConflict` | `sendProxyResponse` 引入 `BridgeErrorMapper.map(...)`，错误场景并行输出结构化字段 | `rg -n "errorCode|rawError|mappingConflict|retryable"` 命中 Reforged `BridgeClient` | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate01_structured_fields.log` |
| FLD-002 | BRIDGE_MESSAGE_CONTRACT 4：`register_ack` 失败必须可机判 | `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/BridgeServer.java` + `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/BridgeErrorMapper.java` | 原失败回包主要依赖字符串 `error` | `register_ack` 失败统一走 `registerAckFailurePayload`，输出 `errorCode/rawError/retryable/mappingConflict` | `rg -n "BRG_VALIDATION_201|BRG_AUTH_101"` 命中首帧/鉴权拒绝路径 | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate03_required_codes.log` |
| FLD-003 | BRIDGE_MESSAGE_CONTRACT 3：`file_response` 失败语义稳定化 | `MapBot_Reforged/src/main/java/com/mapbot/network/BridgeHandlers.java` `buildFileErrorResponse` | 原 `file_response` 错误返回散落且非结构化 | 文件失败路径统一构造成 `error+errorCode+rawError+retryable+mappingConflict` | `rg -n "errorCode|rawError|mappingConflict|retryable"` 命中 `BridgeHandlers`/Alpha 接收链路 | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate01_structured_fields.log` |
| FLD-004 | BRIDGE_MESSAGE_CONTRACT 9：双端大小门禁一致 | Alpha: `BridgeProxy.java` `BridgeFileProxy.java` `BridgeServer.java`; Reforged: `BridgeClient.java` `BridgeHandlers.java` | 历史 Reforged 口径与合同不一致 | 统一 64KiB 单帧 + 46KiB base64 原始载荷门禁，发送前预检 + 接收端拒绝 | `rg -n "BRG_VALIDATION_205"` 命中 Alpha/Reforged 关键拒绝链路 | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate03_required_codes.log` |
