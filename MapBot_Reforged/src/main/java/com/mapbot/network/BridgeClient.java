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
                    break;
                case "command":
                    handleCommand(msg);
                    break;
                case "qq_message":
                    handleQQMessage(msg);
                    break;
                // STEP 13: Alpha 代理请求
                case "get_players":
                    handleGetPlayers(msg);
                    break;
                case "get_status":
                    handleGetStatus(msg);
                    break;
                case "bind_player":
                    handleBindPlayer(msg);
                    break;
                case "sign_in":
                    handleSignIn(msg);
                    break;
                case "accept_reward":
                    handleAcceptReward(msg);
                    break;
                case "get_inventory":
                    handleGetInventory(msg);
                    break;
                case "get_location":
                    handleGetLocation(msg);
                    break;
                case "execute_command":
                    handleExecuteCommand(msg);
                    break;
                case "broadcast":
                    handleBroadcast(msg);
                    break;
                case "get_playtime":
                    handleGetPlaytime(msg);
                    break;
                case "get_cdk":
                    handleGetCdk(msg);
                    break;
                case "stop_server":
                    handleStopServer(msg);
                    break;
                case "cancel_stop":
                    handleCancelStop(msg);
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
        String requestId = extractJsonValue(msg, "requestId");
        LOGGER.info("[Bridge] 收到指令: {}", cmd);
        
        net.minecraft.server.MinecraftServer server = 
            net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> {
                server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack(), cmd);
            });
            sendProxyResponse(requestId, "指令已执行");
        } else {
            sendProxyResponse(requestId, "服务器未就绪");
        }
    }
    
    // ==================== STEP 13: Alpha 代理请求处理 ====================
    
    private void handleGetPlayers(String msg) {
        String requestId = extractJsonValue(msg, "requestId");
        net.minecraft.server.MinecraftServer server = 
            net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        
        if (server == null) {
            sendProxyResponse(requestId, "[服务器] 未就绪");
            return;
        }
        
        var players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            sendProxyResponse(requestId, "[在线] 当前无玩家在线");
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("[在线] ").append(players.size()).append(" 人\n");
        for (var player : players) {
            sb.append("• ").append(player.getName().getString()).append("\n");
        }
        sendProxyResponse(requestId, sb.toString().trim());
    }
    
    private void handleGetStatus(String msg) {
        String requestId = extractJsonValue(msg, "requestId");
        net.minecraft.server.MinecraftServer server = 
            net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        
        if (server == null) {
            sendProxyResponse(requestId, "[服务器] 未就绪");
            return;
        }
        
        // 获取 TPS 和内存
        double tps = com.mapbot.logic.ServerStatusManager.getCurrentTPS();
        String tpsStr = String.format("%.1f", tps);
        Runtime rt = Runtime.getRuntime();
        long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        long maxMB = rt.maxMemory() / 1024 / 1024;
        int playerCount = server.getPlayerList().getPlayers().size();
        
        String result = String.format(
            "[状态] %s\n在线: %d 人\nTPS: %s\n内存: %dMB / %dMB",
            com.mapbot.config.BotConfig.getServerId(),
            playerCount, tpsStr, usedMB, maxMB
        );
        sendProxyResponse(requestId, result);
    }
    
    private void handleBindPlayer(String msg) {
        String requestId = extractJsonValue(msg, "requestId");
        String playerName = extractJsonValue(msg, "arg1");
        String qqStr = extractJsonValue(msg, "arg2");
        
        net.minecraft.server.MinecraftServer server = 
            net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        
        if (server == null) {
            sendProxyResponse(requestId, "[错误] 服务器未就绪");
            return;
        }
        
        try {
            long qq = Long.parseLong(qqStr);
            
            // 解析玩家 UUID
            java.util.Optional<com.mojang.authlib.GameProfile> profile = 
                server.getProfileCache().get(playerName);
            
            if (profile.isEmpty()) {
                // 离线模式
                if (!server.usesAuthentication()) {
                    var uuid = net.minecraft.core.UUIDUtil.createOfflinePlayerUUID(playerName);
                    com.mapbot.data.DataManager.INSTANCE.bind(qq, uuid.toString());
                    
                    // 添加白名单
                    var whitelist = server.getPlayerList().getWhiteList();
                    var gp = new com.mojang.authlib.GameProfile(uuid, playerName);
                    if (!whitelist.isWhiteListed(gp)) {
                        whitelist.add(new net.minecraft.server.players.UserWhiteListEntry(gp));
                        whitelist.save();
                    }
                    
                    sendProxyResponse(requestId, "SUCCESS:" + uuid + ":" + playerName);
                    return;
                }
                sendProxyResponse(requestId, "FAIL:[绑定失败] 玩家不存在");
                return;
            }
            
            String uuid = profile.get().getId().toString();
            
            if (com.mapbot.data.DataManager.INSTANCE.isUUIDBound(uuid)) {
                sendProxyResponse(requestId, "FAIL:[绑定失败] 该游戏ID已被其他QQ绑定");
                return;
            }
            
            com.mapbot.data.DataManager.INSTANCE.bind(qq, uuid);
            
            // 添加白名单
            var whitelist = server.getPlayerList().getWhiteList();
            if (!whitelist.isWhiteListed(profile.get())) {
                whitelist.add(new net.minecraft.server.players.UserWhiteListEntry(profile.get()));
                whitelist.save();
            }
            
            sendProxyResponse(requestId, "SUCCESS:" + uuid + ":" + profile.get().getName());
            
        } catch (Exception e) {
            LOGGER.error("绑定失败", e);
            sendProxyResponse(requestId, "[错误] 绑定失败: " + e.getMessage());
        }
    }
    
    private void handleSignIn(String msg) {
        String requestId = extractJsonValue(msg, "requestId");
        String qqStr = extractJsonValue(msg, "arg1");
        
        try {
            long qq = Long.parseLong(qqStr);
            var item = com.mapbot.logic.SignManager.INSTANCE.rollSignReward(qq);
            
            if (item == null) {
                sendProxyResponse(requestId, "[错误] 奖池配置异常");
                return;
            }
            
            String result = String.format(
                "[签到成功]\n物品: [%s] %s x%d\n%s\n请输入 #accept 领取",
                item.rarity, item.name, item.count,
                com.mapbot.data.loot.LootConfig.INSTANCE.getRarityMessage(item.rarity)
            );
            sendProxyResponse(requestId, result);
            
        } catch (Exception e) {
            sendProxyResponse(requestId, "[错误] 签到失败: " + e.getMessage());
        }
    }
    
    private void handleAcceptReward(String msg) {
        String requestId = extractJsonValue(msg, "requestId");
        String qqStr = extractJsonValue(msg, "arg1");
        
        try {
            long qq = Long.parseLong(qqStr);
            boolean success = com.mapbot.logic.SignManager.INSTANCE.claimOnline(qq);
            
            sendProxyResponse(requestId, success ? "[领取成功] 物品已发放到背包" : "[领取失败] 无待领取奖励或玩家离线");
            
        } catch (Exception e) {
            sendProxyResponse(requestId, "[错误] 领取失败: " + e.getMessage());
        }
    }
    
    private void handleGetInventory(String msg) {
        String requestId = extractJsonValue(msg, "requestId");
        String playerName = extractJsonValue(msg, "arg1");
        
        net.minecraft.server.MinecraftServer server = 
            net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        
        if (server == null) {
            sendProxyResponse(requestId, "[错误] 服务器未就绪");
            return;
        }
        
        var player = server.getPlayerList().getPlayerByName(playerName);
        if (player == null) {
            sendProxyResponse(requestId, "[错误] 玩家不在线: " + playerName);
            return;
        }
        
        String result = com.mapbot.logic.InventoryManager.getPlayerInventory(player);
        sendProxyResponse(requestId, result);
    }
    
    private void handleGetLocation(String msg) {
        String requestId = extractJsonValue(msg, "requestId");
        String playerName = extractJsonValue(msg, "arg1");
        
        net.minecraft.server.MinecraftServer server = 
            net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        
        if (server == null) {
            sendProxyResponse(requestId, "[错误] 服务器未就绪");
            return;
        }
        
        var player = server.getPlayerList().getPlayerByName(playerName);
        if (player == null) {
            sendProxyResponse(requestId, "[错误] 玩家不在线: " + playerName);
            return;
        }
        
        String world = player.level().dimension().location().toString();
        int x = (int) player.getX();
        int y = (int) player.getY();
        int z = (int) player.getZ();
        
        String result = String.format("[位置] %s\n世界: %s\n坐标: %d, %d, %d", 
            playerName, world, x, y, z);
        sendProxyResponse(requestId, result);
    }
    
    private void handleExecuteCommand(String msg) {
        String requestId = extractJsonValue(msg, "requestId");
        String command = extractJsonValue(msg, "arg1");
        
        net.minecraft.server.MinecraftServer server = 
            net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        
        if (server == null) {
            sendProxyResponse(requestId, "[错误] 服务器未就绪");
            return;
        }
        
        server.execute(() -> {
            server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack(), command);
        });
        
        sendProxyResponse(requestId, "[成功] 指令已执行: " + command);
    }
    
    private void handleBroadcast(String msg) {
        String requestId = extractJsonValue(msg, "requestId");
        String message = extractJsonValue(msg, "arg1");
        
        net.minecraft.server.MinecraftServer server = 
            net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        
        if (server != null) {
            server.execute(() -> {
                server.getPlayerList().broadcastSystemMessage(
                    net.minecraft.network.chat.Component.literal(message), false);
            });
        }
        
        sendProxyResponse(requestId, "OK");
    }
    
    private void handleGetPlaytime(String msg) {
        String requestId = extractJsonValue(msg, "requestId");
        String playerName = extractJsonValue(msg, "arg1");
        String modeStr = extractJsonValue(msg, "arg2");
        
        net.minecraft.server.MinecraftServer server = 
            net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        
        if (server == null) {
            sendProxyResponse(requestId, "[错误] 服务器未就绪");
            return;
        }
        
        int mode = 0;
        try {
            mode = Integer.parseInt(modeStr);
        } catch (Exception ignored) {}
        
        final int finalMode = mode;
        server.execute(() -> {
            java.util.UUID uuid = server.getProfileCache().get(playerName)
                    .map(com.mojang.authlib.GameProfile::getId)
                    .orElse(null);
            
            if (uuid == null) {
                sendProxyResponse(requestId, "[错误] 玩家不存在: " + playerName);
                return;
            }
            
            long mins = com.mapbot.data.PlaytimeManager.INSTANCE.getPlaytimeMinutes(uuid, finalMode);
            String time = com.mapbot.data.PlaytimeManager.formatDuration(mins);
            String[] periods = {"日", "周", "月", "总"};
            String period = (finalMode >= 0 && finalMode < periods.length) ? periods[finalMode] : "总";
            
            sendProxyResponse(requestId, String.format("[在线时长] %s (%s)\n时长: %s", playerName, period, time));
        });
    }
    
    private void handleGetCdk(String msg) {
        String requestId = extractJsonValue(msg, "requestId");
        String qqStr = extractJsonValue(msg, "arg1");
        
        try {
            long qq = Long.parseLong(qqStr);
            
            if (!com.mapbot.logic.SignManager.INSTANCE.hasPendingReward(qq)) {
                sendProxyResponse(requestId, "[提示] 您没有待领取的奖励，请先 #sign");
                return;
            }
            
            String code = com.mapbot.logic.SignManager.INSTANCE.generateCdk(qq);
            if (code != null) {
                sendProxyResponse(requestId, String.format("[兑换码] %s\n进服输入: /mapbot cdk %s", code, code));
            } else {
                sendProxyResponse(requestId, "[错误] 生成兑换码失败");
            }
        } catch (Exception e) {
            sendProxyResponse(requestId, "[错误] " + e.getMessage());
        }
    }
    
    private void handleStopServer(String msg) {
        String requestId = extractJsonValue(msg, "requestId");
        String countdownStr = extractJsonValue(msg, "arg1");
        
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            sendProxyResponse(requestId, "[错误] 服务器未就绪");
            return;
        }
        
        int countdown = 0;
        try {
            countdown = Integer.parseInt(countdownStr);
        } catch (Exception ignored) {}
        
        if (countdown == 0) {
            sendProxyResponse(requestId, "[系统] 正在立即关闭服务器...");
            LOGGER.warn("收到立即关服指令");
            server.execute(() -> server.halt(false));
        } else {
            final int seconds = countdown;
            com.mapbot.logic.ServerStatusManager.setStopCancelled(false);
            sendProxyResponse(requestId, String.format("[系统] 服务器将在 %d 秒后关闭", seconds));
            LOGGER.warn("收到倒计时关服指令: {}s", seconds);
            
            new Thread(() -> {
                try {
                    int remaining = seconds;
                    while (remaining > 0) {
                        if (com.mapbot.logic.ServerStatusManager.isStopCancelled()) {
                            return;
                        }
                        
                        if (remaining <= 10 || remaining == 30 || remaining == 60) {
                            final int r = remaining;
                            server.execute(() -> {
                                server.getPlayerList().getPlayers().forEach(p -> 
                                    p.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c[警告] 服务器将在 " + r + " 秒后关闭"))
                                );
                            });
                        }
                        
                        Thread.sleep(1000);
                        remaining--;
                    }
                    
                    if (!com.mapbot.logic.ServerStatusManager.isStopCancelled()) {
                        server.execute(() -> server.halt(false));
                    }
                } catch (InterruptedException e) {
                    LOGGER.error("关服线程中断", e);
                }
            }, "MapBot-StopCountdown").start();
        }
    }
    
    private void handleCancelStop(String msg) {
        String requestId = extractJsonValue(msg, "requestId");
        com.mapbot.logic.ServerStatusManager.setStopCancelled(true);
        sendProxyResponse(requestId, "[系统] 已发送取消请求");
    }
    
    /**
     * 发送代理响应到 Alpha Core
     */
    private void sendProxyResponse(String requestId, String result) {
        String response = String.format(
            "{\"type\":\"proxy_response\",\"requestId\":\"%s\",\"result\":\"%s\"}",
            requestId, escapeJson(result)
        );
        send(response);
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
        
        // 修复: 使用 files 字段直接嵌入 JSON 数组，而非转义后的字符串
        return String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"files\":%s}", requestId, json.toString());
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
