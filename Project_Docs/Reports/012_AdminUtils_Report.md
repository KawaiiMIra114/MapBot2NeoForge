# Task ID: #012 Admin Utilities & Text Formatting

## 执行时间
2026-01-16 23:50 (UTC+8)

---

## Status: ✅ SUCCESS

---

## 创建/修改的文件列表

| 路径 | 操作 | 说明 |
|------|------|------|
| `logic/InboundHandler.java` | 修改 | 添加 #adminunbind, #reload 命令 |
| `logic/GameEventListener.java` | 修改 | 聊天消息颜色代码清理 |
| `logic/ServerStatusManager.java` | 修改 | 更新帮助信息 |

---

## 新增命令

| 命令 | 权限 | 功能 |
|------|------|------|
| `#adminunbind <QQ>` | 管理员 | 强制解绑指定 QQ 的游戏账号 |
| `#reload` | 管理员 | 重载配置和数据文件 |

---

## 技术实现

### 1. #adminunbind

```java
// 流程
1. 权限检查: isAdmin(senderQQ)
2. 解析目标 QQ
3. 检查目标是否已绑定
4. 获取绑定 UUID (解绑前)
5. server.execute() 中执行:
   - DataManager.unbind(targetQQ)
   - 从白名单移除
```

### 2. #reload

```java
// 重载数据管理器
DataManager.INSTANCE.init();

// BotConfig 使用 ModConfigSpec，自动同步
```

### 3. 颜色代码清理

```java
// GameEventListener
private static final String COLOR_CODE_REGEX = "(?i)§[0-9a-fk-or]";

String message = event.getRawText().replaceAll(COLOR_CODE_REGEX, "");
```

---

## Git 提交

| Commit | 内容 |
|--------|------|
| `dab987b` | Task #012 管理员工具与文本格式化 |
