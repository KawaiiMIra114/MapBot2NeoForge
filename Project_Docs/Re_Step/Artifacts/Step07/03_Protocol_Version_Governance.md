# 03 Protocol Version Governance

## 元数据
| 属性 | 值 |
|---|---|
| Step | Step-07 C2 |
| RUN_ID | 20260215T195000Z |
| 评审日期 | 2026-02-15 |
| 依据 | VERSIONING_AND_COMPATIBILITY.md §3 + 代码追踪 |

## 1. 协议版本现状

### 1.1 当前实现
| 要求 | 实现 | 位置 | 判定 |
|---|---|---|---|
| protocol_version 携带 | ❌ 未实现 | 全代码无结果 | **HIGH** |
| request_id 携带 | ⚠ 部分（pendingRequests UUID） | BridgeClient L52 | 部分 |
| feature_flags 携带 | ❌ 未实现 | 全代码无结果 | Medium |
| 版本校验门禁 | ❌ 未实现 | — | **HIGH** |
| 未知字段忽略 | ⚠ Gson 默认忽略未知字段 | — | ✅ 自然兼容 |

### 1.2 version 字段现状
- BridgeMessageHandler L84: 注册消息中记录 `version` 字段到日志
- BridgeMessageHandler L99: 同上
- 用途: 仅展示，未作为兼容门禁

## 2. SemVer 规则评审

### 2.1 定义 (VERSIONING §3.1)
| 版本级别 | 含义 | 当前落地 |
|---|---|---|
| MAJOR | 不兼容变更 | ❌ 无强制校验 |
| MINOR | 向后兼容新增 | ❌ 无版本跟踪 |
| PATCH | 兼容性修正 | ❌ 无版本跟踪 |

### 2.2 向后兼容要求 (§3.2)
| 要求 | 实现 | 判定 |
|---|---|---|
| 同 MAJOR 兼容前 2 个 MINOR | ❌ 无 MAJOR/MINOR 定义 | 缺失 |
| 字段新增仅可选 | ✅ Gson 忽略未知 | 自然满足 |
| 行为变更 feature flag 保护 | ❌ 无 flag | 缺失 |
| 发布说明支持矩阵 | ❌ 无 | 缺失 |

## 3. 兼容矩阵 (To-Be)

### 3.1 建议初始版本
```
Protocol Version: 1.0.0
- MAJOR 1: 当前 Bridge 协议 (JSON over TCP)
- MINOR 0: 初始能力集 (register/ack/command/response)
- PATCH 0: 基线
```

### 3.2 兼容窗口
| Alpha Version | 支持 Protocol | 说明 |
|---|---|---|
| v5.7.x | 1.0.x | 初始版本 |
| v5.8.x | 1.0.x ~ 1.1.x | 新增可选字段 |
| v6.0.x | 2.0.x | 不兼容变更（如需） |

### 3.3 门禁规则
| 条件 | 动作 |
|---|---|
| MAJOR 不匹配 | 拒绝连接 + 返回 `PROTOCOL_VERSION_MISMATCH` |
| MINOR 差 ≥ 3 | 告警 + 返回 `PROTOCOL_VERSION_STALE` |
| 缺少 protocol_version | 临时接受(到期 2026-03-08) + 告警 |

## 4. Feature Flag 策略 (To-Be)

### 4.1 机制
```json
{
  "protocol_version": "1.0.0",
  "request_id": "uuid",
  "feature_flags": ["ENHANCED_ERROR_CODES", "MULTI_SERVER_SYNC"]
}
```

### 4.2 规则
- 新行为变更默认关闭
- 需 flag 启用才切换行为
- 双端协商: 仅当双端均声明支持时启用

## 5. 差距汇总

| ID | 描述 | 风险 | 修复建议 | 截止 |
|---|---|---|---|---|
| PV-01 | protocol_version 未实现 | **High** | 注册消息 + 每请求携带 | 2026-03-08 |
| PV-02 | 版本校验门禁未实现 | **High** | MAJOR 校验 + MINOR 告警 | 2026-03-08 |
| PV-03 | feature_flags 未实现 | **Medium** | 请求结构扩展 | 2026-03-12 |
| PV-04 | 兼容矩阵未发布 | **Medium** | 发布文档更新 | 2026-03-12 |
| PV-05 | SemVer 变更跟踪无机制 | **Low** | CHANGELOG + tag | 2026-03-20 |
