# Task ID: #012-STEP3 消息处理增强 + 权限分离

## 执行时间
2026-01-18 00:35 (UTC+8)

---

## Status: ✅ SUCCESS

---

## 修改的文件

| 路径 | 操作 | 说明 |
|------|------|------|
| `logic/InboundHandler.java` | 大规模重构 | 双群支持 + CQ码解析 + 权限分离 |

---

## 变更详情

### 1. 文件头注释更新
- 添加 Task #012-STEP3 更新说明

### 2. 新增导入
```java
import com.mapbot.utils.CQCodeParser;
import java.util.Set;
```

### 3. 新增常量
```java
private static final Set<String> ADMIN_ONLY_COMMANDS = Set.of(
    "inv", "stopserver", "关服", "reload", 
    "addadmin", "removeadmin", "adminunbind"
);
```

### 4. handleGroupMessage 重构
- 双群来源判断 (`playerGroupId` + `adminGroupId`)
- 消息转发规则：仅从 `playerGroupId` 转发普通消息
- CQ码解析：调用 `CQCodeParser.parse(rawMessage)`
- 空消息处理：纯图片/表情解析后跳过转发

### 5. handleCommand 重构
- 新增参数：`sourceGroupId`, `isFromAdminGroup`
- 权限检查：`ADMIN_ONLY_COMMANDS` 在玩家群执行时返回 `❌ 此命令仅限管理群使用`

### 6. sendReplyToQQ 重载
- 新增 `sendReplyToQQ(long groupId, String message)` 方法
- 旧方法标记为 `@Deprecated`

### 7. 所有命令处理方法签名更新 (13个)
全部添加 `sourceGroupId` 参数，所有回复消息均指定目标群号

---

## 编译验证
```
./gradlew compileJava
BUILD SUCCESSFUL
```

---

## 待续

STEP 3 已完成，可继续 STEP 4 (@提及通知) 或 STEP 5 (末影箱查询)
