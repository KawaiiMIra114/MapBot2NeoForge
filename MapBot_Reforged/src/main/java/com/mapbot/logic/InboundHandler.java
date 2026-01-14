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
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
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

        // 提取发送者信息
        JsonObject sender = json.getAsJsonObject("sender");
        if (sender == null) {
            return;
        }

        String nickname = getStringOrNull(sender, "nickname");
        if (nickname == null) {
            nickname = "未知用户";
        }

        // 提取消息内容
        String message = getStringOrNull(json, "raw_message");
        if (message == null || message.isEmpty()) {
            return;
        }

        // 格式化消息
        String formattedMessage = String.format("[QQ] <%s> %s", nickname, message);

        LOGGER.info("收到群消息: {} -> {}", nickname, message);

        // 关键: 跨线程安全广播
        // WebSocket 回调在独立线程中，必须调度到服务器主线程
        broadcastToServer(formattedMessage);
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
}
