package com.mapbot.data.loot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 签到奖池配置
 * 存储路径: config/mapbot_loot.json
 */
public class LootConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/Loot");
    public static final LootConfig INSTANCE = new LootConfig();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private LootTable table;
    private Path configPath;
    private final Random random = new Random();

    public void init() {
        try {
            configPath = FMLPaths.CONFIGDIR.get().resolve("mapbot_loot.json");
            if (Files.exists(configPath)) {
                load();
            } else {
                createDefault();
                save();
            }
        } catch (Exception e) {
            LOGGER.error("初始化奖池配置失败", e);
        }
    }

    public LootItem roll() {
        if (table == null || table.entries.isEmpty()) return null;

        // 计算总权重
        int totalWeight = table.entries.stream().mapToInt(e -> e.weight).sum();
        int roll = random.nextInt(totalWeight);
        int current = 0;

        for (LootEntry entry : table.entries) {
            current += entry.weight;
            if (roll < current) {
                // 选中该稀有度组，从中随机选一个物品
                return getRandomItemFromEntry(entry);
            }
        }
        return null;
    }
    
    public String getRarityMessage(String rarity) {
        if (table == null) return "";
        return table.messages.getOrDefault(rarity, "获得物品: ");
    }

    private LootItem getRandomItemFromEntry(LootEntry entry) {
        if (entry.items.isEmpty()) return null;
        LootItem item = entry.items.get(random.nextInt(entry.items.size()));
        // 注入稀有度信息
        item.rarity = entry.rarity;
        return item;
    }

    private void load() throws IOException {
        String json = Files.readString(configPath);
        table = GSON.fromJson(json, LootTable.class);
        LOGGER.info("已加载奖池: {} 个稀有度组", table.entries.size());
    }

    private void save() throws IOException {
        Files.writeString(configPath, GSON.toJson(table));
    }

    private void createDefault() {
        table = new LootTable();
        
        // 普通 (Common) - 60%
        LootEntry common = new LootEntry("Common", 60);
        common.items.add(new LootItem("minecraft:apple", 3, "苹果"));
        common.items.add(new LootItem("minecraft:bread", 5, "面包"));
        common.items.add(new LootItem("minecraft:coal", 8, "煤炭"));
        common.items.add(new LootItem("minecraft:glass_bottle", 4, "玻璃瓶"));
        common.items.add(new LootItem("minecraft:honeycomb", 2, "蜜脾"));
        table.entries.add(common);
        table.messages.put("Common", "✨ 签到成功！运气平平，但也收获满满~");

        // 稀有 (Rare) - 30%
        LootEntry rare = new LootEntry("Rare", 30);
        rare.items.add(new LootItem("minecraft:iron_ingot", 5, "铁锭"));
        rare.items.add(new LootItem("minecraft:gold_ingot", 3, "金锭"));
        rare.items.add(new LootItem("minecraft:slime_ball", 4, "粘液球"));
        rare.items.add(new LootItem("minecraft:experience_bottle", 8, "附魔之瓶"));
        table.entries.add(rare);
        table.messages.put("Rare", "🌟 哇！运气不错，获得了稀有物资！");

        // 史诗 (Epic) - 9%
        LootEntry epic = new LootEntry("Epic", 9);
        epic.items.add(new LootItem("minecraft:diamond", 2, "钻石"));
        epic.items.add(new LootItem("minecraft:emerald", 5, "绿宝石"));
        epic.items.add(new LootItem("minecraft:name_tag", 1, "命名牌"));
        table.entries.add(epic);
        table.messages.put("Epic", "🔥 欧气爆发！这是史诗级的奖励！");

        // 传说 (Legendary) - 1%
        LootEntry legendary = new LootEntry("Legendary", 1);
        legendary.items.add(new LootItem("minecraft:netherite_scrap", 1, "下界合金碎片"));
        legendary.items.add(new LootItem("minecraft:golden_apple", 2, "金苹果"));
        legendary.items.add(new LootItem("minecraft:totem_of_undying", 1, "不死图腾"));
        table.entries.add(legendary);
        table.messages.put("Legendary", "👑 【传说降临】 天选之子！全服为你欢呼！");
    }

    // 数据结构
    private static class LootTable {
        List<LootEntry> entries = new ArrayList<>();
        java.util.Map<String, String> messages = new java.util.HashMap<>();
    }

    private static class LootEntry {
        String rarity;
        int weight;
        List<LootItem> items = new ArrayList<>();

        LootEntry(String r, int w) { rarity = r; weight = w; }
    }

    public static class LootItem {
        public String id;
        public int count;
        public String name; // 显示名称
        public transient String rarity; // 运行时注入

        LootItem(String id, int count, String name) {
            this.id = id;
            this.count = count;
            this.name = name;
        }
    }
}
