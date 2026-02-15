# 01 Thread Owner Matrix Review

## 元数据
| 属性 | 值 |
|---|---|
| Step | Step-06 C1 |
| RUN_ID | 20260215T193300Z |
| 评审日期 | 2026-02-15 |
| 评审者 | AI Agent (主代理) |
| 依据 | THREADING_MODEL.md §3.1 + 代码静态追踪 |

## 1. 线程归属验证

### 1.1 Reforged 侧线程清单
| 线程名 | 创建位置 | 用途 | 生命周期管理 |
|---|---|---|---|
| `MapBot-WS-Thread` | BotClient.java:50 | OneBot WebSocket 连接 | daemon=true, 有名 ✓ |
| `MapBot-Bridge` | BridgeClient.java:162 | Bridge TCP 连接主循环 | daemon=true, 有名 ✓ |
| `MapBot-Heartbeat` | BridgeClient.java:230 | 心跳发送 | daemon=true, 有名 ✓ |
| `MapBot-TPSMonitor` | ServerStatusManager.java:107 | TPS 采集 | daemon=true, 有名 ✓ |
| `MapBot-StopCountdown` | StopServerCommand.java:26 | 关服倒计时 | **daemon 未设置**, 有名 ⚠ |
| (匿名) | BridgeHandlers.java:849 | 关服倒计时(Bridge 入口) | **无名+无 daemon** ⚠ |
| (匿名) | DataManager.java:309 | 异步数据保存 | **无名** ⚠ |
| (匿名) | MapBot.java:220 | 启动延迟初始化 | **无名** ⚠ |

### 1.2 Alpha 侧线程清单
| 线程名 | 创建位置 | 用途 | 生命周期管理 |
|---|---|---|---|
| `MetricsSaver` | MetricsStorage.java:43 | 指标持久化 | daemon=true, 有名 ✓ |
| `MetricsCollector` | MetricsCollector.java:33 | 指标采集 | daemon=true, 有名 ✓ |
| (匿名) | ProcessManager.java:32 | 进程流读取 | **无名** ⚠ |
| (匿名) | MapbotAlpha.java:95 | ShutdownHook | 标准 JVM hook ✓ |
| ConsoleWatcher | MapbotAlpha.java:137 | 控制台命令监听 | 有名 ✓ |
| Redis Subscriber | RedisManager.java:87 | Redis 发布订阅 | **无名** ⚠ |

## 2. 资源归属矩阵验证

### 2.1 世界状态/玩家实体（Owner: Game Main Thread）
| 调用位置 | 调用方式 | 在 server.execute 内? | 判定 |
|---|---|---|---|
| BridgeHandlers L91 | server.execute(() → getPlayerList) | ✅ | 安全 |
| BridgeHandlers L111 | server.getPlayerList().getPlayers() | ❌ 在 execute 外 | **HIGH 越界** |
| BridgeHandlers L127 | server.execute(() → ...) | ✅ | 安全 |
| BridgeHandlers L134 | server.getPlayerList().getPlayer() | ❌ 在 execute 外 | **HIGH 越界** |
| BridgeHandlers L152 | server.getPlayerList().size() | ❌ 在 execute 外 | **HIGH 越界** |
| BridgeHandlers L367/423/535/560/627/653 | 多处 getPlayerList | ⚠ 需逐一确认 | 待验证 |
| BridgeHandlers L710/856 | server.execute(() → getPlayerList) | ✅ | 安全 |
| BridgeClient L254 | getCurrentServer (心跳线程) | ❌ 心跳线程 | **HIGH 越界** |
| BridgeClient L256 | getPlayerList().size (心跳线程) | ❌ 心跳线程 | **HIGH 越界** |
| BridgeClient L265 | getCurrentServer (心跳线程) | ❌ 心跳线程 | **HIGH 越界** |

### 2.2 越界总结
- **确认越界**: 5 处 (BridgeHandlers L111/L134/L152 + BridgeClient L254/L256/L265)
- **疑似越界**: 6 处 (BridgeHandlers L367/423/535/560/627/653 需确认调用上下文)
- **安全回切**: 30+ 处 server.execute 调用（良好覆盖）

### 2.3 缓存容器（Owner: Worker/IO）
| 容器 | 类型 | 位置 | 判定 |
|---|---|---|---|
| pendingRequests | ConcurrentHashMap | BridgeClient L52 | ✅ 线程安全 |
| muteExpiryCache | ConcurrentHashMap | BridgeClient L72 | ✅ 线程安全 |
| qqByUuidCache | ConcurrentHashMap | BridgeClient L73 | ✅ 线程安全 |
| muteRefreshInFlight | ConcurrentHashMap.newKeySet | BridgeClient L74 | ✅ 线程安全 |
| connected | AtomicBoolean | BridgeClient L45 | ✅ 原子 |
| running | AtomicBoolean | BridgeClient L46 | ✅ 原子 |
| isConnected | volatile boolean | BotClient L43 | ✅ 可见性保证 |
| isReconnecting | volatile boolean | BotClient L44 | ✅ 可见性保证 |
| shouldReconnect | volatile boolean | BotClient L45 | ✅ 可见性保证 |

## 3. 差距汇总

| ID | 描述 | 位置 | 风险 | 修复建议 |
|---|---|---|---|---|
| TH-01 | 心跳线程直接调用 getCurrentServer + getPlayerList | BridgeClient L254-256,L265 | **High** | 改为主线程快照读取,心跳仅消费快照 |
| TH-02 | BridgeHandlers 多处 getPlayerList 在 execute 外 | BridgeHandlers L111/L134/L152 | **High** | 包裹在 server.execute 内 |
| TH-03 | 匿名线程(关服倒计时) | BridgeHandlers L849 | **Medium** | 统一调度器+命名 |
| TH-04 | DataManager 匿名线程 | DataManager L309 | **Medium** | 命名+daemon 设置 |
| TH-05 | MapBot 启动匿名线程 | MapBot.java L220 | **Low** | 命名 |
| TH-06 | Redis 订阅线程无名 | RedisManager L87 | **Low** | 命名 |
| TH-07 | ProcessManager 匿名线程 | ProcessManager L32 | **Low** | 命名 |

## 4. 白名单/黑名单命中

### 黑名单命中 (4项)
1. ❌ `com.mapbot.network.BridgeClient` 心跳线程直接调用 `getCurrentServer()` 操作玩家/世界 → TH-01
2. ❌ `com.mapbot.network.BridgeHandlers` 部分方法在非 execute 包裹下调用 `getPlayerList()` → TH-02
3. ❌ `BridgeHandlers L849` 使用 `new Thread(...).start()` 执行关服核心流程 → TH-03
4. ❌ `BridgeHandlers L860` 在事件处理路径使用 `Thread.sleep(1000)` → TH-03

### 白名单命中 (5项)
1. ✅ 命令类全部使用 `server.execute()` 回切 (30+处)
2. ✅ 缓存使用 ConcurrentHashMap / AtomicBoolean (9处)
3. ✅ BotClient 使用 volatile 控制状态 (3处)
4. ✅ BridgeClient 连接线程有命名 (MapBot-Bridge/MapBot-Heartbeat)
5. ✅ TPS 监控线程有命名和 daemon 设置

## 5. 结论
- **High 差距**: 2 项 (TH-01, TH-02)
- **Medium 差距**: 2 项 (TH-03, TH-04)
- **Low 差距**: 3 项 (TH-05, TH-06, TH-07)
- 整体评估: 主线程回切机制覆盖良好 (30+处), 但心跳线程和部分 Handler 存在越界风险。
