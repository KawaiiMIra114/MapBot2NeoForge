# 02_Conflict_and_Idempotency_Matrix

## 合同引用
- `A2-DATA-CONSISTENCY` §5.3 冲突解决
- `MB2N-BRIDGE-MESSAGE` §7 幂等键
- `ARCH-A4-FAILURE-MODEL` §3.4 重试污染防护

## 冲突场景矩阵

| 场景ID | 操作 | 冲突类型 | 当前行为 | 合同要求 | 差距 | 修复动作 |
|---|---|---|---|---|---|---|
| CF-01 | bind(qq, uuid) 同UUID被占 | UUID 占用冲突 | 返回 `FAIL:OCCUPIED:{qq}` | 返回 `CONSISTENCY-409` + expected/actual version | PARTIAL: 有冲突检测但错误码不标准 | 统一返回 `CONSISTENCY-409` |
| CF-02 | bind() 同QQ重復绑定 | 重復绑定 | 返回 false | 应以 version 判断是否允许覆盖 | PARTIAL | 引入 entity_version |
| CF-03 | mute(uuid) 并发禁言 | 覆盖竞态 | 后写覆盖 (last-write-wins) | CAS + owner > admin 优先级 | MISSING | 引入 CAS |
| CF-04 | setPermission() 并发 | 权限覆盖 | 后写覆盖 | CAS | MISSING | 引入 CAS |
| CF-05 | AlphaConfig.reload() 并发 | 配置冲突 | synchronized 序列化 | 已闭环 (B2 事务 reload) | DONE | — |
| CF-06 | syncWhitelistSnapshot + unbind 并发 | 白名单不一致 | 无锁，可能同步后又被解绑 | PREPARED→COMMITTED 两阶段 | MISSING | 引入两阶段白名单同步 |
| CF-07 | giveItemToOnlineServers 部分成功 | 半成功 | 返回 `SUCCESS:n/m` 或 `FAIL:*` | PREPARED→COMMITTED→COMPENSATED | MISSING | 引入状态机 |

## 幂等矩阵

| 操作类型 | 幂等键格式（合同） | 当前实现 | 差距 | 修复动作 |
|---|---|---|---|---|
| 变更型 Bridge 请求 | `ik:{serverId}:{type}:{bizKey}:{epoch}` | 无 idempotencyKey 字段 | MISSING | 生产端生成 ik，消费端维护 TTL 去重缓存 |
| bind/unbind | requestId 可关联 | 无去重检查 | MISSING | 基于 requestId 或 idempotencyKey 做写前去重 |
| give_item | requestId | 无重复发放检查 | MISSING (HIGH RISK) | 引入 request_id 去重窗口 |
| whitelist_add/remove | 无 | 部分幂等 (AlreadyExisted 处理) | PARTIAL | 标准化 idempotency 响应 |
| redeem_cdk | CDK code 本身唯一 | removeCdk() 后不可重复 | OK (自然幂等) | — |

## 去重窗口设计（合同 + FAILURE_MODEL）
- **窗口大小**: 单进程内 pending 请求 (ConcurrentHashMap)
- **缺失**: 进程重启后去重上下文丢失
- **合同整改期限**: 2026-03-05
- **临时可接受**: 允许 pending 期内去重；重启后第一次请求不去重

## 综合判定
- **冲突覆盖率**: ~25% (7 场景中 1 个 DONE, 2 个 PARTIAL, 4 个 MISSING)
- **幂等覆盖率**: ~20% (5 类操作中 1 个 OK, 1 个 PARTIAL, 3 个 MISSING)
- **风险等级**: HIGH (give_item 无去重为最高风险)
