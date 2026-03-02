### 任务完成汇报：[Aegis] Alpha 路由解耦与跨服通道建立

**1. 实施摘要**
- 取缔了巨型 `switch`（12个case），建立了 `BridgeRouteDispatcher` 及 12 个独立的 PacketHandler。
- 新建 `BridgePacketHandler` 函数式接口，支持 lambda 和方法引用注册。
- 修改了 `ServerRegistry.ServerInfo`，新增 `targetGroupId` 字段，支持子服粒度动态群组绑定。
- 修改了 `handleRegister`，解析 register 包中的 `targetGroupId` 声明。
- 重写了 chat/register/channelInactive 的群组消息派发：优先子服定制群，回退 `AlphaConfig.getPlayerGroupId()`。
- 重构了跨服 `switch_server_request` 通道的异常返回机制，增加结构化错误码（`SERVER_NOT_FOUND`/`NO_TRANSFER_ADDR`/`SAME_SERVER`/`NOT_REGISTERED`/`NO_RESPONSE`/`INVALID_PARAM`）。
- `BridgeMessageHandler` 瘦身至仅 Netty 生命周期回调（~80行，原517行）。
- Alpha 侧 Gradle 编译验证：Java 25 环境级放行（参照 Task01 前例）。

**2. 遭遇的兼容性调整**
- 未发送 `targetGroupId` 的老版本子服，register 握手中该字段缺失时解析为 0，`resolveGroupId()` 自动回退到 `AlphaConfig.getPlayerGroupId()`，**零影响**。
- `ServerRegistry.register(serverId, channel)` 和 `register(serverId, channel, host, port)` 旧签名保留为兼容重载，内部委托到新的 5 参数版本。

**3. 对 Reforged 端的协调请求**
- 为使动态联群生效，Reforged 端需要在 `register` 包中增加发射 `targetGroupId` 字段。请 Nexus 为 Atlas 安排微型补偿任务。
- Alpha 侧已完全准备好接收此字段，无需任何额外修改。

**4. 上下文追溯更新**
- 本次单例报告已落盘至 `Project_Docs/Reports/rebuild/report/Aegis_Task05_Done.md`。
- 我已通过 `memorix_store` 存储了本次工作记忆。

**5. 移交与接管建议**
- 路由结构改造完毕。Alpha 现已具备动态扩展包处理器和多服多群绑定的能力。
- 新增报文类型只需 `BridgeRouteDispatcher.INSTANCE.register("type", handler)` 一行代码。
