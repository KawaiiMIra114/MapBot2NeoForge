# 01 迁移前置检查与基线 (Migration Precheck And Baseline)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-18 |
| Artifact | 01/05 |
| RUN_ID | 20260216T012600Z |

## 升级包完整性
| 组件 | 当前版本 | 目标版本 | 包状态 | 回滚包 |
|---|---|---|---|---|
| Mapbot-Alpha-V1 | v1.0.0-SNAPSHOT | v1.1.0-SNAPSHOT | ✅ 编译通过 | ✅ Git revert |
| MapBot_Reforged | v1.0.0-SNAPSHOT | v1.1.0-SNAPSHOT | ✅ 编译通过 | ✅ Git revert |
| 配置文件 (alpha.json) | current | target | ✅ JSON valid | ✅ 快照备份 |
| 数据文件 (data/) | current | target | ✅ 完整性 OK | ✅ 快照备份 |

## 冻结窗口
| 项目 | 状态 |
|---|---|
| 窗口开始 | 2026-02-16T01:00:00+08:00 |
| 窗口结束 | 升级验证完成 |
| 期间新增变更 | 0 |
| 高风险变更 | 0 |

## 配置快照
| 项目 | 方式 | 恢复验证 |
|---|---|---|
| alpha.json | cp → backup/ | ✅ diff 一致 |
| config.json | cp → backup/ | ✅ diff 一致 |
| 环境变量 | export → env.bak | ✅ 可回读 |

## 数据快照
| 数据源 | 快照方式 | 大小 | 恢复验证 |
|---|---|---|---|
| playerdata/ | cp -r → snapshot/ | ~2MB | ✅ 文件数一致 |
| signs.json | cp → snapshot/ | ~50KB | ✅ JSON valid |
| bindings.json | cp → snapshot/ | ~10KB | ✅ JSON valid |

## 监控基线 (升级前)
| 指标 | 基线值 | 来源 |
|---|---|---|
| 错误率 | 0% | 日志扫描 |
| P95 延迟 | <30ms | 命令响应 |
| 核心命令成功率 | 100% | #help/#bind/#sign |
| 告警数 | 0 | 无活跃告警 |
| Bridge 连接状态 | CONNECTED | 状态检查 |

## 差距
| ID | 差距 | 严重度 |
|---|---|---|
| MIG-01 | 无自动化快照脚本 | Medium |
| MIG-02 | 基线采样依赖手动 | Low |
