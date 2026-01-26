# MapBot Reforged 项目接续报告 v1.0
> 生成时间: 2026-01-26 19:24  
> 版本: v5.5.0 → v5.6.0 (Task #022 完成后)

---

## 一、项目概述

MapBot Reforged 是一个 QQ 群 ↔ Minecraft 服务器双向互通的机器人系统，由两个主要组件构成：

| 组件 | 目录 | 技术栈 | 职责 |
|------|------|--------|------|
| **Alpha Core** | `Mapbot-Alpha-V1/` | Java 独立应用 + Netty + Redis | QQ 协议对接、命令处理、数据存储、多服调度 |
| **Reforged Mod** | `MapBot_Reforged/` | NeoForge 1.21.1 Mod | 游戏逻辑执行、事件上报、物品发放 |

**通信链路**:
```
QQ群 ←WebSocket→ NapCat ←WebSocket→ Alpha ←TCP Bridge→ Reforged Mod
```

---

## 二、当前已完成功能

### 2.1 核心功能
- [x] QQ ↔ MC 双向聊天 (含 @提及)
- [x] Level 0-2 权限系统
- [x] 禁言拦截系统
- [x] 高级签到系统 (Tag 随机奖池 + CDK)
- [x] **Task #022: Redis 跨服签到迁移** ✅

### 2.2 已修复的 Bug (P0-P3)
| 优先级 | 问题 | 修复 |
|--------|------|------|
| P0 | `SignManager.claimOnline` 物品丢失 | 失败时放回 pendingRewards |
| P0 | `DataManager.bind` UUID 无唯一性检查 | 添加 `isUUIDBound()` |
| P1 | `handleAcceptReward` 响应太简略 | 区分离线/无奖励/背包满情况 |
| P3 | 签到格式不符合用户要求 | 实现新格式 (玩家名+累计天数+在线检测) |

---

## 三、Alpha Core 架构

### 3.1 命令系统

**入口**: `CommandRegistry.dispatch(cmdName, args, senderQQ, sourceGroupId)`

**已注册命令** (21个):
```
签到系统: #sign, #accept, #cdk
绑定系统: #id/#bind, #unbind, #forceunbind
查询系统: #list, #status, #loc, #inv, #time
权限系统: #myperm, #setperm, #addadmin, #removeadmin
管理系统: #mute, #unmute, #stop, #cancelstop, #reload
其他:     #help
```

**权限等级**:
- Level 0: 默认用户
- Level 1: 受信用户 (部分查询)
- Level 2: 管理员 (管理命令)

### 3.2 Bridge 接口 (Alpha → Mod)

| 方法 | action | 用途 |
|------|--------|------|
| `getOnlinePlayerList()` | get_players | 获取在线玩家 |
| `getServerStatus()` | get_status | 获取服务器状态 |
| `resolveAndBind()` | bind_player | 解析并绑定玩家 |
| `getPlayerInventory()` | get_inventory | 获取玩家背包 |
| `getPlayerLocation()` | get_location | 获取玩家位置 |
| `executeCommand()` | execute_command | 执行游戏命令 |
| `broadcast()` | broadcast | 广播消息 |
| `getPlaytime()` | get_playtime | 获取在线时长 |
| `stopServer()` | stop_server | 关闭服务器 |
| `cancelStop()` | cancel_stop | 取消关服 |
| **`rollLoot()`** | roll_loot | 请求抽奖 (Task #022) |
| **`giveItem()`** | give_item | 发放物品 (Task #022) |
| **`redeemCdk()`** | - | CDK 验证 (Alpha 本地) |

### 3.3 Redis Key 设计 (Task #022)

```
mapbot:sign:last:<qq>      → yyyy-MM-dd (最后签到日期)
mapbot:sign:days:<qq>      → int (累计签到天数)
mapbot:sign:pending:<qq>   → JSON (待领取物品, 24h过期)
mapbot:cdk:<code>          → JSON (CDK信息, 24h过期)
mapbot:bindings            → Hash (QQ→UUID绑定)
mapbot:permissions         → Hash (QQ→权限等级)
```

---

## 四、Reforged Mod 架构

### 4.1 Bridge 消息类型处理器

| type | 处理器 | 用途 |
|------|--------|------|
| register_ack | - | 注册确认 |
| heartbeat_ack | - | 心跳确认 |
| proxy_response | handleProxyResponseFromAlpha | 代理响应 |
| command | handleCommand | 执行命令 |
| qq_message | handleQQMessage | QQ消息转游戏 |
| get_players | handleGetPlayers | 获取玩家列表 |
| get_status | handleGetStatus | 获取状态 |
| bind_player | handleBindPlayer | 绑定玩家 |
| sign_in | handleSignIn | 签到 (旧版) |
| accept_reward | handleAcceptReward | 领取奖励 (旧版) |
| get_inventory | handleGetInventory | 获取背包 |
| get_location | handleGetLocation | 获取位置 |
| execute_command | handleExecuteCommand | 执行命令 |
| broadcast | handleBroadcast | 广播 |
| get_playtime | handleGetPlaytime | 获取时长 |
| get_cdk | handleGetCdk | 获取CDK (旧版) |
| stop_server | handleStopServer | 关服 |
| cancel_stop | handleCancelStop | 取消关服 |
| **roll_loot** | handleRollLoot | 抽奖 (Task #022) |
| **give_item** | handleGiveItem | 发物品 (Task #022) |

### 4.2 游戏内命令

```
/mapbot cdk <code>  - 兑换签到奖励 CDK
```

---

## 五、签到流程图 (Task #022 后)

```
#sign 签到流程:
┌────────┐     ┌────────┐     ┌────────┐     ┌────────┐
│ QQ 群  │────▶│ Alpha  │────▶│ Redis  │     │  Mod   │
└────────┘     └────────┘     └────────┘     └────────┘
     │              │              │              │
     │  #sign       │              │              │
     │─────────────▶│ 检查签到     │              │
     │              │─────────────▶│              │
     │              │◀─────────────│              │
     │              │              │              │
     │              │ roll_loot    │              │
     │              │─────────────────────────────▶│
     │              │◀─────────────────────────────│ Item JSON
     │              │              │              │
     │              │ 存 pending   │              │
     │              │─────────────▶│              │
     │              │              │              │
     │◀─────────────│ 回复签到结果  │              │
     │              │              │              │

#accept 领取流程 (在线):
     │  #accept     │              │              │
     │─────────────▶│ 获取 pending │              │
     │              │─────────────▶│              │
     │              │◀─────────────│              │
     │              │              │              │
     │              │ give_item    │              │
     │              │─────────────────────────────▶│ 发放物品
     │              │◀─────────────────────────────│ SUCCESS
     │              │              │              │
     │              │ 删 pending   │              │
     │              │─────────────▶│              │
     │◀─────────────│ 领取成功     │              │
```

---

## 六、关键文件索引

### Alpha Core
```
src/main/java/com/mapbot/alpha/
├── MapbotAlpha.java              # 主入口
├── bridge/
│   ├── BridgeServer.java         # TCP 服务端
│   ├── BridgeMessageHandler.java # 消息处理
│   ├── BridgeProxy.java          # 代理接口 ★
│   └── ServerRegistry.java       # 多服注册
├── command/
│   ├── CommandRegistry.java      # 命令注册中心 ★
│   ├── ICommand.java             # 命令接口
│   └── impl/                     # 21 个命令实现
├── config/
│   └── AlphaConfig.java          # 配置管理
├── data/
│   └── DataManager.java          # 数据管理 (绑定/权限) ★
├── database/
│   └── RedisManager.java         # Redis 连接池
├── logic/
│   └── SignManager.java          # 签到管理 (Redis版) ★
└── network/
    ├── OneBotClient.java         # QQ 协议客户端
    └── LogWebSocketHandler.java  # 日志推送
```

### Reforged Mod
```
src/main/java/com/mapbot/
├── MapBot.java                   # Mod 入口 ★ (/mapbot cdk)
├── config/
│   └── BotConfig.java            # 配置
├── data/
│   ├── DataManager.java          # 本地数据 (累计签到天数等)
│   └── loot/
│       └── LootConfig.java       # 奖池配置
├── logic/
│   ├── SignManager.java          # 签到逻辑 (本地版, 部分废弃)
│   ├── InventoryManager.java     # 背包管理
│   ├── GameEventListener.java    # 游戏事件
│   └── ServerStatusManager.java  # 状态管理
└── network/
    ├── BotClient.java            # QQ WebSocket
    └── BridgeClient.java         # Alpha TCP 客户端 ★
```

---

## 七、待完成任务

目前没有紧急待办任务。可考虑的后续工作：
1. Dashboard 前端完善
2. 多服负载均衡
3. 更多游戏事件上报
4. 命令权限细粒度控制

---

## 八、构建与验证

```powershell
# Alpha
cd Mapbot-Alpha-V1
.\gradlew.bat build

# Reforged
cd MapBot_Reforged
.\gradlew.bat build
```

**当前状态**: 双端构建成功 ✅

---

## 九、Git 最新提交

```
5c46bf9 feat: Task #022 Redis 签到迁移完成
2609ed0 fix: P0-P3 签到系统修复与优化
```
