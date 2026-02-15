# Step04 Sub-agent Counterevidence Rounds

## 轮次 1
- 主代理问题: `/api/users*` 与 `/api/mapbot*` 越权路径是否统一 `AUTH-403`？
- 子代理回答:
  - `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/network/HttpRequestDispatcher.java:580-582` 返回 200 JSON。
  - `Mapbot-Alpha-V1/src/main/java/com/mapbot/alpha/network/HttpRequestDispatcher.java:671-674` 返回 200 JSON。
- 主代理复核与追加问题:
  - 复核成立，未满足合同 `COMMAND_AUTHORIZATION_CONTRACT.md:61`。
  - 追加: 拒绝路径是否有副作用执行风险？

## 轮次 2
- 主代理问题: 角色集合是否严格限制为 `user/admin/owner`，未知角色是否拒绝并审计？
- 子代理回答:
  - `AuthManager.Role` 为 `VIEWER/OPERATOR/ADMIN`（`AuthManager.java:748-752`）。
  - `/api/users` 使用 `Role.valueOf`，未知值走异常而非显式拒绝（`HttpRequestDispatcher.java:605-607,648`）。
- 主代理复核与追加问题:
  - 复核成立，违反 `COMMAND_AUTHORIZATION_CONTRACT.md:24-26,89`。
  - 追加: owner 缺失时系统是否阻断运行？

## 轮次 3
- 主代理问题: 配置是否满足 unknown-key fail-closed 与热重载回滚闭环？
- 子代理回答:
  - `AlphaConfig.load` 未比对未知键，未知键被忽略（`AlphaConfig.java:56-110`）。
  - `ReloadCommand` 没有回滚事务（`ReloadCommand.java:16-20`）。
  - owner/admin 可为空继续运行（`AuthManager.java:240-245`）。
- 主代理复核与追加问题:
  - 复核成立，违反 `CONFIG_SCHEMA_CONTRACT.md:53-54,60`。
  - 追加: 风险是否可量化并绑定门禁？

## 轮次 4
- 主代理问题: 给出量化阻断标准与门禁映射。
- 子代理回答:
  - High 风险 3 类：角色失配、越权码不统一、热重载无回滚。
  - 量化标准：`High 阻断项 == 0` 才允许 GO B3。
- 主代理复核:
  - 采纳，落地为 `gate06_blocking`。
  - 闭环状态: 已量化、可验证、可追溯。
