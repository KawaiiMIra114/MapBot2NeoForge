# 02 Token Rotation and Rollback Blueprint

## 元数据
| 属性 | 值 |
|---|---|
| Step | Step-07 C2 |
| RUN_ID | 20260215T195000Z |
| 评审日期 | 2026-02-15 |
| 依据 | SECURITY_BOUNDARY.md §3.5/§3.6 |

## 1. 当前状态

### 1.1 Token 类型现状
| Token | 当前存储 | 轮换 | 吊销 | 审计 |
|---|---|---|---|---|
| Service Token (bridge.token) | alpha.properties L8 | ❌ | ❌ | ❌ |
| Token Secret | alpha.properties L9 / AuthManager L28 回退 | ❌ | ❌ | ❌ |
| Session Token | ❌ 未实现 | — | — | — |
| Bootstrap Token | ❌ 未实现 | — | — | — |

### 1.2 关键风险
- `DEFAULT_TOKEN_SECRET = "MapBot-Alpha-Secret-2026"` 作为硬编码回退，任何知道此常量的人均可伪造认证。
- alpha.properties 已提交 Git 仓库，密钥对所有仓库访问者可见。

## 2. 轮换五阶段蓝图

### 阶段 1: 准备 (Prepare)
| 项目 | 操作 |
|---|---|
| 生成 K_new | `openssl rand -base64 32` 或等效高熵方法 |
| 分配 Owner | Security Owner + 操作确认人 |
| 依赖方清单 | Alpha Core (AuthManager), Bridge (BridgeServer), Web (HttpRequestDispatcher) |
| 回滚点 | 清单不完整 → 终止，0s 中断 |
| 预检查 | 审计日志可用 ✓, K_old 可恢复 ✓ |

### 阶段 2: 双写 (Dual-Accept)
| 项目 | 操作 |
|---|---|
| 实现变更 | AuthManager 支持 `tokenSecrets: [K_old, K_new]` 数组 + 任一匹配即通过 |
| 签发端 | 仍签发 K_old（灰度客户端可接收 K_new） |
| 观测指标 | auth_success_rate 不低于基线 -1%, auth_failed_invalid_token 不超基线 +0.5% |
| 回滚点 | 指标超限 → 关闭 K_new 接收，仅保留 K_old，30s 中断预算 |

### 阶段 3: 切换 (Cutover)
| 项目 | 操作 |
|---|---|
| 操作 | 签发端切换默认为 K_new |
| 观察窗口 | 30 分钟双接收 |
| 回滚点 | 5 分钟内认证成功率低于基线 -1% → 签发端回滚 K_old，60s 中断预算 |

### 阶段 4: 吊销 (Revoke)
| 项目 | 操作 |
|---|---|
| 操作 | 移除 K_old 接收 + 吊销旧会话 |
| 错误码 | 旧 token 请求返回 `SEC_TOKEN_REVOKED` |
| 审计 | 吊销动作写审计记录（操作者、时间、影响范围） |
| 回滚点 | 大面积 SEC_TOKEN_REVOKED → 恢复 K_old 接收 30 分钟，120s 中断预算 |

### 阶段 5: 审计 (Audit)
| 项目 | 操作 |
|---|---|
| 轮换报告 | 开始/结束时间, 失败请求统计, 回滚是否发生, 遗留客户端清单 |
| 更新计划 | 密钥到期日 + 下次轮换计划 |
| 回滚点 | 审计数据缺失 → 标记轮换失败，不关闭工单，0s 中断 |

## 3. 中断预算总览
| 阶段 | 最大中断 |
|---|---|
| 准备 | 0s |
| 双写 | 30s |
| 切换 | 60s |
| 吊销 | 120s |
| 审计 | 0s |
| **累计上限** | **3 分钟** |

## 4. 实施前提 (当前缺失)
| 前提 | 当前状态 | 需实现 |
|---|---|---|
| AuthManager 支持多密钥 | ❌ 单密钥 L28/L199 | 数组配置 + 遍历匹配 |
| 密钥外部化 | ❌ 文件配置 | 环境变量或密钥管理 |
| SEC_TOKEN_REVOKED 错误码 | ❌ 不存在 | 新增错误码 |
| 审计日志格式 | ❌ 通用日志 | 结构化审计事件 |

## 5. 差距
| ID | 描述 | 风险 | 修复建议 |
|---|---|---|---|
| TR-01 | 多密钥接受机制不存在 | **High** | AuthManager + 配置改造 |
| TR-02 | SEC_TOKEN_REVOKED 错误码缺失 | **Medium** | 新增到 BridgeErrorMapper |
| TR-03 | 审计事件格式未定义 | **Medium** | 定义结构化审计 schema |
