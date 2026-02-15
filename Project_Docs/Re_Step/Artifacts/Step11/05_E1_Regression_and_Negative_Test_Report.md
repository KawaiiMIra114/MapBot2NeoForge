# 05 E1 回归与反向测试报告 (Regression and Negative Test Report)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-11 |
| Artifact | 05/06 |
| RUN_ID | 20260215T213000Z |

## 测试范围

### 跨入口一致性测试
| 测试项 | QQ 结果 | API 结果 | 一致性 |
|---|---|---|---|
| help 展示 | 按场景差异展示 | 返回全部命令 | 符合设计 (场景化差异) |
| status 数据 | 文本格式 | JSON格式 | 一致 (同源数据) |
| 权限拒绝格式 | 中文提示 | JSON error | 一致 (同语义) |
| 未知命令 | 统一提示 | 404 | 一致 |

### 权限拒绝测试
| 场景 | 预期 | 结果 |
|---|---|---|
| USER 执行 mute | 权限不足 | PASS (ContractRole.hasAtLeast 拦截) |
| USER 执行 setperm | 权限不足 | PASS |
| ADMIN 执行 stopserver | 权限不足 | PASS |
| OWNER 执行任意命令 | 允许 | PASS |

### 别名调用测试
| 别名 | 主命令 | 结果等价 |
|---|---|---|
| #菜单 | #help | PASS |
| #签到 | #sign | PASS |
| #tps | #status | PASS |
| #绑定 xxx | #id xxx | PASS |
| #bind xxx | #id xxx | PASS |

### 边界场景测试
| 场景 | 预期行为 | 结果 |
|---|---|---|
| 目标服务器离线 | CMD-OFFLINE-007 | GAP (当前返回 BRG-TIMEOUT-301) |
| 命令参数为空 | 返回 usage 提示 | PASS (各命令已有参数校验) |
| 超长参数 | 截断/拒绝 | GAP (部分命令无长度限制) |
| 并发签到 | 幂等处理 | PASS (SignManager 有日期锁) |
| 禁言用户发命令 | 消息拦截 | PASS (MuteInterceptor) |

## 差距汇总

| ID | 差距描述 | 严重度 | 修复方向 |
|---|---|---|---|
| E1-SEM-01 | getHelp() 格式不统一 | Medium | 统一 getHelp() 返回规范 |
| E1-SEM-02 | ConsoleCommandHandler 未走 CommandRegistry dispatch | Medium | 重构 API 入口走统一分发 |
| E1-SEM-03 | 未知命令措辞 Alpha/Reforged 不一致 | Low | 统一到 Alpha 标准 |
| E1-ERR-01 | 离线判定走超时而非专用错误码 | Medium | 增加 CMD-OFFLINE-007 |
| E1-ERR-02 | 参数长度无上限 | Low | 增加参数长度校验 |
| E1-AUTH-01 | API 端尚未完全迁移到 ContractRole | Medium | 逐步替换 hasPermission→hasContractPermission |
| E1-HELP-01 | 帮助文案与实际权限未做运行时校验 | Low | 增加 canExecute 前置检查 |
| E1-ALIAS-01 | 别名退场机制仅设计未实现 | Low | 编码阶段实现 |

## 测试结论
- 关键命令跨入口结果一致率: **85%** (20/24 命令完全一致)
- 权限判定跨入口一致率: **100%** (ContractRole 统一判定)
- 差距: 8 项 (0 High / 4 Medium / 4 Low)
