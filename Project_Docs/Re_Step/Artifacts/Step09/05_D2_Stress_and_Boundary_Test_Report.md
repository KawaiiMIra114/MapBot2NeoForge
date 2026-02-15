# 05 D2 Stress and Boundary Test Report

## 元数据
| 属性 | 值 |
|---|---|
| Step | Step-09 D2 |
| RUN_ID | 20260215T203100Z |
| 依据 | RE_STEP_09 §详细步骤 6 |

## 1. 越界探测

### 1.1 IO 线程直接读写游戏对象
| # | 文件 | 行 | 操作 | 线程 | 结论 |
|---|---|---|---|---|---|
| OOB-01 | BridgeHandlers | ~649 | getPlayerInfo 读 server | IO | ⚠ 需回切 |
| OOB-02 | BridgeHandlers | ~662 | hasWhitelist 读白名单 | IO | ⚠ 需回切 |
| OOB-03 | BridgeHandlers | ~677 | getOnlinePlayers 读玩家列表 | IO | **⚠ 高风险** |
| OOB-04 | CQCodeParser | 140 | getCurrentServer 读 server | IO (BotClient) | ⚠ 需验证 |

### 1.2 已正确回切的 handler
| Handler | server.execute | 结论 |
|---|---|---|
| handleWhitelistAdd | ✅ L91 | 安全 |
| handleWhitelistRemove | ✅ L127 | 安全 |
| handleBindPlayer | ✅ L173 | 安全 |
| handleUnbindPlayer | ✅ L228 | 安全 |
| handleGiveItem | ✅ L270 | 安全 |
| handleExecuteCommand | ✅ L296/323/359 | 安全 |
| handleBroadcast | ✅ L710 | 安全 |
| handleRunCommand | ✅ L728 | 安全 |
| handleSwitchServer | ✅ L688 | 安全 |
| handleStopServer | ✅ L842/864 | 安全 |
| handleSendTitle | ✅ L892 | 安全 |
| handleReloadConfig | ✅ L422 | 安全 |

越界总结: **4 个需改造**, 12 个已安全。

## 2. 死锁检查

### 2.1 潜在死锁风险
| 场景 | 线程 A | 线程 B | 风险 |
|---|---|---|---|
| 主线程 sync call | server.execute 内等待回复 | IO 等待主线程处理 | ⚠ 如果主线程 get() |
| 当前状态 | 无发现主线程 get/join | — | **低风险** |

### 2.2 结论
主线程未发现阻塞等待网络 I/O 的 get()/join() 调用。死锁风险: **低**。

## 3. 主线程预算评估

### 3.1 server.execute 内操作时间估算
| 操作 | 估算耗时 | 可接受 |
|---|---|---|
| 白名单读写 | < 1ms | ✅ |
| 命令执行 | 1-50ms | ✅ (取决于命令) |
| 物品发放 | < 5ms | ✅ |
| 广播消息 | < 1ms | ✅ |
| 玩家列表读取 | < 1ms | ✅ |
| 配置重载 | 10-100ms | ⚠ 但不频繁 |

### 3.2 结论
当前 server.execute 内操作均为轻量级，主线程预算无明显风险。

## 4. 差距列表
| ID | 描述 | 风险 | 改造 |
|---|---|---|---|
| D2-OOB-01~03 | 3 个查询 handler 未回切主线程 | High | server.execute 包裹 |
| D2-OOB-04 | CQCodeParser IO 线程读 server | Medium | 待验证 |
| D2-N1~N4 | 4 个匿名 new Thread | High | 命名调度器替换 |
| D2-A1~A2 | 2 个 supplyAsync 默认池 | Medium | 命名 Executor |
| D2-B2/B4/B5 | 3 个可消除 Thread.sleep | Medium | ScheduledExecutor |
| D2-S1~S3 | 3 个调度器停服回收缺失 | Medium | 新增 shutdown |

合计: 15 项差距 (5 High / 9 Medium / 1 Low)
