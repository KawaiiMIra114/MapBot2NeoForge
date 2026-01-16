package com.mapbot.network;

import com.google.gson.JsonObject;
import com.mapbot.config.BotConfig;
import com.mapbot.logic.InboundHandler;
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
 * MapBot WebSocket Client
 * 
 * 遵循 .ai_rules.md 规范
 * 负责与 NapCat (OneBot v11) 建立连接。
 * 
 * 特性:
 * - 单例模式
 * - 自动重连 (间隔从配置读取)
 * - 纯 Java 21 标准库实现
 * - 配置驱动 (wsUrl, reconnectInterval, debugMode)
 */
public class BotClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/WS");

    // 单例实例
    public static final BotClient INSTANCE = new BotClient();

    // 核心组件
    private WebSocket webSocket;
    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;

    // 状态标志
    private volatile boolean isConnected = false;
    private volatile boolean isReconnecting = false;
    private volatile boolean shouldReconnect = true; // 控制是否应该重连

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

        shouldReconnect = true; // 允许重连
        String wsUrl = BotConfig.getWsUrl();

        LOGGER.info("正在尝试连接到 {} ...", wsUrl);

        try {
            httpClient.newWebSocketBuilder()
                    .buildAsync(URI.create(wsUrl), new WSListener())
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
     * 断开连接
     */
    public void disconnect() {
        shouldReconnect = false; // 禁止重连
        isConnected = false;
        isReconnecting = false;

        if (webSocket != null) {
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "服务器关闭")
                        .thenRun(() -> LOGGER.info("WebSocket 连接已正常关闭"));
            } catch (Exception e) {
                LOGGER.warn("关闭连接时发生异常: {}", e.getMessage());
            }
            webSocket = null;
        }

        LOGGER.info("BotClient 已断开连接");
    }

    /**
     * 发送 JSON 数据包
     * 
     * @param json Gson JsonObject
     */
    public void sendPacket(JsonObject json) {
        if (webSocket != null && isConnected) {
            String payload = json.toString();

            if (BotConfig.isDebugMode()) {
                LOGGER.info("[DEBUG] 发送数据包: {}", payload);
            }

            webSocket.sendText(payload, true)
                    .thenAccept(ws -> LOGGER.debug("数据包已发送"))
                    .exceptionally(ex -> {
                        LOGGER.error("发送数据包失败: {}", ex.getMessage());
                        return null;
                    });
        } else {
            LOGGER.warn("发送失败: 连接未建立");
        }
    }

    /**
     * 发送群消息的辅助方法
     * 简化向 QQ 群发送消息的操作
     * 
     * @param groupId 目标群号
     * @param message 消息内容
     */
    public void sendGroupMessage(long groupId, String message) {
        if (!isConnected || groupId == 0L) {
            return;
        }
        
        JsonObject params = new JsonObject();
        params.addProperty("group_id", groupId);
        params.addProperty("message", message);
        
        JsonObject packet = new JsonObject();
        packet.addProperty("action", "send_group_msg");
        packet.add("params", params);
        packet.addProperty("echo", "event_" + System.currentTimeMillis());
        
        this.sendPacket(packet);
    }

    /**
     * 调度重连任务
     */
    private void scheduleReconnect() {
        if (!shouldReconnect) {
            LOGGER.debug("重连已禁用，跳过重连调度");
            return;
        }

        this.isConnected = false;

        if (isReconnecting)
            return; // 避免重复调度
        isReconnecting = true;

        int interval = BotConfig.getReconnectInterval();
        LOGGER.info("{}秒后尝试重连...", interval);

        scheduler.schedule(() -> {
            isReconnecting = false; // 重置标志以允许 connect() 执行
            if (shouldReconnect) {
                connect();
            }
        }, interval, TimeUnit.SECONDS);
    }

    /**
     * 内部 WebSocket 监听器
     */
    private class WSListener implements WebSocket.Listener {
        
        /** 消息分片缓冲区 - 用于处理大消息被 WebSocket 拆分的情况 */
        private final StringBuilder messageBuffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            LOGGER.info("WebSocket 通道已打开 (onOpen)");
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            // 将数据追加到缓冲区
            messageBuffer.append(data);
            
            if (BotConfig.isDebugMode()) {
                LOGGER.info("[DEBUG] 收到消息片段 (last={}): {}", last, data);
            } else {
                LOGGER.debug("收到消息片段 (长度: {}, last={})", data.length(), last);
            }
            
            // 只有当 last == true 时，才处理完整消息
            if (last) {
                String completeMessage = messageBuffer.toString();
                messageBuffer.setLength(0); // 清空缓冲区
                
                if (BotConfig.isDebugMode()) {
                    LOGGER.info("[DEBUG] 完整消息: {}", completeMessage);
                }
                
                // 调用入站处理器解析消息
                try {
                    InboundHandler.handleMessage(completeMessage);
                } catch (Exception e) {
                    LOGGER.error("处理入站消息时发生异常: {}", e.getMessage());
                }
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
