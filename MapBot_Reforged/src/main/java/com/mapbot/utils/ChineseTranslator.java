/*
 * MapBot Reforged - 中文翻译查找器
 * 
 * 遵从 .ai_rules.md 中定义的治理规则。
 * 从 Minecraft 的语言文件或内置翻译表加载中文翻译。
 * 
 * 用于将物品名、附魔名等翻译为中文显示。
 * 
 * R3 重构: 硬编码翻译表已提取到 assets/mapbot/lang/mapbot_zh_cn.json
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
 * 优先从 Minecraft 资源包加载，回退到内置 JSON 翻译表
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
        
        // 优先尝试从 classpath 加载 Minecraft 原版语言文件
        String[] mcLangPaths = {
            "/assets/minecraft/lang/zh_cn.json",
            "assets/minecraft/lang/zh_cn.json"
        };
        
        for (String path : mcLangPaths) {
            if (loadJsonFromResource(path)) {
                LOGGER.info("成功加载 Minecraft 中文语言文件: {}", path);
                break;
            }
        }
        
        // 加载 MapBot 内置翻译表 (补充 Minecraft 未覆盖的和 Create 模组翻译)
        String[] mapbotLangPaths = {
            "/assets/mapbot/lang/mapbot_zh_cn.json",
            "assets/mapbot/lang/mapbot_zh_cn.json"
        };
        
        for (String path : mapbotLangPaths) {
            if (loadJsonFromResource(path)) {
                LOGGER.info("成功加载 MapBot 内置翻译表: {}", path);
                break;
            }
        }
        
        loaded = true;
        LOGGER.info("翻译系统已加载, 共 {} 条翻译", translations.size());
    }
    
    /**
     * 从 classpath 资源加载 JSON 翻译文件
     * @return 是否成功加载
     */
    private boolean loadJsonFromResource(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) return false;
            
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> langData = gson.fromJson(reader, type);
                
                if (langData != null) {
                    translations.putAll(langData);
                    LOGGER.debug("从 {} 加载了 {} 条翻译", path, langData.size());
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("加载语言文件失败: {} - {}", path, e.getMessage());
        }
        return false;
    }
    
    /**
     * 翻译 translation key 为中文
     */
    public String translate(String key) {
        if (!loaded) init();
        return translations.getOrDefault(key, key);
    }
    
    /**
     * 翻译 translation key 为中文，带默认值
     */
    public String translate(String key, String fallback) {
        if (!loaded) init();
        return translations.getOrDefault(key, fallback);
    }
    
    /**
     * 检查是否有指定 key 的翻译
     */
    public boolean hasTranslation(String key) {
        if (!loaded) init();
        return translations.containsKey(key);
    }
}
