# 01 命令语义盘点表 (Command Semantics Inventory)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-11 |
| Artifact | 01/06 |
| RUN_ID | 20260215T213000Z |

## 命令清单

### Alpha 端 (24 命令 + 13 别名)

| command_id | 入口 | 权限级 | 别名 | 语义说明 |
|---|---|---|---|---|
| help | QQ/API | USER | 菜单 | 帮助菜单,按场景差异展示 |
| list | QQ | USER | 在线 | 在线玩家列表 |
| status | QQ | USER | 状态,tps | 服务器状态与TPS |
| id | QQ | USER | 绑定,bind | 绑定QQ-MC账号 |
| unbind | QQ | USER | 解绑 | 解绑QQ-MC账号 |
| sign | QQ | USER | 签到 | 每日签到 |
| accept | QQ | USER | 领取 | 领取签到奖品 |
| inv | QQ | USER | - | 查看背包 |
| location | QQ | USER | - | 查看位置 |
| playtime | QQ | USER | - | 游戏时长查询 |
| time | QQ | USER | - | 服务器时间查询 |
| cdk | QQ | USER | - | CDK兑换 |
| myperm | QQ | USER | - | 查看自己权限 |
| mute | QQ | ADMIN | - | 禁言玩家 |
| unmute | QQ | ADMIN | - | 解除禁言 |
| setperm | QQ | OWNER | - | 设置用户权限 |
| addadmin | QQ | OWNER | - | 添加管理员 |
| removeadmin | QQ | OWNER | - | 移除管理员 |
| adminunbind | QQ | ADMIN | - | 强制解绑 |
| agreeunbind | QQ | ADMIN | - | 同意解绑申请 |
| reload | QQ/API | ADMIN | - | 热重载配置 |
| stopserver | QQ | OWNER | - | 停止服务器 |
| cancelstop | QQ | OWNER | - | 取消停服 |

### Reforged 端 (桥接模式,仅分发)

| command_id | 入口 | 说明 |
|---|---|---|
| help | QQ→Bridge | 与Alpha同步,Reforged仅转发 |
| 其他 | QQ→Bridge | Reforged 的CommandRegistry镜像Alpha注册 |

## 语义差异分析

### 已发现差异项
| ID | 差异描述 | 严重度 | 修复方向 |
|---|---|---|---|
| SEM-01 | getHelp() 返回格式不统一 (部分含#前缀,部分不含) | Medium | 统一为不含#前缀的描述 |
| SEM-02 | ConsoleCommandHandler 与 QQ CommandRegistry 入口分离 | Medium | API入口走统一dispatch |
| SEM-03 | 未知命令回执在Alpha/Reforged措辞不同 | Low | 统一为Alpha标准 |
| SEM-04 | 别名缺乏版本化退场机制 | Medium | 增加deprecation窗口 |

## 命令注册统计
- Alpha CommandRegistry: 24 命令注册 + 13 别名
- Reforged CommandRegistry: 镜像注册 (桥接转发)
- 入口: QQ群消息(#前缀) / API控制面(/命令) / Bridge内部调用
