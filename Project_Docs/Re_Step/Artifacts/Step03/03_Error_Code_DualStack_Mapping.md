# Step-03 B1 Error Code Dual-Stack Mapping

- RunID: `20260215T080030Z`

| 结论ID | 合同条款 | 当前实现位点 | 差距 | 修复动作 | 验收方式 | 证据路径 |
| --- | --- | --- | --- | --- | --- | --- |
| ERR-001 | BRIDGE_ERROR_CODE_CONTRACT 4：优先级 `errorCode > 字符串映射 > BRG_INTERNAL_999` | `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/BridgeErrorMapper.java`; `MapBot_Reforged/src/main/java/com/mapbot/network/BridgeErrorMapper.java` | 历史实现不统一 | 双端统一 `map()` 优先级与 fallback 逻辑 | `rg -n "BRG_INTERNAL_999|map\("` 命中双端 mapper | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate01_structured_fields.log` |
| ERR-002 | BRIDGE_ERROR_CODE_CONTRACT 4：冲突必须标记 `mappingConflict` | 同上 + `BridgeClient.sendProxyResponse` + `BridgeHandlers.buildFileErrorResponse` | 历史回包无冲突位 | 回包统一附加 `mappingConflict` | `rg -n "mappingConflict"` 命中 mapper 与回包构造 | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate01_structured_fields.log` |
| ERR-003 | BRIDGE_ERROR_CODE_CONTRACT 3：`unauthorized -> BRG_AUTH_101` | `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/BridgeServer.java` | 历史注册拒绝主语义偏字符串 | unauthorized 拒绝路径显式使用 `BRG_AUTH_101` | `rg -n "BRG_AUTH_101"` 命中关键拒绝路径 | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate03_required_codes.log` |
| ERR-004 | BRIDGE_ERROR_CODE_CONTRACT 3：`register_required -> BRG_VALIDATION_201` | `BridgeServer.java` 首帧校验拒绝 | 历史无统一机器语义 | 首帧非 register 显式使用 `BRG_VALIDATION_201` | `rg -n "BRG_VALIDATION_201"` 命中关键拒绝路径 | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate03_required_codes.log` |
| ERR-005 | BRIDGE_ERROR_CODE_CONTRACT 3 + BRIDGE_MESSAGE_CONTRACT 9.3：超限统一 `BRG_VALIDATION_205` | Alpha `BridgeProxy/BridgeFileProxy/BridgeServer`; Reforged `BridgeClient/BridgeHandlers` | 历史超限字符串分散 | 所有超限主语义统一到 `BRG_VALIDATION_205`，字符串仅保兼容映射 | `gate03_required_codes.exit=0` 且 `gate02_legacy_size_literals.exit=1` | `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate03_required_codes.log`; `Project_Docs/Re_Step/Evidence/Step03/20260215T080030Z/gate02_legacy_size_literals.log` |
