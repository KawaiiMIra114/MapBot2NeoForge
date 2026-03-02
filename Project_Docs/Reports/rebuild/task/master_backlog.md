# MapBot Reforged 重建发包清册 (Master Backlog)

> **文档说明**: 本文件为控制整个重构阶段节奏的核心跟踪任务板。各研发智能体 (Aegis, Atlas) 与总监 (Nexus) 必须在此板领取、标记并更新状态。
> **状态流转**: `[TODO]` -> `[IN-PROGRESS]` -> `[REVIEW]` -> `[DONE]`

---

## 📅 阶段一：P0 - 数据隔离与通信基座统合 (Foundation & SSOT)
**目标**: 根绝严重破坏《数据一致性契约》的问题，提取跨端通信载体，实施并发安全保护。

### 任务 01: 建立双端 `Common` 共享模块 
* **指派**: `Aegis`
* **状态**: `[DONE]`
* **描述**: 抽离 Alpha 与 Reforged 端相同的底层 `Message`, `BridgeError`, 和网络封包规范到独立的库，消灭重复的实体类，重新对接构建脚本依赖。

### 任务 02: Reforged 端 `DataManager` 引擎换血 
* **指派**: `Atlas` 
* **状态**: `[DONE]`
* **描述**: 废止不安全的 `Files.writeString`，使用全量带 CAS 版本号的写入机制 (`expected_version`) 与原子的 `.tmp` 文件替换保存动作，支持并发下的 `CONSISTENCY-409` 保护。

### 任务 03: Alpha 业务越权肃清 (单端计算原则)
* **指派**: `Aegis`
* **状态**: `[DONE]`
* **描述**: 清除 Alpha 侧所有的 `logic` 包（如自己保管的 `SignManager`, `PlaytimeStore` 等副本业务代码）。将 Alpha 退化为网关，签到计算与分发完全交由 Reforged 回传代理计算结果。

---

## 📅 阶段二：P1 - 安全鉴权引擎与网络软化 (Gateway & Security)
**目标**: 实现《命令授权契约》合规，消除路由硬编码。

### 任务 04: Reforged 侧鉴权引擎 (`AuthorizationEngine`) 拔高
* **指派**: `Atlas`
* **状态**: `[DONE]`
* **描述**: 全面剔除 0/1/2 架构，实现 `owner/admin/user` 层级判定；植入拦截 Audit Log (审计日志) 以及针对越权刷命令的 Rate-Limit (速率拦截器)，规范输出 `AUTH-403`。

### 任务 05: Alpha 路由解耦与跨服通道建立
* **指派**: `Aegis`
* **状态**: `[DONE]`
* **描述**: 取缔 `BridgeMessageHandler` 里长串的 `switch/if-else`。构建策略派发模式；取消 `alpha.properties` 中唯一的 `playerGroupId` 硬绑，支持按连入的 `serverId` 独立建立转发频道。

---

## 📅 阶段三：P2 - 代码异味消除与运维可用性 (Clean & Ops)

### 任务 06: Reforged 端历史配置大扫除
* **指派**: `Atlas`
* **状态**: `[TODO]`
* **描述**: 删除 `BotConfig` 与 `BotClient` 中直连 OneBot (NapCat) 的一切遗留逻辑（如 `wsUrl`, `playerGroupId`）。切断所有在单服直接处理群聊通信的幻觉，强制 Reforged 仅监听 Bridge。

### 任务 07: 堵死线程泄漏与修复汉化丢失
* **指派**: `Atlas`
* **状态**: `[TODO]`
* **描述**: 修复清理过期禁言时 `new Thread()` 乱用的泄漏逻辑。利用 Minecraft 原生转换器重构 Loot 系统的英文 TAG 直爆缺陷。 

### 任务 08: 找回 Alpha 进程守护荣光
* **指派**: `Aegis`
* **状态**: `[IN-PROGRESS]`
* **描述**: 恢复被注释遗弃的 `ProcessManager.startServer(...)` 核心调起逻辑。保证当 Minecraft 服务端进程崩塌时，Alpha 能够自发重启它并接手控制台接管。
