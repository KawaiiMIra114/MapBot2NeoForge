# 04 兼容窗口与退场计划 (Compatibility And Deprecation Plan)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-13 |
| Artifact | 04/05 |
| RUN_ID | 20260215T215300Z |

## 退场路径

### 旧字段退场
| 旧字段/格式 | 替代 | N (当前) | N+1 (警告) | N+2 (移除) |
|---|---|---|---|---|
| `SUCCESS:...` 前缀 | `{state:"SUCCESS"}` | 保留 | 双返回 | 移除前缀 |
| `FAIL:...` 前缀 | `{state:"FAILED", errorCode:"BRG_*"}` | 保留 | 双返回 | 移除前缀 |
| 纯文本错误 | 结构化 errorCode | 保留 | 追加 errorCode | 仅结构化 |
| 无 requestId | 所有返回含 requestId | 无此字段 | 追加字段 | 强制必须 |
| 无 state 字段 | 统一三态 | 无此字段 | 追加字段 | 强制必须 |

### 版本时间窗
| 阶段 | 版本 | 行为 | 预计时间 |
|---|---|---|---|
| N (当前) | v5.5.x | 保持旧格式 | 当前 |
| N+1 (双格式) | v5.6.x | 旧+新双返回 | +30天 |
| N+2 (新格式) | v6.0.x | 仅新格式 | +60天 |

### 客户端迁移门禁
1. 新客户端: 优先解析 `errorCode` + `state`
2. 旧客户端: 解析 `SUCCESS:`/`FAIL:` 前缀
3. N+1 期间: 双格式共存,客户端按能力选择
4. N+2 门禁: 旧客户端必须升级到新解析

### 回退开关
```java
// ConfigManager
boolean useLegacyFormat = config.getBoolean("bridge.legacy_format", false);
// 在紧急情况下可回退到旧格式
```

## 权限语义统一

### 角色映射 (E1 已定义)
| 角色 | API | QQ | 说明 |
|---|---|---|---|
| USER | token.role="user" | QQ号默认 | 基础权限 |
| ADMIN | token.role="admin" | setperm授权 | 管理权限 |
| OWNER | token.role="owner" | 配置文件 | 最高权限 |

### 权限拒绝统一文案
```
API: {state:"FAILED", errorCode:"CMD-PERM-001", message:"权限不足"}
QQ:  "你没有执行此命令的权限"
```

### 安全规则
1. 【硬规则】管理群身份 ≠ 权限依据,仅 ContractRole 判定
2. 【硬规则】拒绝文案不泄露 token/路径/堆栈
3. 【硬规则】未定义角色一律拒绝

## 差距
| ID | 差距 | 严重度 |
|---|---|---|
| COMPAT-01 | 无双格式共存机制 | Medium |
| COMPAT-02 | 无回退开关 | Medium |
| COMPAT-03 | 无客户端版本检测 | Low |
