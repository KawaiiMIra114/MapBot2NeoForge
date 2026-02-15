# 01 Security Boundary Review

## 元数据
| 属性 | 值 |
|---|---|
| Step | Step-07 C2 |
| RUN_ID | 20260215T195000Z |
| 评审日期 | 2026-02-15 |
| 评审者 | AI Agent (主代理) |
| 依据 | SECURITY_BOUNDARY.md §3 + 代码静态追踪 |

## 1. 鉴权边界评审

### 1.1 AuthN/AuthZ 实现
| 组件 | 位置 | 机制 | 判定 |
|---|---|---|---|
| `hasContractPermission` | AuthManager L381 | Token → Role → ContractRole → hasAtLeast | ✅ 完整链路 |
| `sendForbidden` | HttpRequestDispatcher L480 | 拒绝未授权请求 + 返回 403 | ✅ 实现 |
| ContractRole 枚举 | ContractRole.java L17 | VIEWER/ADMIN/OWNER 三级 | ✅ 定义完整 |
| 路由保护 | HttpRequestDispatcher 7 处调用 | `/api/servers/*/command`、`/api/mapbot/*` 等 | ✅ 覆盖 |

### 1.2 匿名可达路径扫描
| 路径 | 需鉴权 | 实际 | 判定 |
|---|---|---|---|
| `/api/status` | 是 | ⚠ 需确认是否匿名可达 | 待验证 |
| `/ws` WebSocket升级 | 是 | ⚠ HttpRequestDispatcher L175 构造ws://，需确认鉴权 | 待验证 |
| `/api/servers/*/command` | 是 | ✅ OWNER required (L72-73) | 安全 |
| `/api/mapbot/*` 管理路径 | 是 | ✅ ADMIN/OWNER required (L90-117) | 安全 |

### 1.3 权限级别映射
| Level | Legacy Role | ContractRole | 用途 |
|---|---|---|---|
| L0 | USER | VIEWER | 只读查询 |
| L1 | ADMIN | ADMIN | 管理操作 |
| L2 | OWNER | OWNER | 全权控制 |

## 2. Token 治理评审

### 2.1 Token 分类对照
| 类型 | SECURITY_BOUNDARY 定义 | 实现 | 判定 |
|---|---|---|---|
| Service Token | 服务间调用凭据 | `auth.bridge.token` (alpha.properties L8) | ✅ 存在 |
| Session Token | Web 管理会话 | ⚠ 未发现独立 Session Token 机制 | 缺失 |
| Bootstrap Token | 一次性初始化 | ⚠ 未发现一次性引导机制 | 缺失 |

### 2.2 生成与存储
| 要求 | 实现 | 判定 |
|---|---|---|
| 高熵随机值 | ❌ `DEFAULT_TOKEN_SECRET` 硬编码字符串 | **HIGH** |
| 禁止硬编码 | ❌ AuthManager L28 常量 | **HIGH** |
| 禁止提交到仓库 | ❌ alpha.properties L8/L9 已提交 | **HIGH** |
| 安全配置源 | ❌ 文件配置，非环境变量/密钥管理 | **HIGH** |
| 日志脱敏 | ⚠ 未发现 token 日志脱敏机制 | Medium |

### 2.3 轮换与吊销
| 要求 | 实现 | 判定 |
|---|---|---|
| 轮换周期 | ❌ 未实现 | 缺失 |
| 双写/双接收 | ❌ 未实现 | 缺失 |
| 主动吊销 | ❌ 未实现 | 缺失 |
| 吊销审计 | ❌ 未实现 | 缺失 |

## 3. 传输安全评审

### 3.1 明文传输
| 位置 | 协议 | 风险 | 判定 |
|---|---|---|---|
| HttpRequestDispatcher L175 | `ws://` + host + path | 明文 WebSocket | **HIGH** |
| AlphaConfig L29 | `ws://127.0.0.1:7000` | 回环明文（可接受临时） | Medium |
| Bridge TCP (BridgeServer) | 自定义 TCP 协议 | 无 TLS | **HIGH** |

### 3.2 CORS 策略
| 位置 | 设置 | 风险 | 判定 |
|---|---|---|---|
| HttpRequestDispatcher L254 | `Access-Control-Allow-Origin` | ⚠ 需确认是否 `*` | **HIGH** |
| HttpRequestDispatcher L292 | CORS 处理 | ⚠ 同上 | **HIGH** |

## 4. 最小暴露面评审

### 4.1 端口
| 端口 | 用途 | 必要性 | 判定 |
|---|---|---|---|
| 25560 | Alpha HTTP/WS 管理 API | 必要 | ✅ |
| 25561 | Bridge TCP 服务 | 必要 | ✅ |
| 其他 | 未发现额外端口 | — | ✅ |

### 4.2 调试路由
- 未发现显式调试路由 ✅
- `/api/status` 可能泄露内部信息 ⚠

## 5. 差距汇总

| ID | 描述 | 风险 | 位置 | 修复建议 | 截止 |
|---|---|---|---|---|---|
| SB-01 | DEFAULT_TOKEN_SECRET 硬编码 | **High** | AuthManager L28 | 配置缺失时拒绝启动 | 2026-02-28 |
| SB-02 | 密钥提交到仓库 | **High** | alpha.properties L8/L9 | 移入环境变量/密钥管理 | 2026-02-21 |
| SB-03 | ws:// 明文管理通道 | **High** | HttpRequestDispatcher L175 | 升级 WSS + TLS | 2026-03-15 |
| SB-04 | CORS 可能过于宽松 | **High** | HttpRequestDispatcher L254/L292 | 最小白名单 | 2026-03-05 |
| SB-05 | Bridge TCP 无 TLS | **High** | BridgeServer 全文 | 加 TLS 或 SSH 隧道 | 2026-03-15 |
| SB-06 | Token 轮换机制缺失 | **High** | AuthManager 全文 | 实现 §3.5 五阶段 SOP | 2026-03-15 |
| SB-07 | Session/Bootstrap Token 缺失 | Medium | — | 设计并实现 | 2026-03-20 |
| SB-08 | Token 日志脱敏未验证 | Medium | — | 审计日志脱敏 | 2026-03-10 |

## 6. 结论
- **High**: 6 项 (SB-01 ~ SB-06)
- **Medium**: 2 项 (SB-07, SB-08)
- AuthN/AuthZ 框架已实现 (hasContractPermission + ContractRole)，但密钥管理和传输安全严重不足。
