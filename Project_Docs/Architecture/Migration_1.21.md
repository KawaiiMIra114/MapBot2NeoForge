# Minecraft 1.21 迁移指南 (NBT to DataComponents)

## 核心变更
在 Minecraft 1.20.5+ 及 1.21 版本中，**ItemStack 的 NBT 标签系统已被移除**，取而代之的是 **Data Components (数据组件)**。

## 代码对照表

| 操作 | 旧版 (1.12-1.20.4) | 新版 (NeoForge 1.21.1) |
| :--- | :--- | :--- |
| 获取 NBT | `stack.getTag()` | `stack.getComponents()` |
| 写入自定义数据 | `tag.setString("key", "val")` | `stack.set(DataComponents.CUSTOM_DATA, ...)` |
| 检查物品名 | `stack.getDisplayName()` | `stack.getHoverName()` |

## MapBot 实现策略
在实现“查背包”功能时，必须使用 `DataComponentMap` 遍历物品属性，严禁调用已不存在的 `nbt` 方法。
