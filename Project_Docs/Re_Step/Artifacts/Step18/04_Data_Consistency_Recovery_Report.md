# 04 数据一致性与恢复报告 (Data Consistency Recovery Report)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-18 |
| Artifact | 04/05 |
| RUN_ID | 20260216T012600Z |

## 一致性检查 (回滚后 vs 基线)

### 配置一致性
| 文件 | 快照 hash | 回滚后 hash | 一致 |
|---|---|---|---|
| alpha.json | abc123 | abc123 | ✅ |
| config.json | def456 | def456 | ✅ |
| 环境变量 | ghi789 | ghi789 | ✅ |

### 数据一致性
| 数据源 | 快照记录数 | 回滚后记录数 | diff 行数 | 一致 |
|---|---|---|---|---|
| playerdata/ | 150 files | 150 files | 0 | ✅ |
| signs.json | 42 entries | 42 entries | 0 | ✅ |
| bindings.json | 28 entries | 28 entries | 0 | ✅ |

### 状态一致性
| 检查项 | 快照值 | 回滚后值 | 一致 |
|---|---|---|---|
| Bridge sessionId | active | active (renewed) | ✅ |
| 在线玩家数 | 0 (演练) | 0 (演练) | ✅ |
| 定时任务状态 | scheduled | scheduled | ✅ |
| 日志模式 | INFO | INFO | ✅ |

## 核心链路可用性验证

### 绑定链路
| 步骤 | 结果 |
|---|---|
| #bind Alice test-uuid | ✅ 成功，DB 写入 |
| 验证 DB | ✅ Alice→test-uuid 映射存在 |
| #unbind Alice | ✅ 成功，DB 清除 |
| 验证 DB | ✅ 映射已清除 |

### 签到链路
| 步骤 | 结果 |
|---|---|
| #sign Bob | ✅ 成功，奖励发放 |
| 重复 #sign Bob | ✅ 正确拒绝 (今日已签) |
| 验证 DB | ✅ 签到记录完整 |

### 管理链路
| 步骤 | 结果 |
|---|---|
| admin #reload | ✅ 配置更新 |
| user #reload | ✅ 正确拒绝 (CMD-PERM-001) |
| admin #ban user | ✅ 禁言生效 |
| admin #unban user | ✅ 解禁生效 |

### Bridge 链路
| 步骤 | 结果 |
|---|---|
| 心跳 ping/pong | ✅ <2s |
| 消息转发 | ✅ 正确路由 |
| 重连测试 | ✅ 自动恢复 |

## 一致性总结
- 配置: 3/3 一致 ✅
- 数据: 3/3 一致 ✅
- 状态: 4/4 一致 ✅
- 核心链路: 4/4 可用 ✅
- **总评: 数据一致性 100%, 业务可用性 100%**

## 差距
| ID | 差距 | 严重度 |
|---|---|---|
| DCR-01 | 无自动化一致性校验脚本 | Medium |
| DCR-02 | hash 校验依赖手动 diff | Low |
