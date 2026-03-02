package com.mapbot.alpha.bridge;

import com.mapbot.alpha.config.AlphaConfig;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridge 路由派发器 (Task #05: 策略派发模式)
 *
 * 取代原 BridgeMessageHandler 中的巨型 switch-case，
 * 采用 Handler 注册表模式实现报文类型到处理器的解耦映射。
 */
public class BridgeRouteDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Bridge/Dispatcher");
    public static final BridgeRouteDispatcher INSTANCE = new BridgeRouteDispatcher();

    private final Map<String, BridgePacketHandler> handlers = new ConcurrentHashMap<>();

    private BridgeRouteDispatcher() {
        registerBuiltinHandlers();
    }

    // ─────────────────── 公共 API ───────────────────

    /**
     * 注册一个报文处理器（可在运行时动态扩展）
     */
    public void register(String type, BridgePacketHandler handler) {
        handlers.put(type, handler);
    }

    /**
     * 派发报文到对应处理器
     */
    public void dispatch(ChannelHandlerContext ctx, String serverId, String type, Map<String, Object> data) {
        BridgePacketHandler handler = handlers.get(type);
        if (handler != null) {
            handler.handle(ctx, serverId, data);
        } else {
            LOGGER.warn("未知消息类型: {}", type);
        }
    }

    // ─────────────────── 解析工具 ───────────────────

    /**
     * 获取子服绑定的目标群号（优先子服声明，回退全局默认）
     */
    public static long resolveGroupId(String serverId) {
        if (serverId != null) {
            long targetGroupId = ServerRegistry.INSTANCE.getTargetGroupId(serverId);
            if (targetGroupId > 0) return targetGroupId;
        }
        return AlphaConfig.getPlayerGroupId();
    }

    static String readNonBlank(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) return null;
        return s;
    }

    static long parseDurationMs(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number n) {
            return Math.max(0L, Math.round(n.doubleValue()));
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) return 0L;
        try { return Math.max(0L, Long.parseLong(s)); } catch (NumberFormatException ignored) {}
        try { return Math.max(0L, Math.round(Double.parseDouble(s))); } catch (NumberFormatException ignored) { return 0L; }
    }

    static long parseLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number n) return n.longValue();
        try { return Long.parseLong(String.valueOf(value).trim()); } catch (NumberFormatException ignored) { return 0L; }
    }

    static int parsePort(Object value, int fallback) {
        if (value == null) return fallback;
        if (value instanceof Number n) return n.intValue();
        String s = String.valueOf(value).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) return fallback;
        try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        try { return (int) Math.round(Double.parseDouble(s)); } catch (NumberFormatException ignored) { return fallback; }
    }

    static boolean parseBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        String s = String.valueOf(value).trim();
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }

    static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    static void respondProxy(ChannelHandlerContext ctx, String requestId, String result) {
        String response = String.format(
            "{\"type\":\"proxy_response\",\"requestId\":\"%s\",\"result\":\"%s\"}\n",
            requestId, escapeJson(result)
        );
        ctx.writeAndFlush(response);
    }

    // ─────────────────── 内置 Handler 注册 ───────────────────

    private void registerBuiltinHandlers() {
        register("register",             this::handleRegister);
        register("heartbeat",            this::handleHeartbeat);
        register("event",                this::handleEvent);
        register("chat",                 this::handleChat);
        register("file_response",        this::handleFileResponse);
        register("status_update",        this::handleStatusUpdate);
        register("proxy_response",       this::handleProxyResponse);
        register("redeem_cdk",           this::handleRedeemCdk);
        register("check_mute",           this::handleCheckMute);
        register("get_qq_by_uuid",       this::handleGetQqByUuid);
        register("playtime_add",         this::handlePlaytimeAdd);
        register("switch_server_request", this::handleSwitchServerRequest);
    }

    // ─────────────────── 各报文处理器 ───────────────────

    /**
     * 服务器注册握手（增加 targetGroupId 解析）
     */
    private void handleRegister(ChannelHandlerContext ctx, String ignoredServerId, Map<String, Object> data) {
        String newServerId = String.valueOf(data.get("serverId"));
        String version = String.valueOf(data.getOrDefault("version", "1.0"));

        String transferHost = readNonBlank(data.get("transferHost"));
        int transferPort = parsePort(data.get("transferPort"), 0);
        HostPort normalized = normalizeTransferEndpoint(transferHost, transferPort);
        if (normalized != null) {
            transferHost = normalized.host;
            transferPort = normalized.port;
        }
        if (transferPort <= 0 || transferPort > 65535) {
            transferPort = 0;
        }

        // Task #05: 解析子服声明的目标群号（无损降级：未发送则为 0，回退全局默认）
        long targetGroupId = parseLong(data.get("targetGroupId"));

        ServerRegistry.INSTANCE.register(newServerId, ctx.channel(), transferHost, transferPort, targetGroupId);

        LOGGER.info("服务器已注册: {} (版本: {}, 目标群: {})", newServerId, version,
            targetGroupId > 0 ? targetGroupId : "默认");
        if (transferHost != null && transferPort > 0) {
            LOGGER.info("服务器转移地址: {} -> {}:{}", newServerId, transferHost, transferPort);
        }
        ctx.writeAndFlush("{\"type\":\"register_ack\",\"success\":true}\n");

        // 上线通知发到该子服绑定的群
        long groupId = resolveGroupId(newServerId);
        com.mapbot.alpha.network.OneBotClient.INSTANCE.sendGroupMessage(groupId, "[服务器] " + newServerId + " 已启动");

        // 新子服上线后补齐白名单快照
        java.util.concurrent.CompletableFuture.runAsync(() ->
            BridgeProxy.INSTANCE.syncWhitelistSnapshotToServer(newServerId)
        );
    }

    private void handleHeartbeat(ChannelHandlerContext ctx, String serverId, Map<String, Object> data) {
        if (serverId != null) {
            String players = String.valueOf(data.getOrDefault("players", "0"));
            String tps = String.valueOf(data.getOrDefault("tps", "20.0"));
            String memory = String.valueOf(data.getOrDefault("memory", "0MB"));
            ServerRegistry.INSTANCE.updateStatus(serverId, players, tps, memory);
        }
        ctx.writeAndFlush("{\"type\":\"heartbeat_ack\"}\n");
    }

    private void handleEvent(ChannelHandlerContext ctx, String serverId, Map<String, Object> data) {
        String event = String.valueOf(data.get("event"));
        LOGGER.info("[{}] 事件: {}", serverId, event);
        com.mapbot.alpha.network.LogWebSocketHandler.broadcast("[" + serverId + "] " + event);
    }

    /**
     * 聊天转发（Task #05: 动态群组派发）
     */
    private void handleChat(ChannelHandlerContext ctx, String serverId, Map<String, Object> data) {
        String player = String.valueOf(data.get("player"));
        String content = String.valueOf(data.get("content"));
        if (player == null || content == null) return;

        // 优先使用子服握手时声明的 targetGroupId，否则回退全局默认
        long groupId = resolveGroupId(serverId);
        String serverTag = (serverId != null && !serverId.isBlank()) ? serverId : "Server";
        String qqMsg = "[" + serverTag + "] " + player + ": " + content;
        com.mapbot.alpha.network.OneBotClient.INSTANCE.sendGroupMessage(groupId, qqMsg);
    }

    private void handleFileResponse(ChannelHandlerContext ctx, String serverId, Map<String, Object> data) {
        String requestId = String.valueOf(data.get("requestId"));
        String error = String.valueOf(data.getOrDefault("error", ""));
        String errorCode = readNonBlank(data.get("errorCode"));
        String rawError = readNonBlank(data.get("rawError"));
        boolean retryable = parseBoolean(data.get("retryable"));
        boolean mappingConflict = parseBoolean(data.get("mappingConflict"));

        Object filesObj = data.get("files");
        String result;
        if (filesObj != null) {
            result = com.mapbot.alpha.utils.JsonUtils.toJson(filesObj);
        } else {
            result = String.valueOf(data.getOrDefault("content", ""));
        }

        BridgeFileProxy.completeRequest(requestId, result, error, errorCode, rawError, retryable, mappingConflict);
    }

    private void handleStatusUpdate(ChannelHandlerContext ctx, String serverId, Map<String, Object> data) {
        if (serverId == null) return;
        String players = String.valueOf(data.get("players"));
        String tps = String.valueOf(data.get("tps"));
        String memory = String.valueOf(data.get("memory"));
        ServerRegistry.INSTANCE.updateStatus(serverId, players, tps, memory);
    }

    private void handleProxyResponse(ChannelHandlerContext ctx, String serverId, Map<String, Object> data) {
        String requestId = String.valueOf(data.get("requestId"));
        String result = String.valueOf(data.get("result"));
        BridgeProxy.INSTANCE.completeRequest(requestId, result);
    }

    private void handleRedeemCdk(ChannelHandlerContext ctx, String serverId, Map<String, Object> data) {
        String requestId = String.valueOf(data.get("requestId"));
        String code = String.valueOf(data.get("code"));
        String uuid = String.valueOf(data.get("uuid"));
        String result = BridgeProxy.INSTANCE.redeemCdk(code, uuid);
        respondProxy(ctx, requestId, result);
    }

    private void handleCheckMute(ChannelHandlerContext ctx, String serverId, Map<String, Object> data) {
        String requestId = String.valueOf(data.get("requestId"));
        String uuid = String.valueOf(data.get("uuid"));
        long expiry = 0L;
        if (uuid != null && !uuid.isEmpty()) {
            var dm = com.mapbot.alpha.data.DataManager.INSTANCE;
            if (dm.isMuted(uuid)) {
                expiry = dm.getMuteExpiry(uuid);
            }
        }
        respondProxy(ctx, requestId, String.valueOf(expiry));
    }

    private void handleGetQqByUuid(ChannelHandlerContext ctx, String serverId, Map<String, Object> data) {
        String requestId = String.valueOf(data.get("requestId"));
        String uuid = String.valueOf(data.get("uuid"));
        long qq = -1L;
        if (uuid != null && !uuid.isEmpty()) {
            Long v = com.mapbot.alpha.data.DataManager.INSTANCE.getQQByUUID(uuid);
            if (v != null) qq = v;
        }
        respondProxy(ctx, requestId, String.valueOf(qq));
    }

    /**
     * 在线时长上报（Task #03 内联 Redis 逻辑原样迁入）
     */
    private void handlePlaytimeAdd(ChannelHandlerContext ctx, String serverId, Map<String, Object> data) {
        String uuid = readNonBlank(data.get("uuid"));
        long deltaMs = parseDurationMs(data.get("deltaMs"));
        if (uuid == null || deltaMs <= 0) {
            if (uuid != null && data.get("deltaMs") != null) {
                LOGGER.warn("忽略非法在线时长上报: uuid={}, deltaMs={}", uuid, data.get("deltaMs"));
            }
            return;
        }

        var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
        if (redis.isEnabled()) {
            try {
                String key = "mapbot:playtime:" + uuid;
                String existing = redis.execute(jedis -> jedis.get(key));

                java.util.Map<String, Object> record;
                if (existing != null && !existing.isEmpty()) {
                    try {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> parsed = com.mapbot.alpha.utils.JsonUtils.fromJson(existing, java.util.Map.class);
                        record = parsed != null ? new java.util.LinkedHashMap<>(parsed) : new java.util.LinkedHashMap<>();
                    } catch (Exception e) {
                        record = new java.util.LinkedHashMap<>();
                    }
                } else {
                    record = new java.util.LinkedHashMap<>();
                }

                record.put("dailyMs", parseLong(record.get("dailyMs")) + deltaMs);
                record.put("weeklyMs", parseLong(record.get("weeklyMs")) + deltaMs);
                record.put("monthlyMs", parseLong(record.get("monthlyMs")) + deltaMs);
                record.put("totalMs", parseLong(record.get("totalMs")) + deltaMs);
                record.put("lastReset", java.time.LocalDate.now(java.time.ZoneId.systemDefault()).toString());

                String json = com.mapbot.alpha.utils.JsonUtils.toJson(record);
                redis.execute(jedis -> jedis.set(key, json));
            } catch (Exception e) {
                LOGGER.error("在线时长 Redis 写入失败: uuid={}", uuid, e);
            }
        } else {
            LOGGER.debug("Redis 未启用，忽略在线时长上报: uuid={}", uuid);
        }
    }

    /**
     * 跨服传送请求（Task #05: 增强错误返回机制）
     */
    private void handleSwitchServerRequest(ChannelHandlerContext ctx, String serverId, Map<String, Object> data) {
        String requestId = readNonBlank(data.get("requestId"));
        String targetServer = readNonBlank(data.get("targetServer"));
        String playerName = readNonBlank(data.get("playerName"));

        if (requestId == null || requestId.isBlank()) return;

        if (playerName == null) {
            respondProxy(ctx, requestId, "FAIL:INVALID_PARAM:玩家名为空");
            return;
        }
        if (targetServer == null) {
            respondProxy(ctx, requestId, "FAIL:INVALID_PARAM:目标服务器名为空");
            return;
        }

        String sourceServerId = (serverId == null ? "" : serverId);
        if (sourceServerId.isBlank()) {
            respondProxy(ctx, requestId, "FAIL:NOT_REGISTERED:源服务器未注册，请确认 Bridge 握手已完成");
            return;
        }

        String resolved = BridgeProxy.resolveServerId(targetServer);
        if (resolved == null) {
            String available = BridgeProxy.listServerIds();
            respondProxy(ctx, requestId, "FAIL:SERVER_NOT_FOUND:未找到服务器 " + targetServer + "，当前在线: " + available);
            return;
        }
        if (resolved.equals(sourceServerId)) {
            respondProxy(ctx, requestId, "FAIL:SAME_SERVER:你已经在服务器 " + resolved);
            return;
        }

        ServerRegistry.ServerInfo targetInfo = ServerRegistry.INSTANCE.getServer(resolved);
        if (targetInfo == null || targetInfo.transferHost == null || targetInfo.transferHost.isBlank() || targetInfo.transferPort <= 0) {
            respondProxy(ctx, requestId, "FAIL:NO_TRANSFER_ADDR:目标服务器 " + resolved
                + " 未上报可转移地址，请在目标服 mapbot-common.toml 配置 alpha.transferHost/alpha.transferPort");
            return;
        }

        String transferEndpoint = targetInfo.transferHost + ":" + targetInfo.transferPort;
        String result = BridgeProxy.INSTANCE.sendRequestToServer(
            sourceServerId, "switch_server", playerName, transferEndpoint
        );

        if (result == null || result.isBlank()) {
            respondProxy(ctx, requestId, "FAIL:NO_RESPONSE:源服务器 " + sourceServerId + " 无响应，请稍后重试");
            return;
        }
        respondProxy(ctx, requestId, result);
    }

    // ─────────────────── 端点规范化工具 ───────────────────

    private HostPort normalizeTransferEndpoint(String host, int port) {
        if (host == null || host.isBlank()) return null;
        String h = host.trim();
        int p = port;
        while (true) {
            int colon = h.lastIndexOf(':');
            if (colon <= 0 || colon >= h.length() - 1) break;
            String hostPart = h.substring(0, colon).trim();
            String portPart = h.substring(colon + 1).trim();
            try {
                int parsed = Integer.parseInt(portPart);
                if (hostPart.isEmpty() || parsed <= 0 || parsed > 65535) break;
                h = hostPart;
                p = parsed;
            } catch (NumberFormatException ignored) { break; }
        }
        boolean bracketedIpv6 = h.startsWith("[") && h.endsWith("]") && h.length() > 2;
        if (bracketedIpv6) h = h.substring(1, h.length() - 1);
        if (!bracketedIpv6 && h.indexOf(':') >= 0) return null;
        if (h.isBlank() || p <= 0 || p > 65535) return null;
        String normalizedHost = bracketedIpv6 ? ("[" + h + "]") : h;
        return new HostPort(normalizedHost, p);
    }

    private record HostPort(String host, int port) {}
}
