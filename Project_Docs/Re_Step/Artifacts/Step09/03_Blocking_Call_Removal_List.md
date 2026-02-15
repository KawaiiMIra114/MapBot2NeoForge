# 03 Blocking Call Removal List

## 元数据
| 属性 | 值 |
|---|---|
| Step | Step-09 D2 |
| RUN_ID | 20260215T203100Z |
| 依据 | RE_STEP_09 §详细步骤 3 + 代码扫描 |

## 1. 阻塞调用清单

### 1.1 Thread.sleep
| # | 文件 | 行号 | 上下文 | 线程 | 风险 | 替代方案 |
|---|---|---|---|---|---|---|
| B1 | OneBotClient.java | 130 | 重连循环等待 250ms | OneBotClient 专用线程 | **Low** | ScheduledExecutor.schedule (已可接受) |
| B2 | BridgeHandlers.java | 860 | 停服倒计 1s interval | 匿名线程 | **High** | → 使用 StopServerCommand.SCHEDULER |
| B3 | BridgeClient.java | 195 | 重连延迟 | Bridge 专用线程 | **Low** | ScheduledExecutor.schedule (已可接受) |
| B4 | MapBot.java | 222 | 重连等待 5s | 匿名线程 | **Medium** | → 命名线程 + ScheduledExecutor |
| B5 | MapBot.java | 253 | 停机等待 1s | 匿名线程 | **Medium** | → 命名线程 + ScheduledExecutor |

### 1.2 CompletableFuture.supplyAsync (默认池)
| # | 文件 | 行号 | 上下文 | 风险 | 替代方案 |
|---|---|---|---|---|---|
| A1 | MapBot.java | 118 | switch_server | **Medium** | 显式指定命名 Executor |
| A2 | BindCommand.java | 62 | resolveGameProfile | **Medium** | 显式指定命名 Executor |

### 1.3 匿名 new Thread（无生命周期管理）
| # | 文件 | 行号 | 上下文 | 风险 | 替代方案 |
|---|---|---|---|---|---|
| N1 | BridgeHandlers.java | 849 | 停服倒计线程 | **High** | → StopServerCommand.SCHEDULER |
| N2 | MapBot.java | 220 | 重连线程 | **High** | → 命名 ScheduledExecutorService |
| N3 | DataManager.java | 309 | 保存线程 | **High** | → 命名单线程 Executor |
| N4 | RedisManager.java | 87 | Redis subscriber | **Medium** | → 命名线程 + 停服回收 |

## 2. 改造优先级

### Batch 1: 高风险 (立即)
- N1 → 复用 StopServerCommand.SCHEDULER
- N2 → 新增命名线程 `MapBot-Reconnect`
- N3 → 新增命名线程 `MapBot-DataSaver`
- B2 → 随 N1 一起消除

### Batch 2: 中风险 (跟进)
- A1, A2 → 新增共享命名线程池 `MapBot-AsyncWorker`
- B4, B5 → 随 N2 一起改造
- N4 → 命名为 `MapBot-Redis-Subscriber`

### Batch 3: 低风险 (可延后)
- B1, B3 → 已在专用线程中，风险可控

## 3. D2 后预期扫描结果
| 指标 | 当前 | 目标 |
|---|---|---|
| Thread.sleep 主线程 | 0 | 0 (保持) |
| Thread.sleep 专用线程 | 5 | ≤ 3 (B2/B4/B5 消除) |
| 匿名 new Thread 核心流程 | 4 | 0 |
| supplyAsync 默认池 | 2 | 0 |
