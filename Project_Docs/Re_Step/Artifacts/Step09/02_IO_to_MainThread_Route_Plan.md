# 02 IO to MainThread Route Plan

## 元数据
| 属性 | 值 |
|---|---|
| Step | Step-09 D2 |
| RUN_ID | 20260215T203100Z |
| 依据 | THREADING_MODEL.md §2 + RE_STEP_09 §详细步骤 2 |

## 1. 当前路由分析

### 1.1 BridgeHandlers (Reforged) — 良好模式
```
IO线程(readLoop) 
  → BridgeClient.handleMessage(json) 
    → BridgeHandlers.handleXxx(data, requestId)
      → MinecraftServer server = getServer();
      → server.execute(() -> { 
          // 游戏副作用在主线程执行 ✓
        });
```
**18/22 个 handler 已使用 server.execute 回切主线程** — 这是良好基础。

### 1.2 未回切的 handler（需改造）
| Handler | 行号 | 问题 | 风险 |
|---|---|---|---|
| handleGetPlayerInfo | ~649 | 在 IO 线程读 server 状态 | Medium |
| handleHasWhitelist | ~662 | 在 IO 线程读白名单 | Medium |
| handleGetOnlinePlayers | ~677 | 在 IO 线程读玩家列表 | **High** (ConcurrentModification) |
| handleStopServer 停服倒计线程 | ~849 | 匿名 new Thread + Thread.sleep | **High** |

### 1.3 CQCodeParser (Reforged) — 需验证
```
BotClient IO线程 → InboundHandler → CQCodeParser.parse()
  → ServerLifecycleHooks.getCurrentServer() (L140)
  → 读取 server 状态 → 可能在非主线程
```
风险: **Medium** (读取操作，非写操作)

### 1.4 Alpha 侧
Alpha 是纯 Java 程序（非 MC 模组），没有主线程约束。线程安全通过 ConcurrentHashMap + volatile 保证。
**Alpha 侧无 server.execute 需求。**

## 2. 改造方案

### 2.1 读操作回切（查询类 handler）
```java
// 改造前 (handleGetOnlinePlayers):
MinecraftServer server = getServer();
var players = server.getPlayerList().getPlayers(); // IO 线程直接读 ⚠

// 改造后:
server.execute(() -> {
    var players = server.getPlayerList().getPlayers();
    // 在主线程中安全读取 + 构建回复
    String response = buildPlayerListResponse(players);
    sendResponse(requestId, response);
});
```

### 2.2 匿名线程改造（停服倒计）
```java
// 改造前 (handleStopServer):
new Thread(() -> {
    for (int i = seconds; i > 0; i--) {
        Thread.sleep(1000);
        ...
    }
}).start();

// 改造后: 使用已存在的 StopServerCommand.SCHEDULER
StopServerCommand.startCountdown(server, seconds, ...);
```

### 2.3 supplyAsync 改造
```java
// 改造前:
CompletableFuture.supplyAsync(() -> resolveGameProfile(...))
// 使用默认 ForkJoinPool — 无命名，无生命周期

// 改造后:
CompletableFuture.supplyAsync(() -> resolveGameProfile(...), MAP_BOT_EXECUTOR)
// MAP_BOT_EXECUTOR: 命名线程池，可停服回收
```

## 3. 路由规范
| 线程 | 允许操作 | 禁止操作 |
|---|---|---|
| IO (readLoop) | JSON 解析→DTO | 读写游戏对象 |
| 主线程 (server.execute) | 读写世界/玩家/白名单 | 阻塞等待网络 |
| Worker (命名线程池) | CPU 密集计算/网络请求 | 读写游戏对象 |
| 调度器 (scheduled) | 定时任务/心跳 | 长阻塞操作 |
