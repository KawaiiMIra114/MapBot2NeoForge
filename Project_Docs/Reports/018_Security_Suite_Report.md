# Task #018 执行报告: 综合管理与安全套件

**执行者**: Lazarus
**日期**: 2026-01-20
**状态**: ✅ 完成

---

## 任务目标

建立完整的权限分级体系，实现基于时间的禁言系统，并重构命令架构以提高可维护性。

---

## 变更内容

### 1. 数据层升级 (`DataManager`)

*   **新增字段**:
    *   `Map<Long, Integer> userPermissions`: 存储用户权限等级 (0=User, 1=Mod, 2=Admin)。
    *   `Map<String, Long> mutedPlayers`: 存储禁言玩家 UUID 及解禁时间戳。
*   **迁移逻辑**: 自动将旧版 `admins` 列表迁移为 `userPermissions` (Level 2)。

### 2. 权限分级系统

| 等级 | 常量 | 描述 |
| :--- | :--- | :--- |
| **Level 0** | `USER` | 普通用户，仅基础查询 |
| **Level 1** | `MOD` | 协管员，可执行 `#mute`, `#unmute`, `#inv` |
| **Level 2** | `ADMIN` | 管理员，可执行 `#setperm`, `#stopserver` |

### 3. 命令架构重构

*   **ICommand 接口**: 定义了 `execute()` 和 `getRequiredLevel()`。
*   **CommandRegistry**: 统一负责命令的分发、权限检查和别名管理。
*   **混合模式**: `InboundHandler` 优先尝试分发到新架构命令，未找到则回退到旧版 `switch-case`，确保兼容性。

### 4. 新增管理命令

| 命令 | 权限 | 描述 |
| :--- | :--- | :--- |
| `#mute <玩家> [时长]` | Mod+ | 禁言玩家 (支持 s/m/h/d/forever) |
| `#unmute <玩家>` | Mod+ | 解除禁言 |
| `#setperm <QQ> <等级>` | Admin | 提拔/降职用户 |
| `#myperm` | User | 查看自身权限 |

### 5. 游戏内禁言拦截

*   **修改文件**: `GameEventListener.java`
*   **逻辑**: 在 `onServerChat` 事件最前端拦截。
*   **行为**: 
    *   如果玩家在禁言名单且未过期 -> `event.setCanceled(true)` -> 发送红字提示。
    *   如果已过期 -> 自动解禁并放行。

---

## 测试用例

1.  **提拔协管**:
    *   Admin 执行: `#setperm 123456 1` -> 提示成功。
    *   QQ 123456 执行 `#myperm` -> 显示 Level 1 (Mod)。

2.  **执行禁言**:
    *   Mod 执行: `#mute Steve 10m` -> 提示已禁言。
    *   Steve 在游戏内说话 -> 聊天栏显示 "你已被禁言！解除时间: 2026-01-20 12:10:00"。

3.  **权限越级**:
    *   User 执行 `#mute Steve` -> 提示 "权限拒绝"。

---

**签名**: Lazarus - MapBot Reforged 开发执行者
