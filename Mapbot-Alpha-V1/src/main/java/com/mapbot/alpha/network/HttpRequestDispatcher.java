package com.mapbot.alpha.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * HTTP 请求分发器
 * 处理静态文件、API 请求和 WebSocket 升级
 */
public class HttpRequestDispatcher extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Network/Http");

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        String uri = req.uri();
        
        // WebSocket 升级请求
        if (uri.equals("/ws") && isWebSocketUpgrade(req)) {
            handleWebSocketUpgrade(ctx, req);
            return;
        }
        
        // API 请求
        if (uri.startsWith("/api/")) {
            // 系统状态 API (BUG #6, #7)
            if (uri.equals("/api/status")) {
                sendJson(ctx, getStatusJson());
                return;
            }
            // 配置 API (#10 设置页面)
            if (uri.equals("/api/config")) {
                if (req.method() == HttpMethod.GET) {
                    sendJson(ctx, getConfigJson());
                } else if (req.method() == HttpMethod.POST) {
                    handleConfigSave(ctx, req);
                }
                return;
            }
            // 服务器列表 API (STEP 10)
            if (uri.equals("/api/servers")) {
                sendJson(ctx, com.mapbot.alpha.bridge.ServerRegistry.INSTANCE.toJson());
                return;
            }
            // 服务器命令 API (问题 #4)
            if (uri.matches("/api/servers/.+/command") && req.method() == HttpMethod.POST) {
                handleServerCommand(ctx, req, uri);
                return;
            }
            // 跨服文件 API (STEP 9)
            if (uri.startsWith("/api/remote/")) {
                RemoteFileApiHandler.handle(ctx, req);
                return;
            }
            // 本地文件 API (STEP 8)
            FileApiHandler.handle(ctx, req);
            return;
        }
        
        // 静态资源请求
        handleStaticResource(ctx, req);
    }
    
    private boolean isWebSocketUpgrade(FullHttpRequest req) {
        return req.headers().contains(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET, true);
    }
    
    private void handleWebSocketUpgrade(ChannelHandlerContext ctx, FullHttpRequest req) {
        String wsUrl = "ws://" + req.headers().get(HttpHeaderNames.HOST) + "/ws";
        WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(wsUrl, null, true);
        WebSocketServerHandshaker handshaker = factory.newHandshaker(req);
        
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
            // 添加 WebSocket 帧处理器
            ctx.pipeline().addLast(new LogWebSocketHandler());
            LOGGER.info("WebSocket 握手完成: {}", ctx.channel().remoteAddress());
        }
    }
    
    private void handleStaticResource(ChannelHandlerContext ctx, FullHttpRequest req) {
        if (req.method() != HttpMethod.GET) {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
            return;
        }

        String uri = req.uri();
        String resourcePath;
        String contentType;

        // 路由映射
        if ("/".equals(uri) || "/index.html".equals(uri)) {
            resourcePath = "/web/index.html";
            contentType = "text/html; charset=UTF-8";
        } else if ("/tailwind.js".equals(uri)) {
            resourcePath = "/web/tailwind.js";
            contentType = "application/javascript";
        } else {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }

        try {
            var is = getClass().getResourceAsStream(resourcePath);
            if (is == null) {
                LOGGER.warn("未找到资源: {}", resourcePath);
                sendError(ctx, HttpResponseStatus.NOT_FOUND);
                return;
            }

            byte[] content = is.readAllBytes();
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
            response.content().writeBytes(content);
            HttpUtil.setContentLength(response, content.length);
            ctx.writeAndFlush(response);
        } catch (Exception e) {
            LOGGER.error("发送资源失败: " + uri, e);
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        ctx.writeAndFlush(response);
    }
    
    private void sendJson(ChannelHandlerContext ctx, String json) {
        byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK, 
                io.netty.buffer.Unpooled.wrappedBuffer(bytes));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        HttpUtil.setContentLength(response, bytes.length);
        ctx.writeAndFlush(response);
    }
    
    /**
     * 获取系统状态 JSON (BUG #6, #7 修复)
     */
    private String getStatusJson() {
        var pm = com.mapbot.alpha.process.ProcessManager.INSTANCE;
        boolean running = pm.isRunning();
        long uptime = running ? pm.getUptimeMs() : 0;
        int wsCount = LogWebSocketHandler.getConnectionCount();
        int bridgeCount = com.mapbot.alpha.bridge.ServerRegistry.INSTANCE.getServerCount();
        
        return "{\"mcRunning\":" + running + 
               ",\"mcUptime\":" + uptime +
               ",\"wsConnections\":" + wsCount +
               ",\"bridgeConnections\":" + bridgeCount + "}";
    }
    
    /**
     * 获取配置 JSON (#10 设置页面)
     */
    private String getConfigJson() {
        var cfg = com.mapbot.alpha.config.AlphaConfig.INSTANCE;
        return "{\"wsUrl\":\"" + escapeJson(cfg.getWsUrl()) + "\"," +
               "\"reconnectInterval\":" + cfg.getReconnectInterval() + "," +
               "\"playerGroupId\":\"" + cfg.getPlayerGroupId() + "\"," +
               "\"adminGroupId\":\"" + cfg.getAdminGroupId() + "\"," +
               "\"botQQ\":\"" + cfg.getBotQQ() + "\"," +
               "\"debugMode\":" + cfg.isDebugMode() + "}";
    }
    
    /**
     * 保存配置 (#10 设置页面)
     */
    private void handleConfigSave(ChannelHandlerContext ctx, FullHttpRequest req) {
        try {
            String body = req.content().toString(StandardCharsets.UTF_8);
            var cfg = com.mapbot.alpha.config.AlphaConfig.INSTANCE;
            
            // 简易 JSON 解析
            String wsUrl = extractJsonString(body, "wsUrl");
            if (!wsUrl.isEmpty()) cfg.setWsUrl(wsUrl);
            
            String playerGroup = extractJsonString(body, "playerGroupId");
            if (!playerGroup.isEmpty()) cfg.setPlayerGroupId(Long.parseLong(playerGroup));
            
            String adminGroup = extractJsonString(body, "adminGroupId");
            if (!adminGroup.isEmpty()) cfg.setAdminGroupId(Long.parseLong(adminGroup));
            
            String botQQ = extractJsonString(body, "botQQ");
            if (!botQQ.isEmpty()) cfg.setBotQQ(Long.parseLong(botQQ));
            
            cfg.save();
            sendJson(ctx, "{\"success\":true}");
            LOGGER.info("配置已通过 Web 面板更新");
        } catch (Exception e) {
            sendJson(ctx, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }
    
    private static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : "";
    }
    
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
    
    /**
     * 发送命令到子服务器 (问题 #4)
     */
    private void handleServerCommand(ChannelHandlerContext ctx, FullHttpRequest req, String uri) {
        try {
            // 解析 serverId: /api/servers/{serverId}/command
            String path = uri.substring("/api/servers/".length());
            String serverId = path.substring(0, path.indexOf("/"));
            
            String body = req.content().toString(StandardCharsets.UTF_8);
            String command = extractJsonString(body, "command");
            
            if (command.isEmpty()) {
                sendJson(ctx, "{\"success\":false,\"error\":\"Command is empty\"}");
                return;
            }
            
            // 获取服务器连接
            var server = com.mapbot.alpha.bridge.ServerRegistry.INSTANCE.getServer(serverId);
            if (server == null || !server.isOnline()) {
                sendJson(ctx, "{\"success\":false,\"error\":\"Server not connected\"}");
                return;
            }
            
            // 发送 execute_command 请求到子服务器
            String json = String.format(
                "{\"type\":\"execute_command\",\"requestId\":\"%s\",\"arg1\":\"%s\"}",
                System.currentTimeMillis(), escapeJson(command));
            server.channel.writeAndFlush(json + "\n");
            
            sendJson(ctx, "{\"success\":true}");
            LOGGER.info("[命令] 发送到 {}: {}", serverId, command);
        } catch (Exception e) {
            sendJson(ctx, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }
}
