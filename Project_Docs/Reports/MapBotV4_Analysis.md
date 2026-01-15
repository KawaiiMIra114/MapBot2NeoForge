# MapBotV4 功能分析报告

## 概述

MapBotV4 是一个 Bukkit/Spigot/Paper 插件，使用 **Mirai** 框架直接登录 QQ (需要签名服务)，实现 Minecraft 服务器与 QQ 群的互通。

---

## 一、技术架构

### 核心依赖
| 依赖 | 用途 |
|------|------|
| **Mirai** | QQ 机器人框架 (直接登录QQ) |
| **QSign** | 签名服务 (绕过腾讯验证) |
| **Vault** | 经济系统抽象层 |
| **MySQL** | 玩家数据持久化 |
| **Quartz** | 定时任务调度 |
| **Fastjson** | JSON 解析 |

### 插件式命令系统
```
PluginManager
    ├── 反射扫描 me.maplef.mapbotv4.plugins 包
    ├── 每个插件实现 MapbotPlugin 接口
    │       ├── register() → 返回命令映射
    │       └── onEnable() → 命令执行逻辑
    └── commandHandler() → 动态分发命令
```

---

## 二、功能清单

### 🔗 玩家绑定系统 (BindIDAndQQ.java)

| 命令 | 功能 | 权限 |
|------|------|------|
| `#id <ID>` | 绑定游戏ID | 玩家群 |
| `#updateid` | 更新ID (正版换名后) | 玩家群 |
| `#deleteid <ID>` | 解绑玩家 | 管理员 |

**实现方式**:
- 正版模式: 调用 Mojang API 验证 UUID
- 离线模式: 直接存储名称
- 绑定后: 自动修改群名片、添加白名单

---

### 📋 在线玩家查询 (ListPlayers.java)

| 命令 | 功能 |
|------|------|
| `#list` | 显示在线玩家列表 |

**实现**: `Bukkit.getServer().getOnlinePlayers()` 遍历

---

### 📊 服务器状态 (CheckTPS.java / ServerInfo.java)

| 命令 | 功能 |
|------|------|
| `#tps` | 查询当前 TPS |
| `#serverinfo` | 详细信息 (区块、TPS、MSPT、世界时间) |

**实现**: `Bukkit.getServer().getTPS()`, `getAverageTickTime()`

---

### 💰 经济系统 (CheckMoney.java / Pay.java)

| 命令 | 功能 |
|------|------|
| `#money` | 查询自己余额 |
| `#pay <玩家> <金额>` | 转账给其他玩家 |

**实现**: 通过 Vault API 操作 (`Economy.getBalance()`, `withdrawPlayer()`, `depositPlayer()`)

---

### 🔇 禁言系统 (MutePlayers.java)

| 命令 | 功能 |
|------|------|
| `#mute <玩家> <分钟>` | 同时禁言游戏和QQ群 |
| `#unmute <玩家>` | 解除禁言 |

**实现**: 
- 游戏内: 调用 `mute` 命令
- QQ群: `playerMember.mute(seconds)`

---

### ⏹️ 服务器管理 (StopServer.java)

| 命令 | 功能 |
|------|------|
| `#stopserver [秒]` | 倒计时关服 (默认60秒) |
| `#stopcancel` | 取消关服 |

**实现**: `BukkitRunnable` 定时器，可选传送玩家到其他子服

---

### 🌤️ 天气查询 (Weather.java)

| 命令 | 功能 |
|------|------|
| `#weather <城市>` | 查询天气 |

**实现**: 调用 和风天气 API (`geoapi.qweather.com`)

---

### 📰 趣味功能

| 命令 | 功能 | 实现 |
|------|------|------|
| `#hitokoto` | 一言 | 调用 hitokoto.cn API |
| `#cat` | 随机猫图 | 从数据库读取 |
| `#uploadcat` | 上传猫图 | 百度识图验证 |

---

### ⏰ 定时任务 (loops/)

| 任务 | 功能 |
|------|------|
| **GoodMorning** | 早7点: 早安+天气+新闻+一言 |
| **GoodNight** | 晚11点: 晚安 |
| **TPSCheck** | 定期检查TPS，低于阈值告警 |

**实现**: Quartz 调度器

---

## 三、事件监听 (listeners/)

### GameListeners.java
| 事件 | 行为 |
|------|------|
| `AsyncChatEvent` | 转发聊天到QQ群 |
| `PlayerLoginEvent` | 群内发送登录消息 |
| `PlayerQuitEvent` | 群内发送退出消息 |
| `PlayerDeathEvent` | 群内发送死亡消息 |

### PlayerGroupListeners.java
| 事件 | 行为 |
|------|------|
| 新成员入群 | 发送欢迎消息 |
| 成员退群 | 通知管理群 |

---

## 四、数据库结构

### PLAYER 表
| 字段 | 类型 | 说明 |
|------|------|------|
| NAME | TEXT | 游戏ID |
| QQ | TEXT | QQ号 |
| UUID | TEXT | 正版UUID |
| MSGREC | BOOLEAN | 是否接收消息 |

### cat_images 表
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 自增主键 |
| uploader | TEXT | 上传者 |
| base64 | LONGTEXT | 图片Base64 |

---

## 五、权限系统

| 级别 | 判断条件 | 权限 |
|------|----------|------|
| 普通玩家 | 在 `player-group` 中 | 基础命令 |
| 管理员 | 在 `op-group` 中 | 管理命令 |
| 超级管理 | 在 `super-admin-account` 列表中 | 解绑管理员等 |

---

## 六、MapBot Reforged 迁移评估

### ✅ 可直接迁移
| 功能 | 迁移难度 |
|------|----------|
| #list (在线列表) | ⭐ 简单 |
| #tps, #serverinfo | ⭐ 简单 |
| 聊天转发 | ✅ 已完成 |
| 登录/退出通知 | ✅ 已完成 |
| #inv (库存查询) | ✅ 已完成 |

### ⚠️ 需要适配
| 功能 | 说明 |
|------|------|
| 玩家绑定 | 需要实现数据存储 (文件/SQLite) |
| 天气查询 | 需要配置 API Key |
| 一言 | HTTP 请求，可迁移 |

### ❌ 不适用于 NeoForge
| 功能 | 原因 |
|------|------|
| #money, #pay | NeoForge 无标准经济API |
| #mute | NeoForge 无内置禁言命令 |
| 白名单管理 | NeoForge 使用不同API |

---

## 七、建议迁移优先级

1. **#list** - 简单，高频使用
2. **#tps** - 简单，管理需求
3. **#help** - 命令帮助系统
4. **玩家绑定** - 核心功能，需设计存储方案
5. **天气/一言** - HTTP API，增加趣味性
