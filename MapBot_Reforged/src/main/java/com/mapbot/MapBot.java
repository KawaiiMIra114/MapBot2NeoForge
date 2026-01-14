/*
 * MapBot Reforged - Global AI Governance Protocols
 * 
 * 遵从 .ai_rules.md 中定义的治理规则。
 * 该文件属于 ./MapBot_Reforged/ 活动工作区，允许修改。
 * 
 * All interactions and logic must comply with the strict separation of concerns:
 * NeoForge (Game Logic) <-> WebSocket <-> Bot Logic.
 */

package com.mapbot;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(MapBot.MODID)
public class MapBot {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "mapbot";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the entry point.
    public MapBot(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("MapBot Reforged 初始化开始...");

        // Register the commonSetup method for modloading
        // modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        // NeoForge.EVENT_BUS.register(this);

        LOGGER.info("MapBot Reforged 初始化完成 (Constructed).");
    }
}
