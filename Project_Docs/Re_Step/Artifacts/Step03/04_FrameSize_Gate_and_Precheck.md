# Step-03 B1 Frame Size Gate and Precheck

- RunID: `20260215T080030Z`

| 结论ID | 合同条款 | 当前实现位点 | 差距 | 修复动作 | 验收方式 | 证据路径 |
| --- | --- | --- | --- | --- | --- | --- |
| FS-001 | BRIDGE_MESSAGE_CONTRACT 9.1：单帧 <= 64KiB | `MapBot_Reforged/src/main/java/com/mapbot/network/BridgeClient.java` `send` | 历史发送前缺统一门禁 | `send` 增加 `isFrameTooLarge` 预检，超限拒绝发送并回 `BRG_VALIDATION_205` | `rg -n "isFrameTooLarge|BRG_VALIDATION_205"` 命中 `BridgeClient.send` | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate03_required_codes.log` |
| FS-002 | BRIDGE_MESSAGE_CONTRACT 9.1：接收端拒绝超限帧 | `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/BridgeServer.java` `BridgeFrameSizeGuardHandler` | 历史解码超限回包结构化不足 | 捕获 `TooLongFrameException` 后结构化拒绝，错误码 `BRG_VALIDATION_205` | `rg -n "BridgeFrameSizeGuardHandler|BRG_VALIDATION_205"` 命中 Alpha 拒绝逻辑 | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate03_required_codes.log` |
| FS-003 | BRIDGE_MESSAGE_CONTRACT 9.2：base64 原始载荷 <= 46KiB | Alpha `BridgeFileProxy.java`; Reforged `BridgeHandlers.java` + `BridgeClient.getBridgeBase64RawMaxBytes` | 历史按旧阈值或无统一阈值 | 双端都增加 46KiB 原始载荷预检，超限映射 `BRG_VALIDATION_205` | `rg -n "BASE64_RAW_MAX_BYTES|getBridgeBase64RawMaxBytes|base64_raw_size_exceeded"` 命中关键链路 | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate01_structured_fields.log` |
| FS-004 | BRIDGE_MESSAGE_CONTRACT 9.3：超限错误码统一 | Alpha/Reforged Bridge 关键链路 | 历史主返回路径含旧字符串语义 | 主语义统一 `BRG_VALIDATION_205`，旧字符串仅保留在兼容映射器 | `gate02_legacy_size_literals.exit=1`（关键链路无旧字符串主返回） | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate02_legacy_size_literals.log` |
