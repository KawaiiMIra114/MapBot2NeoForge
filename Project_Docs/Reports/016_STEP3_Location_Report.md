# Task #016-STEP3 执行报告: 玩家位置查询

**执行者**: Lazarus  
**日期**: 2026-01-18  
**状态**: ✅ 完成  
**提交**: `2dbbc47`

---

## 任务目标

实现管理员专用的玩家位置查询功能，显示玩家所在维度、坐标和朝向。

---

## 变更内容

### 修改文件

#### [MODIFY] `InboundHandler.java`

**1. 添加命令到管理员命令集合**
```java
private static final Set<String> ADMIN_ONLY_COMMANDS = Set.of(
    // ...
    "location", "位置"  // Task #016-STEP3
);
```

**2. 命令分发**
```java
case "location", "位置" -> handleLocationCommand(rawArgs, senderQQ, sourceGroupId);
```

**3. 新增方法**

| 方法 | 功能 |
|------|------|
| `handleLocationCommand()` | 处理位置查询命令 |
| `getDimensionDisplayName()` | 维度 ID 转中文名称 |
| `getYawDirection()` | Yaw 角度转方向文字 |

---

#### [MODIFY] `ServerStatusManager.java`

更新 `getHelp()` 帮助信息，添加 `#location` 命令说明。

---

## 技术说明

### 维度显示名称映射

| 维度 ID | 显示名称 |
|---------|----------|
| `minecraft:overworld` | 主世界 |
| `minecraft:the_nether` | 下界 |
| `minecraft:the_end` | 末地 |
| 其他模组维度 | 移除命名空间前缀 |

### 朝向计算

使用 8 方向判断 (南/西南/西/西北/北/东北/东/东南)：

```java
// Minecraft Yaw: 0 = 南, 90 = 西, 180 = 北, 270 = 东
float normalizedYaw = (yaw % 360 + 360) % 360;
```

### 权限控制

命令仅限管理群使用，在玩家群发送会返回 "此命令仅限管理群使用"。

---

## 编译验证

```
./gradlew build -x test
BUILD SUCCESSFUL
```

---

## 使用示例

```
#location Steve

📍 Steve 的位置
─────────
🌍 维度: 主世界
📌 坐标: 128, 64, -256
🧭 朝向: 北 (N)
```

---

## 输出格式

| 字段 | 说明 |
|------|------|
| 📍 玩家名 | 查询目标 |
| 🌍 维度 | 主世界/下界/末地/模组维度 |
| 📌 坐标 | X, Y, Z (取整) |
| 🧭 朝向 | 8 方向显示 |

---

**签名**: Lazarus - MapBot Reforged 开发执行者
