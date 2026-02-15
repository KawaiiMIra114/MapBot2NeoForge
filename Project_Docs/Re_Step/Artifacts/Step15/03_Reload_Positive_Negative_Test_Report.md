# 03 #reload 正负向测试报告

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-15 |
| Artifact | 03/05 |
| RUN_ID | 20260215T221900Z |

## 正向矩阵

### 测试用例
| 用例 | 前置条件 | 操作 | 预期结果 | 验证方式 |
|---|---|---|---|---|
| POS-01 | 配置文件有效 | `#reload` | "重载成功" | QQ 回执 |
| POS-02 | 更改端口配置 | `#reload` | 新端口生效 | 连接测试 |
| POS-03 | 更改权限配置 | `#reload` | 新权限生效 | 命令权限测试 |
| POS-04 | 更改 Bridge token | `#reload` | AuthManager 更新 | Bridge 重连 |
| POS-05 | API 触发重载 | `GET /api/reload` | `{success:true}` | HTTP 响应 |

### 代码实现验证 (ReloadCommand.java)
- Alpha: `ReloadCommand.java` → 调用 `AlphaConfig.reload()` + 各管理器 reinit
- Reforged: `ReloadCommand.java` → 调用 `ConfigManager.reload()`
- 权限检查: ContractRole.ADMIN 以上

## 负向矩阵

### 测试用例
| 用例 | 前置条件 | 操作 | 预期结果 | 验证方式 |
|---|---|---|---|---|
| NEG-01 | 配置 JSON 语法错误 | `#reload` | "重载失败: JSON 解析错误" | QQ 回执 |
| NEG-02 | 配置文件不存在 | `#reload` | "重载失败: 文件不存在" | QQ 回执 |
| NEG-03 | USER 权限用户 | `#reload` | "权限不足" (CMD-PERM-001) | QQ 回执 |
| NEG-04 | Bridge 未连接时 | `#reload` | 重载成功但 Bridge 警告 | QQ 回执 |
| NEG-05 | 并发重载 | 同时 2 个 #reload | 第二个排队或拒绝 | 日志验证 |

## 收口回退验证

### 流程
1. 备份当前配置: `cp alpha.json alpha.json.bak`
2. 修改配置并 `#reload`
3. 验证新配置生效
4. 发现异常 → `cp alpha.json.bak alpha.json` → `#reload`
5. 验证恢复到基线

### 收口判据
| 项目 | 基线 | 收口后 | 结果 |
|---|---|---|---|
| 命令响应 | 正常 | 正常 | ✅ |
| Bridge 连接 | 已连接 | 已连接 | ✅ |
| 配置值 | 原始 | 原始 | ✅ |

## 差距
| ID | 差距 | 严重度 |
|---|---|---|
| RLD-01 | NEG-05 并发重载无锁保护 | High |
| RLD-02 | 无自动配置备份机制 | Medium |
| RLD-03 | 负向场景未实际执行 (设计验证) | Medium |
