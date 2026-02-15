# 02 ProtocolVersion and Capability Design

## 元数据
| 属性 | 值 |
|---|---|
| Step | Step-08 D1 |
| RUN_ID | 20260215T201100Z |
| 依据 | VERSIONING_AND_COMPATIBILITY.md §3.1 + Step07 Artifact 03 |

## 1. 协商流程

### 1.1 注册阶段
```
Reforged → Alpha (register):
{
  "action": "register",
  "serverName": "...",
  "token": "...",
  "protocol_version": "1.0.0",
  "capabilities": ["IDEMPOTENT_DEDUP", "STRUCTURED_ERROR"]
}

Alpha → Reforged (register_ack):
{
  "action": "register_ack",
  "status": "SUCCESS" | "VERSION_MISMATCH",
  "protocol_version": "1.0.0",
  "capabilities": ["IDEMPOTENT_DEDUP", "STRUCTURED_ERROR"],
  "errorCode": null | "BRG_VALIDATION_206"
}
```

### 1.2 请求阶段
```
每条跨端请求携带:
{
  "protocol_version": "1.0.0",
  "request_id": "uuid",
  ...existing fields...
}
```

## 2. 兼容矩阵

### 2.1 MAJOR 门禁
| 条件 | 动作 |
|---|---|
| MAJOR 相同 | 接受 |
| MAJOR 不同 | 拒绝 + `BRG_VALIDATION_206` (新增) |
| protocol_version 缺失 | 临时接受 + 告警日志 (到期 2026-03-08 后拒绝) |

### 2.2 MINOR 告警
| 条件 | 动作 |
|---|---|
| MINOR 差 ≤ 2 | 正常处理 |
| MINOR 差 ≥ 3 | 告警 + `PROTOCOL_VERSION_STALE` 日志 |

## 3. 新增错误码 BRG_VALIDATION_206
| 字段 | 值 |
|---|---|
| 错误码 | BRG_VALIDATION_206 |
| 含义 | protocol_version 不兼容或缺失 |
| retryable | false |
| 动作 | 拒绝连接/请求 |

## 4. 能力位 (Capabilities)
| 能力 | 含义 | 初始状态 |
|---|---|---|
| IDEMPOTENT_DEDUP | 支持幂等去重 | 可选 |
| STRUCTURED_ERROR | 支持结构化错误码 | 默认启用 |

## 5. 代码改造点
| 文件 | 改造 |
|---|---|
| BridgeClient (Reforged) | 注册消息添加 protocol_version + capabilities |
| BridgeServer (Alpha) | 注册处理校验 MAJOR + 记录能力 |
| BridgeMessageHandler (Alpha) | 请求中读取并校验 protocol_version |
| BridgeProxy (Alpha) | 发送请求附带 protocol_version |
| BridgeErrorMapper (Alpha) | 新增 BRG_VALIDATION_206 |

## 6. 审计字段
| 字段 | 值 |
|---|---|
| 协商后日志 | `LOGGER.info("注册成功: server={}, version={}, caps={}",...)` |
| 拒绝日志 | `LOGGER.warn("版本不兼容: remote={}, local={}, errorCode=BRG_VALIDATION_206")` |
| 缺失告警 | `LOGGER.warn("protocol_version 缺失, 临时接受: server={}")` |
