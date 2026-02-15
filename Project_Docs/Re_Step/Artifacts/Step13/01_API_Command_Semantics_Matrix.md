# 01 API-命令语义矩阵 (API Command Semantics Matrix)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-13 |
| Artifact | 01/05 |
| RUN_ID | 20260215T215300Z |

## 高风险动作语义矩阵

### bind (绑定)
| 维度 | QQ 入口 | API 入口 | Bridge 回执 | 一致性 |
|---|---|---|---|---|
| 入参 | `#id <游戏名>` | `POST /api/bind {qq, name}` | proxy_request(bind_player) | ✅ |
| 成功 | 文本: "绑定成功" | `{success:true, state:"SUCCESS"}` | `SUCCESS:<uuid>:<name>` | ⚠️ 格式不同 |
| 失败 | 文本: "[错误] ..." | `{success:false, errorCode:"BRG_*"}` | `FAIL:<reason>` | ⚠️ 错误码不统一 |
| pending | 无此状态 | 应返回 `{state:"PENDING"}` | 5秒超时 | ❌ QQ无pending |
| requestId | ❌ 无 | ❌ 无 | ✅ 有 | ❌ 不一致 |

### unbind (解绑)
| 维度 | QQ 入口 | API 入口 | Bridge 回执 | 一致性 |
|---|---|---|---|---|
| 入参 | `#unbind` | `POST /api/unbind {qq}` | proxy_request(whitelist_remove) | ✅ |
| 成功 | 文本: "解绑成功" | `{success:true}` | `SUCCESS` | ⚠️ |
| 失败 | 文本: "[错误] ..." | `{success:false}` | `FAIL:<reason>` | ⚠️ |

### switch_server (切服)
| 维度 | QQ 入口 | API/Console 入口 | Bridge 回执 | 一致性 |
|---|---|---|---|---|
| 入参 | 无直接命令 | `/mapbot transfer <player> <server>` | proxy_request(switch_server) | N/A |
| 成功 | N/A | 文本: "已执行 /transfer" | `SUCCESS:已执行...` | ⚠️ 非结构化 |
| 超时 | N/A | 文本: "超时" | CompletableFuture timeout | ⚠️ 非PENDING |

### reload (重载)
| 维度 | QQ 入口 | API 入口 | Bridge 回执 | 一致性 |
|---|---|---|---|---|
| 入参 | `#reload` | `GET /api/reload` | N/A | ✅ |
| 成功 | "重载成功" | `{success:true}` | N/A | ✅ |
| 失败 | "重载失败" | `{success:false}` | N/A | ✅ |

### execute_command (执行指令)
| 维度 | QQ 入口 | API 入口 | Bridge 回执 | 一致性 |
|---|---|---|---|---|
| 入参 | N/A | `POST /api/command {cmd}` | proxy_request(execute_command) | N/A |
| 成功 | N/A | `{success:true, output: "..."}` | `SUCCESS:...` | ⚠️ |
| 超时 | N/A | 5秒超时 | 5秒超时 | ✅ |

## 统一语义标准

### 必备返回字段
| 字段 | 说明 | 必须 |
|---|---|---|
| requestId | 请求唯一标识 | ✅ |
| state | SUCCESS/FAILED/PENDING | ✅ |
| errorCode | BRG_* 错误码 | 仅失败时 |
| message | 人类可读消息 | ✅ |

### 差距汇总
| ID | 差距 | 严重度 |
|---|---|---|
| API-01 | QQ 入口无 requestId | High |
| API-02 | API 返回无统一 state 字段 | High |
| API-03 | Bridge 回执 SUCCESS:/FAIL: 前缀非结构化 | Medium |
| API-04 | QQ 无 PENDING 状态展示 | Medium |
| API-05 | switch_server 超时=假成功风险 | High |
