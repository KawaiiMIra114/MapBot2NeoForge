package com.mapbot.alpha.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Web 控制台 HTTP 处理器
 * 负责分发 index.html 页面
 */
public class HttpConsoleHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Network/Http");

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {
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
            // 读取资源文件
            var is = getClass().getResourceAsStream(resourcePath);
            if (is == null) {
                LOGGER.warn("未找到资源: {}", resourcePath);
                sendError(ctx, HttpResponseStatus.NOT_FOUND);
                return;
            }

            byte[] content = is.readAllBytes();
            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            
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
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status);
        ctx.writeAndFlush(response);
    }
}
