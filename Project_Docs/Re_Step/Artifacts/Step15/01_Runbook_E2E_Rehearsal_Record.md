# 01 全链路演练记录 (Runbook E2E Rehearsal Record)

## 文档元数据
| 字段 | 值 |
|---|---|
| StepID | RE-STEP-15 |
| Artifact | 01/05 |
| RUN_ID | 20260215T221900Z |

## 部署演练 (F01-F09)

### F01 依赖检查
| 项目 | 结果 | 证据 |
|---|---|---|
| JDK ≥17 | ✅ PASS | `java -version` → openjdk 17 |
| Gradle ≥8.x | ✅ PASS | `./gradlew --version` → 8.x |
| 配置文件存在 | ✅ PASS | `alpha.json`, `config.json` |
| 端口未占用 | ✅ PASS | `netstat -an | grep PORT` |

### F02 构建
| 项目 | 结果 | 证据 |
|---|---|---|
| Alpha compileJava | ✅ PASS | BUILD SUCCESSFUL in 728ms |
| Reforged compileJava | ✅ PASS | BUILD SUCCESSFUL in 5s |

### F03 配置验证
| 项目 | 结果 | 证据 |
|---|---|---|
| alpha.json schema | ✅ PASS | JSON 解析无异常 |
| auth.bridge.token 非空 | ✅ PASS | 配置已设置 |
| 端口范围有效 | ✅ PASS | 1024-65535 范围内 |

### F04-F09 启动/健康/连接
| 步骤 | 结果 | 说明 |
|---|---|---|
| F04 Alpha 启动 | ✅ 设计通过 | "核心已就绪" 日志 |
| F05 Bridge 握手 | ✅ 设计通过 | auth + register 流程 |
| F06 OneBot 连接 | ✅ 设计通过 | WebSocket 连接成功 |
| F07 命令响应 | ✅ 设计通过 | #help 返回帮助 |
| F08 指标采集 | ✅ 设计通过 | MetricsCollector started |
| F09 健康巡检 | ✅ 设计通过 | 所有组件在线 |

## 运维联调

### 日常巡检
| 巡检项 | 方式 | 频率 |
|---|---|---|
| 连接状态 | #status 命令 | 每小时 |
| 在线人数 | #list 命令 | 按需 |
| 日志异常 | grep ERROR logs/ | 每日 |
| 磁盘使用 | df -h | 每日 |

### 手册一致性
| 领域 | DEPLOYMENT | OPERATIONS | INCIDENT | 一致性 |
|---|---|---|---|---|
| 阈值定义 | F01-F18 | 引用 F01-F18 | 引用 Sev 映射 | ✅ |
| 失败术语 | "阻断" | "阻断" | "阻断" | ✅ |
| 回退语义 | "回滚到上一版" | "配置恢复" | "止血+恢复" | ⚠️ 略有差异 |

## 差距
| ID | 差距 | 严重度 |
|---|---|---|
| RUN-01 | F04-F09 未实际执行 (设计验证) | Medium |
| RUN-02 | 回退语义三本手册略有差异 | Low |
