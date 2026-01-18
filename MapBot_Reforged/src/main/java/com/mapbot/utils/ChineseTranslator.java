/*
 * MapBot Reforged - 中文翻译查找器
 * 
 * 遵从 .ai_rules.md 中定义的治理规则。
 * 从 Minecraft 的语言文件中加载中文翻译。
 * 
 * 用于将物品名、附魔名等翻译为中文显示。
 */

package com.mapbot.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 中文翻译查找器 (单例)
 * 从 Minecraft 资源包加载中文语言文件
 */
public class ChineseTranslator {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/Translator");
    
    /** 单例实例 */
    public static final ChineseTranslator INSTANCE = new ChineseTranslator();
    
    /** 翻译映射表 (translation_key -> 中文) */
    private final Map<String, String> translations = new HashMap<>();
    
    /** 是否已加载 */
    private boolean loaded = false;
    
    private ChineseTranslator() {}
    
    /**
     * 初始化并加载中文语言文件
     * 应在模组初始化时调用
     */
    public void init() {
        if (loaded) {
            return;
        }
        
        // 尝试从 classpath 加载中文语言文件
        // Minecraft 的语言文件路径: assets/minecraft/lang/zh_cn.json
        String[] langPaths = {
            "/assets/minecraft/lang/zh_cn.json",
            "assets/minecraft/lang/zh_cn.json"
        };
        
        for (String path : langPaths) {
            try (InputStream is = getClass().getResourceAsStream(path)) {
                if (is != null) {
                    loadFromStream(is);
                    LOGGER.info("成功加载中文语言文件: {}", path);
                    loaded = true;
                    return;
                }
            } catch (Exception e) {
                LOGGER.debug("尝试加载语言文件失败: {} - {}", path, e.getMessage());
            }
        }
        
        // 如果无法从资源包加载，使用内置的常用物品翻译
        loadBuiltinTranslations();
        loaded = true;
        LOGGER.info("使用内置常用物品翻译表");
    }
    
    /**
     * 从输入流加载语言文件
     */
    private void loadFromStream(InputStream is) {
        try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>() {}.getType();
            Map<String, String> langData = gson.fromJson(reader, type);
            
            if (langData != null) {
                translations.putAll(langData);
                LOGGER.info("加载了 {} 条翻译", langData.size());
            }
        } catch (Exception e) {
            LOGGER.error("解析语言文件失败: {}", e.getMessage());
        }
    }
    
    /**
     * 加载内置的常用物品翻译
     * 当无法从资源包加载时使用
     */
    private void loadBuiltinTranslations() {
        // ===== 常用工具 =====
        translations.put("item.minecraft.diamond_sword", "钻石剑");
        translations.put("item.minecraft.diamond_pickaxe", "钻石镐");
        translations.put("item.minecraft.diamond_axe", "钻石斧");
        translations.put("item.minecraft.diamond_shovel", "钻石锹");
        translations.put("item.minecraft.diamond_hoe", "钻石锄");
        translations.put("item.minecraft.netherite_sword", "下界合金剑");
        translations.put("item.minecraft.netherite_pickaxe", "下界合金镐");
        translations.put("item.minecraft.netherite_axe", "下界合金斧");
        translations.put("item.minecraft.netherite_shovel", "下界合金锹");
        translations.put("item.minecraft.netherite_hoe", "下界合金锄");
        translations.put("item.minecraft.iron_sword", "铁剑");
        translations.put("item.minecraft.iron_pickaxe", "铁镐");
        translations.put("item.minecraft.iron_axe", "铁斧");
        translations.put("item.minecraft.iron_shovel", "铁锹");
        translations.put("item.minecraft.iron_hoe", "铁锄");
        translations.put("item.minecraft.golden_sword", "金剑");
        translations.put("item.minecraft.golden_pickaxe", "金镐");
        translations.put("item.minecraft.golden_axe", "金斧");
        translations.put("item.minecraft.stone_sword", "石剑");
        translations.put("item.minecraft.stone_pickaxe", "石镐");
        translations.put("item.minecraft.stone_axe", "石斧");
        translations.put("item.minecraft.wooden_sword", "木剑");
        translations.put("item.minecraft.wooden_pickaxe", "木镐");
        translations.put("item.minecraft.wooden_axe", "木斧");
        translations.put("item.minecraft.bow", "弓");
        translations.put("item.minecraft.crossbow", "弩");
        translations.put("item.minecraft.trident", "三叉戟");
        translations.put("item.minecraft.shield", "盾牌");
        translations.put("item.minecraft.fishing_rod", "钓鱼竿");
        translations.put("item.minecraft.flint_and_steel", "打火石");
        translations.put("item.minecraft.shears", "剪刀");
        translations.put("item.minecraft.elytra", "鞘翅");
        
        // ===== 护甲 =====
        translations.put("item.minecraft.diamond_helmet", "钻石头盔");
        translations.put("item.minecraft.diamond_chestplate", "钻石胸甲");
        translations.put("item.minecraft.diamond_leggings", "钻石护腿");
        translations.put("item.minecraft.diamond_boots", "钻石靴子");
        translations.put("item.minecraft.netherite_helmet", "下界合金头盔");
        translations.put("item.minecraft.netherite_chestplate", "下界合金胸甲");
        translations.put("item.minecraft.netherite_leggings", "下界合金护腿");
        translations.put("item.minecraft.netherite_boots", "下界合金靴子");
        translations.put("item.minecraft.iron_helmet", "铁头盔");
        translations.put("item.minecraft.iron_chestplate", "铁胸甲");
        translations.put("item.minecraft.iron_leggings", "铁护腿");
        translations.put("item.minecraft.iron_boots", "铁靴子");
        translations.put("item.minecraft.golden_helmet", "金头盔");
        translations.put("item.minecraft.golden_chestplate", "金胸甲");
        translations.put("item.minecraft.golden_leggings", "金护腿");
        translations.put("item.minecraft.golden_boots", "金靴子");
        translations.put("item.minecraft.leather_helmet", "皮革帽子");
        translations.put("item.minecraft.leather_chestplate", "皮革外套");
        translations.put("item.minecraft.leather_leggings", "皮革裤子");
        translations.put("item.minecraft.leather_boots", "皮革靴子");
        translations.put("item.minecraft.chainmail_helmet", "锁链头盔");
        translations.put("item.minecraft.chainmail_chestplate", "锁链胸甲");
        translations.put("item.minecraft.chainmail_leggings", "锁链护腿");
        translations.put("item.minecraft.chainmail_boots", "锁链靴子");
        translations.put("item.minecraft.turtle_helmet", "海龟壳");
        
        // ===== 食物 =====
        translations.put("item.minecraft.apple", "苹果");
        translations.put("item.minecraft.golden_apple", "金苹果");
        translations.put("item.minecraft.enchanted_golden_apple", "附魔金苹果");
        translations.put("item.minecraft.bread", "面包");
        translations.put("item.minecraft.cooked_beef", "牛排");
        translations.put("item.minecraft.beef", "生牛肉");
        translations.put("item.minecraft.cooked_porkchop", "熟猪排");
        translations.put("item.minecraft.porkchop", "生猪排");
        translations.put("item.minecraft.cooked_chicken", "熟鸡肉");
        translations.put("item.minecraft.chicken", "生鸡肉");
        translations.put("item.minecraft.cooked_mutton", "熟羊肉");
        translations.put("item.minecraft.mutton", "生羊肉");
        translations.put("item.minecraft.cooked_rabbit", "熟兔肉");
        translations.put("item.minecraft.rabbit", "生兔肉");
        translations.put("item.minecraft.cooked_cod", "熟鳕鱼");
        translations.put("item.minecraft.cod", "生鳕鱼");
        translations.put("item.minecraft.cooked_salmon", "熟鲑鱼");
        translations.put("item.minecraft.salmon", "生鲑鱼");
        translations.put("item.minecraft.carrot", "胡萝卜");
        translations.put("item.minecraft.golden_carrot", "金胡萝卜");
        translations.put("item.minecraft.potato", "马铃薯");
        translations.put("item.minecraft.baked_potato", "烤马铃薯");
        translations.put("item.minecraft.melon_slice", "西瓜片");
        translations.put("item.minecraft.pumpkin_pie", "南瓜派");
        translations.put("item.minecraft.cookie", "曲奇");
        translations.put("item.minecraft.cake", "蛋糕");
        translations.put("item.minecraft.sweet_berries", "甜浆果");
        translations.put("item.minecraft.glow_berries", "发光浆果");
        
        // ===== 矿物材料 =====
        translations.put("item.minecraft.diamond", "钻石");
        translations.put("item.minecraft.emerald", "绿宝石");
        translations.put("item.minecraft.gold_ingot", "金锭");
        translations.put("item.minecraft.iron_ingot", "铁锭");
        translations.put("item.minecraft.copper_ingot", "铜锭");
        translations.put("item.minecraft.netherite_ingot", "下界合金锭");
        translations.put("item.minecraft.netherite_scrap", "下界合金碎片");
        translations.put("item.minecraft.ancient_debris", "远古残骸");
        translations.put("item.minecraft.coal", "煤炭");
        translations.put("item.minecraft.charcoal", "木炭");
        translations.put("item.minecraft.lapis_lazuli", "青金石");
        translations.put("item.minecraft.redstone", "红石粉");
        translations.put("item.minecraft.quartz", "下界石英");
        translations.put("item.minecraft.amethyst_shard", "紫水晶碎片");
        translations.put("item.minecraft.raw_iron", "粗铁");
        translations.put("item.minecraft.raw_gold", "粗金");
        translations.put("item.minecraft.raw_copper", "粗铜");
        
        // ===== 方块 =====
        translations.put("block.minecraft.diamond_block", "钻石块");
        translations.put("block.minecraft.gold_block", "金块");
        translations.put("block.minecraft.iron_block", "铁块");
        translations.put("block.minecraft.emerald_block", "绿宝石块");
        translations.put("block.minecraft.netherite_block", "下界合金块");
        translations.put("block.minecraft.oak_log", "橡木原木");
        translations.put("block.minecraft.spruce_log", "云杉原木");
        translations.put("block.minecraft.birch_log", "白桦原木");
        translations.put("block.minecraft.jungle_log", "丛林原木");
        translations.put("block.minecraft.acacia_log", "金合欢原木");
        translations.put("block.minecraft.dark_oak_log", "深色橡木原木");
        translations.put("block.minecraft.mangrove_log", "红树原木");
        translations.put("block.minecraft.cherry_log", "樱花原木");
        translations.put("block.minecraft.stone", "石头");
        translations.put("block.minecraft.cobblestone", "圆石");
        translations.put("block.minecraft.deepslate", "深板岩");
        translations.put("block.minecraft.cobbled_deepslate", "深板岩圆石");
        translations.put("block.minecraft.dirt", "泥土");
        translations.put("block.minecraft.grass_block", "草方块");
        translations.put("block.minecraft.sand", "沙子");
        translations.put("block.minecraft.gravel", "沙砾");
        translations.put("block.minecraft.obsidian", "黑曜石");
        translations.put("block.minecraft.crying_obsidian", "哭泣的黑曜石");
        translations.put("block.minecraft.chest", "箱子");
        translations.put("block.minecraft.ender_chest", "末影箱");
        translations.put("block.minecraft.shulker_box", "潜影盒");
        translations.put("block.minecraft.crafting_table", "工作台");
        translations.put("block.minecraft.furnace", "熔炉");
        translations.put("block.minecraft.blast_furnace", "高炉");
        translations.put("block.minecraft.smoker", "烟熏炉");
        translations.put("block.minecraft.anvil", "铁砧");
        translations.put("block.minecraft.enchanting_table", "附魔台");
        translations.put("block.minecraft.brewing_stand", "酿造台");
        translations.put("block.minecraft.beacon", "信标");
        translations.put("block.minecraft.conduit", "潮涌核心");
        translations.put("block.minecraft.respawn_anchor", "重生锚");
        translations.put("block.minecraft.torch", "火把");
        translations.put("block.minecraft.lantern", "灯笼");
        translations.put("block.minecraft.soul_lantern", "灵魂灯笼");
        translations.put("block.minecraft.glowstone", "荧石");
        translations.put("block.minecraft.sea_lantern", "海晶灯");
        translations.put("block.minecraft.end_rod", "末地烛");
        
        // ===== 附魔 =====
        translations.put("enchantment.minecraft.sharpness", "锋利");
        translations.put("enchantment.minecraft.smite", "亡灵杀手");
        translations.put("enchantment.minecraft.bane_of_arthropods", "节肢杀手");
        translations.put("enchantment.minecraft.knockback", "击退");
        translations.put("enchantment.minecraft.fire_aspect", "火焰附加");
        translations.put("enchantment.minecraft.looting", "抢夺");
        translations.put("enchantment.minecraft.sweeping_edge", "横扫之刃");
        translations.put("enchantment.minecraft.efficiency", "效率");
        translations.put("enchantment.minecraft.silk_touch", "精准采集");
        translations.put("enchantment.minecraft.fortune", "时运");
        translations.put("enchantment.minecraft.unbreaking", "耐久");
        translations.put("enchantment.minecraft.mending", "经验修补");
        translations.put("enchantment.minecraft.protection", "保护");
        translations.put("enchantment.minecraft.fire_protection", "火焰保护");
        translations.put("enchantment.minecraft.blast_protection", "爆炸保护");
        translations.put("enchantment.minecraft.projectile_protection", "弹射物保护");
        translations.put("enchantment.minecraft.feather_falling", "摔落保护");
        translations.put("enchantment.minecraft.respiration", "水下呼吸");
        translations.put("enchantment.minecraft.aqua_affinity", "水下速掘");
        translations.put("enchantment.minecraft.depth_strider", "深海探索者");
        translations.put("enchantment.minecraft.frost_walker", "冰霜行者");
        translations.put("enchantment.minecraft.soul_speed", "灵魂疾行");
        translations.put("enchantment.minecraft.swift_sneak", "迅捷潜行");
        translations.put("enchantment.minecraft.thorns", "荆棘");
        translations.put("enchantment.minecraft.power", "力量");
        translations.put("enchantment.minecraft.punch", "冲击");
        translations.put("enchantment.minecraft.flame", "火矢");
        translations.put("enchantment.minecraft.infinity", "无限");
        translations.put("enchantment.minecraft.multishot", "多重射击");
        translations.put("enchantment.minecraft.piercing", "穿透");
        translations.put("enchantment.minecraft.quick_charge", "快速装填");
        translations.put("enchantment.minecraft.loyalty", "忠诚");
        translations.put("enchantment.minecraft.riptide", "激流");
        translations.put("enchantment.minecraft.channeling", "引雷");
        translations.put("enchantment.minecraft.impaling", "穿刺");
        translations.put("enchantment.minecraft.lure", "饵钓");
        translations.put("enchantment.minecraft.luck_of_the_sea", "海之眷顾");
        translations.put("enchantment.minecraft.curse_of_vanishing", "消失诅咒");
        translations.put("enchantment.minecraft.curse_of_binding", "绑定诅咒");
        
        // ===== 药水效果 =====
        translations.put("item.minecraft.potion", "药水");
        translations.put("item.minecraft.splash_potion", "喷溅药水");
        translations.put("item.minecraft.lingering_potion", "滞留药水");
        translations.put("item.minecraft.tipped_arrow", "药箭");
        
        // ===== 其他常用物品 =====
        translations.put("item.minecraft.ender_pearl", "末影珍珠");
        translations.put("item.minecraft.eye_of_ender", "末影之眼");
        translations.put("item.minecraft.blaze_rod", "烈焰棒");
        translations.put("item.minecraft.blaze_powder", "烈焰粉");
        translations.put("item.minecraft.ghast_tear", "恶魂之泪");
        translations.put("item.minecraft.nether_star", "下界之星");
        translations.put("item.minecraft.totem_of_undying", "不死图腾");
        translations.put("item.minecraft.name_tag", "命名牌");
        translations.put("item.minecraft.lead", "拴绳");
        translations.put("item.minecraft.saddle", "鞍");
        translations.put("item.minecraft.arrow", "箭");
        translations.put("item.minecraft.spectral_arrow", "光灵箭");
        translations.put("item.minecraft.firework_rocket", "烟火火箭");
        translations.put("item.minecraft.experience_bottle", "附魔之瓶");
        translations.put("item.minecraft.book", "书");
        translations.put("item.minecraft.enchanted_book", "附魔书");
        translations.put("item.minecraft.writable_book", "书与笔");
        translations.put("item.minecraft.written_book", "成书");
        translations.put("item.minecraft.map", "地图");
        translations.put("item.minecraft.filled_map", "地图");
        translations.put("item.minecraft.compass", "指南针");
        translations.put("item.minecraft.clock", "时钟");
        translations.put("item.minecraft.spyglass", "望远镜");
        translations.put("item.minecraft.bucket", "桶");
        translations.put("item.minecraft.water_bucket", "水桶");
        translations.put("item.minecraft.lava_bucket", "熔岩桶");
        translations.put("item.minecraft.milk_bucket", "奶桶");
        translations.put("item.minecraft.bone", "骨头");
        translations.put("item.minecraft.bone_meal", "骨粉");
        translations.put("item.minecraft.string", "线");
        translations.put("item.minecraft.slime_ball", "黏液球");
        translations.put("item.minecraft.leather", "皮革");
        translations.put("item.minecraft.feather", "羽毛");
        translations.put("item.minecraft.egg", "鸡蛋");
        translations.put("item.minecraft.snowball", "雪球");
        translations.put("item.minecraft.paper", "纸");
        translations.put("item.minecraft.stick", "木棍");
        
        // ===== 机械动力 (Create Mod) - 材料 =====
        translations.put("item.create.andesite_alloy", "安山合金");
        translations.put("item.create.zinc_ingot", "锌锭");
        translations.put("item.create.brass_ingot", "黄铜锭");
        translations.put("item.create.crushed_raw_iron", "粉碎的铁矿石");
        translations.put("item.create.crushed_raw_gold", "粉碎的金矿石");
        translations.put("item.create.crushed_raw_copper", "粉碎的铜矿石");
        translations.put("item.create.crushed_raw_zinc", "粉碎的锌矿石");
        translations.put("item.create.brass_sheet", "黄铜板");
        translations.put("item.create.iron_sheet", "铁板");
        translations.put("item.create.golden_sheet", "金板");
        translations.put("item.create.copper_sheet", "铜板");
        translations.put("item.create.brass_nugget", "黄铜粒");
        translations.put("item.create.zinc_nugget", "锌粒");
        translations.put("item.create.exp_nugget", "经验粒");
        translations.put("item.create.polished_rose_quartz", "精制的玫瑰石英");
        translations.put("item.create.rose_quartz", "玫瑰石英");
        translations.put("item.create.sturdy_sheet", "坚固板");
        translations.put("item.create.powdered_obsidian", "黑曜石粉");
        translations.put("item.create.cinder_flour", "火山灰");
        translations.put("item.create.wheat_flour", "小麦粉");
        translations.put("item.create.dough", "面团");
        translations.put("item.create.chocolate_chip", "巧克力碎屑");
        
        // ===== 机械动力 - 工具装备 =====
        translations.put("item.create.wrench", "扳手");
        translations.put("item.create.goggles", "工程师护目镜");
        translations.put("item.create.diving_helmet", "潜水头盔");
        translations.put("item.create.diving_boots", "潜水靴");
        translations.put("item.create.copper_diving_helmet", "铜制潜水头盔");
        translations.put("item.create.copper_diving_boots", "铜制潜水靴");
        translations.put("item.create.netherite_diving_helmet", "下界合金潜水头盔");
        translations.put("item.create.netherite_diving_boots", "下界合金潜水靴");
        translations.put("item.create.copper_backtank", "铜制背罐");
        translations.put("item.create.netherite_backtank", "下界合金背罐");
        translations.put("item.create.extendo_grip", "延伸握把");
        translations.put("item.create.wand_of_symmetry", "对称魔杖");
        translations.put("item.create.potato_cannon", "马铃薯炮");
        
        // ===== 机械动力 - 机械零件 =====
        translations.put("item.create.cogwheel", "齿轮");
        translations.put("item.create.large_cogwheel", "大齿轮");
        translations.put("item.create.shaft", "传动轴");
        translations.put("item.create.propeller", "螺旋桨");
        translations.put("item.create.whisk", "搅拌器");
        translations.put("item.create.electron_tube", "电子管");
        translations.put("item.create.precision_mechanism", "精密机械构件");
        
        // ===== 机械动力 - 方块/机器 =====
        translations.put("block.create.andesite_casing", "安山岩机壳");
        translations.put("block.create.brass_casing", "黄铜机壳");
        translations.put("block.create.copper_casing", "铜制机壳");
        translations.put("block.create.railway_casing", "铁路机壳");
        translations.put("block.create.mechanical_press", "动力辊压机");
        translations.put("block.create.mechanical_mixer", "动力搅拌器");
        translations.put("block.create.mechanical_saw", "动力锯");
        translations.put("block.create.mechanical_drill", "动力钻头");
        translations.put("block.create.mechanical_harvester", "动力收割机");
        translations.put("block.create.mechanical_plough", "动力犁");
        translations.put("block.create.mechanical_bearing", "动力轴承");
        translations.put("block.create.mechanical_piston", "动力活塞");
        translations.put("block.create.sticky_mechanical_piston", "黏性动力活塞");
        translations.put("block.create.mechanical_crafter", "动力合成器");
        translations.put("block.create.mechanical_arm", "动力机械臂");
        translations.put("block.create.mechanical_pump", "动力泵");
        translations.put("block.create.millstone", "石磨");
        translations.put("block.create.crushing_wheel", "粉碎轮");
        translations.put("block.create.encased_fan", "鼓风机");
        translations.put("block.create.deployer", "机械手");
        translations.put("block.create.basin", "工作盆");
        translations.put("block.create.blaze_burner", "烈焰人燃烧室");
        translations.put("block.create.item_drain", "分液池");
        translations.put("block.create.spout", "注液器");
        translations.put("block.create.chute", "溜槽");
        translations.put("block.create.smart_chute", "智能溜槽");
        translations.put("block.create.depot", "置物台");
        translations.put("block.create.weighted_ejector", "加重弹射器");
        translations.put("block.create.funnel", "漏斗");
        translations.put("block.create.andesite_funnel", "安山岩漏斗");
        translations.put("block.create.brass_funnel", "黄铜漏斗");
        translations.put("block.create.belt", "传送带");
        translations.put("block.create.gearbox", "齿轮箱");
        translations.put("block.create.clutch", "离合器");
        translations.put("block.create.gearshift", "换挡器");
        translations.put("block.create.encased_chain_drive", "链式传动箱");
        translations.put("block.create.adjustable_chain_gearshift", "可调节链式换挡器");
        translations.put("block.create.sequenced_gearshift", "时序换挡器");
        translations.put("block.create.rotation_speed_controller", "转速控制器");
        translations.put("block.create.creative_motor", "创造动力源");
        translations.put("block.create.water_wheel", "水车");
        translations.put("block.create.large_water_wheel", "大型水车");
        translations.put("block.create.windmill_bearing", "风车轴承");
        translations.put("block.create.hand_crank", "手摇曲柄");
        translations.put("block.create.steam_engine", "蒸汽引擎");
        translations.put("block.create.flywheel", "飞轮");
        translations.put("block.create.fluid_tank", "流体储罐");
        translations.put("block.create.item_vault", "物品保险库");
        translations.put("block.create.creative_fluid_tank", "创造流体储罐");
        translations.put("block.create.fluid_pipe", "液管");
        translations.put("block.create.copper_valve_handle", "铜制阀门把手");
        translations.put("block.create.smart_fluid_pipe", "智能液管");
        translations.put("block.create.portable_storage_interface", "便携式存储接口");
        translations.put("block.create.portable_fluid_interface", "便携式流体接口");
        translations.put("block.create.redstone_contact", "红石触点");
        translations.put("block.create.redstone_link", "红石链接器");
        translations.put("block.create.nixie_tube", "辉光管");
        translations.put("block.create.stockpile_switch", "存量检测器");
        translations.put("block.create.content_observer", "内容观察器");
        translations.put("block.create.display_board", "显示板");
        translations.put("block.create.display_link", "显示链接器");
        translations.put("block.create.track", "列车轨道");
        translations.put("block.create.track_station", "列车站台");
        translations.put("block.create.track_signal", "列车信号机");
        translations.put("block.create.track_observer", "列车观察器");
        translations.put("block.create.controls", "列车控制器");
        
        // ===== 机械动力 - 矿石方块 =====
        translations.put("block.create.zinc_ore", "锌矿石");
        translations.put("block.create.deepslate_zinc_ore", "深层锌矿石");
        translations.put("block.create.raw_zinc_block", "粗锌块");
        translations.put("block.create.zinc_block", "锌块");
        translations.put("block.create.brass_block", "黄铜块");
        translations.put("block.create.industrial_iron_block", "工业铁块");
        
        // ===== 机械动力 - 列车相关 =====
        translations.put("item.create.schedule", "列车时刻表");
        translations.put("item.create.empty_blaze_burner", "空白的烈焰人燃烧室");
        translations.put("block.create.train_door", "列车门");
        translations.put("block.create.train_trapdoor", "列车活板门");
        
        // ===== 机械动力 - 其他 =====
        translations.put("item.create.filter", "过滤器");
        translations.put("item.create.attribute_filter", "属性过滤器");
        translations.put("item.create.super_glue", "强力胶");
        translations.put("item.create.minecart_coupling", "矿车连接器");
        translations.put("item.create.blaze_cake", "烈焰蛋糕");
        translations.put("item.create.sweet_roll", "甜面包卷");
        translations.put("item.create.honeyed_apple", "蜜渍苹果");
        translations.put("item.create.builders_tea", "建筑工之茶");
        translations.put("item.create.chromatic_compound", "异彩化合物");
        translations.put("item.create.shadow_steel", "暗影钢");
        translations.put("item.create.refined_radiance", "精炼光芒");
        
        LOGGER.info("加载了 {} 条内置翻译", translations.size());
    }
    
    /**
     * 翻译 translation key 为中文
     * 
     * @param key 翻译键 (如 item.minecraft.diamond_sword)
     * @return 中文翻译，找不到则返回原 key
     */
    public String translate(String key) {
        if (!loaded) {
            init();
        }
        return translations.getOrDefault(key, key);
    }
    
    /**
     * 翻译 translation key 为中文，带默认值
     * 
     * @param key 翻译键
     * @param fallback 找不到时的默认值
     * @return 中文翻译或默认值
     */
    public String translate(String key, String fallback) {
        if (!loaded) {
            init();
        }
        return translations.getOrDefault(key, fallback);
    }
    
    /**
     * 检查是否有指定 key 的翻译
     */
    public boolean hasTranslation(String key) {
        if (!loaded) {
            init();
        }
        return translations.containsKey(key);
    }
}
