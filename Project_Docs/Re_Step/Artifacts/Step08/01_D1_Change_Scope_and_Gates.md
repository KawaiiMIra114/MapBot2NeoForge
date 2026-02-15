# 01 D1 Change Scope and Gates

## 元数据
| 属性 | 值 |
|---|---|
| Step | Step-08 D1 |
| RUN_ID | 20260215T201100Z |
| 日期 | 2026-02-15 |
| 依据 | RE_STEP_08 §步骤目标 + TASK_STEP_08_D1 §目标 |

## 1. D1 变更范围

### 1.1 核心变更
| # | 能力 | 当前状态 | D1 目标 | 改动位置 |
|---|---|---|---|---|
| C1 | protocol_version 协商 | ❌ 未实现 | 注册+请求携带+MAJOR拒绝 | BridgeClient + BridgeServer + BridgeMessageHandler |
| C2 | 幂等去重缓存 | ❌ 未实现 | 变更型请求 idempotencyKey + TTL | BridgeProxy + BridgeClient |
| C3 | 断连快失败 | ⚠ complete("") | completeExceptionally + FAILED_DISCONNECT | BridgeClient handleDisconnect + BridgeFileProxy |

### 1.2 已落地（仅验证）
| # | 能力 | 状态 | 验证项 |
|---|---|---|---|
| V1 | BRG_ 错误码双栈 | ✅ 14 常量 | 确认覆盖率 |
| V2 | 帧大小门禁 | ✅ 64KiB/46KiB | 确认使用位点 |

## 2. 禁止项（D1 期间不得做）
| # | 禁止项 | 原因 |
|---|---|---|
| P1 | 不得修改命令语义或权限模型 | 属于 E1/E2 |
| P2 | 不得修改数据一致性模型 | 属于 D3 |
| P3 | 不得引入新业务命令 | 属于 E 阶段 |
| P4 | 不得修改线程模型 | 属于 D2 |
| P5 | 不得混入安全传输改造 | 属于 D 后续批次 |

## 3. 代码门禁
| 门禁 | 规则 | 阈值 |
|---|---|---|
| G1 | 每次提交必须双端编译通过 | exitCode=0 |
| G2 | 不得引入 P1-P5 禁止项 | 0 违规 |
| G3 | 新增代码无 TODO/FIXME | 新增 0 |
| G4 | D1 专项关键字全部有覆盖 | protocol_version≥2, idempotency≥1, FAILED_DISCONNECT≥1 |

## 4. 回滚触发条件
| 条件 | 动作 |
|---|---|
| 编译失败 3 轮 | 冻结变更，回滚到上次成功 commit |
| 运行时注册失败 | 回滚 protocol_version 改动 |
| 幂等键误判 > 0 | 回滚去重缓存 |
| 断连后仍有长堆积 | 回滚 fast-fail 改动 |

## 5. 实施顺序
```
1. protocol_version (C1) → 编译验证
2. idempotency (C2) → 编译验证
3. disconnect fast-fail (C3) → 编译验证
4. 错误码覆盖验证 (V1) → 编译验证
5. 帧大小使用验证 (V2) → 编译验证
6. 全量集成编译 + D1 专项复查
```
