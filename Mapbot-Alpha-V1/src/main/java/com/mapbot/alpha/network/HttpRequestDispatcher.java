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
            // 服务器列表 API (STEP 10)
            if (uri.equals("/api/servers")) {
                sendJson(ctx, com.mapbot.alpha.bridge.ServerRegistry.INSTANCE.toJson());
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
}
