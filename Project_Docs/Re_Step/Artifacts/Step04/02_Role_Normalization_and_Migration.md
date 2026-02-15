# 角色规范化与迁移 (Step-04 B2 修复后)

> 更新时间: 2026-02-15T16:40:00+08:00
> 新文件: `ContractRole.java`

## 合同角色模型

```java
public enum ContractRole {
    USER,   // 普通用户 — 只读
    ADMIN,  // 管理员 — 可执行操作
    OWNER;  // 所有者 — 完全控制
}
```

## Legacy 映射表

| Legacy Role | ContractRole | 映射方法 |
|---|---|---|
| VIEWER | USER | `ContractRole.fromLegacy()` |
| OPERATOR | ADMIN | `ContractRole.fromLegacy()` |
| ADMIN | OWNER | `ContractRole.fromLegacy()` |
| 未知/null | — | 返回 `Optional.empty()` → 拒绝 |

## 字符串解析

`ContractRole.fromString()` 支持:
- 合同名: `user`, `admin`, `owner` (大小写不敏感)
- Legacy 名: `VIEWER`, `OPERATOR` (向后兼容)
- 未知值 → `Optional.empty()`

## 权限判断

```java
// 推荐入口 (Step-04 B2)
AuthManager.INSTANCE.hasContractPermission(token, ContractRole.OWNER);
AuthManager.INSTANCE.getContractRole(token);

// Legacy 入口 (仍可用，但不推荐)
AuthManager.INSTANCE.hasPermission(token, Role.ADMIN);
```

## 未知角色拒绝审计

未知角色映射失败时:
1. `hasContractPermission()` 返回 `false`
2. 记录 WARN 审计日志
3. 调用方应返回 HTTP 403 + AUTH-403

## 变更影响

| 文件 | 变更 |
|---|---|
| `ContractRole.java` | **新建** — 合同角色枚举 |
| `AuthManager.java` | 新增 `hasContractPermission()` / `getContractRole()` |
| `HttpRequestDispatcher.java` | 所有 `hasPermission(Role.X)` → `hasContractPermission(ContractRole.Y)` |
