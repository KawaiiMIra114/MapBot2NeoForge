# Task ID: #009 Data Persistence & Network Refactoring

## 执行时间
2026-01-15 22:00 (UTC+8)

---

## Status: ✅ SUCCESS

---

## 创建/修改的文件列表

| 路径 | 操作 | 说明 |
|------|------|------|
| `data/DataManager.java` | 新建 | 数据持久化管理器 |
| `network/BotClient.java` | 修改 | 修复 WebSocket 消息分片 |
| `logic/InboundHandler.java` | 重构 | Java 21 语法 + 权限检查 |
| `MapBot.java` | 修改 | 初始化 DataManager |
| `logic/ServerStatusManager.java` | 修改 | 更新帮助信息 |
| `settings.gradle` | 新建 | NeoForge Maven 仓库配置 |
| `build.gradle` | 修改 | moddev 2.0.139 + Gradle 8.8 |
| `gradle-wrapper.properties` | 修改 | Gradle 8.8 |

---

## 新增功能

### DataManager - 数据持久化

**存储路径**: `config/mapbot_data.json`

**数据结构**:
```json
{
    "admins": [123456789, 987654321],
    "playerBindings": {
        "123456789": "player-uuid"
    }
}
```

**API**:
| 方法 | 说明 |
|------|------|
| `isAdmin(long qq)` | 检查是否为管理员 |
| `addAdmin(long qq)` | 添加管理员 |
| `removeAdmin(long qq)` | 移除管理员 |
| `bind(long qq, String uuid)` | 绑定玩家 |
| `unbind(long qq)` | 解绑玩家 |

---

## 新增命令

| 命令 | 功能 | 权限 |
|------|------|------|
| `#addadmin <QQ>` | 添加管理员 | 管理员 (首次无限制) |
| `#removeadmin <QQ>` | 移除管理员 | 管理员 |
| `#stopserver` | 关闭服务器 | 管理员 ✅ 已实现权限检查 |

---

## 技术改进

### 1. WebSocket 消息分片修复

```java
// BotClient.WSListener
private final StringBuilder messageBuffer = new StringBuilder();

public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
    messageBuffer.append(data);
    if (last) {
        InboundHandler.handleMessage(messageBuffer.toString());
        messageBuffer.setLength(0);
    }
    // ...
}
```

### 2. Java 21 Switch 表达式

```java
switch (commandName) {
    case "inv" -> handleInventoryCommand(message);
    case "list", "在线" -> handleListCommand();
    case "stopserver", "关服" -> handleStopServerCommand(senderQQ);
    default -> sendReplyToQQ("❓ 未知命令");
}
```

### 3. 线程安全

DataManager 使用 `ReadWriteLock` 保证并发安全：

```java
private final ReadWriteLock lock = new ReentrantReadWriteLock();

public boolean isAdmin(long qq) {
    lock.readLock().lock();
    try {
        return data.admins.contains(qq);
    } finally {
        lock.readLock().unlock();
    }
}
```

---

## Git 提交记录

| Commit | 内容 |
|--------|------|
| `67c99c0` | Task #009 主体实现 |
| `c3fc40e` | 添加 settings.gradle |
| `465e85d` | moddev 版本 → 2.0.139 |
| `fa77eb2` | Gradle 版本 → 8.8 |

---

## 完成 TODO

- [x] **权限系统**: #stopserver 已实现管理员检查 (来自 #008 TODO)
