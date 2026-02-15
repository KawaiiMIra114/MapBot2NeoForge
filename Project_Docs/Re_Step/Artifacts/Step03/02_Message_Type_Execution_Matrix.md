# Step-03 B1 Message Type Execution Matrix

- RunID: `20260215T080030Z`

## 主链路
| 结论ID | 合同条款 | 当前实现位点 | 差距 | 修复动作 | 验收方式 | 证据路径 |
| --- | --- | --- | --- | --- | --- | --- |
| MTX-001 | BRIDGE_MESSAGE_CONTRACT 4：首帧必须 `register` | `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/BridgeServer.java` `BridgeRegistrationAuthHandler` | 历史失败场景结构化字段不足 | 首帧非 `register` 统一拒绝为 `BRG_VALIDATION_201`，并断连 | `rg -n "BRG_VALIDATION_201"` 命中首帧拒绝逻辑 | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate03_required_codes.log` |
| MTX-002 | BRIDGE_MESSAGE_CONTRACT 4：注册鉴权失败拒绝 | 同文件 `reject(... unauthorized ...)` | 历史 unauthorized 仅文本语义 | unauthorized 固定映射 `BRG_AUTH_101`，`register_ack` 输出结构化字段 | `rg -n "BRG_AUTH_101"` 命中注册拒绝路径 | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate03_required_codes.log` |
| MTX-003 | BRIDGE_MESSAGE_CONTRACT 3：`proxy_response` 兼容+结构化并行 | `MapBot_Reforged/src/main/java/com/mapbot/network/BridgeClient.java` `sendProxyResponse` | 错误场景仅字符串 `result` | 保留 `result` 兼容字段并追加 `errorCode/rawError/retryable/mappingConflict` | `rg -n "errorCode|rawError|mappingConflict|retryable"` 命中 `BridgeClient` | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate01_structured_fields.log` |
| MTX-004 | BRIDGE_MESSAGE_CONTRACT 3/9：`file_response` 错误统一与门禁化 | `MapBot_Reforged/src/main/java/com/mapbot/network/BridgeHandlers.java` | 原文件错误分散，超限语义不一致 | 引入 `buildFileErrorResponse` + `sendFileResponse`，统一错误结构和超限回退 | `rg -n "BRG_VALIDATION_205"` 命中 `BridgeHandlers` 关键拒绝分支 | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate03_required_codes.log` |

## 强制反例
| 反例ID | 合同条款 | 当前实现位点 | 差距 | 修复动作 | 验收方式 | 证据路径 |
| --- | --- | --- | --- | --- | --- | --- |
| CE-01 首帧不是 register | BRIDGE_MESSAGE_CONTRACT 4 + BRIDGE_ERROR_CODE_CONTRACT 3 | `BridgeServer.java` | 历史以文本错误为主 | 失败码固定为 `BRG_VALIDATION_201`，并断连 | `rg -n "BRG_VALIDATION_201"` 命中拒绝路径 | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate03_required_codes.log` |
| CE-02 `errorCode` 与 `rawError` 映射冲突 | BRIDGE_ERROR_CODE_CONTRACT 4 | Alpha/Reforged `BridgeErrorMapper.java` | 原实现无冲突标记位 | 冲突时保留显式 `errorCode` 并输出 `mappingConflict=true` | `rg -n "mappingConflict"` 命中 mapper 与回包构造点 | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate01_structured_fields.log` |
| CE-03 payload 超 64KiB | BRIDGE_MESSAGE_CONTRACT 9.1/9.3 | Reforged `BridgeClient.send` + Alpha `BridgeFrameSizeGuardHandler` | 历史存在发送/接收口径偏差 | 发送前预检拒绝 + 接收端拒绝统一 `BRG_VALIDATION_205` | `rg -n "BRG_VALIDATION_205"` 命中双方关键链路 | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate03_required_codes.log` |
| CE-04 变更型请求被隐式自动重试 | BRIDGE_MESSAGE_CONTRACT 6 | `BridgeClient.requestAlpha` + `BridgeHandlers` 变更型处理分支 | 历史无显式门禁描述 | 保持 mutation 无自动重试，发送失败直接返回 `FAIL:BRG_VALIDATION_205` | 代码检索无 mutation 自动重放循环且发送失败即时返回 | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate03_required_codes.log` |
