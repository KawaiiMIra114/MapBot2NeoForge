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
import com.mapbot.logic.ServerStatusManager;
import com.mapbot.network.BotClient;
import com.mapbot.network.BridgeClient;
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
import java.util.concurrent.CompletableFuture;

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
     * 注册游戏内命令 (Task #022: CDK 验证走 Alpha Redis)
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
                            
                            // 向 Alpha 发送 CDK 验证请求
                            String result = BridgeClient.INSTANCE.redeemCdk(code, uuidStr);
                            
                            if (result == null) {
                                ctx.getSource().sendFailure(Component.literal("§c[MapBot] 服务器无响应"));
                                return 0;
                            }
                            
                            if (result.startsWith("VALID:")) {
                                // 解析物品并发放
                                String itemJson = result.substring(6);
                                boolean success = giveItemToPlayer(player, itemJson);
                                if (success) {
                                    ctx.getSource().sendSuccess(() -> Component.literal("§a[MapBot] 兑换成功！物品已发放"), false);
                                } else {
                                    ctx.getSource().sendFailure(Component.literal("§c[MapBot] 发放物品失败"));
                                }
                            } else {
                                String error = result.replace("INVALID:", "");
                                ctx.getSource().sendFailure(Component.literal("§c[MapBot] " + error));
                            }
                            return 1;
                        })
                    )
                )
        );

        event.getDispatcher().register(
            Commands.literal("server")
                .then(Commands.argument("server_name", StringArgumentType.word())
                    .executes(ctx -> {
                        String targetServer = StringArgumentType.getString(ctx, "server_name");
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        var source = ctx.getSource();
                        var server = source.getServer();
                        String playerName = player.getName().getString();
                        String playerUuid = player.getUUID().toString();

                        source.sendSuccess(() -> Component.literal("§e[MapBot] 正在请求切换到服务器: " + targetServer), false);
                        CompletableFuture
                            .supplyAsync(() -> BridgeClient.INSTANCE.requestServerSwitch(targetServer, playerName, playerUuid))
                            .whenComplete((result, throwable) -> {
                                if (server == null) {
                                    return;
                                }
                                server.execute(() -> {
                                    if (throwable != null) {
                                        source.sendFailure(Component.literal("§c[MapBot] 切服请求失败: " + throwable.getMessage()));
                                        return;
                                    }
                                    if (result != null && result.startsWith("SUCCESS")) {
                                        String msg = result.contains(":") ? result.substring(result.indexOf(':') + 1) : "已发送切服请求";
                                        source.sendSuccess(() -> Component.literal("§a[MapBot] " + msg), false);
                                        return;
                                    }
                                    String error = (result == null || result.isBlank()) ? "未知错误" : result.replace("FAIL:", "");
                                    source.sendFailure(Component.literal("§c[MapBot] " + error));
                                });
                            });
                        return 1;
                    })
                )
        );

        // /q <message> - 转发消息到 QQ 群
        event.getDispatcher().register(
            Commands.literal("q")
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        String message = StringArgumentType.getString(ctx, "message");
                        String playerName = player.getName().getString();
                        String uuid = player.getUUID().toString();

                        // 禁言检查
                        long expiry = BridgeClient.INSTANCE.checkMuteExpiry(uuid);
                        if (expiry != 0L) {
                            if (expiry == -1L || System.currentTimeMillis() <= expiry) {
                                String timeStr = (expiry == -1L) ? "永久" :
                                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(expiry));
                                ctx.getSource().sendFailure(Component.literal("§c你已被禁言！解除时间: " + timeStr));
                                return 0;
                            }
                        }

                        // 处理 @提及
                        String finalMessage = message;
                        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("^@(\\S+)\\s*(.*)$").matcher(message);
                        if (matcher.matches()) {
                            String targetName = matcher.group(1);
                            String content = matcher.group(2) != null ? matcher.group(2) : "";
                            net.minecraft.server.MinecraftServer server = ctx.getSource().getServer();
                            if (server != null) {
                                java.util.Optional<com.mojang.authlib.GameProfile> profile = server.getProfileCache().get(targetName);
                                if (profile.isPresent()) {
                                    String targetUuid = profile.get().getId().toString();
                                    long targetQQ = BridgeClient.INSTANCE.getQQByUUID(targetUuid);
                                    if (targetQQ != -1L && targetQQ != 0L) {
                                        finalMessage = String.format("[CQ:at,qq=%d]%s", targetQQ, content);
                                    }
                                }
                            }
                        }

                        // 转发到 QQ
                        BridgeClient.INSTANCE.sendChat(playerName, finalMessage);
                        ctx.getSource().sendSuccess(() -> Component.literal("§a[QQ] 消息已发送"), false);
                        return 1;
                    })
                )
        );

        LOGGER.info("已注册游戏命令: /mapbot cdk, /server <server_name>, /q <message>");
    }
    
    /**
     * 发放物品给玩家 (Task #022)
     */
    private boolean giveItemToPlayer(ServerPlayer player, String itemJson) {
        try {
            var json = com.google.gson.JsonParser.parseString(itemJson).getAsJsonObject();
            String itemId = json.get("id").getAsString();
            int count = json.get("count").getAsInt();
            
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.parse(itemId);
            net.minecraft.world.item.Item mcItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
            
            if (mcItem == null || mcItem == net.minecraft.world.item.Items.AIR) {
                return false;
            }
            
            net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(mcItem, count);
            
            if (player.getInventory().add(stack)) {
                return true;
            } else {
                player.drop(stack, false);
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("发放物品失败", e);
            return false;
        }
    }

    /**
     * 服务器启动中事件
     * 初始化数据和连接
     */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("服务器正在启动，准备连接到 NapCat...");

        // 初始化奖池配置
        com.mapbot.data.loot.LootConfig.INSTANCE.init();
        LOGGER.info("奖池配置已初始化");

        // 初始化签到缓存 (R1: 持久化支持)
        SignManager.INSTANCE.init();
        LOGGER.info("签到缓存已初始化");

        long groupId = BotConfig.getTargetGroupId();
        if (groupId == 0L) {
            LOGGER.warn("目标群号未配置 (targetGroupId = 0)，消息同步功能已禁用。");
            LOGGER.warn("请编辑 config/mapbot-common.toml 设置正确的群号。");
        } else {
            LOGGER.info("目标群号: {}", groupId);
        }

        // Alpha 中枢模式下，QQ 指令统一由 Alpha 处理，Reforged 不再直连 OneBot
        if (isStandaloneOneBotMode()) {
            BotClient.INSTANCE.connect();
        } else {
            LOGGER.info("检测到 Alpha 中枢模式，已禁用 Reforged 直连 OneBot（避免重复处理 QQ 指令）");
        }

        // 启动 Alpha Core Bridge 连接 (STEP 11/12 - 中枢模式)
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
                if (isStandaloneOneBotMode() && playerGroupId > 0 && BotClient.INSTANCE.isConnected()) {
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
        if (isStandaloneOneBotMode() && playerGroupId > 0 && BotClient.INSTANCE.isConnected()) {
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

    private boolean isStandaloneOneBotMode() {
        String alphaHost = BotConfig.getAlphaHost();
        return alphaHost == null || alphaHost.trim().isEmpty();
    }
}
