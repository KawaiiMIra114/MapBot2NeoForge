package com.mapbot.alpha.network;

import com.mapbot.alpha.logic.InboundHandler;
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
 * OneBot WebSocket 客户端 (Alpha Core)
 * 完整版 - 集成 InboundHandler 处理消息
 */
public class OneBotClient implements WebSocket.Listener {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/BotClient");
    public static final OneBotClient INSTANCE = new OneBotClient();

    private WebSocket webSocket;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private String serverUrl;
    private StringBuilder messageBuffer = new StringBuilder();

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

    private void scheduleReconnect() {
        scheduler.schedule(() -> connect(serverUrl), 5, TimeUnit.SECONDS);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        messageBuffer.append(data);
        
        if (last) {
            String msg = messageBuffer.toString();
            messageBuffer = new StringBuilder();
            
            // 交给 InboundHandler 处理
            try {
                InboundHandler.handleMessage(msg);
            } catch (Exception e) {
                LOGGER.error("处理消息异常", e);
            }
        }
        
        webSocket.request(1);
        return null;
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        LOGGER.info("OneBot WebSocket 已打开");
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
    
    /**
     * 发送群消息
     */
    public void sendGroupMessage(long groupId, String message) {
        if (webSocket == null) {
            LOGGER.warn("无法发送消息: OneBot 未连接");
            return;
        }
        
        String json = String.format(
            "{\"action\":\"send_group_msg\",\"params\":{\"group_id\":%d,\"message\":\"%s\"}}",
            groupId, escapeJson(message)
        );
        
        webSocket.sendText(json, true);
        LOGGER.debug("[发送到QQ群 {}] {}", groupId, message);
    }
    
    /**
     * 发送私聊消息
     */
    public void sendPrivateMessage(long userId, String message) {
        if (webSocket == null) return;
        
        String json = String.format(
            "{\"action\":\"send_private_msg\",\"params\":{\"user_id\":%d,\"message\":\"%s\"}}",
            userId, escapeJson(message)
        );
        
        webSocket.sendText(json, true);
    }
    
    /**
     * 发送原始数据包
     */
    public void sendPacket(String json) {
        if (webSocket == null) return;
        webSocket.sendText(json, true);
    }
    
    public boolean isConnected() {
        return webSocket != null;
    }
    
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
