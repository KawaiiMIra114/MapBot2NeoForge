# Task #016-STEP2 执行报告: 在线时长查询

**执行者**: Lazarus  
**日期**: 2026-01-18  
**状态**: ✅ 完成  
**提交**: `63e715e`

---

## 任务目标

实现玩家在线时长追踪和查询功能，支持多时段统计 (今日/本周/本月/总计)。

---

## 变更内容

### 新增文件

#### [NEW] `PlaytimeManager.java`

独立的在线时长管理器，核心功能:

| 方法 | 功能 |
|------|------|
| `onPlayerLogin(UUID)` | 记录玩家登录时间 |
| `onPlayerLogout(UUID)` | 计算并保存本次在线时长 |
| `onServerStopping()` | 服务器关闭时保存所有在线玩家数据 |
| `getPlaytimeMinutes(UUID, mode)` | 获取指定时段的在线时长 |
| `formatDuration(minutes)` | 格式化时长显示 (分钟/小时/天) |

**周期重置机制**:
- 每日: 日期变更时重置 `dailyMs`
- 每周: 周一时重置 `weeklyMs`
- 每月: 每月1号时重置 `monthlyMs`

---

### 修改文件

#### [MODIFY] `DataManager.java`

扩展数据模型:

```java
// 新增 PlaytimeRecord 内部类
public static class PlaytimeRecord {
    public long dailyMs;
    public long weeklyMs;
    public long monthlyMs;
    public long totalMs;
    public String lastReset;  // YYYY-MM-DD
}

// 新增方法
getPlaytimeRecord(String uuidStr)
savePlaytimeRecord(String uuidStr, PlaytimeRecord record)
```

---

#### [MODIFY] `GameEventListener.java`

在现有事件中添加钩子:

```java
// onPlayerLoggedIn
PlaytimeManager.INSTANCE.onPlayerLogin(player.getUUID());

// onPlayerLoggedOut  
PlaytimeManager.INSTANCE.onPlayerLogout(player.getUUID());

// onServerStopping
PlaytimeManager.INSTANCE.onServerStopping();
```

---

#### [MODIFY] `InboundHandler.java`

添加命令处理:

```java
case "playtime", "在线时长" -> handlePlaytimeCommand(rawArgs, senderQQ, sourceGroupId);
```

命令格式: `#playtime <玩家名> [时段]`
- 时段: 0=今天, 1=本周, 2=本月, 3=总计 (默认 0)

---

#### [MODIFY] `ServerStatusManager.java`

更新 `getHelp()` 帮助信息，添加 `#playtime` 命令说明。

---

## 技术说明

### 数据持久化

使用现有 `DataManager` 的 JSON 存储机制 (`mapbot_data.json`)，新增 `playerPlaytime` 字段:

```json
{
  "admins": [...],
  "playerBindings": {...},
  "playerPlaytime": {
    "uuid-string": {
      "dailyMs": 1234567,
      "weeklyMs": 2345678,
      "monthlyMs": 3456789,
      "totalMs": 9876543,
      "lastReset": "2026-01-18"
    }
  }
}
```

### 线程安全

- `PlaytimeManager.loginTimes` 使用 `ConcurrentHashMap`
- 所有 `DataManager` 操作使用 `ReadWriteLock` 保护
- `handlePlaytimeCommand` 使用 `server.execute()` 确保主线程执行

### 实时在线时长

查询时自动计算当前会话时间:

```java
long currentSessionMs = System.currentTimeMillis() - loginTime;
return (baseMs + currentSessionMs) / 60000;
```

---

## 编译验证

```
./gradlew build -x test
BUILD SUCCESSFUL
```

---

## 使用示例

```
#playtime Steve       → 📊 Steve 今天的在线时长 ⏱️ 45 分钟
#playtime Steve 1     → 📊 Steve 本周的在线时长 ⏱️ 3 小时 20 分钟
#playtime Steve 3     → 📊 Steve 总计的在线时长 ⏱️ 2 天 5 小时
```

---

**签名**: Lazarus - MapBot Reforged 开发执行者
