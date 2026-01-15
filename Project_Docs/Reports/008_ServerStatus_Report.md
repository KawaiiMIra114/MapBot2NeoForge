# Task ID: #008 Server Management & Status

## 执行时间
2026-01-15 20:05 (UTC+8)

---

## Status: ✅ SUCCESS

---

## 创建/修改的文件列表

| 路径 | 操作 | 说明 |
|------|------|------|
| `logic/ServerStatusManager.java` | 新建 | 服务器状态查询 |
| `logic/InboundHandler.java` | 修改 | 添加命令路由 |

---

## 新增命令

| 命令 | 别名 | 功能 |
|------|------|------|
| `#list` | `#在线` | 查看在线玩家列表 |
| `#tps` | `#status`, `#状态` | 查看服务器状态 (MSPT/TPS/内存) |
| `#help` | `#菜单` | 显示命令帮助 |
| `#stopserver` | `#关服` | 远程关闭服务器 (TODO: 权限) |

---

## 输出示例

### #list
```
📊 在线玩家: 3/20
Steve, Alex, Notch
```

### #tps
```
📊 服务器状态
─────────
⏱️ MSPT: 32.50 ms
📈 TPS: 20.0
💾 内存: 2048/4096 MB (50.0%)
👥 玩家: 3/20
🌍 世界: world
```

### #help
```
📖 MapBot Reforged 命令帮助
─────────────────────
#help / #菜单 - 显示此帮助
#list / #在线 - 查看在线玩家
#tps / #status - 查看服务器状态
#inv <玩家名> - 查看玩家背包
─────────────────────
普通消息将转发到游戏内
```

---

## 线程安全

所有服务器 API 调用都包裹在 `server.execute()` 中：

```java
server.execute(() -> {
    String result = ServerStatusManager.getList();
    sendReplyToQQ(result);
});
```

---

## TODO

1. **权限系统**: #stopserver 需要实现管理员白名单
2. **更多命令**: #weather, #hitokoto 等趣味功能
