# 热重载事务流程 (Step-04 B2 修复后)

> 更新时间: 2026-02-15T16:40:00+08:00
> 修改文件: `AlphaConfig.java`, `ReloadCommand.java`

## 事务流程

```
#reload 命令
  └→ AlphaConfig.INSTANCE.reload()
       ├─ 1. Parse: 读取 alpha.properties → stagingProps
       ├─ 2. Validate: ConfigSchema.validate(stagingProps)
       │     └─ 失败 → 返回 ReloadResult.failure() → 保持旧配置
       ├─ 3. Staging: 保存旧配置快照 (rollbackSnapshot + 全部字段备份)
       ├─ 4. Atomic Swap: props.clear() → props.putAll(staging) → 重新解析字段
       ├─ 5. Audit: configVersion++ → 记录 [RELOAD-AUDIT] 日志
       │     └─ 返回 ReloadResult.success(prevVer, newVer)
       └─ 6. Rollback (异常时): 恢复 rollbackSnapshot + 全部字段 → 记录回滚日志
             └─ 返回 ReloadResult.failure(prevVer, reason)
```

## ReloadResult

```java
public record ReloadResult(boolean success, int prevVersion, int newVersion, String message) {
    public static ReloadResult success(int prev, int next);
    public static ReloadResult failure(int prev, String reason);
    public String toSummary();
}
```

## 审计日志格式

```
[RELOAD-AUDIT] 开始热重载 prevVersion=3 time=2026-02-15T08:40:00Z
[RELOAD-AUDIT] 热重载成功 v3 -> v4 time=2026-02-15T08:40:00Z
```

失败时:
```
[RELOAD-AUDIT] 校验失败 (rollback 到 v3): 配置校验失败 (2 个错误): ...
```

异常回滚时:
```
[RELOAD-AUDIT] 应用配置异常，执行回滚到 v3
[RELOAD-AUDIT] 回滚完成，当前仍为 v3
```

## 版本管理

- `configVersion` 字段，起始值 0，每次成功 load/reload 后 +1
- `lastValidProps` 保存最近一次通过校验的配置快照
- 通过 `AlphaConfig.INSTANCE.getConfigVersion()` 查询

## ReloadCommand 输出示例

成功:
```
[成功] 配置版本 v3 -> v4
配置版本: v4
[Bridge Auth] token=****...
[子服务器] 2/2 重载成功
```

失败:
```
[失败] 保持 v3 — 配置校验失败 (1 个错误):
  1. 未知配置键: 'foo.bar' (fail-closed: 不允许未注册的键)
配置版本: v3
...
```
