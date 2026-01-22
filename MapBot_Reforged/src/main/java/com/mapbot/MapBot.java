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

import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import com.mapbot.logic.SignManager;

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
     * 注册游戏内命令
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("mapbot")
                .then(Commands.literal("cdk")
                    .then(Commands.argument("code", StringArgumentType.string())
                        .executes(ctx -> {
                            String code = StringArgumentType.getString(ctx, "code");
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String uuidStr = player.getUUID().toString();
                            
                            // 执行兑换
                            String result = SignManager.INSTANCE.redeemCdk(uuidStr, code);
                            
                            // 反馈结果
                            if (result.startsWith("兑换成功")) {
                                ctx.getSource().sendSuccess(() -> Component.literal("§a[MapBot] " + result), false);
                            } else {
                                ctx.getSource().sendFailure(Component.literal("§c[MapBot] " + result));
                            }
                            return 1;
                        })
                    )
                )
        );
        LOGGER.info("已注册游戏命令: /mapbot cdk");
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
        
        // 初始化奖池配置
        com.mapbot.data.loot.LootConfig.INSTANCE.init();
        LOGGER.info("奖池配置已初始化");

        long groupId = BotConfig.getTargetGroupId();
        if (groupId == 0L) {
            LOGGER.warn("目标群号未配置 (targetGroupId = 0)，消息同步功能已禁用。");
            LOGGER.warn("请编辑 config/mapbot-common.toml 设置正确的群号。");
        } else {
            LOGGER.info("目标群号: {}", groupId);
        }

        // 启动 Alpha Core Bridge 连接 (STEP 11/12 - 中枢模式)
        // OneBot 连接由 Alpha Core 统一管理，此处仅启动 Bridge 客户端
        // 旧: BotClient.INSTANCE.connect(); // 已禁用，改为中枢模式
        com.mapbot.network.BridgeClient.INSTANCE.connect();
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
        
        // 断开 Alpha Core Bridge 连接 (STEP 11)
        com.mapbot.network.BridgeClient.INSTANCE.disconnect();
        
        BotClient.INSTANCE.disconnect();
    }
}
