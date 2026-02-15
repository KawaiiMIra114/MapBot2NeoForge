# 03 错误码与 HTTP 映射 (ErrorCode And HTTP Mapping)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-13 |
| Artifact | 03/05 |
| RUN_ID | 20260215T215300Z |

## Bridge 错误码 → HTTP 映射

### 已有 BRG_* 码 (BridgeErrorMapper)
| BRG 错误码 | 含义 | HTTP | QQ 文案 |
|---|---|---|---|
| BRG-AUTH-101 | 认证失败 | 401 | - (内部) |
| BRG-AUTH-102 | Token 过期 | 401 | - (内部) |
| BRG-VALIDATION-201 | 协议校验失败 | 400 | "[错误] 请求格式错误" |
| BRG-TIMEOUT-301 | 响应超时 | 504 | "[超时] 服务器响应超时" |
| BRG-EXEC-401 | 服务端执行失败 | 500 | "[错误] 命令执行出错" |
| BRG-EXECUTION-402 | 执行失败(离线/未知物品) | 409 | "[错误] 操作失败" |

### E1 新增 CMD_* 码
| CMD 错误码 | 含义 | HTTP | QQ 文案 |
|---|---|---|---|
| CMD-PERM-001 | 权限不足 | 403 | "你没有执行此命令的权限" |
| CMD-ARGS-002 | 参数错误 | 400 | "用法: #{cmd} {usage}" |
| CMD-BIND-003 | 未绑定 | 400 | "请先使用 #id 绑定" |
| CMD-STATE-004 | 状态冲突 | 409 | "操作无法在当前状态执行" |
| CMD-COOL-005 | 冷却中 | 429 | "请等待 {n} 秒后再试" |
| CMD-TARGET-006 | 目标不存在 | 404 | "找不到指定的玩家" |
| CMD-OFFLINE-007 | 目标离线 | 503 | "指定服务器当前离线" |

### HTTP 映射规则
| HTTP 状态码 | 使用场景 | 对应 BRG/CMD |
|---|---|---|
| 200 | 成功 | - |
| 202 | PENDING (待确认) | - |
| 400 | 参数错误 | BRG-VALIDATION-201, CMD-ARGS-002, CMD-BIND-003 |
| 401 | 认证失败 | BRG-AUTH-101/102 |
| 403 | 权限不足 | CMD-PERM-001 |
| 404 | 目标不存在 | CMD-TARGET-006 |
| 409 | 冲突/执行失败 | BRG-EXECUTION-402, CMD-STATE-004 |
| 429 | 限流 | CMD-COOL-005 |
| 500 | 服务端错误 | BRG-EXEC-401 |
| 503 | 服务不可用 | CMD-OFFLINE-007 |
| 504 | 超时 | BRG-TIMEOUT-301 |

## 双栈策略
```
优先: 结构化错误 → {errorCode: "BRG-TIMEOUT-301", message: "...", retryable: true}
兜底: 字符串错误 → {errorCode: "BRG-INTERNAL-999", message: "未知错误", retryable: false}
```

### 当前差距
| ID | 差距 | 严重度 |
|---|---|---|
| ERR-01 | sendProxyResponse 返回 "FAIL:..." 非结构化 | High |
| ERR-02 | API 端无统一 errorCode 返回 | High |
| ERR-03 | BridgeErrorMapper 仅在 Reforged 端 | Medium |
| ERR-04 | 兜底 BRG-INTERNAL-999 未实现 | Medium |
| ERR-05 | 重试指引未包含在错误返回中 | Low |
