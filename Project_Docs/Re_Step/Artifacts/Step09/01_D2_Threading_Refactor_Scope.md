# 01 D2 Threading Refactor Scope

## 元数据
| 属性 | 值 |
|---|---|
| Step | Step-09 D2 |
| RUN_ID | 20260215T203100Z |
| 依据 | THREADING_MODEL.md + CRITIQUE §3.6/3.9 + Step06 Artifacts |

## 1. 改造范围

### 1.1 线程归属当前状态
| 线程 | 归属 | 命名 | 生命周期 |
|---|---|---|---|
| MapBot-Bridge | BridgeClient | ✅ 命名 | ⚠ new Thread |
| MapBot-Heartbeat | BridgeClient | ✅ 命名 | ScheduledExecutor |
| MapBot-WS-Thread | BotClient | ✅ 命名 | ScheduledExecutor |
| MapBot-TPSMonitor | ServerStatusManager | ✅ 命名 | ScheduledExecutor |
| MapBot-StopCountdown | StopServerCommand | ✅ 命名 | ScheduledExecutor |
| MetricsSaver | MetricsStorage | ✅ 命名 | ScheduledExecutor |
| MetricsCollector | MetricsCollector | ✅ 命名 | ScheduledExecutor |
| (匿名停服) | BridgeHandlers:849 | ❌ 匿名 | ❌ new Thread |
| (匿名重连) | MapBot:220 | ❌ 匿名 | ❌ new Thread |
| (匿名保存) | DataManager:309 | ❌ 匿名 | ❌ new Thread |
| (Redis订阅) | RedisManager:87 | ❌ 匿名 | ❌ new Thread |
| consoleWatcher | MapbotAlpha:137 | ✅ 但裸线程 | new Thread |
| ShutdownHook | MapbotAlpha:95 | ✅ JVM 管理 | 可接受 |

### 1.2 D2 核心改造项
| # | 改造项 | 风险 | 产物 |
|---|---|---|---|
| T1 | 匿名线程 → 命名调度器 | **High** | Artifact 02 |
| T2 | 主线程 sleep 清理 | **Medium** | Artifact 03 |
| T3 | IO 线程副作用回切验证 | **High** | Artifact 02 |
| T4 | supplyAsync → 命名线程池 | **Medium** | Artifact 03 |
| T5 | 快照读替代跨线程引用 | **Medium** | Artifact 04 |
| T6 | 调度器停服回收 | **High** | Artifact 04 |

## 2. 冻结规则
| 规则 | 说明 |
|---|---|
| F1 | 只改线程归属和执行路径 |
| F2 | 不改业务语义 |
| F3 | 不改 Bridge 协议语义 (D1 已冻结) |
| F4 | 不改数据一致性模型 (属 D3) |
| F5 | 不新增命令 |

## 3. 回滚边界
| 条件 | 动作 |
|---|---|
| 线程改造导致死锁 | git revert + 恢复原线程模型 |
| 主线程卡顿超预算 | 回滚当批改造 |
| server.execute 内异常导致崩溃 | 恢复直接调用 + 加异常捕获 |
