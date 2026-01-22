/*
 * Bridge Client - 连接 MapBot Alpha Core 的客户端
 * 
 * 职责：
 * 1. 与 Alpha Core 保持 TCP 长连接
 * 2. 服务器注册和心跳
 * 3. 上报游戏事件
 * 4. 执行远程指令
 * 5. 代理文件操作
 */
package com.mapbot.network;

import com.mapbot.config.BotConfig;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Alpha Core Bridge 客户端
 * 用于连接 MapBot Alpha 中枢
 */
public class BridgeClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final BridgeClient INSTANCE = new BridgeClient();
    
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    private ScheduledExecutorService heartbeatExecutor;
    private ExecutorService messageExecutor;
    
    // 文件请求回调
    private final ConcurrentHashMap<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();
    
    private String serverId;
    private String alphaHost;
    private int alphaPort;
    
    private BridgeClient() {}
    
    /**
     * 连接到 Alpha Core
     */
    public void connect() {
        // 从配置读取连接信息
        this.serverId = BotConfig.getServerId();
        this.alphaHost = BotConfig.getAlphaHost();
        this.alphaPort = BotConfig.getAlphaPort();
        
        if (alphaHost == null || alphaHost.isEmpty() || alphaPort <= 0) {
            LOGGER.warn("[Bridge] Alpha Core 连接未配置，Bridge 功能已禁用");
            return;
        }
        
        running.set(true);
        messageExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "Bridge-Reader"));
        
        // 异步连接
        CompletableFuture.runAsync(this::doConnect);
    }
    
    private void doConnect() {
        while (running.get() && !connected.get()) {
            try {
                LOGGER.info("[Bridge] 正在连接 Alpha Core: {}:{}", alphaHost, alphaPort);
                
                socket = new Socket(alphaHost, alphaPort);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                
                connected.set(true);
                LOGGER.info("[Bridge] 已连接到 Alpha Core");
                
                // 发送注册消息
                sendRegister();
                
                // 启动心跳
                startHeartbeat();
                
                // 启动消息接收循环
                messageExecutor.submit(this::readLoop);
                
            } catch (Exception e) {
                LOGGER.warn("[Bridge] 连接 Alpha Core 失败: {}，5秒后重试...", e.getMessage());
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    private void sendRegister() {
        String msg = String.format("{\"type\":\"register\",\"serverId\":\"%s\",\"version\":\"1.0\"}", serverId);
        send(msg);
        LOGGER.info("[Bridge] 已发送注册: {}", serverId);
    }
    
    private void startHeartbeat() {
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "Bridge-Heartbeat"));
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (connected.get()) {
                send("{\"type\":\"heartbeat\",\"timestamp\":" + System.currentTimeMillis() + "}");
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    private void readLoop() {
        try {
            String line;
            while (connected.get() && (line = reader.readLine()) != null) {
                handleMessage(line);
            }
        } catch (Exception e) {
            if (connected.get()) {
                LOGGER.warn("[Bridge] 连接断开: {}", e.getMessage());
            }
        } finally {
            handleDisconnect();
        }
    }
    
    private void handleMessage(String msg) {
        try {
            String type = extractJsonValue(msg, "type");
            
            switch (type) {
                case "register_ack":
                    LOGGER.info("[Bridge] 注册确认收到");
                    break;
                case "heartbeat_ack":
                    // 心跳响应，静默处理
                    break;
                case "command":
                    handleCommand(msg);
                    break;
                case "qq_message":
                    handleQQMessage(msg);
                    break;
                case "file_list":
                case "file_read":
                case "file_write":
                case "file_delete":
                    handleFileRequest(msg);
                    break;
                default:
                    LOGGER.debug("[Bridge] 收到消息: {}", msg);
            }
        } catch (Exception e) {
            LOGGER.error("[Bridge] 处理消息失败", e);
        }
    }
    
    private void handleCommand(String msg) {
        String cmd = extractJsonValue(msg, "cmd");
        LOGGER.info("[Bridge] 收到指令: {}", cmd);
        // TODO: 在游戏线程中执行指令
    }
    
    /**
     * 处理来自 Alpha 的 QQ 消息，广播到游戏内
     * STEP 12: QQ -> MC 消息流转
     */
    private void handleQQMessage(String msg) {
        String sender = extractJsonValue(msg, "sender");
        String content = extractJsonValue(msg, "content");
        
        if (sender.isEmpty() || content.isEmpty()) return;
        
        String formattedMsg = String.format("[QQ] %s: %s", sender, content);
        LOGGER.info("[QQ->MC] {}", formattedMsg);
        
        // 在主服务器线程中广播消息
        net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().execute(() -> {
            net.minecraft.server.MinecraftServer server = 
                net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.getPlayerList().broadcastSystemMessage(
                    net.minecraft.network.chat.Component.literal(formattedMsg), 
                    false
                );
            }
        });
    }
    
    private void handleFileRequest(String msg) {
        String type = extractJsonValue(msg, "type");
        String requestId = extractJsonValue(msg, "requestId");
        String path = extractJsonValue(msg, "path");
        
        try {
            String response;
            switch (type) {
                case "file_list":
                    response = handleFileList(requestId, path);
                    break;
                case "file_read":
                    response = handleFileRead(requestId, path);
                    break;
                case "file_write":
                    String content = extractJsonValue(msg, "content");
                    response = handleFileWrite(requestId, path, content);
                    break;
                case "file_delete":
                    response = handleFileDelete(requestId, path);
                    break;
                default:
                    response = String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"error\":\"Unknown action\"}", requestId);
            }
            send(response);
        } catch (Exception e) {
            send(String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"error\":\"%s\"}", requestId, e.getMessage()));
        }
    }
    
    private String handleFileList(String requestId, String path) throws IOException {
        File dir = new File(path.isEmpty() ? "." : path);
        if (!dir.isDirectory()) {
            return String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"error\":\"Not a directory\"}", requestId);
        }
        
        StringBuilder json = new StringBuilder("[");
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (i > 0) json.append(",");
                File f = files[i];
                json.append("{\"name\":\"").append(escapeJson(f.getName())).append("\",");
                json.append("\"isDir\":").append(f.isDirectory()).append(",");
                json.append("\"size\":").append(f.isFile() ? f.length() : 0).append("}");
            }
        }
        json.append("]");
        
        return String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"content\":\"%s\"}", requestId, escapeJson(json.toString()));
    }
    
    private String handleFileRead(String requestId, String path) throws IOException {
        File file = new File(path);
        if (!file.isFile()) {
            return String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"error\":\"Not a file\"}", requestId);
        }
        
        String content = java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8);
        return String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"content\":\"%s\"}", requestId, escapeJson(content));
    }
    
    private String handleFileWrite(String requestId, String path, String content) throws IOException {
        File file = new File(path);
        java.nio.file.Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        return String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"content\":\"ok\"}", requestId);
    }
    
    private String handleFileDelete(String requestId, String path) throws IOException {
        File file = new File(path);
        boolean deleted = file.delete();
        if (deleted) {
            return String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"content\":\"ok\"}", requestId);
        } else {
            return String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"error\":\"Delete failed\"}", requestId);
        }
    }
    
    private void handleDisconnect() {
        connected.set(false);
        
        // 清理资源
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
        }
        
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
        
        // 如果还在运行则尝试重连
        if (running.get()) {
            LOGGER.info("[Bridge] 3秒后尝试重连...");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            doConnect();
        }
    }
    
    /**
     * 发送消息
     */
    public synchronized void send(String msg) {
        if (!connected.get() || writer == null) return;
        
        try {
            writer.write(msg);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            LOGGER.error("[Bridge] 发送消息失败", e);
        }
    }
    
    /**
     * 发送事件
     */
    public void sendEvent(String event, String data) {
        String msg = String.format("{\"type\":\"event\",\"event\":\"%s\",\"data\":%s}", event, data);
        send(msg);
    }
    
    /**
     * 发送聊天消息到 Alpha Core
     * STEP 12: MC -> Alpha -> QQ
     */
    public void sendChat(String player, String content) {
        String msg = String.format("{\"type\":\"chat\",\"player\":\"%s\",\"content\":\"%s\"}", 
                escapeJson(player), escapeJson(content));
        send(msg);
    }
    
    /**
     * 发送状态更新
     */
    public void sendStatusUpdate(String players, String tps, String memory) {
        String msg = String.format("{\"type\":\"status_update\",\"players\":\"%s\",\"tps\":\"%s\",\"memory\":\"%s\"}", 
                players, tps, memory);
        send(msg);
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        running.set(false);
        connected.set(false);
        
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
        }
        if (messageExecutor != null) {
            messageExecutor.shutdownNow();
        }
        
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {}
        
        LOGGER.info("[Bridge] 已断开与 Alpha Core 的连接");
    }
    
    public boolean isConnected() {
        return connected.get();
    }
    
    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) {
            search = "\"" + key + "\":";
            start = json.indexOf(search);
            if (start == -1) return "";
            start += search.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return json.substring(start, end).trim();
        }
        start += search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
    
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
