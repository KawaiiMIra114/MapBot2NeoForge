/*
 * MapBot Reforged - 游戏事件监听器
 * 
 * 遵从 .ai_rules.md 中定义的治理规则。
 * 负责捕获 Minecraft 服务端事件并转发到 QQ 群。
 * 
 * 参考: ./Project_Docs/Architecture/System_Design.md
 */

package com.mapbot.logic;

import com.google.gson.JsonObject;
import com.mapbot.MapBot;
import com.mapbot.network.BotClient;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 游戏事件监听器
 * 监听 Minecraft 服务端事件并通过 BotClient 发送到 QQ 群
 */
@EventBusSubscriber(modid = MapBot.MODID)
public class GameEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/Event");

    // TODO: 应从配置文件读取
    private static final long TARGET_GROUP_ID = 123456789L;

    /**
     * 监听玩家聊天事件
     * 忽略以 / 开头的命令消息
     */
    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        String message = event.getMessage().getString();
        ServerPlayer player = event.getPlayer();

        // 忽略命令消息，避免泄露
        if (message.startsWith("/")) {
            return;
        }

        String playerName = player.getName().getString();
        String formattedMessage = String.format("[服务器] <%s> %s", playerName, message);

        LOGGER.debug("捕获聊天: {} -> {}", playerName, message);

        // 构建 OneBot v11 格式的 JSON
        JsonObject params = new JsonObject();
        params.addProperty("group_id", TARGET_GROUP_ID);
        params.addProperty("message", formattedMessage);

        JsonObject packet = new JsonObject();
        packet.addProperty("action", "send_group_msg");
        packet.add("params", params);
        packet.addProperty("echo", "chat_" + System.currentTimeMillis());

        BotClient.INSTANCE.sendPacket(packet);
    }

    /**
     * 监听玩家登录事件
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        String playerName = player.getName().getString();
        String formattedMessage = String.format("🟢 玩家 %s 加入了服务器", playerName);

        LOGGER.info("玩家登录: {}", playerName);

        JsonObject params = new JsonObject();
        params.addProperty("group_id", TARGET_GROUP_ID);
        params.addProperty("message", formattedMessage);

        JsonObject packet = new JsonObject();
        packet.addProperty("action", "send_group_msg");
        packet.add("params", params);
        packet.addProperty("echo", "join_" + System.currentTimeMillis());

        BotClient.INSTANCE.sendPacket(packet);
    }

    /**
     * 监听玩家登出事件
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        String playerName = player.getName().getString();
        String formattedMessage = String.format("🔴 玩家 %s 离开了服务器", playerName);

        LOGGER.info("玩家登出: {}", playerName);

        JsonObject params = new JsonObject();
        params.addProperty("group_id", TARGET_GROUP_ID);
        params.addProperty("message", formattedMessage);

        JsonObject packet = new JsonObject();
        packet.addProperty("action", "send_group_msg");
        packet.add("params", params);
        packet.addProperty("echo", "leave_" + System.currentTimeMillis());

        BotClient.INSTANCE.sendPacket(packet);
    }
}
