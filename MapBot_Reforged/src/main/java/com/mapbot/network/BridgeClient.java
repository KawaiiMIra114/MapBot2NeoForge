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
import java.nio.file.Path;
import java.util.Set;
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
    private static final long RECONNECT_DELAY_MS = 3000L;
    private static final long BRIDGE_FILE_MAX_BYTES = 256 * 1024L;
    private static final Set<String> BRIDGE_FILE_MUTATION_WHITELIST = Set.of("config", "world/serverconfig");
    private static final int DEFAULT_ALPHA_BRIDGE_PORT = 25661;
    
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
    private final Set<String> muteRefreshInFlight = ConcurrentHashMap.newKeySet();
    
    private String serverId;
    private String alphaHost;
    private int alphaPort;
    private String alphaToken;
    private String transferHost;
    private int transferPort;
    
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
     * 请求 Alpha 执行跨服切换
     * @return SUCCESS:* 或 FAIL:*
     */
    public String requestServerSwitch(String targetServer, String playerName, String playerUuid) {
        if (!connected.get()) return "FAIL:Bridge 未连接";

        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("targetServer", targetServer == null ? "" : targetServer.trim());
            payload.addProperty("playerName", playerName == null ? "" : playerName.trim());
            payload.addProperty("playerUuid", playerUuid == null ? "" : playerUuid.trim());
            payload.addProperty("sourceServerId", serverId == null ? "" : serverId);

            String result = requestAlpha("switch_server_request", payload, 15000);
            if (result == null || result.isEmpty()) {
                return "FAIL:Alpha 未响应 (超时)";
            }
            return result;
        } catch (Exception e) {
            LOGGER.error("[Bridge] 跨服切换请求失败", e);
            return "FAIL:" + e.getMessage();
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
        alphaToken = BotConfig.getAlphaToken();
        transferHost = BotConfig.getTransferHost();
        transferPort = BotConfig.getTransferPort();
        
        if (alphaHost == null || alphaHost.isEmpty()) {
            LOGGER.warn("[Bridge] Alpha Core 地址未配置，跳过连接");
            return;
        }
        if (alphaToken == null || alphaToken.isBlank()) {
            LOGGER.error("[Bridge] alphaToken 未配置，已停止 Bridge 连接。请在 config/mapbot-common.toml 的 alpha.alphaToken 填入 Alpha 端 auth.bridge.token");
            return;
        }
        
        if (alphaPort >= 25560 && alphaPort <= 25566) {
            LOGGER.warn("[Bridge] alphaPort={} 属于保留端口段 25560-25566，已自动回退到 {}", alphaPort, DEFAULT_ALPHA_BRIDGE_PORT);
            alphaPort = DEFAULT_ALPHA_BRIDGE_PORT;
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
                if (running.get()) {
                    LOGGER.error("[Bridge] 连接/会话异常: {}", e.getMessage());
                }
            } finally {
                handleDisconnect();
            }

            if (running.get()) {
                LOGGER.info("[Bridge] {}秒后尝试重连...", RECONNECT_DELAY_MS / 1000L);
                try {
                    Thread.sleep(RECONNECT_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
    
    private void sendRegister() {
        String escapedServerId = escapeJson(serverId == null ? "" : serverId);
        String token = alphaToken == null ? "" : alphaToken.trim();
        TransferEndpoint endpoint = normalizeTransferEndpoint(transferHost, transferPort);
        String msg;
        if (token.isEmpty()) {
            msg = String.format("{\"type\":\"register\",\"serverId\":\"%s\"}", escapedServerId);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"type\":\"register\",\"serverId\":\"").append(escapedServerId).append("\"");
            sb.append(",\"token\":\"").append(escapeJson(token)).append("\"");
            if (endpoint != null) {
                sb.append(",\"transferHost\":\"").append(escapeJson(endpoint.host)).append("\"");
                sb.append(",\"transferPort\":").append(endpoint.port);
            }
            sb.append("}");
            msg = sb.toString();
        }
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
                case "register_ack":   handleRegisterAck(json); break;
                case "heartbeat_ack":  break;
                case "proxy_response": BridgeHandlers.handleProxyResponseFromAlpha(json, this); break;
                case "command":        BridgeHandlers.handleCommand(json, this); break;
                case "qq_message":     BridgeHandlers.handleQQMessage(json, this); break;
                case "get_players":    BridgeHandlers.handleGetPlayers(json, this); break;
                case "has_player":     BridgeHandlers.handleHasPlayer(json, this); break;
                case "get_status":     BridgeHandlers.handleGetStatus(json, this); break;
                case "bind_player":    BridgeHandlers.handleBindPlayer(json, this); break;
                case "whitelist_add":  BridgeHandlers.handleWhitelistAdd(json, this); break;
                case "whitelist_remove": BridgeHandlers.handleWhitelistRemove(json, this); break;
                case "resolve_uuid":   BridgeHandlers.handleResolveUuid(json, this); break;
                case "resolve_name":   BridgeHandlers.handleResolveName(json, this); break;
                case "reload_config":  BridgeHandlers.handleReloadConfig(json, this); break;
                case "switch_server":  BridgeHandlers.handleSwitchServer(json, this); break;
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
                case "file_mkdir": case "file_upload":
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
            resolveSafePath(path);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void handleRegisterAck(JsonObject json) {
        boolean success = false;
        try {
            if (json.has("success") && !json.get("success").isJsonNull()) {
                success = json.get("success").getAsBoolean();
            }
        } catch (Exception ignored) {
            success = false;
        }

        if (success) {
            LOGGER.info("[Bridge] 注册成功: serverId={}", serverId);
            return;
        }

        String reason = "unknown";
        try {
            if (json.has("error") && !json.get("error").isJsonNull()) {
                reason = json.get("error").getAsString();
            }
        } catch (Exception ignored) {
        }

        LOGGER.error("[Bridge] 注册被 Alpha 拒绝: reason={}, serverId={}. 请检查 config/mapbot-common.toml 的 alpha.alphaToken 是否与 Alpha 端 auth.bridge.token 一致",
                reason, serverId);
        running.set(false);
        connected.set(false);
        handleDisconnect();
    }

    /**
     * 获取 Bridge 文件操作单次最大字节数
     */
    long getBridgeFileMaxBytes() {
        return BRIDGE_FILE_MAX_BYTES;
    }

    /**
     * 校验文件写入/删除是否在白名单目录中
     */
    boolean isMutationPathAllowed(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        try {
            Path targetPath = resolveSafePath(path).toPath();
            File serverRoot = new File(".").getCanonicalFile();
            for (String allowed : BRIDGE_FILE_MUTATION_WHITELIST) {
                File allowedRoot = new File(serverRoot, allowed).getCanonicalFile();
                if (targetPath.startsWith(allowedRoot.toPath())) {
                    return true;
                }
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    /**
     * 解析并返回安全路径（必须位于服务器目录内）
     */
    File resolveSafePath(String path) throws IOException {
        File serverRoot = new File(".").getCanonicalFile();
        File target = (path == null || path.isBlank())
                ? serverRoot
                : new File(path).getCanonicalFile();
        if (!target.toPath().startsWith(serverRoot.toPath())) {
            throw new IOException("Access denied: path outside server directory");
        }
        return target;
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

    private TransferEndpoint normalizeTransferEndpoint(String rawHost, int rawPort) {
        String host = rawHost == null ? "" : rawHost.trim();
        int port = rawPort;
        if (host.isEmpty()) {
            return null;
        }

        // 容错: 若用户把 host:port 或 host:port:port 写进 transferHost，自动拆分端口
        while (true) {
            int colon = host.lastIndexOf(':');
            if (colon <= 0 || colon >= host.length() - 1) {
                break;
            }
            String hostPart = host.substring(0, colon).trim();
            String portPart = host.substring(colon + 1).trim();
            try {
                int parsed = Integer.parseInt(portPart);
                if (hostPart.isEmpty() || parsed < 1 || parsed > 65535) {
                    break;
                }
                host = hostPart;
                port = parsed;
            } catch (NumberFormatException ignored) {
                break;
            }
        }

        boolean bracketedIpv6 = host.startsWith("[") && host.endsWith("]") && host.length() > 2;
        if (bracketedIpv6) {
            host = host.substring(1, host.length() - 1);
        }
        if (!bracketedIpv6 && host.indexOf(':') >= 0) {
            LOGGER.warn("[Bridge] transferHost={} 含非法冒号，忽略本服转移地址上报", rawHost);
            return null;
        }

        if (port < 1 || port > 65535) {
            LOGGER.warn("[Bridge] transferPort={} 非法，忽略本服转移地址上报", port);
            return null;
        }
        if (host.isBlank()) {
            return null;
        }
        String normalizedHost = bracketedIpv6 ? ("[" + host + "]") : host;
        return new TransferEndpoint(normalizedHost, port);
    }

    private static final class TransferEndpoint {
        private final String host;
        private final int port;

        private TransferEndpoint(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }

    // ==================== 连接管理 ====================
    
    private void handleDisconnect() {
        connected.set(false);
        
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
            heartbeatExecutor = null;
        }

        try {
            if (reader != null) reader.close();
        } catch (Exception ignored) {
        }
        try {
            if (writer != null) writer.close();
        } catch (Exception ignored) {
        }
        try {
            if (socket != null) socket.close();
        } catch (Exception ignored) {
        }

        reader = null;
        writer = null;
        socket = null;
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

        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
            heartbeatExecutor = null;
        }
        if (messageExecutor != null) {
            messageExecutor.shutdownNow();
            messageExecutor = null;
        }

        pendingRequests.forEach((id, future) -> future.complete(""));
        pendingRequests.clear();
        handleDisconnect();
        
        LOGGER.info("[Bridge] 已断开与 Alpha Core 的连接");
    }
    
    public boolean isConnected() {
        return connected.get();
    }

    // ==================== P0: Alpha 数据查询 ====================

    /**
     * P0: 查询玩家 UUID 当前禁言到期时间
     * 非阻塞：优先返回缓存，缓存过期时异步刷新
     * @return 0=未禁言，-1=永久禁言，其他=到期时间戳(ms)
     */
    public long checkMuteExpiry(String uuid) {
        if (uuid == null || uuid.isEmpty()) return 0L;

        CacheLong cached = muteExpiryCache.get(uuid);
        if (cached == null || !cached.isFresh()) {
            refreshMuteExpiryAsync(uuid);
        }
        return cached != null ? cached.value : 0L;
    }

    /**
     * 预热禁言缓存（玩家上线时调用）
     */
    public void prefetchMuteExpiry(String uuid) {
        if (uuid == null || uuid.isEmpty()) return;
        refreshMuteExpiryAsync(uuid);
    }

    private void refreshMuteExpiryAsync(String uuid) {
        if (!connected.get()) {
            return;
        }
        if (!muteRefreshInFlight.add(uuid)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            long expiry = 0L;
            try {
                JsonObject payload = new JsonObject();
                payload.addProperty("uuid", uuid);
                String result = requestAlpha("check_mute", payload, 500);
                if (result != null && !result.isEmpty()) {
                    expiry = Long.parseLong(result);
                }
            } catch (Exception ignored) {
            } finally {
                muteExpiryCache.put(uuid, new CacheLong(expiry, System.currentTimeMillis()));
                muteRefreshInFlight.remove(uuid);
            }
        });
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

        String requestId = type + "_" + System.currentTimeMillis() + "_" + ThreadLocalRandom.current().nextInt(1000, 10000);
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
