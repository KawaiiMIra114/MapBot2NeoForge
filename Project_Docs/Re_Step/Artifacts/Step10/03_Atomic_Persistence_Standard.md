# 03 Atomic Persistence Standard

## 元数据
| 属性 | 值 |
|---|---|
| Step | Step-10 D3 |
| RUN_ID | 20260215T205400Z |
| 依据 | RE_STEP_10 §详细步骤 3 + FAILURE_MODEL |

## 1. 当前持久化审计

### 1.1 写入方式清单
| 文件 | 写入方 | 方式 | 原子性 | 备份 |
|---|---|---|---|---|
| sign_cache.json | SignManager | tmp → ATOMIC_MOVE ✅ | ✅ | ❌ |
| data.json (Alpha) | DataManager | 直接覆写 ⚠ | ❌ | ❌ |
| metrics.json | MetricsStorage | 直接覆写 ⚠ | ❌ | ❌ |
| playtime.json (Reforged) | PlaytimeManager | 直接覆写 ⚠ | ❌ | ❌ |
| config.properties | AlphaConfig | 只读 (reload) | N/A | N/A |

### 1.2 风险: 非原子写
直接覆写 (FileWriter/Gson.toJson): 中途崩溃 → 半文件 → 数据丢失

## 2. 统一原子持久化流程

### 2.1 标准流程 (SignManager 模式推广)
```
1. write(data, targetPath):
   tmpPath = targetPath.resolveSibling(name + ".tmp")
   
2. 写入 tmpPath:
   - BufferedWriter → Gson.toJson(data)
   - flush() + 关闭流
   
3. 校验 tmpPath:
   - 文件大小 > 0
   - JSON 可解析
   
4. 创建备份:
   backupPath = targetPath.resolveSibling(name + ".bak")
   if (targetPath.exists()) Files.copy(target → backup)
   
5. 原子替换:
   try {
     Files.move(tmp, target, ATOMIC_MOVE, REPLACE_EXISTING)
   } catch (AtomicMoveNotSupportedException) {
     Files.move(tmp, target, REPLACE_EXISTING)
   }
   
6. 清理:
   保留最近 3 个 .bak 文件
```

### 2.2 失败恢复
```
启动时检查:
1. if target.exists() && valid(target) → 使用 target
2. elif target.tmp.exists() && valid(tmp) → move tmp → target
3. elif target.bak.exists() && valid(bak) → copy bak → target
4. else → 空数据初始化 + 告警
```

## 3. 改造矩阵
| # | 文件 | 当前 | 目标 | 优先级 |
|---|---|---|---|---|
| AP-01 | DataManager (Alpha) | 直接覆写 | tmp→校验→备份→ATOMIC | **P0** |
| AP-02 | MetricsStorage (Alpha) | 直接覆写 | tmp→校验→ATOMIC | **P1** |
| AP-03 | PlaytimeManager (Reforged) | 直接覆写 | tmp→校验→ATOMIC | **P1** |
| AP-04 | SignManager (Reforged) | tmp→ATOMIC ✅ | 增加备份+校验 | **P2** |
| AP-05 | DataManager (Reforged) | 直接覆写 | tmp→校验→ATOMIC | **P1** |

## 4. 通过/失败标准
| 标准 | 验证方法 |
|---|---|
| 通过 | 所有持久化路径均使用 tmp→ATOMIC 模式 |
| 通过 | 断电后可从 .bak 或 .tmp 恢复 |
| 失败 | 存在直接覆写且无恢复路径 |
