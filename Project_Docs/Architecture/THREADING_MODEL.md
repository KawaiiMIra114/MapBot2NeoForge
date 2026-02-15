# THREADING_MODEL

## 0. 元数据
| 字段 | 值 |
|---|---|
| Document-ID | ARCH-A4-THREADING-MODEL |
| Status | Active |
| Owner | KnowledgeBase A4 |
| Last-Updated | 2026-02-14 |
| Applicable-Version | v5.7.x（参考 `Project_Docs/CURRENT_STATUS.md`） |
| Scope | `MapBot_Reforged/` + `Mapbot-Alpha-V1/` |
| Depends-On | Alpha Core + Reforged Bridge 双端协作架构 |
| Review-Cadence | 每月一次，或线程模型变更时立即复审 |

## 1. 目标与边界
- 目标: 明确线程归属与跨线程访问约束，避免竞态、死锁与主线程阻塞。
- 边界: 本文覆盖游戏侧 Mod、Alpha 核心桥接链路及其共享状态访问，不定义业务命令细节。

## 2. 术语与版本口径
- `Game Main Thread`: NeoForge 服务器主线程，唯一允许访问世界状态/玩家实体的线程。
- `IO Thread`: WebSocket/Netty 回调线程，负责网络收发，不直接执行游戏逻辑。
- `Worker Thread`: 后台任务线程池，负责可并行且不触碰游戏对象的计算/查询。
- `Owner Thread`: 某资源允许发生写入与副作用提交的唯一线程。

## 3. 强制规则
### 3.1 线程归属矩阵
| 资源/对象 | Owner Thread | 允许访问方式 |
|---|---|---|
| 世界状态、玩家实体、背包操作、白名单写入 | `Game Main Thread` | 仅主线程直接访问；跨线程必须通过 `server.execute(...)` 回切 |
| OneBot/Bridge 入站消息缓冲 | `IO Thread`（生产）+ `Worker Thread`（解析） | 以不可变 DTO 或队列传递，不共享可变对象 |
| 重连调度与心跳状态 | `Scheduler Thread` | 原子变量/线程安全容器；禁止与游戏对象直接耦合 |
| 绑定/禁言/时长权威数据 | `Alpha Core` | Reforged 仅做短 TTL 缓存，不作为权威源 |
| 缓存映射（如 UUID->QQ、mute 状态） | `Worker/IO` | 仅使用并发容器，写入需带时间戳与来源版本 |

### 3.2 跨线程访问约束
- 任何来自网络回调的业务动作，必须先转换为不可变命令对象，再进入主线程或 Worker 执行。
- 跨线程传递仅允许: 基本类型、不可变值对象、显式线程安全容器句柄。
- 禁止把 `Player`、`ServerLevel`、`ItemStack` 等游戏对象引用保存到异步线程延后使用。
- 需要等待异步结果时，主线程只允许“注册回调”，禁止 `join/get` 阻塞等待。
- 锁顺序固定: `cache lock -> session lock -> persistence lock`，不得反序申请。

### 3.3 禁止模式
- 禁止在 `IO Thread` 直接调用 Minecraft API（包括发放物品、白名单变更、踢人等）。
- 禁止在主线程执行网络请求、磁盘长阻塞、外部进程等待。
- 禁止在持锁区执行可能重入的回调（网络发送、日志扩展器、事件广播）。
- 禁止使用“非 volatile 标志位 + 双检锁”实现连接状态判断。
- 禁止同一可变对象在多个线程直接读写（即使“理论上很快”也不允许）。

## 4. 可操作检查点
- [ ] 代码扫描: 任何游戏状态写操作均位于主线程入口（如 `server.execute` 回切路径）。
- [ ] 代码扫描: 主线程路径不存在 `Future#get/join`、`Thread.sleep`、阻塞网络调用。
- [ ] 代码扫描: 缓存容器均为并发安全实现，且写入带 TTL/版本戳。
- [ ] 压测: 并发注入高频群消息与游戏事件，确认无 `ConcurrentModificationException`/死锁。
- [ ] 观测: 日志中可区分 `main/io/worker` 线程名，便于追踪越界调用。
- [ ] 退出流程: 重连调度器与后台线程池在停服时可有序关闭，无悬挂线程。

## 5. 可执行实验计划
### 实验 T-1: 线程越界探测实验
- 目标: 验证网络线程/工作线程不会直接触达 MC 世界与玩家对象写操作。
- 步骤:
1. 启动 Alpha + Reforged，打开 debug 日志并启用线程名打印。
2. 以 200 并发压测控制台指令与群消息转发（持续 10 分钟）。
3. 同步触发玩家上下线、聊天、进度播报等事件。
4. 检索日志中“非主线程访问世界对象”与并发异常关键字。
- 采样窗口: 连续 10 分钟，1 秒粒度采样日志与线程快照。
- 通过阈值:
1. 线程越界告警数 = 0。
2. `ConcurrentModificationException` = 0。
3. 主线程写操作路径命中 `server.execute` 比例 = 100%（抽样）。
- 失败处置:
1. 立即冻结涉及网络->游戏副作用的变更合并。
2. 对越界调用点打热补丁（回切主线程）并复测。
3. 24 小时内补充根因与防回归规则。

### 实验 T-2: 主线程预算与阻塞检查实验
- 目标: 验证主线程不存在阻塞等待，且关键操作时延在预算内。
- 步骤:
1. 启动 TPS 监控并注入高频命令流（`list/status/inv/location` 混合）。
2. 运行静态扫描命令检查 `join/get/sleep`：
```bash
rg -n "\\.get\\(.*TimeUnit|\\.join\\(|Thread\\.sleep\\(" MapBot_Reforged/src/main/java/com/mapbot/network Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge
```
3. 对主线程耗时进行采样并统计 P95/P99。
- 采样窗口: 压测 15 分钟，统计窗口按 1 分钟滚动。
- 通过阈值:
1. 主线程路径阻塞调用命中数 = 0。
2. 主线程任务执行耗时 P95 < 50ms，P99 < 100ms。
3. 压测期间 `Can't keep up` 告警 <= 1 次。
- 失败处置:
1. 立刻降级高耗时功能（限流/关闭非核心命令）。
2. 拆分阻塞点到异步线程并回切主线程提交副作用。
3. 超过 48 小时未恢复则触发发布门禁阻断。

### 实验 T-3: 锁顺序与死锁回归实验
- 目标: 验证并发场景下锁顺序不反转，系统无死锁。
- 步骤:
1. 并发执行绑定、禁言、签到、白名单同步相关路径。
2. 人工注入高频断连/重连，增加状态竞争。
3. 每 5 秒抓取一次线程栈（jstack 或等效工具）并分析阻塞链。
- 采样窗口: 20 分钟；线程栈采样间隔 5 秒。
- 通过阈值:
1. 死锁检测次数 = 0。
2. 长阻塞线程（>30 秒）数量 = 0。
3. 锁顺序违规样本 = 0（`cache -> session -> persistence`）。
- 失败处置:
1. 立即回滚最近并发相关改动。
2. 引入锁分层断言或统一锁管理器后再放量。
3. 形成死锁复盘并追加 CI 线程栈回归用例。

## 6. 现状差距表（As-Is vs To-Be）
| 主题 | As-Is（当前实现） | To-Be（目标） | 证据路径 | 风险等级 | 整改期限 | Owner |
|---|---|---|---|---|---|---|
| 心跳线程访问游戏对象 | Bridge 心跳线程直接读取 `ServerLifecycleHooks.getCurrentServer()` 与 `getPlayerList()` | 心跳线程仅读取主线程快照对象，不直接触碰 MC 对象 | `MapBot_Reforged/src/main/java/com/mapbot/network/BridgeClient.java:233`, `MapBot_Reforged/src/main/java/com/mapbot/network/BridgeClient.java:253`, `MapBot_Reforged/src/main/java/com/mapbot/network/BridgeClient.java:255` | High | 2026-02-28 | Reforged Maintainer |
| 关服倒计时线程模型 | 倒计时通过 `new Thread` + `Thread.sleep` 手写生命周期 | 统一由调度器管理，支持中断、停服清理和线程命名规范 | `MapBot_Reforged/src/main/java/com/mapbot/network/BridgeHandlers.java:849`, `MapBot_Reforged/src/main/java/com/mapbot/network/BridgeHandlers.java:860` | Medium | 2026-03-07 | Reforged Maintainer |
| 主线程保护机制 | 依赖人工代码审查，没有统一线程越界扫描门禁 | 引入自动扫描 + CI fail-fast + 例外白名单审计 | `Project_Docs/Architecture/THREADING_MODEL.md:50`, `Project_Docs/Architecture/THREADING_MODEL.md:54` | Medium | 2026-03-10 | QA + Architecture Owner |

## 7. 线程越界白名单/黑名单规则
### 7.1 白名单（允许模式，至少满足其一）
- 仅在 `server.execute(() -> ...)` 闭包内执行世界状态写操作。
- 仅在主线程事件回调（如 `ServerChatEvent`）中读取 `ServerPlayer` 实体字段。
- 使用 `ConcurrentHashMap` / `Atomic*` 读写连接状态与缓存元数据。
- 网络线程仅做 JSON 解析、DTO 构建、入队，不直接操作 MC API。
- 异步任务仅处理纯计算、字符串转换、外部请求结果解析。
- 通过 `CompletableFuture` 回调把结果再投递回主线程后落地副作用。
- 关服流程使用可管理调度器并在停服时显式 shutdown。
- 日志中标记线程名（`main/io/worker/scheduler`）用于审计。

### 7.2 黑名单（命中即违规）
- 在 `com.mapbot.network.*` 线程中直接调用 `ServerLifecycleHooks.getCurrentServer()` 并操作玩家/世界对象。
- 在非主线程直接调用 `server.getPlayerList().*`、`player.connection.*`、`level().*`。
- 在主线程路径执行 `Future#get`、`join`、阻塞网络调用。
- 在事件处理/命令执行主路径使用 `Thread.sleep(...)`。
- 持有 `ServerPlayer`/`ItemStack`/`ServerLevel` 引用跨线程延后使用。
- 在持锁区进行网络发送、事件广播或可能重入回调。
- 使用无内存语义标志位控制连接状态（如非 volatile 双检锁）。
- 由业务代码私自创建匿名线程执行核心流程（`new Thread(...).start()`）。

### 7.3 自动扫描（CI）
1. 基础扫描命令：
```bash
rg -n "ServerLifecycleHooks\\.getCurrentServer\\(|getPlayerList\\(|player\\.connection\\.|level\\(" MapBot_Reforged/src/main/java/com/mapbot/network
rg -n "Thread\\.sleep\\(|new Thread\\(" MapBot_Reforged/src/main/java Mapbot-Alpha-V1/src/main/java
rg -n "\\.get\\(.*TimeUnit|\\.join\\(" MapBot_Reforged/src/main/java/com/mapbot/network Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge
```
2. 门禁规则：
- `com.mapbot.network` 包命中 `getCurrentServer/getPlayerList/level/player.connection` 直接判失败（除显式豁免清单）。
- `new Thread(` 命中后必须在豁免清单登记用途、退出条件和 Owner。
- 主线程路径（`server.execute` 闭包内）命中 `get/join/sleep` 直接判失败。
3. 例外机制：
- 白名单文件采用逐条登记（路径 + 行号 + 失效日期），到期自动失效并重新审查。

### 7.4 误杀案例与豁免流程
#### 误杀案例 A（静态分析误判）
- 场景: `com.mapbot.network.*` 中仅调用 `ServerLifecycleHooks.getCurrentServer()` 判空或读取无副作用元数据，未触达玩家/世界写操作。
- 误杀原因: 规则按包路径与关键词匹配，无法区分“只读安全快照”与“越界副作用”。
- 临时豁免条件:
1. 代码注释标记 `THREAD_EXEMPT_READONLY`。
2. PR 中附线程上下文说明与压测证据。
3. 豁免有效期不超过 14 天。

#### 误杀案例 B（测试代码误判）
- 场景: 集成测试/启动探针中使用 `new Thread(...)` 模拟并发，不进入生产逻辑。
- 误杀原因: 黑名单直接匹配 `new Thread(`。
- 临时豁免条件:
1. 仅允许位于 `*Test.java` 或明确测试目录。
2. 必须设置线程名称和退出条件。
3. 合并前登记失效日期并指向替代方案（调度器/线程池）。

#### 豁免流程（统一）
1. 提交豁免申请: 记录规则命中项、文件行号、原因、Owner、失效日期。
2. 双人审批: 模块 Owner + 架构 Owner 同时批准。
3. 限时生效: 豁免窗口默认 14 天，超期自动失效并恢复阻断。
4. 回归核验: 豁免结束前必须提交替代实现或延长期限申请（含新证据）。
