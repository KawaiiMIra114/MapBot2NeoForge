# 04 Snapshot Read and Scheduler Shutdown

## 元数据
| 属性 | 值 |
|---|---|
| Step | Step-09 D2 |
| RUN_ID | 20260215T203100Z |
| 依据 | THREADING_MODEL.md §3 + RE_STEP_09 §详细步骤 4/5 |

## 1. 快照读策略

### 1.1 当前跨线程共享状态
| 对象 | 所在文件 | 保护方式 | 风险 |
|---|---|---|---|
| bindings | DataManager (Alpha) | ConcurrentHashMap | ✅ 安全 |
| playerNames | DataManager (Alpha) | ConcurrentHashMap | ✅ 安全 |
| permissions | DataManager (Alpha) | ConcurrentHashMap | ✅ 安全 |
| admins | DataManager (Alpha) | ConcurrentHashMap.newKeySet | ✅ 安全 |
| validTokens | AuthManager (Alpha) | ConcurrentHashMap | ✅ 安全 |
| bridgeAuthConfig | AuthManager (Alpha) | volatile | ✅ 安全 |
| tpsHistory | MetricsCollector (Alpha) | ConcurrentHashMap + ConcurrentLinkedDeque | ✅ 安全 |
| servers | ServerRegistry (Alpha) | ConcurrentHashMap | ✅ 安全 |
| pendingRequests | BridgeProxy/FileProxy | ConcurrentHashMap | ✅ 安全 |
| loginTimes | PlaytimeManager (Reforged) | ConcurrentHashMap | ✅ 安全 |
| COMMAND_COOLDOWNS | InboundHandler (双端) | ConcurrentHashMap | ✅ 安全 |
| stopCancelled | ServerStatusManager | volatile | ✅ 安全 |
| getPlayerList().getPlayers() | MinecraftServer | **无保护** | ⚠ 需 server.execute |
| getWhiteList() | PlayerList | **无保护** | ⚠ 需 server.execute |

### 1.2 快照策略
| 场景 | 策略 |
|---|---|
| Alpha 侧 (纯 Java) | ConcurrentHashMap + volatile — 已足够 |
| Reforged 查询 handler | 改为 server.execute 内读取 + 构建不可变 DTO |
| ServerStatusManager | 已使用 volatile — 可接受 |
| 需要玩家列表的场景 | server.execute 内创建 List.copyOf + 传出 |

## 2. 调度器清单与生命周期

### 2.1 当前调度器
| 调度器 | 位置 | 类型 | 命名 | 停服回收 |
|---|---|---|---|---|
| heartbeatExecutor | BridgeClient | ScheduledExecutorService | ✅ MapBot-Heartbeat | ⚠ 仅 disconnect |
| messageExecutor | BridgeClient | ExecutorService | ✅ MapBot-Bridge | ⚠ 仅 disconnect |
| scheduler | BotClient | ScheduledExecutorService | ✅ MapBot-WS-Thread | ⚠ 仅 disconnect |
| scheduler | ServerStatusManager | ScheduledExecutorService | ✅ MapBot-TPSMonitor | ✅ stop() |
| SCHEDULER | StopServerCommand | ScheduledExecutorService | ✅ MapBot-StopCountdown | ❌ static final 无停服 |
| saveScheduler | MetricsStorage (Alpha) | ScheduledExecutorService | ✅ MetricsSaver | ✅ stop() |
| scheduler | MetricsCollector (Alpha) | ScheduledExecutorService | ✅ MetricsCollector | ✅ stop() |
| scheduler | OneBotClient (Alpha) | ScheduledExecutorService | ❌ 未命名 | ⚠ 仅 close |

### 2.2 停服回收改造
```
Reforged 停服顺序 (MapBot.onServerStopping):
1. BridgeClient.disconnect()    → 关闭 heartbeat + message + socket
2. BotClient.disconnect()       → 关闭 scheduler + WS
3. ServerStatusManager.stop()   → 关闭 TPS 监控
4. StopServerCommand.SCHEDULER.shutdown() → ⚠ 需新增

Alpha 停服顺序 (MapbotAlpha ShutdownHook):
1. MetricsCollector.stop()
2. MetricsStorage.stop()
3. OneBotClient.close()
4. RedisManager.close()         → ⚠ subscriber 线程需回收
```

### 2.3 改造项
| # | 改造 | 文件 | 内容 |
|---|---|---|---|
| S1 | StopServerCommand SCHEDULER 停服回收 | StopServerCommand | 新增 shutdown() 调用 |
| S2 | OneBotClient scheduler 命名 | OneBotClient | 添加线程工厂命名 |
| S3 | RedisManager subscriber 停服回收 | RedisManager | interrupt + join |
| S4 | 匿名线程改为命名调度器 | 见 Artifact 03 | N1~N4 |

## 3. 停服后线程验证
| 检查项 | 预期 |
|---|---|
| Thread.activeCount() | 仅 JVM 系统线程 |
| MapBot-* 命名线程 | 全部 TERMINATED |
| 匿名线程 | 0 个活跃 |
| pending 请求 | 全部清零 (BRG_TRANSPORT_303) |
