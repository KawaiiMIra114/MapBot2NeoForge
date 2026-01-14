package com.mapbot.network;

import com.google.gson.JsonObject;
import com.mapbot.logic.InboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MapBot WebSocket Client
 * 
 * 遵循 .ai_rules.md 规范
 * 负责与 NapCat (OneBot v11) 建立连接。
 * 
 * 特性:
 * - 单例模式
 * - 自动重连 (5秒间隔)
 * - 纯 Java 21 标准库实现
 */
public class BotClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/WS");
    private static final String WS_URL = "ws://127.0.0.1:3000";

    // 单例实例
    public static final BotClient INSTANCE = new BotClient();

    // 核心组件
    private WebSocket webSocket;
    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;

    // 状态标志
    private boolean isConnected = false;
    private boolean isReconnecting = false;

    private BotClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MapBot-WS-Thread");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * 启动连接
     */
    public void connect() {
        if (isConnected || isReconnecting) {
            return;
        }

        LOGGER.info("正在尝试连接到 {} ...", WS_URL);

        try {
            httpClient.newWebSocketBuilder()
                    .buildAsync(URI.create(WS_URL), new WSListener())
                    .whenComplete((ws, error) -> {
                        if (error != null) {
                            LOGGER.error("连接建立失败: {}", error.getMessage());
                            scheduleReconnect();
                        } else {
                            this.webSocket = ws;
                            this.isConnected = true;
                            this.isReconnecting = false;
                            LOGGER.info("连接成功建立!");
                        }
                    });
        } catch (Exception e) {
            LOGGER.error("连接初始化异常: {}", e.getMessage());
            scheduleReconnect();
        }
    }

    /**
     * 发送 JSON 数据包
     * 
     * @param json Gson JsonObject
     */
    public void sendPacket(JsonObject json) {
        if (webSocket != null && isConnected) {
            String payload = json.toString();
            webSocket.sendText(payload, true)
                    .thenAccept(ws -> LOGGER.debug("数据包已发送: {}", payload))
                    .exceptionally(ex -> {
                        LOGGER.error("发送数据包失败: {}", ex.getMessage());
                        return null;
                    });
        } else {
            LOGGER.warn("发送失败: 连接未建立");
        }
    }

    /**
     * 调度重连任务
     */
    private void scheduleReconnect() {
        this.isConnected = false;

        if (isReconnecting)
            return; // 避免重复调度
        isReconnecting = true;

        LOGGER.info("5秒后尝试重连...");
        scheduler.schedule(() -> {
            isReconnecting = false; // 重置标志以允许 connect() 执行
            connect();
        }, 5, TimeUnit.SECONDS);
    }

    /**
     * 内部 WebSocket 监听器
     */
    private class WSListener implements WebSocket.Listener {

        @Override
        public void onOpen(WebSocket webSocket) {
            // 注意: 标准库 Listener 没有独立的 onOpen，这里通常通过 buildAsync 的回调处理
            // 但此方法会被调用以 signaling Initial request.
            LOGGER.info("WebSocket 通道已打开 (onOpen)");
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            LOGGER.debug("收到原始消息: {}", data);

            // 调用入站处理器解析消息
            try {
                InboundHandler.handleMessage(data.toString());
            } catch (Exception e) {
                LOGGER.error("处理入站消息时发生异常: {}", e.getMessage());
            }

            webSocket.request(1); // 请求下一条消息
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            LOGGER.warn("连接关闭: {} {}", statusCode, reason);
            BotClient.this.webSocket = null;
            scheduleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            LOGGER.error("连接错误: {}", error.getMessage());
            BotClient.this.webSocket = null;
            scheduleReconnect();
        }
    }

    /**
     * 获取当前是否已连接
     */
    public boolean isConnected() {
        return isConnected;
    }
}
