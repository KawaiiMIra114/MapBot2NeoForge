/*
 * MapBot Reforged - 游戏事件监听器
 * 
 * 遵从 .ai_rules.md 中定义的治理规则。
 * 捕获 Minecraft 游戏事件并转发到 QQ 群。
 * 
 * Task #011: 实现 Game-to-QQ 事件桥接
 * - 聊天同步
 * - 加入/退出通知
 * - 死亡消息
 */

package com.mapbot.logic;

import com.mapbot.MapBot;
import com.mapbot.config.BotConfig;
import com.mapbot.data.PlaytimeManager;
import com.mapbot.network.BotClient;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mapbot.data.DataManager;

/**
 * 游戏事件监听器
 * 将服务器事件转发到 QQ 群
 */
@EventBusSubscriber(modid = MapBot.MODID, bus = EventBusSubscriber.Bus.GAME)
public class GameEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/Event");
    
    /** Minecraft 颜色代码正则: §0-9, §a-f, §k-o, §r */
    private static final String COLOR_CODE_REGEX = "(?i)§[0-9a-fk-or]";
    
    /** 玩家名提及正则: @名字 (空格) 消息 */
    private static final java.util.regex.Pattern AT_PATTERN = java.util.regex.Pattern.compile("^@([a-zA-Z0-9_]{3,16})(\\s.*)?$");
    
    /** Tick 开始时间戳 (纳秒) */
    private static long lastTickStart = 0L;

    // ================== Tick 监控 ==================

    /**
     * 服务器 Tick 开始事件
     * 记录开始时间
     */
    @SubscribeEvent
    public static void onServerTickPre(ServerTickEvent.Pre event) {
        lastTickStart = System.nanoTime();
    }

    /**
     * 服务器 Tick 结束事件
     * 计算本次 Tick 耗时并记录
     */
    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        if (lastTickStart == 0L) {
            return;
        }
        long duration = System.nanoTime() - lastTickStart;
        ServerStatusManager.recordTick(duration);
    }
    
    // ================== Task #016 STEP1: 服务器生命周期事件 ==================
    
    /**
     * 服务器启动完成事件
     * 启动 TPS 监控器
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("服务器启动完成，启动 TPS 监控器");
        ServerStatusManager.startTPSMonitor();
    }
    
    /**
     * 服务器停止事件
     * 停止 TPS 监控器
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("服务器正在停止，关闭 TPS 监控器");
        ServerStatusManager.stopTPSMonitor();
        
        // Task #016-STEP2: 保存所有在线玩家的时长数据
        PlaytimeManager.INSTANCE.onServerStopping();
    }

    // ================== 消息同步 ==================

    /**
     * 聊天消息同步
     * 当玩家在游戏内发送消息时，转发到 QQ 群
     */
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        long groupId = BotConfig.getTargetGroupId();
        if (groupId == 0L) {
            return;
        }
        
        String playerName = event.getPlayer().getName().getString();
        // 清理颜色代码
        String message = event.getRawText().replaceAll(COLOR_CODE_REGEX, "");
        
        // Task #017-STEP2: 处理 @提及
        // 检查消息是否以 @开头
        java.util.regex.Matcher matcher = AT_PATTERN.matcher(message);
        if (matcher.matches()) {
            String targetName = matcher.group(1);
            String content = matcher.group(2) != null ? matcher.group(2) : "";
            
            // 查找该名字对应的 UUID (需要 Server 实例)
            net.minecraft.server.MinecraftServer server = event.getPlayer().getServer();
            if (server != null) {
                // 尝试从缓存查找玩家 UUID
                java.util.Optional<com.mojang.authlib.GameProfile> profile = server.getProfileCache().get(targetName);
                
                if (profile.isPresent()) {
                    String uuid = profile.get().getId().toString();
                    long targetQQ = DataManager.INSTANCE.getQQByUUID(uuid);
                    
                    if (targetQQ != -1L && targetQQ != 0L) {
                        // 构建 CQ 码消息
                        message = String.format("[CQ:at,qq=%d]%s", targetQQ, content);
                        LOGGER.debug("解析到 @提及: {} -> QQ {}", targetName, targetQQ);
                    }
                }
            }
        }
        
        // 格式: [玩家名] 消息内容
        String formattedMessage = String.format("[%s] %s", playerName, message);
        
        LOGGER.debug("转发聊天消息: {}", formattedMessage);
        BotClient.INSTANCE.sendGroupMessage(groupId, formattedMessage);
    }

    /**
     * 玩家加入服务器同步
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        long groupId = BotConfig.getTargetGroupId();
        if (groupId == 0L) {
            return;
        }
        
        // 确保是服务端玩家
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        String playerName = player.getName().getString();
        
        // 格式: [+] 玩家名 加入了服务器
        String formattedMessage = String.format("[+] %s 加入了服务器", playerName);
        
        LOGGER.info("玩家加入: {}", playerName);
        BotClient.INSTANCE.sendGroupMessage(groupId, formattedMessage);
        
        // Task #016-STEP2: 记录登录时间
        PlaytimeManager.INSTANCE.onPlayerLogin(player.getUUID());
    }

    /**
     * 玩家离开服务器同步
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        long groupId = BotConfig.getTargetGroupId();
        if (groupId == 0L) {
            return;
        }
        
        // 确保是服务端玩家
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        String playerName = player.getName().getString();
        
        // 格式: [-] 玩家名 离开了服务器
        String formattedMessage = String.format("[-] %s 离开了服务器", playerName);
        
        LOGGER.info("玩家离开: {}", playerName);
        BotClient.INSTANCE.sendGroupMessage(groupId, formattedMessage);
        
        // Task #016-STEP2: 计算并保存在线时长
        PlaytimeManager.INSTANCE.onPlayerLogout(player.getUUID());
    }

    /**
     * 玩家死亡消息同步
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        long groupId = BotConfig.getTargetGroupId();
        if (groupId == 0L) {
            return;
        }
        
        LivingEntity entity = event.getEntity();
        
        // 只处理玩家死亡
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        
        // 获取原版死亡消息
        String deathMessage = event.getSource()
                .getLocalizedDeathMessage(player)
                .getString();
        
        // 格式: [☠️] 死亡消息
        String formattedMessage = String.format("[☠️] %s", deathMessage);
        
        LOGGER.info("玩家死亡: {}", deathMessage);
        BotClient.INSTANCE.sendGroupMessage(groupId, formattedMessage);
    }
}
