# 02 ForbiddenPattern Scan Report

## 元数据
| 属性 | 值 |
|---|---|
| Step | Step-06 C1 |
| RUN_ID | 20260215T193300Z |
| 评审日期 | 2026-02-15 |
| 评审者 | AI Agent (主代理) |
| 依据 | THREADING_MODEL.md §3.3 禁止模式 + 代码静态扫描 |

## 1. 扫描范围
- `MapBot_Reforged/src/main/java/com/mapbot/**/*.java`
- `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/**/*.java`

## 2. 扫描结果

### 2.1 `new Thread()` 扫描 (16处)
| # | 文件 | 行号 | 线程名 | daemon | 风险 | 判定 |
|---|---|---|---|---|---|---|
| 1 | BotClient.java | 50 | MapBot-WS-Thread | ✓ | Low | ✅ 允许 |
| 2 | BridgeClient.java | 162 | MapBot-Bridge | ✓ | Low | ✅ 允许 |
| 3 | BridgeClient.java | 230 | MapBot-Heartbeat | ✓ | Low | ✅ 允许 |
| 4 | BridgeHandlers.java | 849 | (匿名) | ✗ | **High** | ❌ 禁止模式 |
| 5 | ServerStatusManager.java | 107 | MapBot-TPSMonitor | ✓ | Low | ✅ 允许 |
| 6 | DataManager.java | 309 | (匿名) | ✗ | **Medium** | ⚠ 需修复 |
| 7 | MapBot.java | 220 | (匿名) | ✗ | **Medium** | ⚠ 需修复 |
| 8 | StopServerCommand.java | 26 | MapBot-StopCountdown | ✗ | Low | ⚠ 需设 daemon |
| 9 | ProcessManager.java | 32 | (匿名) | ✗ | **Medium** | ⚠ 需修复 |
| 10 | MetricsStorage.java | 43 | MetricsSaver | ✓ | Low | ✅ 允许 |
| 11 | MetricsCollector.java | 33 | MetricsCollector | ✓ | Low | ✅ 允许 |
| 12 | MapbotAlpha.java | 95 | (ShutdownHook) | N/A | None | ✅ JVM机制 |
| 13 | MapbotAlpha.java | 137 | ConsoleWatcher | ✓ | Low | ✅ 允许 |
| 14 | RedisManager.java | 87 | (匿名) | ✗ | **Medium** | ⚠ 需修复 |
| 15 | OneBotClient.java | — | (WebSocket lib内部) | N/A | None | ✅ 库内部 |
| 16 | — | — | — | — | — | — |

### 2.2 `Thread.sleep()` 扫描 (5处)
| # | 文件 | 行号 | 上下文 | 所在线程 | 风险 | 判定 |
|---|---|---|---|---|---|---|
| 1 | BridgeHandlers.java | 860 | 关服倒计时延迟1s | 匿名线程 | **High** | ❌ 业务线程 sleep |
| 2 | BridgeClient.java | 195 | 重连延迟 | Bridge线程 | **Medium** | ⚠ 应用调度器 |
| 3 | MapBot.java | 222 | 启动延迟5s | 匿名线程 | **Medium** | ⚠ 应用调度器 |
| 4 | MapBot.java | 253 | 重试延迟1s | 匿名线程 | **Medium** | ⚠ 应用调度器 |
| 5 | OneBotClient.java | 130 | WebSocket重连延迟 | WS线程 | Low | ⚠ 可接受 |

### 2.3 `.join()` / `.get()` 阻塞调用扫描
| # | 文件 | 行号 | 调用方法 | 所在线程 | 风险 | 判定 |
|---|---|---|---|---|---|---|
| — | — | — | 无 Future.join/get 阻塞调用发现 | — | — | ✅ 安全 |

备注: BridgeHandlers 中 `.get()` 调用均为 Optional.get()（值获取），非 Future.get()。

### 2.4 IO线程 → MC API 越界扫描
参见 Artifact 01 §2.1 — 5 处确认越界 + 6 处疑似越界。

### 2.5 主线程阻塞网络调用扫描
| # | 文件 | 行号 | 描述 | 判定 |
|---|---|---|---|---|
| — | — | — | 主线程路径未发现阻塞网络调用 | ✅ |

## 3. 差距汇总

| ID | 描述 | 位置 | 风险 | 修复建议 | 截止 |
|---|---|---|---|---|---|
| FP-01 | 匿名线程执行关服核心流程 | BridgeHandlers L849 | **High** | 统一调度器 + 命名 + 可中断 | 2026-02-28 |
| FP-02 | 关服线程内 Thread.sleep | BridgeHandlers L860 | **High** | 用 ScheduledExecutorService 替代 | 2026-02-28 |
| FP-03 | 重连延迟使用 Thread.sleep | BridgeClient L195 | **Medium** | 用延迟调度替代阻塞等待 | 2026-03-07 |
| FP-04 | 启动延迟使用匿名线程+sleep | MapBot L220-253 | **Medium** | 调度器延迟初始化 | 2026-03-07 |
| FP-05 | DataManager 匿名线程 | DataManager L309 | **Medium** | 命名+daemon | 2026-03-07 |
| FP-06 | ProcessManager 匿名线程 | ProcessManager L32 | **Medium** | 命名 | 2026-03-14 |
| FP-07 | RedisManager 订阅线程无名 | RedisManager L87 | **Medium** | 命名 | 2026-03-14 |

## 4. 结论
- **High 违规**: 2 项 (FP-01, FP-02) — 关服倒计时匿名线程+sleep
- **Medium 违规**: 5 项 (FP-03 ~ FP-07) — 无名/sleep模式
- **无 Future 阻塞**: 0 项 ✅
- **无主线程阻塞网络**: 0 项 ✅
- 整体: 主线程保护良好，但后台线程管理不规范（多个匿名线程和 Thread.sleep）。
