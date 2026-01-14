# 通信协议规范 (Protocol Specification)

## 1. 连接方式
* **类型**: Reverse WebSocket (反向 WS) 或 Client 模式
* **标准**: OneBot v11
* **地址**: `ws://127.0.0.1:3000` (默认 NapCat 地址)

## 2. 上行数据 (Minecraft -> Bot)
当游戏内发生事件时，发送标准 OneBot `send_group_msg` 动作。

**JSON 示例**:
```json
{
    "action": "send_group_msg",
    "params": {
        "group_id": 123456789,
        "message": "[服务器] <Steve> 大家好！"
    },
    "echo": "msg_uuid_001"
}
```

## 3. 下行数据 (Bot -> Minecraft)
监听来自 NapCat 的 WebSocket 推送。

关键事件类型:
* **message** (群消息): 用于双向聊天同步。
* **meta_event** (心跳): lifecycle 和 heartbeat，用于判断连接存活。

## 4. 迁移注意 (Migration Note)
原 Bukkit 版使用 Mirai API，现已废弃。

所有逻辑必须适配 JSON 格式，不再依赖 Java 对象序列化。
