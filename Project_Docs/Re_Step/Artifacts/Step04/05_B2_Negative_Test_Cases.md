# B2 否定测试用例 (Step-04 修复后)

> 更新时间: 2026-02-15T16:40:00+08:00

## B-01: 角色模型否定用例

| # | 输入 | 预期行为 |
|---|---|---|
| N1 | `ContractRole.fromLegacy(null)` | 返回 `Optional.empty()` |
| N2 | `ContractRole.fromString("UNKNOWN")` | 返回 `Optional.empty()` |
| N3 | `ContractRole.fromString("")` | 返回 `Optional.empty()` |
| N4 | `hasContractPermission(token, OWNER)` 当用户角色为 VIEWER | 返回 `false` (USER < OWNER) |
| N5 | `hasContractPermission(null, ADMIN)` | 返回 `false` |
| N6 | `hasContractPermission(expiredToken, ADMIN)` | 返回 `false` (TokenInfo=null) |

## B-02: AUTH-403 否定用例

| # | 请求 | 预期响应 |
|---|---|---|
| N7 | `GET /api/users` TOKEN=VIEWER | `403 {"errorCode":"AUTH-403","error":"Permission denied. OWNER required."}` |
| N8 | `POST /api/servers/x/command` TOKEN=VIEWER | `403 {"errorCode":"AUTH-403","error":"Permission denied. ADMIN required."}` |
| N9 | `GET /api/config` TOKEN=OPERATOR | `403 {"errorCode":"AUTH-403","error":"Permission denied. OWNER required."}` |
| N10 | `GET /api/mapbot/data` TOKEN=VIEWER | `403 {"errorCode":"AUTH-403","error":"Permission denied. OWNER required."}` |
| N11 | 无 TOKEN 请求 | `403 AUTH-403` (token 验证失败) |

## B-03: 配置 fail-closed 否定用例

| # | 场景 | 预期行为 |
|---|---|---|
| N12 | `alpha.properties` 含 `foo.bar=123` | 校验失败: "未知配置键: 'foo.bar'" |
| N13 | `redis.port=abc` | 校验失败: "不是有效的整数" |
| N14 | `redis.port=99999` | 校验失败: "值超出范围 (允许 1~65535)" |
| N15 | `redis.enabled=maybe` | 校验失败: "不是有效的布尔值" |
| N16 | `redis.database=16` | 校验失败: "值超出范围 (允许 0~15)" |

## B-04: reload 事务否定用例

| # | 场景 | 预期行为 |
|---|---|---|
| N17 | 配置文件不存在时 `#reload` | `ReloadResult.failure()` + 保持旧配置 |
| N18 | 新配置含未知键时 `#reload` | `ReloadResult.failure()` + 保持旧版本号 |
| N19 | 新配置类型错误时 `#reload` | `ReloadResult.failure()` + 保持旧版本号 |
| N20 | 应用过程异常时 | 回滚到旧快照 + 保持旧版本号 |

## 静态验证结果

| 检查项 | grep 命令 | 结果 |
|---|---|---|
| legacy 鉴权遗留 | `hasPermission\(token.*Role\.(ADMIN\|OPERATOR\|VIEWER)` | ✅ 0 匹配 |
| 200+错误文本遗留 | `sendJson.*error.*Permission` | ✅ 0 匹配 |
