package com.mapbot.alpha.bridge;

import com.mapbot.alpha.config.AlphaConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridge 消息处理器
 * 处理来自 MC 服务端的消息
 * STEP 13: 添加 proxy_response 处理
 */
public class BridgeMessageHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Bridge/Handler");
    
    private String serverId = null;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LOGGER.info("新的 Bridge 连接: {}", ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        LOGGER.debug("收到 Bridge 消息: {}", msg);
        
        try {
            var data = com.mapbot.alpha.utils.JsonUtils.fromJson(msg, java.util.Map.class);
            if (data == null) return;
            
            String type = String.valueOf(data.get("type"));
            
            switch (type) {
                case "register":
                    handleRegister(ctx, data);
                    break;
                case "heartbeat":
                    handleHeartbeat(ctx, data);
                    break;
                case "event":
                    handleEvent(ctx, data);
                    break;
                case "chat":
                    handleChat(data);
                    break;
                case "file_response":
                    handleFileResponse(data);
                    break;
                case "status_update":
                    handleStatusUpdate(data);
                    break;
                case "proxy_response":
                    handleProxyResponse(data);
                    break;
                case "redeem_cdk":
                    handleRedeemCdk(ctx, data);
                    break;
                // P0: Reforged 端数据查询/上报（统一走 Alpha Redis）
                case "check_mute":
                    handleCheckMute(ctx, data);
                    break;
                case "get_qq_by_uuid":
                    handleGetQqByUuid(ctx, data);
                    break;
                case "playtime_add":
                    handlePlaytimeAdd(data);
                    break;
                case "switch_server_request":
                    handleSwitchServerRequest(ctx, data);
                    break;
                default:
                    LOGGER.warn("未知消息类型: {}", type);
            }
        } catch (Exception e) {
            LOGGER.error("处理 Bridge 消息失败: " + msg, e);
        }
    }
    
    private void handleRegister(ChannelHandlerContext ctx, java.util.Map<String, Object> data) {
        serverId = String.valueOf(data.get("serverId"));
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

        ServerRegistry.INSTANCE.register(serverId, ctx.channel(), transferHost, transferPort);
        
        LOGGER.info("服务器已注册: {} (版本: {})", serverId, version);
        if (transferHost != null && transferPort > 0) {
            LOGGER.info("服务器转移地址: {} -> {}:{}", serverId, transferHost, transferPort);
        }
        ctx.writeAndFlush("{\"type\":\"register_ack\",\"success\":true}\n");
        
        String notifyMsg = "[服务器] " + serverId + " 已启动";
        long groupId = AlphaConfig.getPlayerGroupId();
        com.mapbot.alpha.network.OneBotClient.INSTANCE.sendGroupMessage(groupId, notifyMsg);

        // 新子服上线后补齐白名单快照（异步执行，避免阻塞注册回包）
        final String registeredServerId = serverId;
        java.util.concurrent.CompletableFuture.runAsync(() ->
            BridgeProxy.INSTANCE.syncWhitelistSnapshotToServer(registeredServerId)
        );
    }
    
    private void handleHeartbeat(ChannelHandlerContext ctx, java.util.Map<String, Object> data) {
        if (serverId != null) {
            String players = String.valueOf(data.getOrDefault("players", "0"));
            String tps = String.valueOf(data.getOrDefault("tps", "20.0"));
            String memory = String.valueOf(data.getOrDefault("memory", "0MB"));
            
            ServerRegistry.INSTANCE.updateStatus(serverId, players, tps, memory);
        }
        ctx.writeAndFlush("{\"type\":\"heartbeat_ack\"}\n");
    }
    
    private void handleEvent(ChannelHandlerContext ctx, java.util.Map<String, Object> data) {
        String event = String.valueOf(data.get("event"));
        LOGGER.info("[{}] 事件: {}", serverId, event);
        com.mapbot.alpha.network.LogWebSocketHandler.broadcast("[" + serverId + "] " + event);
    }
    
    /**
     * 处理来自 MC 的聊天消息 -> QQ 群
     */
    private void handleChat(java.util.Map<String, Object> data) {
        String player = String.valueOf(data.get("player"));
        String content = String.valueOf(data.get("content"));
        if (player == null || content == null) return;
        
        long groupId = AlphaConfig.getPlayerGroupId();
        String format = AlphaConfig.INSTANCE.getBridgeIngameMsgFormat();
        String qqMsg = format.replace("{player}", player).replace("{content}", content);
        com.mapbot.alpha.network.OneBotClient.INSTANCE.sendGroupMessage(groupId, qqMsg);
    }
    
    private void handleFileResponse(java.util.Map<String, Object> data) {
        String requestId = String.valueOf(data.get("requestId"));
        String error = String.valueOf(data.getOrDefault("error", ""));
        String errorCode = readNonBlank(data.get("errorCode"));
        String rawError = readNonBlank(data.get("rawError"));
        boolean retryable = parseBoolean(data.get("retryable"));
        boolean mappingConflict = parseBoolean(data.get("mappingConflict"));
        
        // 文件列表可能是 "files" 字段 (Array)
        Object filesObj = data.get("files");
        String result;
        if (filesObj != null) {
            result = com.mapbot.alpha.utils.JsonUtils.toJson(filesObj);
        } else {
            result = String.valueOf(data.getOrDefault("content", ""));
        }
        
        com.mapbot.alpha.bridge.BridgeFileProxy.completeRequest(
            requestId,
            result,
            error,
            errorCode,
            rawError,
            retryable,
            mappingConflict
        );
    }
    
    private void handleStatusUpdate(java.util.Map<String, Object> data) {
        if (serverId == null) return;
        
        String players = String.valueOf(data.get("players"));
        String tps = String.valueOf(data.get("tps"));
        String memory = String.valueOf(data.get("memory"));
        
        ServerRegistry.INSTANCE.updateStatus(serverId, players, tps, memory);
    }
    
    /**
     * 处理 Bridge 代理响应
     * STEP 13: 用于命令执行结果返回
     */
    private void handleProxyResponse(java.util.Map<String, Object> data) {
        String requestId = String.valueOf(data.get("requestId"));
        String result = String.valueOf(data.get("result"));
        
        // 传递给 BridgeProxy 完成请求
        BridgeProxy.INSTANCE.completeRequest(requestId, result);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (serverId != null) {
            ServerRegistry.INSTANCE.unregister(serverId);
            LOGGER.info("服务器断开连接: {}", serverId);
            long groupId = AlphaConfig.getPlayerGroupId();
            com.mapbot.alpha.network.OneBotClient.INSTANCE.sendGroupMessage(groupId, "[服务器] " + serverId + " 断开连接");
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent e) {
            if (e.state() == IdleState.READER_IDLE) {
                LOGGER.warn("Bridge 连接超时，断开: {}", serverId);
                ctx.close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("Bridge 连接异常: " + serverId, cause);
        ctx.close();
    }
    
    /**
     * Task #022: 处理来自 Mod 的 CDK 兑换验证请求
     */
    private void handleRedeemCdk(ChannelHandlerContext ctx, java.util.Map<String, Object> data) {
        String requestId = String.valueOf(data.get("requestId"));
        String code = String.valueOf(data.get("code"));
        String uuid = String.valueOf(data.get("uuid"));
        
        String result = BridgeProxy.INSTANCE.redeemCdk(code, uuid);
        
        // 发送响应
        String response = String.format(
            "{\"type\":\"proxy_response\",\"requestId\":\"%s\",\"result\":\"%s\"}\n",
            requestId, escapeJson(result)
        );
        ctx.writeAndFlush(response);
    }
    
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    /**
     * P0: 检查禁言状态（同时触发 Alpha 侧的过期清理）
     * 返回值：0=未禁言，-1=永久禁言，其他=到期时间戳(ms)
     */
    private void handleCheckMute(ChannelHandlerContext ctx, java.util.Map<String, Object> data) {
        String requestId = String.valueOf(data.get("requestId"));
        String uuid = String.valueOf(data.get("uuid"));

        long expiry = 0L;
        if (uuid != null && !uuid.isEmpty()) {
            var dm = com.mapbot.alpha.data.DataManager.INSTANCE;
            if (dm.isMuted(uuid)) {
                expiry = dm.getMuteExpiry(uuid);
            }
        }

        String response = String.format(
            "{\"type\":\"proxy_response\",\"requestId\":\"%s\",\"result\":\"%s\"}\n",
            requestId, String.valueOf(expiry)
        );
        ctx.writeAndFlush(response);
    }

    /**
     * P0: UUID -> QQ 反查
     */
    private void handleGetQqByUuid(ChannelHandlerContext ctx, java.util.Map<String, Object> data) {
        String requestId = String.valueOf(data.get("requestId"));
        String uuid = String.valueOf(data.get("uuid"));

        long qq = -1L;
        if (uuid != null && !uuid.isEmpty()) {
            Long v = com.mapbot.alpha.data.DataManager.INSTANCE.getQQByUUID(uuid);
            if (v != null) qq = v;
        }

        String response = String.format(
            "{\"type\":\"proxy_response\",\"requestId\":\"%s\",\"result\":\"%s\"}\n",
            requestId, String.valueOf(qq)
        );
        ctx.writeAndFlush(response);
    }

    /**
     * P0: 上报在线时长增量（由 Reforged 端触发）
     */
    private void handlePlaytimeAdd(java.util.Map<String, Object> data) {
        String uuid = readNonBlank(data.get("uuid"));
        long deltaMs = parseDurationMs(data.get("deltaMs"));
        if (uuid == null || deltaMs <= 0) {
            if (uuid != null && data.get("deltaMs") != null) {
                LOGGER.warn("忽略非法在线时长上报: uuid={}, deltaMs={}", uuid, data.get("deltaMs"));
            }
            return;
        }
        com.mapbot.alpha.logic.PlaytimeStore.INSTANCE.addPlaytime(uuid, deltaMs);
    }

    /**
     * 处理 Reforged 的 /server 跨服请求
     */
    private void handleSwitchServerRequest(ChannelHandlerContext ctx, java.util.Map<String, Object> data) {
        String requestId = readNonBlank(data.get("requestId"));
        String targetServer = readNonBlank(data.get("targetServer"));
        String playerName = readNonBlank(data.get("playerName"));

        if (requestId == null || requestId.isBlank()) return;
        if (playerName == null) {
            respondProxy(ctx, requestId, "FAIL:玩家名为空");
            return;
        }
        if (targetServer == null) {
            respondProxy(ctx, requestId, "FAIL:目标服务器名为空");
            return;
        }

        String sourceServerId = (serverId == null ? "" : serverId);
        if (sourceServerId.isBlank()) {
            respondProxy(ctx, requestId, "FAIL:源服务器未注册");
            return;
        }

        String resolved = BridgeProxy.resolveServerId(targetServer);
        if (resolved == null) {
            respondProxy(ctx, requestId, "FAIL:未找到服务器 " + targetServer + "，当前在线: " + BridgeProxy.listServerIds());
            return;
        }
        if (resolved.equals(sourceServerId)) {
            respondProxy(ctx, requestId, "FAIL:你已经在服务器 " + resolved);
            return;
        }

        ServerRegistry.ServerInfo targetInfo = ServerRegistry.INSTANCE.getServer(resolved);
        if (targetInfo == null || targetInfo.transferHost == null || targetInfo.transferHost.isBlank() || targetInfo.transferPort <= 0) {
            respondProxy(ctx, requestId, "FAIL:目标服务器未上报可转移地址，请在目标服 mapbot-common.toml 配置 alpha.transferHost/alpha.transferPort");
            return;
        }

        String transferEndpoint = targetInfo.transferHost + ":" + targetInfo.transferPort;
        String result = BridgeProxy.INSTANCE.sendRequestToServer(
            sourceServerId,
            "switch_server",
            playerName,
            transferEndpoint
        );

        if (result == null || result.isBlank()) {
            respondProxy(ctx, requestId, "FAIL:源服务器无响应");
            return;
        }
        respondProxy(ctx, requestId, result);
    }

    private void respondProxy(ChannelHandlerContext ctx, String requestId, String result) {
        String response = String.format(
            "{\"type\":\"proxy_response\",\"requestId\":\"%s\",\"result\":\"%s\"}\n",
            requestId, escapeJson(result)
        );
        ctx.writeAndFlush(response);
    }

    private String readNonBlank(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) return null;
        return s;
    }

    private long parseDurationMs(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number n) {
            return Math.max(0L, Math.round(n.doubleValue()));
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) return 0L;
        try {
            return Math.max(0L, Long.parseLong(s));
        } catch (NumberFormatException ignored) {
        }
        try {
            return Math.max(0L, Math.round(Double.parseDouble(s)));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private int parsePort(Object value, int fallback) {
        if (value == null) return fallback;
        if (value instanceof Number n) {
            return n.intValue();
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) return fallback;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ignored) {
        }
        try {
            return (int) Math.round(Double.parseDouble(s));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private boolean parseBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        String s = String.valueOf(value).trim();
        return "true".equalsIgnoreCase(s) || "1".equals(s);
    }

    private HostPort normalizeTransferEndpoint(String host, int port) {
        if (host == null || host.isBlank()) return null;

        String h = host.trim();
        int p = port;
        // 容错: 允许误填 host:port 或 host:port:port，连续剥离尾部端口片段
        while (true) {
            int colon = h.lastIndexOf(':');
            if (colon <= 0 || colon >= h.length() - 1) {
                break;
            }
            String hostPart = h.substring(0, colon).trim();
            String portPart = h.substring(colon + 1).trim();
            try {
                int parsed = Integer.parseInt(portPart);
                if (hostPart.isEmpty() || parsed <= 0 || parsed > 65535) {
                    break;
                }
                h = hostPart;
                p = parsed;
            } catch (NumberFormatException ignored) {
                break;
            }
        }
        boolean bracketedIpv6 = h.startsWith("[") && h.endsWith("]") && h.length() > 2;
        if (bracketedIpv6) {
            h = h.substring(1, h.length() - 1);
        }
        if (!bracketedIpv6 && h.indexOf(':') >= 0) return null;
        if (h.isBlank() || p <= 0 || p > 65535) {
            return null;
        }
        String normalizedHost = bracketedIpv6 ? ("[" + h + "]") : h;
        return new HostPort(normalizedHost, p);
    }

    private static final class HostPort {
        private final String host;
        private final int port;

        private HostPort(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }
}
