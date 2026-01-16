# Task ID: #010 Player Binding & Whitelist System

## 执行时间
2026-01-16 23:30 (UTC+8)

---

## Status: ✅ SUCCESS

---

## 创建/修改的文件列表

| 路径 | 操作 | 说明 |
|------|------|------|
| `data/DataManager.java` | 修改 | 添加 isQQBound, isUUIDBound, getQQByUUID 方法 |
| `logic/InboundHandler.java` | 修改 | 添加 #id 和 #unbind 命令 |
| `logic/ServerStatusManager.java` | 修改 | 更新帮助信息 |

---

## 新增命令

| 命令 | 别名 | 功能 |
|------|------|------|
| `#id <游戏ID>` | `#bind`, `#绑定` | 绑定QQ与Minecraft账号，自动加白名单 |
| `#unbind` | `#removeid`, `#解绑` | 解绑账号，自动移除白名单 |

---

## 技术实现

### 1. 绑定流程

```
用户输入 #id Steve
    ↓
Step A: 快速验证
- 正则检查: ^[a-zA-Z0-9_]{3,16}$
- 检查 QQ 是否已绑定
    ↓
Step B: 异步解析 GameProfile (不阻塞主线程)
- 正版模式: ProfileCache → Mojang API
- 离线模式: UUIDUtil.createOfflinePlayerUUID()
    ↓
Step C: 主线程执行 (server.execute)
- 检查 UUID 是否被绑定
- DataManager.bind(qq, uuid)
- UserWhiteList.add() + save()
    ↓
成功回复
```

### 2. 白名单集成

```java
UserWhiteList whitelist = server.getPlayerList().getWhiteList();
if (!whitelist.isWhiteListed(profile)) {
    whitelist.add(new UserWhiteListEntry(profile));
    whitelist.save(); // 持久化到 whitelist.json
}
```

### 3. 线程安全

- 使用 `CompletableFuture.supplyAsync()` 异步解析玩家档案
- 使用 `.thenAcceptAsync(..., server)` 确保在主线程执行绑定

---

## 输出示例

### #id (成功)
```
✅ 绑定成功！
已将 Steve 加入白名单
现在可以进入服务器了
```

### #id (玩家名已被绑定)
```
❌ 该游戏ID已被其他QQ绑定
如有疑问请联系管理员
```

### #unbind
```
✅ 解绑成功！
已从白名单中移除
```

---

## Git 提交

| Commit | 内容 |
|--------|------|
| `3456065` | Task #010 玩家绑定与白名单系统 |

---

## 与 MapBotV4 对比

| 原版命令 | 新版命令 | 状态 |
|----------|----------|------|
| `#id <ID>` | `#id <ID>` | ✅ 一致 |
| `#updateid` | - | ❌ 未实现 (可用 unbind + id 替代) |
| `#deleteid <ID>` | `#unbind` | ⚠️ 原版需指定ID，新版解绑自己 |
