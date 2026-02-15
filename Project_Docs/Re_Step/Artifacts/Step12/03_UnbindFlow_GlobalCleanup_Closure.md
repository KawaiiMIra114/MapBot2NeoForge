# 03 解绑链路全服闭环设计 (UnbindFlow Global Cleanup Closure)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-12 |
| Artifact | 03/06 |
| RUN_ID | 20260215T214100Z |

## 当前实现分析

### Alpha 端 (UnbindCommand / ForceUnbindCommand / AgreeUnbindCommand)
- `DataManager.unbind(qq)` → 删除权威绑定
- `BridgeProxy.syncWhitelistRemoveToAllServers(uuid)` → fan-out 白名单移除
- 三种解绑入口: 自助/强制/审批

### Reforged 端 (BridgeHandlers.handleWhitelistRemove)
- 接收白名单移除消息
- `whitelist remove {name}` → 执行控制台命令
- 返回 proxy_response

## 全服闭环策略

### 设计原则
1. **解绑触发全服扫描**: 所有已注册子服都必须执行白名单移除
2. **残留检测**: 解绑后应能验证全服残留计数 = 0
3. **失败补偿**: 子服离线时记录 pending task,上线后自动补偿

### 当前差距
| ID | 差距 | 严重度 | 修复方向 |
|---|---|---|---|
| UNBIND-01 | 离线子服白名单残留 | High | 增加 pending cleanup queue |
| UNBIND-02 | 无全服残留验证 | Medium | 增加 whitelist audit |
| UNBIND-03 | 强制解绑无审计日志 | Medium | 增加 AUDIT log |
| UNBIND-04 | AgreeUnbind→生产Unbind 链路复杂 | Low | 简化审批链路 |

### 补偿机制
```
子服离线 → 记录 PENDING_CLEANUP(uuid, serverId, timestamp)
子服上线 → 检查 pending 队列 → 执行 whitelist_remove → 标记 COMPLETED
超时 (24h) → 告警 + 运维介入
```

### 最终一致性判据
- 权威数据: DataManager 中 qq-uuid 映射已删除 ✓
- 子服数据: 全部已注册子服白名单中无该 UUID ✓
- 残留验证: 全服 whitelist audit 通过 ✓
