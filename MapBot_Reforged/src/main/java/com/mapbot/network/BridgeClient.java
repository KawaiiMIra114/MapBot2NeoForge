/*
 * Bridge Client - 连接 MapBot Alpha Core 的客户端
 * 
 * 职责：
 * 1. 与 Alpha Core 保持 TCP 长连接
 * 2. 服务器注册和心跳
 * 3. 上报游戏事件
 * 4. 执行远程指令
 * 5. 代理文件操作
 * 
 * R2+R4 重构: 消息处理逻辑已拆分到 BridgeHandlers.java
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
import com.google.gson.*;

/**
 * Alpha Core Bridge 客户端
 * 用于连接 MapBot Alpha 中枢
 */
public class BridgeClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final BridgeClient INSTANCE = new BridgeClient();
    private static final Gson GSON = new GsonBuilder().create();
    
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    private ScheduledExecutorService heartbeatExecutor;
    private ExecutorService messageExecutor;
    
    // 文件请求回调
    private final ConcurrentHashMap<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();

    // ===== P0: 数据统一管理 (缓存读取 Alpha 数据) =====

    private static final long CACHE_TTL_MS = 3000;

    private static final class CacheLong {
        final long value;
        final long fetchedAtMs;

        CacheLong(long value, long fetchedAtMs) {
            this.value = value;
            this.fetchedAtMs = fetchedAtMs;
        }

        boolean isFresh() {
            return System.currentTimeMillis() - fetchedAtMs <= CACHE_TTL_MS;
        }
    }

    private final ConcurrentHashMap<String, CacheLong> muteExpiryCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CacheLong> qqByUuidCache = new ConcurrentHashMap<>();
    
    private String serverId;
    private String alphaHost;
    private int alphaPort;
    
    /**
     * Task #022: 向 Alpha 发送 CDK 兑换验证请求
     * @return "VALID:{itemJson}" 或 "INVALID:原因"
     */
    public String redeemCdk(String code, String uuid) {
        if (!connected.get()) return "INVALID:Bridge 未连接";
        
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("code", code);
            payload.addProperty("uuid", uuid);
            payload.addProperty("serverId", serverId);
            
            String result = requestAlpha("redeem_cdk", payload, 3000);
            if (result == null || result.isEmpty()) {
                return "INVALID:Alpha 未响应 (超时)";
            }
            return result;
        } catch (Exception e) {
            LOGGER.error("[Bridge] CDK 兑换请求失败", e);
            return "INVALID:" + e.getMessage();
        }
    }
    
    /**
     * 连接到 Alpha Core
     */
    public void connect() {
        if (running.get()) return;
        
        // 从配置读取
        serverId = BotConfig.getServerId();
        alphaHost = BotConfig.getAlphaHost();
        alphaPort = BotConfig.getAlphaPort();
        
        if (alphaHost == null || alphaHost.isEmpty()) {
            LOGGER.warn("[Bridge] Alpha Core 地址未配置，跳过连接");
            return;
        }
        
        running.set(true);
        messageExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "MapBot-Bridge");
            t.setDaemon(true);
            return t;
        });
        messageExecutor.submit(this::doConnect);
    }
    
    private void doConnect() {
        while (running.get()) {
            try {
                LOGGER.info("[Bridge] 正在连接到 Alpha Core: {}:{}", alphaHost, alphaPort);
                socket = new Socket(alphaHost, alphaPort);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                
                connected.set(true);
                LOGGER.info("[Bridge] 已连接到 Alpha Core");
                
                sendRegister();
                startHeartbeat();
                readLoop();
                
            } catch (Exception e) {
                LOGGER.error("[Bridge] 连接失败: {}", e.getMessage());
                connected.set(false);
                
                if (running.get()) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }
    
    private void sendRegister() {
        String msg = String.format("{\"type\":\"register\",\"serverId\":\"%s\"}", serverId);
        send(msg);
    }
    
    private void startHeartbeat() {
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
        }
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MapBot-Heartbeat");
            t.setDaemon(true);
            return t;
        });
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            if (connected.get()) {
                int online = getOnlinePlayers();
                String tps = getServerTps();
                String memory = getMemoryUsage();
                long uptime = getServerUptime();
                String msg = String.format(
                    "{\"type\":\"heartbeat\",\"players\":%d,\"tps\":\"%s\",\"memory\":\"%s\",\"uptime\":%d}",
                    online, tps, memory, uptime
                );
                send(msg);
            }
        }, 10, 30, TimeUnit.SECONDS);
    }
    
    /**
     * 获取在线玩家数
     */
    private int getOnlinePlayers() {
        net.minecraft.server.MinecraftServer server =
            net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return 0;
        return server.getPlayerList().getPlayers().size();
    }
    
    /**
     * 获取服务器 TPS
     */
    private String getServerTps() {
        try {
            net.minecraft.server.MinecraftServer server =
                net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                long[] tickTimes = server.getTickTimesNanos();
                if (tickTimes.length > 0) {
                    double avgTickMs = 0;
                    for (long t : tickTimes) avgTickMs += t;
                    avgTickMs = avgTickMs / tickTimes.length / 1_000_000.0;
                    double tps = Math.min(20.0, 1000.0 / avgTickMs);
                    return String.format("%.1f", tps);
                }
            }
        } catch (Exception ignored) {}
        return "20.0";
    }
    
    /**
     * 获取内存使用
     */
    private String getMemoryUsage() {
        Runtime rt = Runtime.getRuntime();
        long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        long maxMB = rt.maxMemory() / 1024 / 1024;
        return usedMB + "MB/" + maxMB + "MB";
    }
    
    /**
     * 获取服务器运行时长（毫秒）
     */
    private long getServerUptime() {
        return java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
    }
    
    // ==================== 读取循环 ====================

    private void readLoop() {
        try {
            String line;
            while (connected.get() && (line = reader.readLine()) != null) {
                handleMessage(line);
            }
        } catch (Exception e) {
            if (running.get()) {
                LOGGER.error("[Bridge] 读取消息失败: {}", e.getMessage());
            }
        } finally {
            handleDisconnect();
        }
    }
    
    /**
     * R2: 统一 JSON 解析入口
     * 整个消息只解析一次，分发给 BridgeHandlers 中的处理器
     */
    private void handleMessage(String msg) {
        try {
            JsonObject json = GSON.fromJson(msg, JsonObject.class);
            String type = json.has("type") ? json.get("type").getAsString() : "";
            
            switch (type) {
                case "register_ack":   LOGGER.info("[Bridge] 注册确认收到"); break;
                case "heartbeat_ack":  break;
                case "proxy_response": BridgeHandlers.handleProxyResponseFromAlpha(json, this); break;
                case "command":        BridgeHandlers.handleCommand(json, this); break;
                case "qq_message":     BridgeHandlers.handleQQMessage(json, this); break;
                case "get_players":    BridgeHandlers.handleGetPlayers(json, this); break;
                case "has_player":     BridgeHandlers.handleHasPlayer(json, this); break;
                case "get_status":     BridgeHandlers.handleGetStatus(json, this); break;
                case "bind_player":    BridgeHandlers.handleBindPlayer(json, this); break;
                case "resolve_uuid":   BridgeHandlers.handleResolveUuid(json, this); break;
                case "sign_in":        BridgeHandlers.handleSignIn(json, this); break;
                case "accept_reward":  BridgeHandlers.handleAcceptReward(json, this); break;
                case "get_inventory":  BridgeHandlers.handleGetInventory(json, this); break;
                case "get_location":   BridgeHandlers.handleGetLocation(json, this); break;
                case "execute_command":BridgeHandlers.handleExecuteCommand(json, this); break;
                case "broadcast":      BridgeHandlers.handleBroadcast(json, this); break;
                case "get_playtime":   BridgeHandlers.handleGetPlaytime(json, this); break;
                case "get_cdk":        BridgeHandlers.handleGetCdk(json, this); break;
                case "stop_server":    BridgeHandlers.handleStopServer(json, this); break;
                case "cancel_stop":    BridgeHandlers.handleCancelStop(json, this); break;
                case "file_list": case "file_read": case "file_write": case "file_delete":
                    BridgeHandlers.handleFileRequest(json, this); break;
                case "roll_loot":      BridgeHandlers.handleRollLoot(json, this); break;
                case "give_item":      BridgeHandlers.handleGiveItem(json, this); break;
                default:               LOGGER.debug("[Bridge] 收到消息: {}", msg);
            }
        } catch (Exception e) {
            LOGGER.error("[Bridge] 处理消息失败", e);
        }
    }

    // ==================== R4: BridgeHandlers 辅助方法 (包可见) ====================

    /**
     * 完成 pending 请求
     */
    void completePendingRequest(String requestId, String result) {
        var future = pendingRequests.remove(requestId);
        if (future != null) future.complete(result);
    }

    /**
     * 校验文件路径安全性（防止路径遍历攻击）
     */
    boolean isPathSafe(String path) {
        try {
            File serverRoot = new File(".").getCanonicalFile();
            File target = new File(path).getCanonicalFile();
            return target.getPath().startsWith(serverRoot.getPath());
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 发送代理响应到 Alpha Core
     */
    void sendProxyResponse(String requestId, String result) {
        String response = String.format(
            "{\"type\":\"proxy_response\",\"requestId\":\"%s\",\"result\":\"%s\"}",
            requestId, escapeJson(result)
        );
        send(response);
    }
    
    /**
     * JSON 转义
     */
    String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    // ==================== 连接管理 ====================
    
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

    // ==================== P0: Alpha 数据查询 ====================

    /**
     * P0: 查询玩家 UUID 当前禁言到期时间
     * @return 0=未禁言，-1=永久禁言，其他=到期时间戳(ms)
     */
    public long checkMuteExpiry(String uuid) {
        if (uuid == null || uuid.isEmpty()) return 0L;

        CacheLong cached = muteExpiryCache.get(uuid);
        if (cached != null && cached.isFresh()) {
            return cached.value;
        }

        long expiry = 0L;
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("uuid", uuid);
            String result = requestAlpha("check_mute", payload, 500);
            if (result != null && !result.isEmpty()) {
                expiry = Long.parseLong(result);
            }
        } catch (Exception ignored) {}

        muteExpiryCache.put(uuid, new CacheLong(expiry, System.currentTimeMillis()));
        return expiry;
    }

    /**
     * P0: 通过 UUID 反查绑定的 QQ
     * @return -1 表示未绑定
     */
    public long getQQByUUID(String uuid) {
        if (uuid == null || uuid.isEmpty()) return -1L;

        CacheLong cached = qqByUuidCache.get(uuid);
        if (cached != null && cached.isFresh()) {
            return cached.value;
        }

        long qq = -1L;
        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("uuid", uuid);
            String result = requestAlpha("get_qq_by_uuid", payload, 500);
            if (result != null && !result.isEmpty()) {
                qq = Long.parseLong(result);
            }
        } catch (Exception ignored) {}

        qqByUuidCache.put(uuid, new CacheLong(qq, System.currentTimeMillis()));
        return qq;
    }

    /**
     * P0: 上报在线时长增量到 Alpha（跨服统一存储）
     */
    public void sendPlaytimeAdd(String uuid, long deltaMs) {
        if (!connected.get()) return;
        if (uuid == null || uuid.isEmpty() || deltaMs <= 0) return;

        try {
            String json = String.format(
                "{\"type\":\"playtime_add\",\"uuid\":\"%s\",\"deltaMs\":%d}",
                escapeJson(uuid), deltaMs
            );
            send(json);
        } catch (Exception e) {
            LOGGER.error("[Bridge] 上报在线时长失败", e);
        }
    }

    private String requestAlpha(String type, JsonObject payload, long timeoutMs) throws Exception {
        if (!connected.get()) return null;

        String requestId = type + "_" + System.currentTimeMillis();
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        JsonObject req = new JsonObject();
        req.addProperty("type", type);
        req.addProperty("requestId", requestId);
        if (payload != null) {
            for (var e : payload.entrySet()) {
                req.add(e.getKey(), e.getValue());
            }
        }

        send(req.toString());

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } finally {
            pendingRequests.remove(requestId);
        }
    }
}
