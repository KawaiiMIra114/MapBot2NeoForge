# Task ID: #012-STEP2 双群结构配置

## 执行时间
2026-01-18 00:18 (UTC+8)

---

## Status: ✅ SUCCESS

---

## 修改的文件

| 路径 | 操作 | 说明 |
|------|------|------|
| `config/BotConfig.java` | 修改 | 新增双群配置 |

---

## 变更详情

### 新增配置项
| 配置项 | 类型 | 说明 |
|--------|------|------|
| `playerGroupId` | Long | 玩家群号 - 普通消息转发到游戏 |
| `adminGroupId` | Long | 管理群号 - 敏感命令专用 |

### 新增方法
- `getPlayerGroupId()` - 获取玩家群号
- `getAdminGroupId()` - 获取管理群号

### 兼容性
- `getTargetGroupId()` 保留，标记为 `@Deprecated`
- 内部实现改为调用 `getPlayerGroupId()`

---

## 配置文件示例

升级后 `mapbot-common.toml` 将包含:
```toml
[messaging]
# 玩家群号 - 普通消息转发到游戏
playerGroupId = 123456789
# 管理群号 - 敏感命令专用
adminGroupId = 987654321
```

---

## 待续

STEP 2 已完成，需在 STEP 3 中修改 `InboundHandler.java` 识别消息来源群
