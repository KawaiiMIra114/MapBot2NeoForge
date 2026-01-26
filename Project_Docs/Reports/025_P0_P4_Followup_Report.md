# Task #025: P0-P4 一次性复查与补齐报告

> 任务编号: #025  
> 优先级: P0-P4  
> 完成时间: 2026-01-26  
> 说明: 基于 `Report_01_Continue.md` 的 P0-P4 清单进行复查与补齐，并对已标记“完成”的 P0/P1 进行再次验证与修正。

---

## 一、结论摘要

1. **P1 安全修正**：修复 `#addadmin` 在“已有管理员”场景下被普通用户滥用的权限漏洞（现在仅在“无管理员首次初始化”时放行）。  
2. **P2 控制台增强**：WebSocket 控制台支持 `/server <id>` 切换与 `/back` 返回；服务器控制台自动补全 `/` 前缀；命令执行结果回显。并新增 `users.html` 的原生管理页 + `/api/mapbot/*` 管理接口。  
3. **P3 多服联动**：`#status` 汇总所有在线服务器运行参数，可选 `#status all` 展示玩家分布；签到奖励发放支持“按玩家在线服务器集合”进行多服发放；离线则走 CDK。  
4. **P0 数据统一管理**：Reforged 端不再初始化/写入本地 `mapbot_data.json`；禁言/绑定/在线时长等以 Alpha 为唯一数据源（Redis 优先），Mod 端仅做短 TTL 缓存 + 事件上报。  
5. **P4 游戏事件播报**：死亡消息补充维度与坐标；新增进度（Advancement）播报。

---

## 二、关键改动文件

### Alpha Core
- `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/command/impl/AddAdminCommand.java`：修复权限漏洞（系统已有管理员时必须 Admin）。  
- `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/network/LogWebSocketHandler.java`：实现控制台 `/server` 切换、自动补全与回显。  
- `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/network/HttpRequestDispatcher.java`：新增 `/users.html` 静态路由 + `/api/mapbot/*` 管理 API。  
- `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/BridgeProxy.java`：增加多服发放、在线检测、解析 UUID 等能力；绑定冲突在 Alpha 侧判定。  
- `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/bridge/BridgeMessageHandler.java`：处理 Mod 上报的 `playtime_add` 及 Mod 查询 `check_mute/get_qq_by_uuid`。  
- `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/logic/PlaytimeStore.java`：新增在线时长统一存储（Redis 优先）。  
- `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/command/impl/PlaytimeCommand.java`：改为读取 Alpha `PlaytimeStore`。  
- `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/command/impl/TimeCommand.java`：新增 `#time`（按绑定账号查询在线时长，管理员可查他人）。  
- `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/command/impl/StatusCommand.java`：改为汇总多服状态与可选玩家分布。  
- `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/command/impl/SignCommand.java` / `AcceptCommand.java`：使用多服在线检测与多服发放。
- `Mapbot-Alpha-V1/src/main/resources/web/users.html`：新增原生用户数据管理页。

### Reforged Mod
- `MapBot_Reforged/src/main/java/com/mapbot/MapBot.java`：取消本地数据管理器初始化（避免生成/写入 `mapbot_data.json`）。  
- `MapBot_Reforged/src/main/java/com/mapbot/network/BridgeClient.java`：新增 `has_player/resolve_uuid`；增加对 Alpha 的数据查询缓存（禁言/UUID→QQ）；上报在线时长增量。  
- `MapBot_Reforged/src/main/java/com/mapbot/data/PlaytimeManager.java`：改为仅追踪会话并上报 `playtime_add`，不做本地持久化。  
- `MapBot_Reforged/src/main/java/com/mapbot/logic/GameEventListener.java`：禁言判断改走 Alpha；死亡消息补坐标；新增进度播报。

---

## 三、构建验证

```powershell
# Alpha
cd Mapbot-Alpha-V1
.\gradlew.bat build --no-daemon

# Reforged
cd MapBot_Reforged
.\gradlew.bat build --no-daemon
```

结果：双端 `BUILD SUCCESSFUL`。

