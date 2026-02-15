# 01 E2 关键链路统一状态模型 (CriticalFlow StateModel)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-12 |
| Artifact | 01/06 |
| RUN_ID | 20260215T214100Z |

## 关键链路清单

### 1. 绑定链路 (bind)
```
状态机: INIT → RESOLVING → AUTHORITATIVE_WRITE → FANOUT → ACK_AGGREGATION → DONE/FAILED
终态: DONE(成功) | FAILED(失败+原因码)
```
| 阶段 | 操作 | 回执点 | 失败分类 |
|---|---|---|---|
| RESOLVING | Mojang API 查 UUID | UUID 解析成功/失败 | API_TIMEOUT / NOT_FOUND |
| AUTH_WRITE | DataManager.bind(qq, uuid) | 权威数据写入 | IO_ERROR / DUPLICATE |
| FANOUT | BridgeProxy → 子服 whitelist_add | 子服回执 | TIMEOUT / OFFLINE |
| ACK_AGG | 聚合所有子服回执 | 总回执返回用户 | PARTIAL_FAIL |

### 2. 解绑链路 (unbind)
```
状态机: INIT → VALIDATE → AUTH_DELETE → FANOUT_REMOVE → CLEANUP_CHECK → DONE/FAILED
终态: DONE(全服清理完成) | PARTIAL(残留子服) | FAILED
```
| 阶段 | 操作 | 回执点 | 失败分类 |
|---|---|---|---|
| VALIDATE | 检查绑定存在 | 验证通过/不存在 | NOT_BOUND |
| AUTH_DELETE | DataManager.unbind(qq) | 权威删除 | IO_ERROR |
| FANOUT_REMOVE | 全服 whitelist_remove | 子服回执 | TIMEOUT / OFFLINE |
| CLEANUP_CHECK | 验证残留 | 残留计数=0 | RESIDUAL |

### 3. 切服链路 (switch_server / transfer)
```
状态机: INIT → VALIDATE → TRANSFER_CMD → PENDING → ACK/TIMEOUT → DONE/FAILED
终态: DONE(转移成功) | TIMEOUT(pending) | FAILED
```
| 阶段 | 操作 | 回执点 | 失败分类 |
|---|---|---|---|
| VALIDATE | 检查玩家在线+目标服务器 | 校验通过 | NOT_ONLINE / TARGET_OFFLINE |
| TRANSFER_CMD | 执行 /transfer host port | 命令已提交 | EXEC_FAIL |
| PENDING | 等待执行结果 | CompletableFuture | TIMEOUT |
| ACK | 收到成功/失败回执 | 最终状态 | DISCONNECT |

## 统一状态码
| 码值 | 含义 |
|---|---|
| FLOW-SUCCESS | 链路完整执行成功 |
| FLOW-PARTIAL | 部分子服失败(bind/unbind fan-out) |
| FLOW-TIMEOUT | 等待回执超时 |
| FLOW-FAILED | 链路执行失败 |
| FLOW-COMPENSATED | 补偿完成 |
