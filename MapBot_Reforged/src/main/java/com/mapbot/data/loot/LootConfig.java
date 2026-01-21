package com.mapbot.data.loot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
        if (totalWeight <= 0) return null;
        
        int roll = random.nextInt(totalWeight);
        int current = 0;

        for (LootEntry entry : table.entries) {
            current += entry.weight;
            if (roll < current) {
                // 选中该稀有度组
                return resolveItem(entry);
            }
        }
        return null;
    }
    
    public String getRarityMessage(String rarity) {
        if (table == null) return "";
        return table.messages.getOrDefault(rarity, "[系统] 获得物品: ");
    }

    /**
     * 解析 LootItem (处理 ITEM 和 TAG)
     */
    private LootItem resolveItem(LootEntry entry) {
        if (entry.items.isEmpty()) return null;
        LootItem configItem = entry.items.get(random.nextInt(entry.items.size()));
        
        // 如果是具体物品，直接返回
        if ("ITEM".equalsIgnoreCase(configItem.type)) {
            LootItem result = new LootItem(configItem.id, configItem.count, configItem.name);
            result.rarity = entry.rarity;
            return result;
        } 
        
        // 如果是标签，随机解析一个
        if ("TAG".equalsIgnoreCase(configItem.type)) {
            return resolveTag(configItem, entry.rarity);
        }
        
        return null;
    }
    
    private LootItem resolveTag(LootItem tagItem, String rarity) {
        try {
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(tagItem.id));
            
            // 获取 Tag 下的所有物品
            var items = StreamSupport.stream(BuiltInRegistries.ITEM.getTagOrEmpty(tagKey).spliterator(), false)
                    .map(holder -> holder.value())
                    .collect(Collectors.toList());
            
            if (items.isEmpty()) {
                LOGGER.warn("标签为空或未注册: {}", tagItem.id);
                // 回退: 返回一个默认物品 (如苹果) 避免报错
                LootItem fallback = new LootItem("minecraft:apple", tagItem.count, "苹果 (TagFallback)");
                fallback.rarity = rarity;
                return fallback;
            }
            
            // 随机选一个
            Item randomItem = items.get(random.nextInt(items.size()));
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(randomItem);
            
            // 获取物品显示名称 (这里只能拿到 ID 对应的翻译键，SignCommand 会再次翻译)
            // 为了简单，我们暂且用 ID 的 path 部分作为临时名称，SignManager 发放时会显示真实名称
            String name = tagItem.name.isEmpty() ? id.getPath() : tagItem.name;
            
            LootItem result = new LootItem(id.toString(), tagItem.count, name);
            result.rarity = rarity;
            return result;
            
        } catch (Exception e) {
            LOGGER.error("解析 Tag 失败: {}", tagItem.id, e);
            return null;
        }
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
        
        // Common (60%) - 基础建材与资源
        LootEntry common = new LootEntry("Common", 60);
        common.items.add(new LootItem("minecraft:planks", 32, "随机木板", "TAG"));
        common.items.add(new LootItem("minecraft:logs", 16, "随机原木", "TAG"));
        common.items.add(new LootItem("minecraft:wool", 16, "随机羊毛", "TAG"));
        common.items.add(new LootItem("minecraft:leaves", 16, "随机树叶", "TAG"));
        common.items.add(new LootItem("minecraft:saplings", 4, "随机树苗", "TAG"));
        common.items.add(new LootItem("minecraft:flowers", 4, "随机花朵", "TAG"));
        common.items.add(new LootItem("minecraft:sand", 32, "沙子")); // Item
        common.items.add(new LootItem("minecraft:gravel", 32, "沙砾")); // Item
        common.items.add(new LootItem("minecraft:glass", 16, "玻璃")); // Item
        common.items.add(new LootItem("minecraft:coal", 16, "煤炭")); // Item
        common.items.add(new LootItem("minecraft:bread", 8, "面包")); // Item
        table.entries.add(common);
        table.messages.put("Common", "[提示] 签到成功，获得基础建材或资源。");

        // Rare (30%) - 进阶资源与装饰
        LootEntry rare = new LootEntry("Rare", 30);
        rare.items.add(new LootItem("minecraft:iron_ingot", 8, "铁锭"));
        rare.items.add(new LootItem("minecraft:gold_ingot", 8, "金锭"));
        rare.items.add(new LootItem("minecraft:redstone", 16, "红石粉"));
        rare.items.add(new LootItem("minecraft:lapis_lazuli", 16, "青金石"));
        rare.items.add(new LootItem("minecraft:quartz", 16, "下界石英"));
        rare.items.add(new LootItem("minecraft:candles", 8, "随机蜡烛", "TAG"));
        rare.items.add(new LootItem("minecraft:banners", 1, "随机旗帜", "TAG"));
        rare.items.add(new LootItem("minecraft:beds", 1, "随机床", "TAG"));
        rare.items.add(new LootItem("minecraft:music_discs", 1, "随机唱片", "TAG"));
        table.entries.add(rare);
        table.messages.put("Rare", "[提示] 运气不错，获得了稀有资源。");

        // Epic (9%) - 珍贵宝物
        LootEntry epic = new LootEntry("Epic", 9);
        epic.items.add(new LootItem("minecraft:diamond", 4, "钻石"));
        epic.items.add(new LootItem("minecraft:emerald", 8, "绿宝石"));
        epic.items.add(new LootItem("minecraft:ender_pearl", 4, "末影珍珠"));
        epic.items.add(new LootItem("minecraft:blaze_rod", 4, "烈焰棒"));
        epic.items.add(new LootItem("minecraft:ghast_tear", 2, "恶魂之泪"));
        epic.items.add(new LootItem("minecraft:shulker_shell", 2, "潜影壳"));
        table.entries.add(epic);
        table.messages.put("Epic", "[提示] 欧气爆发，这是史诗级的奖励！");

        // Legendary (1%) - 传说神器
        LootEntry legendary = new LootEntry("Legendary", 1);
        legendary.items.add(new LootItem("minecraft:netherite_ingot", 1, "下界合金锭"));
        legendary.items.add(new LootItem("minecraft:golden_apple", 4, "金苹果"));
        legendary.items.add(new LootItem("minecraft:enchanted_golden_apple", 1, "附魔金苹果"));
        legendary.items.add(new LootItem("minecraft:totem_of_undying", 1, "不死图腾"));
        legendary.items.add(new LootItem("minecraft:beacon", 1, "信标"));
        legendary.items.add(new LootItem("minecraft:nether_star", 1, "下界之星"));
        table.entries.add(legendary);
        table.messages.put("Legendary", "[警告] 传说降临！天选之子。");
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
        public String type = "ITEM"; // ITEM 或 TAG
        public String id; // 物品ID 或 Tag名 (不带#)
        public int count;
        public String name; // 显示名称 (TAG时可为空)
        public transient String rarity;

        // 兼容旧 ITEM 构造
        LootItem(String id, int count, String name) {
            this.id = id;
            this.count = count;
            this.name = name;
        }
        
        // TAG 构造
        LootItem(String id, int count, String name, String type) {
            this.id = id;
            this.count = count;
            this.name = name;
            this.type = type;
        }
    }
}
