package com.mapbot.alpha.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * HTTP 请求分发器
 * 处理静态文件、API 请求和 WebSocket 升级
 */
public class HttpRequestDispatcher extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Network/Http");
    private static final String REDIS_PASSWORD_MASK = "********";

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        String uri = req.uri();
        QueryStringDecoder uriDecoder = new QueryStringDecoder(uri);
        String path = uriDecoder.path();
        
        // WebSocket 升级请求（必须认证）
        if (path.equals("/ws") && isWebSocketUpgrade(req)) {
            String wsToken = extractRequestToken(req, uriDecoder);
            if (!com.mapbot.alpha.security.AuthManager.INSTANCE.validateToken(wsToken)) {
                sendUnauthorized(ctx);
                return;
            }
            handleWebSocketUpgrade(ctx, req, wsToken, path);
            return;
        }
        
        // 登录 API (无需认证)
        if (path.equals("/api/login") && req.method() == HttpMethod.POST) {
            handleLogin(ctx, req);
            return;
        }
        
        // 静态资源 (无需认证)
        if (!path.startsWith("/api/")) {
            handleStaticResource(ctx, req);
            return;
        }
        
        // === API 认证拦截 ===
        String token = extractBearerToken(req.headers().get(HttpHeaderNames.AUTHORIZATION));
        
        if (!com.mapbot.alpha.security.AuthManager.INSTANCE.validateToken(token)) {
            sendUnauthorized(ctx);
            return;
        }
        
        // API 请求 (已认证)
        if (path.startsWith("/api/")) {
            // Metrics 历史数据 API
            if (path.matches("/api/metrics/.+/history")) {
                handleMetricsHistory(ctx, path);
                return;
            }
            // 系统状态 API (BUG #6, #7)
            if (path.equals("/api/status")) {
                sendJson(ctx, getStatusJson());
                return;
            }
            // 配置 API (#10 设置页面)
            if (path.equals("/api/config")) {
                if (!com.mapbot.alpha.security.AuthManager.INSTANCE.hasContractPermission(token, com.mapbot.alpha.security.ContractRole.OWNER)) {
                    sendForbidden(ctx, "Permission denied. OWNER required.");
                    return;
                }
                if (req.method() == HttpMethod.GET) {
                    sendJson(ctx, getConfigJson());
                } else if (req.method() == HttpMethod.POST) {
                    handleConfigSave(ctx, req);
                }
                return;
            }
            // 服务器列表 API (STEP 10)
            if (path.equals("/api/servers")) {
                sendJson(ctx, com.mapbot.alpha.bridge.ServerRegistry.INSTANCE.toJson());
                return;
            }
            // 服务器命令 API (问题 #4)
            if (path.matches("/api/servers/.+/command") && req.method() == HttpMethod.POST) {
                if (!com.mapbot.alpha.security.AuthManager.INSTANCE.hasContractPermission(token, com.mapbot.alpha.security.ContractRole.ADMIN)) {
                    sendForbidden(ctx, "Permission denied. ADMIN required.");
                    return;
                }
                handleServerCommand(ctx, req, path);
                return;
            }
            // 服务器重启/停止 API
            if (path.matches("/api/servers/.+/restart") && req.method() == HttpMethod.POST) {
                if (!com.mapbot.alpha.security.AuthManager.INSTANCE.hasContractPermission(token, com.mapbot.alpha.security.ContractRole.ADMIN)) {
                    sendForbidden(ctx, "Permission denied. ADMIN required.");
                    return;
                }
                handleServerControl(ctx, path, "restart");
                return;
            }
            if (path.matches("/api/servers/.+/stop") && req.method() == HttpMethod.POST) {
                if (!com.mapbot.alpha.security.AuthManager.INSTANCE.hasContractPermission(token, com.mapbot.alpha.security.ContractRole.ADMIN)) {
                    sendForbidden(ctx, "Permission denied. ADMIN required.");
                    return;
                }
                handleServerControl(ctx, path, "stop");
                return;
            }
            // 跨服文件 API (STEP 9)
            if (path.startsWith("/api/remote/")) {
                if (!com.mapbot.alpha.security.AuthManager.INSTANCE.hasContractPermission(token, com.mapbot.alpha.security.ContractRole.ADMIN)) {
                    sendForbidden(ctx, "Permission denied. ADMIN required.");
                    return;
                }
                RemoteFileApiHandler.handle(ctx, req);
                return;
            }
            // 用户管理 API (需要 ADMIN 权限)
            if (path.startsWith("/api/users")) {
                handleUsersApi(ctx, req, path, token);
                return;
            }
            // MapBot 数据管理 API (需要 ADMIN 权限)
            if (path.startsWith("/api/mapbot")) {
                handleMapbotDataApi(ctx, req, path, token);
                return;
            }
            // 本地文件 API (STEP 8)
            FileApiHandler.handle(ctx, req);
            return;
        }
        
        sendError(ctx, HttpResponseStatus.NOT_FOUND);
    }
    
    private boolean isWebSocketUpgrade(FullHttpRequest req) {
        return req.headers().contains(HttpHeaderNames.UPGRADE, HttpHeaderValues.WEBSOCKET, true);
    }

    private String extractRequestToken(FullHttpRequest req, QueryStringDecoder decoder) {
        String token = extractBearerToken(req.headers().get(HttpHeaderNames.AUTHORIZATION));
        if (token != null) return token;

        token = firstQueryValue(decoder, "token");
        if (token != null) return token;

        token = firstQueryValue(decoder, "access_token");
        if (token != null) return token;

        return null;
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader == null) return null;
        if (!authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private String firstQueryValue(QueryStringDecoder decoder, String key) {
        var values = decoder.parameters().get(key);
        if (values == null || values.isEmpty()) return null;
        String value = values.get(0);
        if (value == null) return null;
        value = value.trim();
        return value.isEmpty() ? null : value;
    }
    
    private void handleWebSocketUpgrade(ChannelHandlerContext ctx, FullHttpRequest req, String token, String path) {
        String wsUrl = "ws://" + req.headers().get(HttpHeaderNames.HOST) + path;
        WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(wsUrl, null, true);
        WebSocketServerHandshaker handshaker = factory.newHandshaker(req);
        
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
            // 添加 WebSocket 帧处理器
            ctx.pipeline().addLast(new LogWebSocketHandler(token));
            String username = com.mapbot.alpha.security.AuthManager.INSTANCE.getUsername(token);
            LOGGER.info("WebSocket 握手完成: {} user={}", ctx.channel().remoteAddress(), username);
        }
    }
    
    private void handleStaticResource(ChannelHandlerContext ctx, FullHttpRequest req) {
        if (req.method() != HttpMethod.GET) {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
            return;
        }

        String uri = new QueryStringDecoder(req.uri()).path();
        String resourcePath;
        String contentType;

        // 路由映射 (暂时默认使用原 HTML，Vue 前端需调试)
        if ("/".equals(uri) || "/index.html".equals(uri)) {
            resourcePath = "/web/index.html";
            contentType = "text/html; charset=UTF-8";
        } else if ("/users.html".equals(uri)) {
            resourcePath = "/web/users.html";
            contentType = "text/html; charset=UTF-8";
        } else if (uri.startsWith("/vue") || uri.startsWith("/assets/")) {
            // Vue 构建的静态资源 (/vue 或 /assets/*)
            String vuePath = uri.startsWith("/vue") ? uri.substring(4) : uri;
            if (vuePath.isEmpty() || vuePath.equals("/")) vuePath = "/index.html";
            resourcePath = "/web-vue" + vuePath;
            contentType = getMimeType(vuePath);
        } else if ("/tailwind.js".equals(uri)) {
            resourcePath = "/web/tailwind.js";
            contentType = "application/javascript";
        } else if ("/vite.svg".equals(uri)) {
            resourcePath = "/web-vue/vite.svg";
            contentType = "image/svg+xml";
        } else {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }

        boolean spaFallbackCandidate = uri.startsWith("/vue/")
                && !uri.contains("/assets/")
                && !uri.substring(uri.lastIndexOf('/') + 1).contains(".");

        try {
            // 尝试多种方式加载资源
            var is = getClass().getResourceAsStream(resourcePath);
            if (is == null) {
                // 备选：使用 ClassLoader
                is = getClass().getClassLoader().getResourceAsStream(resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath);
            }
            if (is == null && spaFallbackCandidate) {
                resourcePath = "/web-vue/index.html";
                contentType = "text/html; charset=UTF-8";
                is = getClass().getResourceAsStream(resourcePath);
                if (is == null) {
                    is = getClass().getClassLoader().getResourceAsStream(resourcePath.substring(1));
                }
            }
            if (is == null) {
                LOGGER.warn("未找到资源: {} (尝试了 Class 和 ClassLoader)", resourcePath);
                sendError(ctx, HttpResponseStatus.NOT_FOUND);
                return;
            }

            byte[] content = is.readAllBytes();
            is.close();
            
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            response.content().writeBytes(content);
            HttpUtil.setContentLength(response, content.length);
            ctx.writeAndFlush(response);
            LOGGER.debug("发送资源: {} ({} bytes)", resourcePath, content.length);
        } catch (Exception e) {
            LOGGER.error("发送资源失败: " + uri, e);
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 根据文件扩展名获取 MIME 类型
     */
    private String getMimeType(String uri) {
        if (uri.endsWith(".js")) return "application/javascript";
        if (uri.endsWith(".css")) return "text/css";
        if (uri.endsWith(".html")) return "text/html; charset=UTF-8";
        if (uri.endsWith(".json")) return "application/json";
        if (uri.endsWith(".svg")) return "image/svg+xml";
        if (uri.endsWith(".png")) return "image/png";
        if (uri.endsWith(".jpg") || uri.endsWith(".jpeg")) return "image/jpeg";
        if (uri.endsWith(".woff2")) return "font/woff2";
        if (uri.endsWith(".woff")) return "font/woff";
        return "application/octet-stream";
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
        
        boolean redisConnected = com.mapbot.alpha.database.RedisManager.INSTANCE.isConnected();
        
        return "{\"mcRunning\":" + running + 
               ",\"mcUptime\":" + uptime +
               ",\"wsConnections\":" + wsCount +
               ",\"bridgeConnections\":" + bridgeCount + 
               ",\"redisConnected\":" + redisConnected + "}";
    }
    
    /**
     * 获取配置 JSON (#10 设置页面)
     */
    private String getConfigJson() {
        var cfg = com.mapbot.alpha.config.AlphaConfig.INSTANCE;
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("wsUrl", cfg.getWsUrl());
        data.put("wsToken", maskSecret(cfg.getWsToken()));
        data.put("reconnectInterval", cfg.getReconnectInterval());
        data.put("playerGroupId", cfg.getPlayerGroupId());
        data.put("adminGroupId", cfg.getAdminGroupId());
        data.put("botQQ", cfg.getBotQQ());
        data.put("debugMode", cfg.isDebugMode());
        data.put("bridgeIngameMsgFormat", cfg.getBridgeIngameMsgFormat());
        data.put("adminQQs", cfg.getAdminQQs());
        data.put("redisEnabled", cfg.isRedisEnabled());
        data.put("redisHost", cfg.getRedisHost());
        data.put("redisPort", cfg.getRedisPort());
        data.put("redisPassword", maskSecret(cfg.getRedisPassword()));
        data.put("redisDatabase", cfg.getRedisDatabase());
        
        return com.mapbot.alpha.utils.JsonUtils.toJson(data);
    }
    
    private void handleConfigSave(ChannelHandlerContext ctx, FullHttpRequest req) {
        try {
            String json = req.content().toString(java.nio.charset.StandardCharsets.UTF_8);
            var data = com.mapbot.alpha.utils.JsonUtils.fromJson(json, java.util.Map.class);
            if (data == null) {
                data = new java.util.HashMap<>();
            }
            
            var cfg = com.mapbot.alpha.config.AlphaConfig.INSTANCE;
            if (data.containsKey("wsUrl")) cfg.setWsUrl(String.valueOf(data.get("wsUrl")));
            if (data.containsKey("wsToken")) {
                String submitted = String.valueOf(data.get("wsToken"));
                // 如果前端回传的是掩码，跳过不更新；否则更新 token
                if (!submitted.matches("\\*+") && !"null".equals(submitted)) {
                    cfg.setWsToken(submitted);
                }
            }
            if (data.containsKey("adminQQs")) cfg.setAdminQQs(String.valueOf(data.get("adminQQs")));
            
            if (data.containsKey("redisEnabled")) cfg.setRedisEnabled(Boolean.parseBoolean(String.valueOf(data.get("redisEnabled"))));

            boolean hasRedisUpdate =
                    data.containsKey("redisHost")
                    || data.containsKey("redisPort")
                    || data.containsKey("redisPassword")
                    || data.containsKey("redisDatabase");
            if (hasRedisUpdate) {
                String host = data.containsKey("redisHost")
                        ? String.valueOf(data.get("redisHost"))
                        : cfg.getRedisHost();
                int port = parseIntOrDefault(data.get("redisPort"), cfg.getRedisPort());
                int db = parseIntOrDefault(data.get("redisDatabase"), cfg.getRedisDatabase());
                String pass = cfg.getRedisPassword();
                if (data.containsKey("redisPassword")) {
                    String submitted = toNullableString(data.get("redisPassword"));
                    if (submitted == null) {
                        pass = "";
                    } else if (!REDIS_PASSWORD_MASK.equals(submitted)) {
                        pass = submitted;
                    }
                }
                cfg.setRedisConfig(host, port, pass, db);
            }
            
            if (data.containsKey("playerGroupId")) {
                try { cfg.setPlayerGroupId(Long.parseLong(String.valueOf(data.get("playerGroupId")))); } catch (Exception ignored) {}
            }
            if (data.containsKey("adminGroupId")) {
                try { cfg.setAdminGroupId(Long.parseLong(String.valueOf(data.get("adminGroupId")))); } catch (Exception ignored) {}
            }
            if (data.containsKey("botQQ")) {
                try { cfg.setBotQQ(Long.parseLong(String.valueOf(data.get("botQQ")))); } catch (Exception ignored) {}
            }
            
            cfg.save();
            sendJson(ctx, "{\"success\":true}");
        } catch (Exception e) {
            LOGGER.error("保存配置失败", e);
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
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
    
    /**
     * 处理用户登录
     */
    private void handleLogin(ChannelHandlerContext ctx, FullHttpRequest req) {
        try {
            String body = req.content().toString(StandardCharsets.UTF_8);
            String username = extractJsonString(body, "username");
            String password = extractJsonString(body, "password");
            
            String token = com.mapbot.alpha.security.AuthManager.INSTANCE.login(username, password);
            
            if (token != null) {
                sendJson(ctx, "{\"success\":true,\"token\":\"" + token + "\"}");
            } else {
                sendJson(ctx, "{\"success\":false,\"error\":\"用户名或密码错误\"}");
            }
        } catch (Exception e) {
            sendJson(ctx, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }
    
    /**
     * 发送 401 未授权响应
     */
    private void sendUnauthorized(ChannelHandlerContext ctx) {
        byte[] bytes = "{\"error\":\"Unauthorized\"}".getBytes(StandardCharsets.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED,
                io.netty.buffer.Unpooled.wrappedBuffer(bytes));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        response.headers().set("WWW-Authenticate", "Bearer");
        HttpUtil.setContentLength(response, bytes.length);
        ctx.writeAndFlush(response);
    }

    private void sendForbidden(ChannelHandlerContext ctx, String message) {
        String escaped = escapeJson(message);
        byte[] bytes = ("{\"errorCode\":\"AUTH-403\",\"error\":\"" + escaped + "\"}").getBytes(StandardCharsets.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN,
                io.netty.buffer.Unpooled.wrappedBuffer(bytes));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        HttpUtil.setContentLength(response, bytes.length);
        ctx.writeAndFlush(response);
    }

    private String maskSecret(String secret) {
        return (secret == null || secret.isEmpty()) ? "" : REDIS_PASSWORD_MASK;
    }

    private int parseIntOrDefault(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            return (int) Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String toNullableString(Object value) {
        if (value == null) return null;
        String s = String.valueOf(value);
        if ("null".equalsIgnoreCase(s)) return null;
        return s;
    }
    
    /**
     * 处理 Metrics 历史数据请求
     */
    private void handleMetricsHistory(ChannelHandlerContext ctx, String uri) {
        try {
            // 解析 serverId: /api/metrics/{serverId}/history
            String path = uri.substring("/api/metrics/".length());
            String serverId = path.substring(0, path.indexOf("/"));
            
            var collector = com.mapbot.alpha.metrics.MetricsCollector.INSTANCE;
            var tps = collector.getTpsHistory(serverId);
            var memory = collector.getMemoryHistory(serverId);
            var players = collector.getPlayersHistory(serverId);
            
            StringBuilder json = new StringBuilder("{\"tps\":[");
            for (int i = 0; i < tps.size(); i++) {
                if (i > 0) json.append(",");
                json.append("[").append(tps.get(i).timestamp).append(",").append(tps.get(i).value).append("]");
            }
            json.append("],\"memory\":[");
            for (int i = 0; i < memory.size(); i++) {
                if (i > 0) json.append(",");
                json.append("[").append(memory.get(i).timestamp).append(",").append(memory.get(i).value).append("]");
            }
            json.append("],\"players\":[");
            for (int i = 0; i < players.size(); i++) {
                if (i > 0) json.append(",");
                json.append("[").append(players.get(i).timestamp).append(",").append(players.get(i).value).append("]");
            }
            json.append("]}");
            
            sendJson(ctx, json.toString());
        } catch (Exception e) {
            sendJson(ctx, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }
    
    /**
     * 处理服务器重启/停止请求
     */
    private void handleServerControl(ChannelHandlerContext ctx, String uri, String action) {
        try {
            String path = uri.substring("/api/servers/".length());
            String serverId = path.substring(0, path.indexOf("/"));
            
            var server = com.mapbot.alpha.bridge.ServerRegistry.INSTANCE.getServer(serverId);
            if (server == null || !server.isOnline()) {
                sendJson(ctx, "{\"success\":false,\"error\":\"Server not connected\"}");
                return;
            }
            
            String json = String.format("{\"type\":\"%s_server\",\"requestId\":\"%s\"}",
                    action, System.currentTimeMillis());
            server.channel.writeAndFlush(json + "\n");
            
            sendJson(ctx, "{\"success\":true}");
            LOGGER.info("[控制] {} 服务器: {}", action, serverId);
        } catch (Exception e) {
            sendJson(ctx, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }
    
    /**
     * 用户管理 API
     */
    private void handleUsersApi(ChannelHandlerContext ctx, FullHttpRequest req, String uri, String token) {
        var auth = com.mapbot.alpha.security.AuthManager.INSTANCE;
        
        // 检查 OWNER 权限 (Step-04 B2: 用户管理需要 OWNER 角色)
        if (!auth.hasContractPermission(token, com.mapbot.alpha.security.ContractRole.OWNER)) {
            sendForbidden(ctx, "Permission denied. OWNER required.");
            return;
        }
        
        HttpMethod method = req.method();
        
        try {
            // GET /api/users - 列出所有用户
            if (uri.equals("/api/users") && method == HttpMethod.GET) {
                var users = auth.listUsers();
                sendJson(ctx, com.mapbot.alpha.utils.JsonUtils.toJson(users));
                return;
            }
            
            // POST /api/users - 创建用户
            if (uri.equals("/api/users") && method == HttpMethod.POST) {
                String body = req.content().toString(java.nio.charset.StandardCharsets.UTF_8);
                CreateUserRequest request = com.mapbot.alpha.utils.JsonUtils.fromJson(body, CreateUserRequest.class);
                
                if (request.username == null || request.password == null) {
                    sendJson(ctx, "{\"error\":\"username and password required\"}");
                    return;
                }
                
                var role = request.role != null ? 
                    com.mapbot.alpha.security.AuthManager.Role.valueOf(request.role.toUpperCase()) :
                    com.mapbot.alpha.security.AuthManager.Role.VIEWER;
                
                if (auth.createUser(request.username, request.password, role)) {
                    sendJson(ctx, "{\"success\":true}");
                } else {
                    sendJson(ctx, "{\"error\":\"User already exists\"}");
                }
                return;
            }
            
            // DELETE /api/users/{username} - 删除用户
            if (uri.startsWith("/api/users/") && method == HttpMethod.DELETE) {
                String username = uri.substring("/api/users/".length());
                if (auth.deleteUser(username)) {
                    sendJson(ctx, "{\"success\":true}");
                } else {
                    sendJson(ctx, "{\"error\":\"Cannot delete user\"}");
                }
                return;
            }
            
            // PUT /api/users/{username}/password - 修改密码
            if (uri.matches("/api/users/.+/password") && method == HttpMethod.PUT) {
                String username = uri.split("/")[3];
                String body = req.content().toString(java.nio.charset.StandardCharsets.UTF_8);
                PasswordRequest request = com.mapbot.alpha.utils.JsonUtils.fromJson(body, PasswordRequest.class);
                
                if (auth.changePassword(username, request.password)) {
                    sendJson(ctx, "{\"success\":true}");
                } else {
                    sendJson(ctx, "{\"error\":\"User not found\"}");
                }
                return;
            }
            
            // PUT /api/users/{username}/role - 修改角色
            if (uri.matches("/api/users/.+/role") && method == HttpMethod.PUT) {
                String username = uri.split("/")[3];
                String body = req.content().toString(java.nio.charset.StandardCharsets.UTF_8);
                RoleRequest request = com.mapbot.alpha.utils.JsonUtils.fromJson(body, RoleRequest.class);
                
                var role = com.mapbot.alpha.security.AuthManager.Role.valueOf(request.role.toUpperCase());
                if (auth.changeRole(username, role)) {
                    sendJson(ctx, "{\"success\":true}");
                } else {
                    sendJson(ctx, "{\"error\":\"User not found\"}");
                }
                return;
            }
            
            sendJson(ctx, "{\"error\":\"Unknown users API\"}");
        } catch (Exception e) {
            LOGGER.error("用户管理 API 错误", e);
            sendJson(ctx, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    /**
     * MapBot 数据管理 API
     * 用于可视化管理 Redis 中的 bindings/permissions/admins/mutes 等数据
     */
    private void handleMapbotDataApi(ChannelHandlerContext ctx, FullHttpRequest req, String uri, String token) {
        var auth = com.mapbot.alpha.security.AuthManager.INSTANCE;

        // 检查 OWNER 权限 (Step-04 B2: MapBot 数据管理需要 OWNER 角色)
        if (!auth.hasContractPermission(token, com.mapbot.alpha.security.ContractRole.OWNER)) {
            sendForbidden(ctx, "Permission denied. OWNER required.");
            return;
        }

        HttpMethod method = req.method();
        try {
            // GET /api/mapbot/data
            if (uri.equals("/api/mapbot/data") && method == HttpMethod.GET) {
                var dm = com.mapbot.alpha.data.DataManager.INSTANCE;
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("admins", dm.getAdmins());
                data.put("bindings", dm.getAllBindings());
                data.put("permissions", dm.getAllPermissions());
                data.put("mutes", dm.getAllMutes());
                sendJson(ctx, com.mapbot.alpha.utils.JsonUtils.toJson(data));
                return;
            }

            String body = req.content().toString(java.nio.charset.StandardCharsets.UTF_8);
            java.util.Map<String, Object> payload = com.mapbot.alpha.utils.JsonUtils.fromJson(body, java.util.Map.class);
            if (payload == null) payload = java.util.Collections.emptyMap();

            // POST /api/mapbot/permission {qq, level}
            if (uri.equals("/api/mapbot/permission") && method == HttpMethod.POST) {
                long qq = Long.parseLong(String.valueOf(payload.get("qq")));
                int level = Integer.parseInt(String.valueOf(payload.get("level")));
                com.mapbot.alpha.data.DataManager.INSTANCE.setPermission(qq, level);
                sendJson(ctx, "{\"success\":true}");
                return;
            }

            // POST /api/mapbot/unbind {qq}
            if (uri.equals("/api/mapbot/unbind") && method == HttpMethod.POST) {
                long qq = Long.parseLong(String.valueOf(payload.get("qq")));
                boolean ok = com.mapbot.alpha.data.DataManager.INSTANCE.unbind(qq);
                sendJson(ctx, "{\"success\":" + ok + "}");
                return;
            }

            // POST /api/mapbot/admin/add {qq}
            if (uri.equals("/api/mapbot/admin/add") && method == HttpMethod.POST) {
                long qq = Long.parseLong(String.valueOf(payload.get("qq")));
                com.mapbot.alpha.data.DataManager.INSTANCE.addAdmin(qq);
                sendJson(ctx, "{\"success\":true}");
                return;
            }

            // POST /api/mapbot/admin/remove {qq}
            if (uri.equals("/api/mapbot/admin/remove") && method == HttpMethod.POST) {
                long qq = Long.parseLong(String.valueOf(payload.get("qq")));
                com.mapbot.alpha.data.DataManager.INSTANCE.removeAdmin(qq);
                sendJson(ctx, "{\"success\":true}");
                return;
            }

            // POST /api/mapbot/mute {uuid, expiryMs}
            if (uri.equals("/api/mapbot/mute") && method == HttpMethod.POST) {
                String uuid = String.valueOf(payload.get("uuid"));
                long expiryMs = payload.containsKey("expiryMs") ? Long.parseLong(String.valueOf(payload.get("expiryMs"))) : -1L;
                com.mapbot.alpha.data.DataManager.INSTANCE.mute(uuid, expiryMs);
                sendJson(ctx, "{\"success\":true}");
                return;
            }

            // POST /api/mapbot/unmute {uuid}
            if (uri.equals("/api/mapbot/unmute") && method == HttpMethod.POST) {
                String uuid = String.valueOf(payload.get("uuid"));
                com.mapbot.alpha.data.DataManager.INSTANCE.unmute(uuid);
                sendJson(ctx, "{\"success\":true}");
                return;
            }

            sendJson(ctx, "{\"error\":\"Unknown mapbot API\"}");
        } catch (Exception e) {
            LOGGER.error("MapBot 数据管理 API 错误", e);
            sendJson(ctx, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }
    
    // 用户 API DTO
    private static class CreateUserRequest {
        public String username;
        public String password;
        public String role;
    }
    
    private static class PasswordRequest {
        public String password;
    }
    
    private static class RoleRequest {
        public String role;
    }
}
