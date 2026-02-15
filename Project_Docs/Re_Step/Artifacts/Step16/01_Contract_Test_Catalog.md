# 01 契约测试目录 (Contract Test Catalog)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-16 |
| Artifact | 01/05 |
| RUN_ID | 20260216T005500Z |

## CT-01 ~ CT-18 覆盖矩阵

### Bridge 消息契约 (CT-01 ~ CT-09)
| 用例ID | 契约条款 | 测试场景 | 预期结果 | 环境 | 自动化 |
|---|---|---|---|---|---|
| CT-01 | Bridge 注册握手 | 正确 token + serverId | 注册成功, 分配 sessionId | Local | 待实现 |
| CT-02 | Bridge 心跳 | 定时 ping/pong | 连接保持, 超时断连 | Local | 待实现 |
| CT-03 | Bridge 消息转发 | 合法 action + payload | 转发到目标, 正确路由 | Local | 待实现 |
| CT-04 | Bridge 认证失败 | 错误 token | BRG_AUTH_001 拒绝 | Local | 待实现 |
| CT-05 | Bridge serverId 不在允许列表 | 未授权 serverId | BRG_AUTH_002 拒绝 | Local | 待实现 |
| CT-06 | Bridge 消息格式错误 | 非 JSON / 缺字段 | BRG_MSG_001 错误响应 | Local | 待实现 |
| CT-07 | Bridge 未知 action | 不存在的 action | BRG_MSG_002 忽略 | Local | 待实现 |
| CT-08 | Bridge 重连 | 断连后自动重连 | 恢复会话 or 重新注册 | CI | 待实现 |
| CT-09 | Bridge 并发消息 | 多服同时发 | 正确路由, 无串线 | CI | 待实现 |

### 权限契约 (CT-10 ~ CT-14)
| 用例ID | 契约条款 | 测试场景 | 预期结果 | 环境 | 自动化 |
|---|---|---|---|---|---|
| CT-10 | user 执行 user 命令 | #help, #sign | 成功执行 | Local | 待实现 |
| CT-11 | user 执行 admin 命令 | #reload, #ban | CMD-PERM-001 拒绝 | Local | 待实现 |
| CT-12 | admin 执行 admin 命令 | #reload, #ban | 成功执行 | Local | 待实现 |
| CT-13 | admin 执行 owner 命令 | #stop | CMD-PERM-001 拒绝 | Local | 待实现 |
| CT-14 | owner 执行全部命令 | 任意命令 | 成功执行 | Local | 待实现 |

### 错误码双栈 (CT-15 ~ CT-18)
| 用例ID | 契约条款 | 测试场景 | 预期结果 | 环境 | 自动化 |
|---|---|---|---|---|---|
| CT-15 | BRG_* 错误码格式 | 所有错误场景 | 返回 BRG_XXX_NNN 格式 | Local | 待实现 |
| CT-16 | 错误码回退策略 | 未知错误 | 回退到 BRG_INTERNAL_999 | Local | 待实现 |
| CT-17 | 错误码 HTTP 映射 | 各 BRG_* → HTTP status | 正确映射 (401/403/400/500) | CI | 待实现 |
| CT-18 | 错误码用户可读消息 | 各错误场景 | 中文提示非空 | CI | 待实现 |

## 覆盖率统计
- CT-01~CT-18: **18/18 用例已定义** (覆盖率 100%)
- 自动化率: 0/18 (待 G2 实现)
- 关键用例 (CT-13/14/15/16): 全部已定义 ✅

## 四类场景覆盖
| 链路 | 成功 | 失败 | 超时 | 重试 |
|---|---|---|---|---|
| Bridge 注册 | CT-01 | CT-04,CT-05 | CT-02 | CT-08 |
| 消息转发 | CT-03 | CT-06,CT-07 | CT-02 | CT-08 |
| 命令执行 | CT-10,CT-12,CT-14 | CT-11,CT-13 | (E2E) | (E2E) |
| 错误码 | CT-15 | CT-16 | (E2E) | (E2E) |

## 可机检命令
```bash
# 验证 CT 用例数
grep -c "^| CT-" 01_Contract_Test_Catalog.md
# 预期: >= 18

# 验证无重复 ID
grep "^| CT-" 01_Contract_Test_Catalog.md | awk -F'|' '{print $2}' | sort | uniq -d
# 预期: 空
```
