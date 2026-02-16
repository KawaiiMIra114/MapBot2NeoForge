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
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final AtomicBoolean startupNoticeSent = new AtomicBoolean(false);

    public void connect(String url) {
        this.serverUrl = url;
        LOGGER.info("正在连接 OneBot: {}", url);
        
        var builder = httpClient.newWebSocketBuilder();
        
        // OneBot V11 鉴权: 若配置了 access_token, 通过 Authorization header 传递
        String token = com.mapbot.alpha.config.AlphaConfig.getWsToken();
        if (token != null && !token.isEmpty()) {
            builder.header("Authorization", "Bearer " + token);
            LOGGER.info("OneBot \u9274\u6743: \u5df2\u643a\u5e26 access_token");
        }
        
        builder.buildAsync(URI.create(url), this)
                .whenComplete((ws, ex) -> {
                    if (ex != null) {
                        LOGGER.error("OneBot 连接失败: {}", ex.getMessage());
                        scheduleReconnect();
                    } else {
                        this.webSocket = ws;
                        LOGGER.info("OneBot 连接成功");
                        if (startupNoticeSent.compareAndSet(false, true)) {
                            long playerGroupId = com.mapbot.alpha.config.AlphaConfig.getPlayerGroupId();
                            if (playerGroupId > 0) {
                                sendGroupMessage(playerGroupId, "起床📢别过少爷生活📢");
                            }
                        }
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
     * 发送群消息并等待发送完成（用于关机钩子等需要尽量送达的场景）
     */
    public boolean sendGroupMessageBlocking(long groupId, String message, long timeoutMs) {
        if (groupId <= 0) {
            return false;
        }
        if ((serverUrl == null || serverUrl.isBlank()) && webSocket == null) {
            LOGGER.warn("无法发送消息: OneBot 地址未配置");
            return false;
        }
        // 关机阶段尽量补一次连接机会，避免“恰好断线”时直接丢通知。
        if (webSocket == null && serverUrl != null && !serverUrl.isBlank()) {
            connect(serverUrl);
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (webSocket == null) {
            LOGGER.warn("无法发送消息: OneBot 未连接");
            return false;
        }
        long timeout = timeoutMs > 0 ? timeoutMs : 1000L;
        String json = String.format(
            "{\"action\":\"send_group_msg\",\"params\":{\"group_id\":%d,\"message\":\"%s\"}}",
            groupId, escapeJson(message)
        );
        try {
            webSocket.sendText(json, true).orTimeout(timeout, TimeUnit.MILLISECONDS).join();
            LOGGER.debug("[发送到QQ群 {}] {}", groupId, message);
            return true;
        } catch (Exception e) {
            LOGGER.warn("发送群消息失败: groupId={}, message={}, reason={}",
                groupId, message, e.getMessage());
            return false;
        }
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
