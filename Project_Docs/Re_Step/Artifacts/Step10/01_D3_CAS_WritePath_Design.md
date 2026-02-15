# 01 D3 CAS WritePath Design

## 元数据
| 属性 | 值 |
|---|---|
| Step | Step-10 D3 |
| RUN_ID | 20260215T205400Z |
| 依据 | DATA_CONSISTENCY_CONTRACT + CRITIQUE §3.4 + RE_STEP_10 §详细步骤 1 |

## 1. 当前状态

### 1.1 写路径审计
| 数据实体 | 写入方 | 保护方式 | 并发安全 |
|---|---|---|---|
| bindings (Alpha) | DataManager.setBinding() | ConcurrentHashMap.put | ⚠ 最后写入覆盖 |
| permissions | DataManager.setPermission() | ConcurrentHashMap.put | ⚠ 最后写入覆盖 |
| mutes | DataManager.setMute() | ConcurrentHashMap.put | ⚠ 最后写入覆盖 |
| playerNames | DataManager.setPlayerName() | ConcurrentHashMap.put | ⚠ 最后写入覆盖 |
| sign cache | SignManager.saveSignCache() | Files.move ATOMIC | ✅ 原子写 |
| metrics | MetricsCollector.record*() | ConcurrentLinkedDeque.add | ✅ 追加式 |
| config | AlphaConfig.reload() | volatile swap | ✅ 原子读 |
| whitelist | server.getWhiteList() | 主线程 server.execute | ✅ 主线程串行 |
| playtime | PlaytimeStore/Manager | ConcurrentHashMap.put | ⚠ 最后写入覆盖 |

### 1.2 冲突风险矩阵
| 场景 | 写者 A | 写者 B | 结果 |
|---|---|---|---|
| 同时 bind 同一 QQ | Bridge-A | Bridge-B | 后者覆盖前者 |
| 同时改权限 | API | Bridge | 后者覆盖前者 |
| 同时禁言 | API | API | 后者覆盖前者 |

## 2. CAS 写入设计

### 2.1 版本化实体
```java
public class VersionedValue<T> {
    private volatile long version;
    private volatile T value;
    
    public boolean casUpdate(T newValue, long expectedVersion) {
        synchronized (this) {
            if (this.version != expectedVersion) return false;
            this.value = newValue;
            this.version++;
            return true;
        }
    }
}
```

### 2.2 冲突错误码
| 错误码 | 含义 | 可重试 |
|---|---|---|
| CONSISTENCY-409 | CAS 版本冲突，需要重读后重试 | ✅ 是 |
| BRG_VALIDATION_206 | 幂等重复请求 | ❌ 否 |

### 2.3 改造优先级
| 实体 | 优先级 | 理由 |
|---|---|---|
| bindings | **P0** | 绑定是核心链路，冲突影响最大 |
| permissions | **P0** | 权限冲突可能导致提权 |
| mutes | **P1** | 禁言冲突影响较小 |
| playerNames | **P2** | 仅展示用，冲突无安全影响 |
| playtime | **P2** | 累加式写入，冲突概率低 |

## 3. 回滚边界
| 条件 | 动作 |
|---|---|
| CAS 导致大量 409 | 放宽版本检查/降级为最后写入 |
| 版本号溢出 | 使用 long, 实际上不可能溢出 |
