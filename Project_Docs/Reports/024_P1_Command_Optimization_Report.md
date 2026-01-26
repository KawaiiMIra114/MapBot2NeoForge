# Task #024: P1 命令逻辑优化报告

> 任务编号: #024  
> 优先级: P1  
> 完成时间: 2026-01-26  
> 版本: v5.6.0 → v5.7.0

---

## 一、任务目标

优化 MapBot Alpha Core 命令系统的用户体验,包括:

1. **#help 分群权限显示** - 根据群类型和用户权限智能显示可用命令
2. **#addadmin 首次自动成功** - 解决初次部署无管理员的问题
3. **#addadmin 语法增强** - 支持设置不同权限等级
4. **#id 绑定冲突提示优化** - 显示占用者 QQ 并提供解决方案
5. **新增 #agreeunbind 命令** - 管理员强制解绑功能

---

## 二、变更文件清单

### Alpha Core (5 个文件)

| 文件路径 | 变更类型 | 说明 |
|---------|---------|------|
| `command/impl/HelpCommand.java` | 重构 | 实现分群权限显示逻辑 |
| `command/impl/AddAdminCommand.java` | 增强 | 首次自动成功 + 语法增强 |
| `command/CommandRegistry.java` | 修改 | 添加 addadmin 特殊处理逻辑 |
| `command/impl/BindCommand.java` | 优化 | 绑定冲突提示优化 |
| `command/impl/AgreeUnbindCommand.java` | 新增 | 管理员强制解绑命令 |
| `logic/InboundHandler.java` | 修改 | 注册 agreeunbind 命令 |

### Reforged Mod (1 个文件)

| 文件路径 | 变更类型 | 说明 |
|---------|---------|------|
| `network/BridgeClient.java` | 修改 | handleBindPlayer 返回占用者 QQ |

---

## 三、功能详细说明

### 3.1 #help 分群权限显示

**优化前问题**:
- 所有用户看到相同的命令列表
- 包含大量无权执行的命令,造成困惑

**优化后行为**:

| 场景 | 显示内容 |
|------|---------|
| 玩家群执行 `#help` | 仅显示 Level 0 命令 (所有人可用) |
| 管理群执行 `#help` | 显示当前用户权限可执行的命令 |
| 任意群执行 `#help all` | 显示全部命令,分为 [可用命令] 和 [需更高权限] 两部分 |

**#help all 输出示例**:
```
=== MapBot 命令帮助 ===

[可用命令]
#sign - 每日签到
#id - 绑定游戏ID: #id <玩家名>
#list - 查看在线玩家
#help - 显示命令帮助 (#help all 查看全部)

[需更高权限]
#inv - 查看玩家背包 [需 Level 1]
#location - 查看玩家位置 [需 Level 1]
#mute - 禁言玩家 [需 Level 2]
#stopserver - 关闭服务器 [需 Admin]
#addadmin - 添加管理员/设置权限 [需 Admin]

[提示] 输入 #help all 查看全部命令
```

**核心实现**:
```java
private boolean canExecute(ICommand cmd, int userLevel, boolean isAdmin) {
    if (cmd.requiresAdmin() && !isAdmin) return false;
    if (userLevel < cmd.requiredPermLevel()) return false;
    return true;
}
```

---

### 3.2 #addadmin 首次自动成功

**优化前问题**:
- 初次部署时系统无管理员
- 无法执行任何管理命令,陷入死锁

**优化后行为**:
- 检测到系统无管理员时,第一次执行 `#addadmin` 自动成功
- 提示: `[系统] 系统无管理员,已自动添加 QQ 123456789 为首位管理员`

**实现位置**:
1. `AddAdminCommand.execute()` - 检查管理员列表是否为空
2. `CommandRegistry.dispatch()` - 跳过权限检查

**核心代码**:
```java
// AddAdminCommand.java
if (DataManager.INSTANCE.getAdmins().isEmpty()) {
    DataManager.INSTANCE.addAdmin(targetQQ);
    return String.format("[系统] 系统无管理员,已自动添加 QQ %d 为首位管理员", targetQQ);
}

// CommandRegistry.java
if ("addadmin".equals(name) && DataManager.INSTANCE.getAdmins().isEmpty()) {
    // 跳过权限检查,直接执行
}
```

---

### 3.3 #addadmin 语法增强

**优化前语法**:
```
#addadmin @用户  → 添加为 Admin
```

**优化后语法**:
```
#addadmin @用户         → 添加为 Admin (超级管理员)
#addadmin @用户 1       → 设置为 Level 1 (受信用户)
#addadmin @用户 2       → 设置为 Level 2 (普通管理员)
#addadmin @用户 admin   → 添加为 Admin (超级管理员)
```

**权限等级说明**:

| 等级 | 名称 | 可用命令 |
|------|------|---------|
| Level 0 | 普通用户 | #sign, #bind, #list, #help |
| Level 1 | 受信用户 | + #inv, #location, #playtime |
| Level 2 | 管理员 | + #mute, #unmute, #setperm |
| Admin | 超级管理员 | + #addadmin, #removeadmin, #stopserver, #agreeunbind |

**实现逻辑**:
```java
if (parts.length > 1) {
    String levelArg = parts[1].toLowerCase();
    if ("admin".equals(levelArg)) {
        setAsAdmin = true;
    } else if ("1".equals(levelArg)) {
        setAsAdmin = false;
        permLevel = 1;
    } else if ("2".equals(levelArg)) {
        setAsAdmin = false;
        permLevel = 2;
    }
}
```

---

### 3.4 #id 绑定冲突提示优化

**优化前提示**:
```
[绑定失败] 该游戏账号已被绑定
```

**优化后提示**:
```
[提示] 该游戏账号已被 QQ:123456789 绑定
如确认此账号归您所有,请联系管理员使用以下命令解绑:
#agreeunbind 123456789
```

**实现流程**:
1. Mod 端 `handleBindPlayer()` 检测到 UUID 冲突
2. 遍历绑定表查找占用者 QQ
3. 返回 `FAIL:OCCUPIED:<占用者QQ>`
4. Alpha 端 `BindCommand` 解析并格式化提示

**核心代码**:
```java
// BridgeClient.java (Mod 端)
if (com.mapbot.data.DataManager.INSTANCE.isUUIDBound(uuid)) {
    Long occupierQQ = null;
    for (var entry : com.mapbot.data.DataManager.INSTANCE.getAllBindings().entrySet()) {
        if (uuid.equals(entry.getValue())) {
            occupierQQ = entry.getKey();
            break;
        }
    }
    String occupierInfo = (occupierQQ != null) ? String.valueOf(occupierQQ) : "未知";
    sendProxyResponse(requestId, "FAIL:OCCUPIED:" + occupierInfo);
    return;
}

// BindCommand.java (Alpha 端)
if (result != null && result.startsWith("FAIL:OCCUPIED:")) {
    String occupierQQ = result.substring("FAIL:OCCUPIED:".length());
    return String.format(
        "[提示] 该游戏账号已被 QQ:%s 绑定\n" +
        "如确认此账号归您所有,请联系管理员使用以下命令解绑:\n" +
        "#agreeunbind %s",
        occupierQQ, occupierQQ
    );
}
```

---

### 3.5 新增 #agreeunbind 命令

**命令用途**: 管理员强制解除指定 QQ 的绑定 (用于处理冲突)

**使用场景**:
1. 玩家 A 发现账号被 QQ B 占用
2. 玩家 A 联系管理员
3. 管理员核实后执行 `#agreeunbind <B的QQ>`
4. 解绑成功,玩家 A 可重新绑定

**命令语法**:
```
#agreeunbind <QQ号>
```

**权限要求**: Admin (超级管理员)

**输出示例**:
```
[成功] 已强制解绑 QQ 123456789 (UUID: 550e8400-e29b-41d4-a716-446655440000)
```

**完整实现**:
```java
public class AgreeUnbindCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        String target = args.trim().replaceAll("[^0-9]", "");
        if (target.isEmpty()) {
            return "[错误] 用法: #agreeunbind <QQ号>\n" +
                   "示例: #agreeunbind 123456789";
        }
        
        long targetQQ = Long.parseLong(target);
        
        // 检查是否已绑定
        String uuid = DataManager.INSTANCE.getBinding(targetQQ);
        if (uuid == null) {
            return String.format("[提示] QQ %d 未绑定任何账号", targetQQ);
        }
        
        // 执行解绑
        boolean success = DataManager.INSTANCE.unbind(targetQQ);
        if (success) {
            return String.format("[成功] 已强制解绑 QQ %d (UUID: %s)", targetQQ, uuid);
        } else {
            return "[错误] 解绑失败";
        }
    }
    
    @Override
    public String getHelp() {
        return "管理员强制解绑: #agreeunbind <QQ号>";
    }
    
    @Override
    public boolean requiresAdmin() {
        return true;
    }
}
```

---

## 四、测试验证

### 4.1 编译测试

```powershell
# Alpha Core
cd Mapbot-Alpha-V1
.\gradlew.bat build --no-daemon
# 结果: BUILD SUCCESSFUL

# Reforged Mod
cd MapBot_Reforged
.\gradlew.bat build --no-daemon
# 结果: BUILD SUCCESSFUL
```

### 4.2 功能测试清单

| 测试项 | 测试场景 | 预期结果 | 状态 |
|--------|---------|---------|------|
| #help 分群显示 | 玩家群执行 #help | 仅显示 Level 0 命令 | 待测试 |
| #help 分群显示 | 管理群执行 #help | 显示当前用户可用命令 | 待测试 |
| #help all | 任意群执行 #help all | 显示全部命令并标注权限 | 待测试 |
| #addadmin 首次 | 无管理员时执行 | 自动成功并提示 | 待测试 |
| #addadmin 语法 | #addadmin @用户 1 | 设置为 Level 1 | 待测试 |
| #addadmin 语法 | #addadmin @用户 2 | 设置为 Level 2 | 待测试 |
| #addadmin 语法 | #addadmin @用户 admin | 设置为 Admin | 待测试 |
| #id 冲突提示 | 绑定已占用账号 | 显示占用者 QQ | 待测试 |
| #agreeunbind | 管理员强制解绑 | 解绑成功并显示 UUID | 待测试 |
| #agreeunbind | 非管理员执行 | 提示权限不足 | 待测试 |

---

## 五、代码审查要点

### 5.1 权限检查逻辑

**CommandRegistry.dispatch()** 中的权限检查顺序:
1. 特殊处理: addadmin 命令在无管理员时跳过检查
2. 管理群专属检查
3. Admin 权限检查
4. Level 权限检查

**注意事项**:
- `addadmin` 命令的 `requiresAdmin()` 返回 `false`,避免常规权限检查
- 首次自动成功后,后续执行仍需 Admin 权限

### 5.2 绑定冲突处理

**数据流**:
```
BindCommand (Alpha)
  ↓ BridgeProxy.resolveAndBind()
  ↓ Bridge TCP
  ↓ BridgeClient.handleBindPlayer() (Mod)
  ↓ 检测 UUID 冲突
  ↓ 查找占用者 QQ
  ↓ 返回 FAIL:OCCUPIED:<QQ>
  ↓ BindCommand 解析并格式化
  ↓ 返回用户友好提示
```

**边界情况**:
- 占用者 QQ 未找到时显示 "未知"
- 离线模式服务器的 UUID 生成逻辑

### 5.3 命令注册

**InboundHandler 静态初始化块**:
```java
CommandRegistry.register("agreeunbind", new AgreeUnbindCommand());
```

确保命令在系统启动时正确注册。

---

## 六、用户文档更新建议

### 6.1 命令帮助文档

需要更新以下内容:
1. #help 命令的 `all` 参数说明
2. #addadmin 命令的等级参数说明
3. 新增 #agreeunbind 命令文档
4. 权限等级对照表

### 6.2 常见问题 FAQ

**Q: 初次部署如何添加管理员?**  
A: 直接执行 `#addadmin @自己`,系统会自动添加首位管理员。

**Q: 如何处理绑定冲突?**  
A: 联系管理员使用 `#agreeunbind <占用者QQ>` 强制解绑。

**Q: Level 1 和 Level 2 有什么区别?**  
A: Level 1 可查询他人信息,Level 2 可执行管理命令 (禁言/设置权限等)。

---

## 七、后续优化建议

### 7.1 命令系统增强

| 优先级 | 建议 | 说明 |
|--------|------|------|
| P2 | #mute 时长格式优化 | 支持 `1h`, `1d`, `永久` 格式 |
| P2 | #time 查询他人 | 管理员可 `#time @用户` 查他人 |
| P3 | #status 信息增强 | 添加服务器版本、Mod 列表 |
| P3 | 错误提示优化 | 将技术异常转为用户友好提示 |

### 7.2 权限系统优化

- 考虑引入角色组 (Role) 概念
- 支持自定义权限节点
- 权限继承机制

### 7.3 审计日志

- 记录管理员操作 (addadmin, agreeunbind 等)
- 提供操作历史查询命令

---

## 八、总结

本次任务完成了 P1 优先级的 5 项命令逻辑优化,显著提升了用户体验:

1. **智能帮助系统** - 根据用户权限动态显示可用命令
2. **零配置部署** - 首次执行自动添加管理员
3. **灵活权限管理** - 支持多级权限设置
4. **友好错误提示** - 绑定冲突时提供明确解决方案
5. **管理员工具** - 新增强制解绑命令

所有修改已通过编译测试,待实际环境验证后即可投入使用。

---

**变更统计**:
- 新增文件: 1 个 (AgreeUnbindCommand.java)
- 修改文件: 6 个
- 新增命令: 1 个 (#agreeunbind)
- 增强命令: 2 个 (#help, #addadmin)
- 优化命令: 1 个 (#id)
- 代码行数: +约 200 行

**版本更新**: v5.6.0 → v5.7.0
