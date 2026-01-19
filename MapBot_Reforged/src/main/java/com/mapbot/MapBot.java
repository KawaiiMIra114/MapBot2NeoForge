/*
 * MapBot Reforged - 模组主入口
 * 
 * 遵从 .ai_rules.md 中定义的治理规则。
 * 该文件属于 ./MapBot_Reforged/ 活动工作区，允许修改。
 * 
 * All interactions and logic must comply with the strict separation of concerns:
 * NeoForge (Game Logic) <-> WebSocket <-> Bot Logic.
 */

package com.mapbot;

import com.mapbot.config.BotConfig;
import com.mapbot.data.DataManager;
import com.mapbot.logic.ServerStatusManager;
import com.mapbot.network.BotClient;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

/**
 * MapBot Reforged 主模组类
 * 负责初始化配置系统和管理 WebSocket 连接生命周期
 */
@Mod(MapBot.MODID)
public class MapBot {
    // 模组 ID，所有引用的统一来源
    public static final String MODID = "mapbot";
    // SLF4J 日志记录器
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 模组构造函数 - 入口点
     */
    public MapBot(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("MapBot Reforged 初始化开始...");

        // 注册配置系统 (Type.COMMON: 服务端和客户端共享配置)
        modContainer.registerConfig(ModConfig.Type.COMMON, BotConfig.SPEC, "mapbot-common.toml");
        LOGGER.info("配置系统已注册: mapbot-common.toml");

        // 注册服务器生命周期事件监听器
        NeoForge.EVENT_BUS.register(this);

        LOGGER.info("MapBot Reforged 初始化完成 (Constructed).");
    }

    /**
     * 服务器启动中事件
     * 初始化数据和连接
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("服务器正在启动，准备连接到 NapCat...");

        // 初始化数据管理器
        DataManager.INSTANCE.init();
        LOGGER.info("数据管理器已初始化");

        long groupId = BotConfig.getTargetGroupId();
        if (groupId == 0L) {
            LOGGER.warn("目标群号未配置 (targetGroupId = 0)，消息同步功能已禁用。");
            LOGGER.warn("请编辑 config/mapbot-common.toml 设置正确的群号。");
        } else {
            LOGGER.info("目标群号: {}", groupId);
        }

        // 启动 WebSocket 连接
        BotClient.INSTANCE.connect();
    }
    
    /**
     * 服务器启动完成事件
     * 发送早安消息
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        // 启动 TPS 监控
        ServerStatusManager.startTPSMonitor();
        
        // 延迟 5 秒发送早安，确保 WebSocket 已连接
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                long playerGroupId = BotConfig.getPlayerGroupId();
                if (playerGroupId > 0 && BotClient.INSTANCE.isConnected()) {
                    BotClient.INSTANCE.sendGroupMessage(playerGroupId, "早安");
                    LOGGER.info("已发送早安消息");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * 服务器停止事件
     * 发送晚安消息并断开 WebSocket
     */
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("服务器正在停止...");
        
        // 停止 TPS 监控
        ServerStatusManager.stopTPSMonitor();
        
        // 发送晚安消息
        long playerGroupId = BotConfig.getPlayerGroupId();
        if (playerGroupId > 0 && BotClient.INSTANCE.isConnected()) {
            BotClient.INSTANCE.sendGroupMessage(playerGroupId, "晚安");
            LOGGER.info("已发送晚安消息");
            
            // 等待消息发送完成
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        BotClient.INSTANCE.disconnect();
    }
}
