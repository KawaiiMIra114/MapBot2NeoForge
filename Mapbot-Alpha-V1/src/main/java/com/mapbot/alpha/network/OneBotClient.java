package com.mapbot.alpha.network;

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
 */
public class OneBotClient implements WebSocket.Listener {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/BotClient");
    public static final OneBotClient INSTANCE = new OneBotClient();

    private WebSocket webSocket;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private String serverUrl;

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
        String msg = data.toString();
        // TODO: 解析 OneBot 消息并分发
        // 这里的逻辑将与 InboundHandler 类似
        webSocket.request(1);
        return null;
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
}
