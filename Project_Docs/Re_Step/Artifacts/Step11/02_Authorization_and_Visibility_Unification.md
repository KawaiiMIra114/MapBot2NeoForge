# 02 鉴权与可见性统一设计 (Authorization and Visibility Unification)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-11 |
| Artifact | 02/06 |
| RUN_ID | 20260215T213000Z |

## 鉴权判定链路

### 当前架构
```
QQ消息 → InboundHandler.processCommand()
       → CommandRegistry.dispatch(cmd, args, context)
       → ICommand.getRequiredPermission() → ContractRole check
       → ICommand.execute()
```

### 权限层级
| ContractRole | Legacy Role | 数值 | 可执行范围 |
|---|---|---|---|
| USER | VIEWER | 0 | 基础命令 (help/list/status/sign/accept...) |
| ADMIN | OPERATOR | 1 | 管理命令 (mute/unmute/reload/adminunbind...) |
| OWNER | ADMIN | 2 | 最高权限 (setperm/addadmin/removeadmin/stopserver...) |

### 鉴权判定流程
1. `ICommand.getRequiredPermission()` 返回所需 `ContractRole`
2. `CommandRegistry.dispatch()` 调用 `AuthManager.hasContractPermission()`
3. 失败: 返回统一拒绝语义 (无泄露具体原因)
4. 成功: 执行命令并返回回执

### 可见性策略
| 场景 | 帮助展示范围 | 说明 |
|---|---|---|
| 玩家群 | USER 命令 | 仅展示基础功能命令 |
| 管理群 | ADMIN 命令 | 默认展示管理命令,`#help all` 展示全部 |
| 私聊 | USER 命令 | 同玩家群策略 |
| API控制面 | 全部 | 按token角色过滤 |

## 鉴权差异与统一方案

### 差异项
| ID | 差异 | 当前状态 | 统一方案 |
|---|---|---|---|
| AUTH-01 | QQ 端通过 DataManager.getPermLevel() 判定 | 已映射到 ContractRole | 保持 ContractRole 为唯一判定源 |
| AUTH-02 | API 端通过 AuthManager.hasPermission(token, Role) | 已有 hasContractPermission() | 逐步迁移到 hasContractPermission() |
| AUTH-03 | 拒绝码未标准化 | 部分返回中文提示 | 统一为 AUTH-403 + 本地化消息 |

## 拒绝语义标准
```
权限不足: "你没有执行此命令的权限"
无效指令: "[提示] 未知命令,输入 #help 查看帮助"
参数错误: "[错误] 用法: #命令 <参数>"
```
