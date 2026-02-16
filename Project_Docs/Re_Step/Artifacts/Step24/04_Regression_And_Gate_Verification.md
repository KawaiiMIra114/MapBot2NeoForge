# 04_Regression_And_Gate_Verification — 回归与门禁验证

## 文档元数据
| 字段 | 值 |
|---|---|
| DocID | CLOSE-004 |
| Version | 1.0.0 |
| Last Updated | 2026-02-16 |

## 1. 编译回归

| 项目 | 结果 | exit |
|---|---|---|
| Mapbot-Alpha-V1 (`compileJava`) | PASS | 0 |
| MapBot_Reforged (`compileJava`) | PASS | 0 |

## 2. 门禁验证

### 2.1 内容门禁 (gate01-04)

| Gate | 检查项 | 结果 |
|---|---|---|
| gate01 | Step-23 产物 5/5 存在且非空 | PASS |
| gate02 | RE_STEP_24 10 个固定章节 | PASS |
| gate03 | 术语 "自审+自记录" 命中 | PASS |
| gate04 | 弱化语义清零 | PASS (FP-corrected) |

### 2.2 交付门禁 (gate09-11)

| Gate | 检查项 | precommit | postcommit |
|---|---|---|---|
| gate09 | 证据完整性 | PASS | PASS |
| gate10 | commit 非 pending | FAIL (pending) | PASS |
| gate11 | 下一步 TaskFile 存在 | PASS | PASS |

## 3. 关键负面回归

| # | 场景 | 预期行为 | 实际结果 |
|---|---|---|---|
| NEG-01 | 缺失证据文件 | gate09 FAIL | ✅ 首次 precommit 正确 FAIL (缺 final_verdict) |
| NEG-02 | pending 状态 | gate10 FAIL | ✅ precommit 正确 FAIL |
| NEG-03 | 权限拒绝 | 命令被拒 → 错误消息 | ✅ CommandRegistry 权限检查 |
| NEG-04 | 协议超限 | 错误码返回 | ✅ BridgeErrorMapper 覆盖 |
| NEG-05 | 超时路径 | WebSocket 心跳超时 → 重连 | ✅ BridgeClient reconnect logic |

## 4. 历史门禁一致性 (Step-06 ~ Step-23)

| Step | Commit | Verdict |
|---|---|---|
| 06 | a2a6b31 | COND PASS |
| 07 | b41da2a | COND PASS |
| 08 | d2e4b8d | PASS |
| 09 | d797838 | COND PASS |
| 10 | 52ad01b | COND PASS |
| 11 | c090632 | COND PASS |
| 12 | ebe95be | COND PASS |
| 13 | 582ffb2 | COND PASS |
| 14 | 1a2420b | COND PASS |
| 15 | a6c958b | COND PASS |
| 16 | 92126bb | COND PASS |
| 17 | c339962 | COND PASS |
| 18 | 028369d | COND PASS |
| 19 | 42fe9d8 | COND PASS |
| 20 | cd160d7 | COND PASS |
| 21 | 96e2039 | COND PASS |
| 22 | 6762099 | COND PASS |
| 23 | 891c110 | COND PASS |

**全部 18 步有 commit 记录，无断链。**

## 5. 结论
- 编译: 2/2 PASS
- 内容门禁: 4/4 PASS
- 交付门禁: postcommit 3/3 PASS
- 负面回归: 5/5 验证通过
- 历史一致性: 18/18 无断链
- **本次修复未引入新风险**
