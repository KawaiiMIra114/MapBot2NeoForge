package com.mapbot.alpha.network;

import com.mapbot.alpha.bridge.ServerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * OneBot WebSocket 客户端 (Alpha 核心版)
 * STEP 12: 统一 QQ 消息接入，分发到各 MC 服务器
 */
public class OneBotClient implements WebSocket.Listener {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/BotClient");
    public static final OneBotClient INSTANCE = new OneBotClient();

    private WebSocket webSocket;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private String serverUrl;
    
    // 目标群号 (从配置读取)
    private long targetGroupId = 0;

    public void connect(String url) {
        this.serverUrl = url;
        LOGGER.info("正在连接 OneBot: {}", url);
        
        httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(url), this)
                .whenComplete((ws, ex) -> {
                    if (ex != null) {
                        LOGGER.error("OneBot 连接失败: {}", ex.getMessage());
                        scheduleReconnect();
                    } else {
                        this.webSocket = ws;
                        LOGGER.info("OneBot 连接成功");
                    }
                });
    }
    
    public void setTargetGroupId(long groupId) {
        this.targetGroupId = groupId;
    }

    private void scheduleReconnect() {
        scheduler.schedule(() -> connect(serverUrl), 5, TimeUnit.SECONDS);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        String msg = data.toString();
        
        try {
            // 解析 OneBot 消息
            String postType = extractJsonValue(msg, "post_type");
            
            if ("message".equals(postType)) {
                String msgType = extractJsonValue(msg, "message_type");
                
                if ("group".equals(msgType)) {
                    handleGroupMessage(msg);
                }
            }
        } catch (Exception e) {
            LOGGER.error("处理 OneBot 消息异常", e);
        }
        
        webSocket.request(1);
        return null;
    }
    
    /**
     * 处理群消息 - 分发到所有 MC 服务器
     */
    private void handleGroupMessage(String msg) {
        // 提取群号
        String groupIdStr = extractJsonValue(msg, "group_id");
        if (groupIdStr.isEmpty()) return;
        
        long groupId;
        try {
            groupId = Long.parseLong(groupIdStr);
        } catch (NumberFormatException e) {
            return;
        }
        
        // 只处理目标群
        if (targetGroupId > 0 && groupId != targetGroupId) {
            return;
        }
        
        // 提取消息内容
        String rawMessage = extractJsonValue(msg, "raw_message");
        String senderNickname = extractNestedValue(msg, "sender", "nickname");
        
        if (rawMessage.isEmpty()) return;
        
        LOGGER.info("[QQ->MC] {}: {}", senderNickname, rawMessage);
        
        // 分发到所有连接的 MC 服务器
        String dispatchMsg = String.format(
            "{\"type\":\"qq_message\",\"sender\":\"%s\",\"content\":\"%s\",\"groupId\":%d}",
            escapeJson(senderNickname), escapeJson(rawMessage), groupId
        );
        
        ServerRegistry.INSTANCE.broadcast(dispatchMsg + "\n");
    }
    
    /**
     * 发送群消息 (由 Bridge 调用)
     */
    public void sendGroupMessage(long groupId, String message) {
        if (webSocket == null) {
            LOGGER.warn("无法发送群消息: OneBot 未连接");
            return;
        }
        
        String json = String.format(
            "{\"action\":\"send_group_msg\",\"params\":{\"group_id\":%d,\"message\":\"%s\"}}",
            groupId, escapeJson(message)
        );
        
        webSocket.sendText(json, true);
        LOGGER.debug("[MC->QQ] 发送到群 {}: {}", groupId, message);
    }
    
    /**
     * 发送群消息 (使用默认群号)
     */
    public void sendGroupMessage(String message) {
        if (targetGroupId > 0) {
            sendGroupMessage(targetGroupId, message);
        }
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        LOGGER.warn("OneBot 连接关闭: {} - {}", statusCode, reason);
        scheduleReconnect();
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        LOGGER.error("OneBot 错误: {}", error.getMessage());
        scheduleReconnect();
    }
    
    public boolean isConnected() {
        return webSocket != null;
    }
    
    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) {
            // 尝试非字符串值
            search = "\"" + key + "\":";
            start = json.indexOf(search);
            if (start == -1) return "";
            start += search.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            if (end == -1) return "";
            return json.substring(start, end).trim();
        }
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }
    
    private String extractNestedValue(String json, String parent, String key) {
        String parentSearch = "\"" + parent + "\":";
        int parentStart = json.indexOf(parentSearch);
        if (parentStart == -1) return "";
        
        int braceStart = json.indexOf("{", parentStart);
        if (braceStart == -1) return "";
        
        // 找到嵌套对象的结束位置
        int depth = 1;
        int braceEnd = braceStart + 1;
        while (depth > 0 && braceEnd < json.length()) {
            char c = json.charAt(braceEnd);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            braceEnd++;
        }
        
        String nested = json.substring(braceStart, braceEnd);
        return extractJsonValue(nested, key);
    }
    
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
