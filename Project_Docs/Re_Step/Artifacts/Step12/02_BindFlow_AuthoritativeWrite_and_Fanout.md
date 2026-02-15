# 02 绑定链路权威写入与分发设计 (BindFlow Authoritative Write and Fanout)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-12 |
| Artifact | 02/06 |
| RUN_ID | 20260215T214100Z |

## 当前实现分析

### Alpha 端 (BindCommand)
1. 参数校验 → Mojang API 查 UUID
2. `DataManager.bind(qq, uuid)` → 权威数据写入 (JSON 文件)
3. `BridgeProxy.syncWhitelistToAllServers(uuid)` → fan-out 白名单
4. 返回成功回执

### Reforged 端 (BridgeHandlers.handleBindPlayer)
1. 接收 Alpha 的 bind_player 消息
2. 解析 UUID → GameProfile
3. `addToWhitelist(server, profile)` → 本地白名单添加
4. 返回 proxy_response

## 权威先行策略

### 设计原则
1. **权威数据先落盘**: DataManager.bind() 必须在 fan-out 前完成
2. **fan-out 可重试**: 白名单分发失败不影响权威数据
3. **回执聚合**: 所有子服结果汇总后返回用户

### 当前差距
| ID | 差距 | 严重度 | 修复方向 |
|---|---|---|---|
| BIND-01 | fan-out 无统一聚合 (各子服独立) | High | 实现 AckAggregator |
| BIND-02 | fan-out 失败无重试 | Medium | 增加定时重试 |
| BIND-03 | 无 requestId 级追踪 | Medium | 增加 requestId 打点 |
| BIND-04 | Mojang API 超时无降级 | Low | 增加缓存+降级策略 |

### 幂等策略
- `DataManager.bind()` 已有重复检测 (同QQ号已绑定判断)
- 白名单 `addToWhitelist()` 已有 `isWhiteListed()` 前置检查
- fan-out 消息幂等: 重复分发不会导致数据异常

## 回执语义
```
成功: "✅ 绑定成功! 游戏名: {name}, 已自动添加白名单"
部分失败: "⚠️ 绑定成功但部分服务器白名单同步失败 (将自动重试)"
失败: "❌ 绑定失败: {原因}"
```
