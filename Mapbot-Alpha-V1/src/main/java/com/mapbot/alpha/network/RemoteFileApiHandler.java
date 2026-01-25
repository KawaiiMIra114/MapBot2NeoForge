package com.mapbot.alpha.network;

import com.mapbot.alpha.bridge.BridgeFileProxy;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * 跨服文件管理 API 处理器
 * STEP 9: 通过 Bridge 代理访问远程服务器文件
 */
public class RemoteFileApiHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Network/RemoteFileApi");
    
    public static void handle(ChannelHandlerContext ctx, FullHttpRequest req) {
        String uri = req.uri();
        HttpMethod method = req.method();
        
        try {
            // /api/remote/{serverId}/files/list?path=xxx
            // /api/remote/{serverId}/files/read?path=xxx
            // /api/remote/{serverId}/files/write
            // /api/remote/{serverId}/files/delete?path=xxx
            
            // 解析 serverId
            String path = uri.substring("/api/remote/".length());
            int slashIdx = path.indexOf('/');
            if (slashIdx == -1) {
                sendJson(ctx, HttpResponseStatus.BAD_REQUEST, "{\"error\": \"Invalid path format\"}");
                return;
            }
            
            String serverId = path.substring(0, slashIdx);
            String action = path.substring(slashIdx);
            
            if (action.startsWith("/files/list")) {
                handleList(ctx, serverId, getQueryParam(uri, "path"));
            } else if (action.startsWith("/files/read")) {
                handleRead(ctx, serverId, getQueryParam(uri, "path"));
            } else if (action.equals("/files/write") && method == HttpMethod.POST) {
                handleWrite(ctx, req, serverId);
            } else if (action.startsWith("/files/delete") && method == HttpMethod.DELETE) {
                handleDelete(ctx, serverId, getQueryParam(uri, "path"));
            } else {
                sendJson(ctx, HttpResponseStatus.NOT_FOUND, "{\"error\": \"Unknown remote file action\"}");
            }
        } catch (Exception e) {
            LOGGER.error("远程文件 API 处理失败", e);
            sendJson(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    
    private static void handleList(ChannelHandlerContext ctx, String serverId, String path) {
        BridgeFileProxy.FileResponse resp = BridgeFileProxy.listDir(serverId, path);
        if (resp.isSuccess()) {
            // 现在 content 直接是 JSON 数组格式
            sendJson(ctx, HttpResponseStatus.OK, resp.content != null ? resp.content : "[]");
        } else {
            sendJson(ctx, HttpResponseStatus.BAD_REQUEST, "{\"error\": \"" + resp.error + "\"}");
        }
    }
    
    private static void handleRead(ChannelHandlerContext ctx, String serverId, String path) {
        BridgeFileProxy.FileResponse resp = BridgeFileProxy.readFile(serverId, path);
        if (resp.isSuccess()) {
            sendJson(ctx, HttpResponseStatus.OK, "{\"content\":\"" + escapeJson(resp.content) + "\"}");
        } else {
            sendJson(ctx, HttpResponseStatus.BAD_REQUEST, "{\"error\": \"" + resp.error + "\"}");
        }
    }
    
    private static void handleWrite(ChannelHandlerContext ctx, FullHttpRequest req, String serverId) {
        String body = req.content().toString(StandardCharsets.UTF_8);
        String path = extractJsonValue(body, "path");
        String content = extractJsonValue(body, "content");
        
        BridgeFileProxy.FileResponse resp = BridgeFileProxy.writeFile(serverId, path, content);
        if (resp.isSuccess()) {
            sendJson(ctx, HttpResponseStatus.OK, "{\"success\": true}");
        } else {
            sendJson(ctx, HttpResponseStatus.BAD_REQUEST, "{\"error\": \"" + resp.error + "\"}");
        }
    }
    
    private static void handleDelete(ChannelHandlerContext ctx, String serverId, String path) {
        BridgeFileProxy.FileResponse resp = BridgeFileProxy.deleteFile(serverId, path);
        if (resp.isSuccess()) {
            sendJson(ctx, HttpResponseStatus.OK, "{\"success\": true}");
        } else {
            sendJson(ctx, HttpResponseStatus.BAD_REQUEST, "{\"error\": \"" + resp.error + "\"}");
        }
    }
    
    private static String getQueryParam(String uri, String key) {
        int idx = uri.indexOf("?" + key + "=");
        if (idx == -1) idx = uri.indexOf("&" + key + "=");
        if (idx == -1) return "";
        
        int start = idx + key.length() + 2;
        int end = uri.indexOf("&", start);
        if (end == -1) end = uri.length();
        
        return java.net.URLDecoder.decode(uri.substring(start, end), StandardCharsets.UTF_8);
    }
    
    private static String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '"' && json.charAt(i-1) != '\\') break;
            sb.append(c);
        }
        return sb.toString().replace("\\\"", "\"").replace("\\n", "\n");
    }
    
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
    
    private static void sendJson(ChannelHandlerContext ctx, HttpResponseStatus status, String json) {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, Unpooled.wrappedBuffer(bytes));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        HttpUtil.setContentLength(response, bytes.length);
        ctx.writeAndFlush(response);
    }
}
