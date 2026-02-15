# 05 D3 FaultInjection Test Report

## 元数据
| 属性 | 值 |
|---|---|
| Step | Step-10 D3 |
| RUN_ID | 20260215T205400Z |
| 依据 | RE_STEP_10 §详细步骤 6 |

## 1. 并发冲突注入

### 1.1 场景: 同时 bind 同一 QQ
| 注入 | 预期 | 当前行为 | 改造后预期 |
|---|---|---|---|
| 两个 Bridge 同时发送 bind(qq=123, A/B) | 一个成功/一个 409 | 后者覆盖前者 ⚠ | CAS: 版本不匹配 → CONSISTENCY-409 |

### 1.2 场景: 同时改权限
| 注入 | 预期 | 当前行为 | 改造后预期 |
|---|---|---|---|
| API + Bridge 同时 setPermission | 一个成功/一个 409 | 后者覆盖 ⚠ | CAS 版本冲突 → 409 |

### 1.3 当前验证状态
- **未实施**: CAS 机制尚未编码
- **设计已完成**: Artifact 01 定义了 VersionedValue + CONSISTENCY-409

## 2. 快照损坏注入

### 2.1 场景: data.json 被截断
| 注入 | 预期 | 当前行为 | 改造后预期 |
|---|---|---|---|
| 写入半截 JSON | 检测 + 回退到 .bak | Gson 解析异常 → 数据丢失 ⚠ | checksum 校验失败 → 使用 .bak |

### 2.2 场景: data.json 完全缺失
| 注入 | 预期 | 当前行为 | 改造后预期 |
|---|---|---|---|
| 删除 data.json | 从 .bak 恢复或空初始化 | FileNotFound → 异常 ⚠ | 尝试 .bak → 空初始化 + 告警 |

### 2.3 当前验证状态
- **部分验证**: SignManager 有 ATOMIC_MOVE
- **未实施**: 通用 checksum + .bak 回退

## 3. 日志乱序注入

### 3.1 场景: event_log 行乱序
| 注入 | 预期 | 当前行为 | 改造后预期 |
|---|---|---|---|
| 交换两行 event_log | 按 seq 排序后回放 | 无 event_log 系统 | seq 单调递增 → 排序回放 |

### 3.2 场景: event_log 重复行
| 注入 | 预期 | 当前行为 | 改造后预期 |
|---|---|---|---|
| 复制一条事件 | 去重后跳过 | 无 event_log | txId 去重 → 跳过 |

### 3.3 当前验证状态
- **未实施**: event_log 系统不存在

## 4. 重启风暴注入

### 4.1 场景: 快速连续重启 5 次
| 注入 | 预期 | 当前行为 | 改造后预期 |
|---|---|---|---|
| 连续重启 | 每次启动恢复到一致状态 | 可能读到半文件 | snapshot + 校验 → 一致恢复 |

### 4.2 当前验证状态
- **未验证**: 无自动化重启测试

## 5. 差距列表
| ID | 描述 | 风险 | 改造状态 |
|---|---|---|---|
| D3-CAS-01 | CAS 写入保护不存在 | **High** | 设计完成 (Artifact 01) |
| D3-CAS-02 | CONSISTENCY-409 未实现 | **High** | 设计完成 |
| D3-REC-01 | 通用恢复框架不存在 | **High** | 设计完成 (Artifact 02) |
| D3-REC-02 | event_log 不存在 | **High** | 设计完成 |
| D3-REC-03 | snapshot checksum 不存在 | **Medium** | 设计完成 |
| D3-AP-01 | DataManager 非原子写 | **High** | 设计完成 (Artifact 03) |
| D3-AP-02 | MetricsStorage 非原子写 | **Medium** | 设计完成 |
| D3-AP-03 | PlaytimeManager 非原子写 | **Medium** | 设计完成 |
| D3-AP-04 | 无 .bak 备份 | **Medium** | 设计完成 |
| D3-CMP-01 | 无通用补偿状态机 | **High** | 设计完成 (Artifact 04) |
| D3-CMP-02 | CDK 半成功无补偿 | **High** | 设计完成 |
| D3-CMP-03 | 白名单+绑定非原子 | **Medium** | 设计完成 |

合计: **12 项差距** (7 High / 5 Medium)
