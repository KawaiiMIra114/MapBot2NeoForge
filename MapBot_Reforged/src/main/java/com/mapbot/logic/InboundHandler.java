/*
 * MapBot Reforged - 入站消息处理器
 * 
 * 遵从 .ai_rules.md 中定义的治理规则。
 * 负责解析来自 NapCat 的 WebSocket 消息并调度到游戏主线程执行。
 * 
 * 参考: ./Project_Docs/Architecture/Protocol_Spec.md
 * 关键: 使用 ServerLifecycleHooks 实现跨线程安全调用。
 */

package com.mapbot.logic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mapbot.config.BotConfig;
import com.mapbot.network.BotClient;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 入站消息处理器
 * 解析来自 QQ 的 JSON 消息，并在游戏主线程中广播
 */
public class InboundHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/Inbound");

    /**
     * 处理从 WebSocket 接收到的原始文本消息
     * 
     * @param rawJson 原始 JSON 字符串
     */
    public static void handleMessage(String rawJson) {
        try {
            JsonObject json = JsonParser.parseString(rawJson).getAsJsonObject();

            // 检查是否为消息类型
            String postType = getStringOrNull(json, "post_type");

            if ("message".equals(postType)) {
                handleGroupMessage(json);
            } else if ("meta_event".equals(postType)) {
                handleMetaEvent(json);
            } else {
                LOGGER.debug("忽略未知事件类型: {}", postType);
            }

        } catch (Exception e) {
            LOGGER.error("解析 JSON 失败: {}", e.getMessage());
        }
    }

    /**
     * 处理群消息事件
     */
    private static void handleGroupMessage(JsonObject json) {
        String messageType = getStringOrNull(json, "message_type");

        // 仅处理群消息
        if (!"group".equals(messageType)) {
            return;
        }
        
        // 安全检查: 验证消息来源群号
        long targetGroupId = BotConfig.getTargetGroupId();
        if (targetGroupId == 0L) {
            return; // 未配置群号，跳过
        }
        
        long sourceGroupId = getLongOrZero(json, "group_id");
        if (sourceGroupId != targetGroupId) {
            LOGGER.debug("忽略来自其他群的消息: {}", sourceGroupId);
            return;
        }

        // 提取消息内容
        String message = getStringOrNull(json, "raw_message");
        if (message == null || message.isEmpty()) {
            return;
        }

        // 提取发送者信息
        JsonObject sender = json.getAsJsonObject("sender");
        String nickname = "未知用户";
        if (sender != null) {
            String senderNick = getStringOrNull(sender, "nickname");
            if (senderNick != null) {
                nickname = senderNick;
            }
        }

        LOGGER.info("收到群消息: {} -> {}", nickname, message);

        // === 命令分发 ===
        if (message.startsWith("#inv ")) {
            // Task #007: 库存查询命令
            handleInventoryCommand(message);
        } else if (message.startsWith("#")) {
            // 其他命令，暂不处理
            LOGGER.debug("未知命令: {}", message);
        } else {
            // 普通消息，转发到游戏
            String formattedMessage = String.format("§b[QQ]§r <%s> %s", nickname, message);
            broadcastToServer(formattedMessage);
        }
    }

    /**
     * 处理 #inv <玩家名> 命令
     * Task #007 新增
     */
    private static void handleInventoryCommand(String message) {
        // 解析玩家名
        String targetPlayerName = message.substring(5).trim();
        
        if (targetPlayerName.isEmpty()) {
            sendReplyToQQ("❌ 用法: #inv <玩家名>");
            return;
        }
        
        LOGGER.info("收到库存查询请求: {}", targetPlayerName);
        
        // 关键: 线程调度
        // 必须在服务器主线程执行 getPlayerList() 操作
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        
        if (server == null) {
            sendReplyToQQ("❌ 服务器未就绪");
            return;
        }
        
        // 调度到服务器主线程
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayerByName(targetPlayerName);
            
            // 调用 InventoryManager 获取库存信息
            String result = InventoryManager.getPlayerInventory(player);
            
            // 发送结果回 QQ
            sendReplyToQQ(result);
        });
    }

    /**
     * 处理元事件 (心跳等)
     */
    private static void handleMetaEvent(JsonObject json) {
        String metaEventType = getStringOrNull(json, "meta_event_type");

        if ("heartbeat".equals(metaEventType)) {
            LOGGER.debug("收到心跳事件");
        } else if ("lifecycle".equals(metaEventType)) {
            String subType = getStringOrNull(json, "sub_type");
            LOGGER.info("生命周期事件: {}", subType);
        }
    }

    /**
     * 发送回复消息到 QQ 群
     */
    private static void sendReplyToQQ(String message) {
        long targetGroupId = BotConfig.getTargetGroupId();
        if (targetGroupId == 0L) {
            LOGGER.warn("无法发送回复: 目标群号未配置");
            return;
        }
        
        JsonObject params = new JsonObject();
        params.addProperty("group_id", targetGroupId);
        params.addProperty("message", message);
        
        JsonObject packet = new JsonObject();
        packet.addProperty("action", "send_group_msg");
        packet.add("params", params);
        packet.addProperty("echo", "reply_" + System.currentTimeMillis());
        
        BotClient.INSTANCE.sendPacket(packet);
    }

    /**
     * 安全地在服务器主线程广播消息
     * 
     * @param message 要广播的消息
     */
    private static void broadcastToServer(String message) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        if (server == null) {
            LOGGER.warn("服务器实例不可用，无法广播消息");
            return;
        }

        // 使用 execute() 将任务调度到服务器主线程
        server.execute(() -> {
            Component chatComponent = Component.literal(message);
            server.getPlayerList().broadcastSystemMessage(chatComponent, false);
            LOGGER.debug("消息已广播到服务器");
        });
    }

    /**
     * 安全地从 JsonObject 获取字符串值
     */
    private static String getStringOrNull(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element != null && !element.isJsonNull()) {
            return element.getAsString();
        }
        return null;
    }
    
    /**
     * 安全地从 JsonObject 获取 long 值
     */
    private static long getLongOrZero(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element != null && !element.isJsonNull()) {
            try {
                return element.getAsLong();
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }
}