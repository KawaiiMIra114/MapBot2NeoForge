# 04 Go/No-Go 决策模板 (Decision Template)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-17 |
| Artifact | 04/05 |
| RUN_ID | 20260216T010500Z |

## 决策模板

### 元数据
| 字段 | 值 |
|---|---|
| 版本 | (填写发布版本号) |
| 日期 | (填写决策日期) |
| 决策者 | Solo Maintainer |
| 流水线运行 ID | (填写 CI run ID) |

### 门禁结果汇总
| Stage | 硬门禁 | 软门禁 | 结果 |
|---|---|---|---|
| Pre-Build | PB-01~03 | — | PASS/FAIL |
| Build | BD-01~02 | — | PASS/FAIL |
| Test | TS-01~03 | TS-04 | PASS/FAIL |
| Quality | QA-01~03 | QA-04 | PASS/FAIL |
| Release | RL-01~03 | — | PASS/FAIL |

### 阻断项清单
| 门禁 ID | 描述 | 证据链接 | 修复计划 | 截止时间 |
|---|---|---|---|---|
| (空=无阻断项) | | | | |

### 风险记录 (软门禁)
| 门禁 ID | 描述 | 风险等级 | 缓释措施 | 豁免有效期 |
|---|---|---|---|---|
| (空=无风险) | | | | |

### 时间线
| 时间 | 事件 |
|---|---|
| T-2h | 流水线启动 |
| T-1h | 测试完成 |
| T-30min | 质量检查完成 |
| T-10min | 回滚就绪确认 |
| T-0 | Go/No-Go 决策 |

### 最终决策
```
Decision: PASS (Go) / FAIL (No-Go)
Justification: (填写理由)
Hard Gates: X/12 passed
Soft Gates: X/2 passed (Y warnings)
Rollback Ready: Yes/No
Next Action: (发布 / 修复 / 回退)
```

## 决策规则
1. **Go**: 全部 12 个硬门禁 PASS + 回滚就绪 + 无 P0 阻断项
2. **No-Go**: 任一硬门禁 FAIL, 或回滚未就绪, 或 P0 阻断项 >0
3. 决策结果必须与流水线状态一致 (禁止流水线 FAIL 但人工 Go)

## 差距
| ID | 差距 | 严重度 |
|---|---|---|
| DEC-01 | 无自动化决策输出 | Medium |
| DEC-02 | 无流水线集成 | Medium |
