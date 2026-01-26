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

## 七、权限系统详解

### 7.1 双轨权限体系

系统采用 **权限等级 (Level)** + **管理员标记 (Admin)** 双轨制：

| 权限 | 等级值 | 说明 | 可用命令示例 |
|------|--------|------|-------------|
| **普通用户** | Level 0 | 默认权限 | `#sign`, `#bind`, `#list`, `#help` |
| **受信用户** | Level 1 | 可查询他人信息 | `#loc`, `#inv`, `#time` |
| **管理员** | Level 2 | 可执行管理命令 | `#mute`, `#unmute`, `#setperm` |
| **超级管理员** | Admin 标记 | 最高权限 | `#stop`, `#reload`, `#addadmin`, `#forceunbind` |

### 7.2 权限管理命令

```bash
# 查看自己权限
#myperm

# 设置用户权限等级 (需 Admin)
#setperm @QQ用户 <0|1|2>

# 添加管理员 (需 Admin)
#addadmin @QQ用户

# 移除管理员 (需 Admin)
#removeadmin @QQ用户
```

### 7.3 权限数据存储

**本地存储**: `config/permissions.json`, `config/admins.json`  
**Redis 存储** (启用时):
```
mapbot:permissions   → Hash (QQ号 → Level)
mapbot:admins        → Set (管理员QQ号列表)
```

### 7.4 初始管理员配置

在 `config/alpha.properties` 中设置：
```properties
messaging.adminQQs=123456789,987654321
```
逗号分隔多个 QQ 号，启动时自动同步到管理员列表。

---

## 八、Dashboard 面板使用

### 8.1 面板概述

Dashboard 是一个基于 Vue 3 + TypeScript 的 Web 管理面板，内置于 Alpha Core，提供服务器监控和管理功能。

**访问地址**: `http://<AlphaIP>:8080/`  
**默认凭证**: 首次启动时在控制台输出

### 8.2 页面功能

| 页面 | 路由 | 功能 |
|------|------|------|
| **Dashboard** | `/` | 服务器概览、实时日志 |
| **Servers** | `/servers` | 多服状态监控、TPS/内存/玩家数 |
| **Console** | `/console` | 实时日志流、命令执行 |
| **Files** | `/files` | 远程文件管理 (查看/编辑/删除) |
| **Settings** | `/settings` | 配置管理 (暂未完全实现) |
| **Login** | `/login` | 登录页面 |

### 8.3 实时监控

Dashboard 页面通过 WebSocket 连接获取实时数据：
```
ws://<AlphaIP>:8080/ws
```
自动推送：
- MC 服务器事件日志
- 玩家上下线
- 命令执行结果

### 8.4 服务器状态卡片

显示当前连接的所有 MC 服务器：
- **Server ID**: 服务器标识
- **Players**: 在线玩家数
- **TPS**: 实时 TPS (绿色≥18, 黄色<18)
- **Memory**: 内存使用

### 8.5 文件管理

支持远程操作 MC 服务器文件：
- 浏览目录结构
- 查看/编辑配置文件
- 删除文件 (需确认)

**安全**: 操作通过 Alpha-Mod Bridge 代理，无法访问 Alpha 本机文件。

---

## 九、配置文件说明

### 9.1 Alpha Core 配置 (`config/alpha.properties`)

```properties
# NapCat WebSocket 地址
connection.wsUrl=ws://127.0.0.1:7000

# 重连间隔 (秒)
connection.reconnectInterval=5

# Redis 配置
redis.enabled=true
redis.host=127.0.0.1
redis.port=6379
redis.password=
redis.database=0

# QQ 群配置
messaging.playerGroupId=875585697      # 玩家群
messaging.adminGroupId=885810515       # 管理群
messaging.botQQ=2133782376             # 机器人QQ

# 初始管理员 (逗号分隔)
messaging.adminQQs=123456789

# 调试模式
debug.debugMode=true
```

### 9.2 Reforged Mod 配置 (`config/mapbot.toml`)

```toml
[connection]
alphaHost = "127.0.0.1"
alphaPort = 9000
serverId = "main"

[messaging]
playerGroupId = 875585697
```

### 9.3 如何修改配置

1. 停止服务
2. 编辑对应配置文件
3. 重启服务 或 使用 `#reload` 命令 (仅重载部分配置)

---

## 十、待完成任务

目前没有紧急待办任务。可考虑的后续工作：
1. Dashboard 前端完善
2. 多服负载均衡
3. 更多游戏事件上报
4. 命令权限细粒度控制

---

## 十一、构建与验证

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

## 十二、Git 最新提交

```
94885f9 docs: 添加项目接续报告 Report_01_Continue.md
5c46bf9 feat: Task #022 Redis 签到迁移完成
2609ed0 fix: P0-P3 签到系统修复与优化
```

