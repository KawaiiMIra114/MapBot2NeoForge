# 配置 Schema 校验规则 (Step-04 B2 修复后)

> 更新时间: 2026-02-15T16:40:00+08:00
> 新文件: `ConfigSchema.java`

## 设计原则

- **Fail-Closed**: 未知键 → 校验失败 → 中止加载
- **严格类型**: int/long/boolean/string 类型强校验
- **范围约束**: 端口 1-65535、Redis DB 0-15 等
- **白名单制**: 仅注册的键允许存在

## 已注册键白名单

### 连接配置
| 键 | 类型 | 范围 | 必填 |
|---|---|---|---|
| `connection.wsUrl` | string | — | ✅ |
| `connection.reconnectInterval` | int | 1-300 | ✅ |
| `connection.listenPort` | int | 1-65535 | ✅ |
| `server.listenPort` | int | 1-65535 | ❌ (legacy) |
| `connection.bridgePort` | int | 1-65535 | ❌ (legacy) |

### Bridge 配置
| 键 | 类型 | 范围 | 必填 |
|---|---|---|---|
| `bridge.listenPort` | int | 1-65535 | ✅ |

### Minecraft 目标
| 键 | 类型 | 范围 | 必填 |
|---|---|---|---|
| `minecraft.targetHost` | string | — | ✅ |
| `minecraft.targetPort` | int | 1-65535 | ✅ |
| `server.targetMcHost` | string | — | ❌ (legacy) |
| `server.targetMcPort` | int | 1-65535 | ❌ (legacy) |

### Redis 配置
| 键 | 类型 | 范围 | 必填 |
|---|---|---|---|
| `redis.host` | string | — | ✅ |
| `redis.port` | int | 1-65535 | ✅ |
| `redis.password` | string | — | ✅ |
| `redis.database` | int | 0-15 | ✅ |
| `redis.enabled` | boolean | true/false | ✅ |

### 消息配置
| 键 | 类型 | 范围 | 必填 |
|---|---|---|---|
| `messaging.playerGroupId` | long | 0-MAX | ✅ |
| `messaging.adminGroupId` | long | 0-MAX | ✅ |
| `messaging.adminQQs` | string | — | ✅ |
| `messaging.botQQ` | long | 0-MAX | ✅ |

### 安全配置
| 键 | 类型 | 必填 |
|---|---|---|
| `auth.bridge.token` | string | ❌ |
| `auth.bridge.sharedToken` | string | ❌ |
| `auth.consoleToken` | string | ❌ |
| `auth.bridge.allowedServerIds` | string | ❌ |
| `auth.allowedServerIds` | string | ❌ |
| `auth.tokenSecret` | string | ❌ |
| `auth.bootstrapAdmin.*` | string/boolean | ❌ |

### 其他
| 键 | 类型 | 必填 |
|---|---|---|
| `debug.debugMode` | boolean | ✅ |
| `bridge.ingameMsgFormat` | string | ❌ |

## 校验流程

```
alpha.properties -> ConfigSchema.validate(props)
  ├─ 未知键检查 → error
  ├─ 类型检查 → error  
  ├─ 范围检查 → error
  └─ 全部通过 → ValidationResult(passed=true)
```

## 校验失败行为

- `load()`: 中止加载，保持旧配置
- `reload()`: 校验失败返回 `ReloadResult.failure()`，保持旧版本
