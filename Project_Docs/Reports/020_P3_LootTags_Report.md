# Task #020-P3 执行报告: 奖池系统升级 (Tag 支持)

**执行者**: Lazarus
**日期**: 2026-01-21
**状态**: ✅ 完成

---

## 任务目标

大幅扩充签到奖池的内容，并引入 Minecraft Tag (标签) 支持，以实现对模组物品的自动兼容和随机化。

---

## 技术变更

### 1. 数据结构升级 (`LootItem`)
*   新增 `type` 字段:
    *   `ITEM` (默认): 指定具体的物品 ID (如 `minecraft:diamond`)。
    *   `TAG`: 指定标签名 (如 `minecraft:planks`)。

### 2. 抽奖逻辑升级 (`LootConfig.roll`)
*   **Tag 解析**:
    *   使用 `BuiltInRegistries.ITEM.getTagOrEmpty` 获取标签下的所有物品。
    *   从中随机选取一个具体的 Item ID。
    *   如果标签为空，回退到 `minecraft:apple` 防止报错。
*   **动态命名**:
    *   对于 Tag 生成的物品，暂使用 ID 的 Path 部分作为名称 (例如 `oak_planks`)，后续由客户端或服务器语言文件负责翻译。

### 3. 默认奖池扩充
利用 Tag 系统，我们将默认奖池从原来的约 20 种物品扩充到了**数百种** (取决于安装的模组数量)。

| 稀有度 | 新增 Tag 示例 | 包含内容 |
| :--- | :--- | :--- |
| **Common** | `planks`, `logs`, `wool`, `leaves`, `saplings`, `flowers` | 所有木材、羊毛、树苗、花朵 |
| **Rare** | `candles`, `banners`, `beds`, `music_discs` | 所有颜色蜡烛/床/旗帜，所有唱片 |

---

## 优势
*   **模组兼容**: 安装了 Biomes O' Plenty? 签到能抽到樱花木板！
*   **维护简单**: 配置 `minecraft:logs` 一行，顶替了写几十行原木 ID。
*   **体验丰富**: 玩家每天签到都能拿到不一样的方块，不再是枯燥的苹果面包。

---

**签名**: Lazarus - MapBot Reforged 开发执行者
