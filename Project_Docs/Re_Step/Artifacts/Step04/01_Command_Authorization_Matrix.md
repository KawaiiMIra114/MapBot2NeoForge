# 命令权限矩阵 (Step-04 B2 修复后)

> 更新时间: 2026-02-15T16:40:00+08:00
> 修复内容: 全部鉴权决策已迁移到 ContractRole(USER/ADMIN/OWNER) 模型

## 合同角色定义

| ContractRole | Legacy Role | 权限说明 |
|---|---|---|
| USER | VIEWER | 只读访问 |
| ADMIN | OPERATOR | 可执行操作命令 |
| OWNER | ADMIN | 完全控制（用户管理、配置变更） |

## HTTP API 权限矩阵

| API 路径 | 方法 | 所需角色 | 拒绝响应 | 副作用 |
|---|---|---|---|---|
| `/api/config` | GET/POST | OWNER | 403 AUTH-403 | 无 |
| `/api/servers/*/command` | POST | ADMIN | 403 AUTH-403 | 无 |
| `/api/servers/*/restart` | POST | ADMIN | 403 AUTH-403 | 无 |
| `/api/servers/*/stop` | POST | ADMIN | 403 AUTH-403 | 无 |
| `/api/remote/*` | ALL | ADMIN | 403 AUTH-403 | 无 |
| `/api/users*` | ALL | OWNER | 403 AUTH-403 | 无 |
| `/api/mapbot*` | ALL | OWNER | 403 AUTH-403 | 无 |

## QQ 命令权限矩阵

| 命令 | requiresAdmin | requiredPermLevel | adminGroupOnly |
|---|---|---|---|
| `#reload` | ✅ | 0 | ❌ |
| `#addadmin` | ✅* | 0 | ❌ |
| `#status` | ❌ | 0 | ❌ |
| `#bind` | ❌ | 0 | ❌ |
| `#sign` | ❌ | 0 | ❌ |

> *`#addadmin` 在无管理员时允许首个注册*

## 拒绝响应格式

所有越权拒绝统一为:
```json
HTTP 403 Forbidden
{"errorCode":"AUTH-403","error":"Permission denied. OWNER/ADMIN required."}
```

## 审计日志

未知角色映射失败会输出 WARN 级别审计日志:
```
[AUDIT] 鉴权拒绝: 未知角色映射失败 user=xxx legacyRole=xxx required=xxx
```
