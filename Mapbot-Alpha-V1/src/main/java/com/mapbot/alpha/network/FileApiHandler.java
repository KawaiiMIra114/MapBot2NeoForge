package com.mapbot.alpha.network;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

/**
 * 文件管理 API 处理器
 * STEP 8: 本地文件管理
 */
public class FileApiHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Network/FileApi");
    
    // 允许访问的根目录 (可配置)
    private static Path ROOT_DIR = Paths.get(System.getProperty("user.dir"));
    
    public static void setRootDir(String path) {
        ROOT_DIR = Paths.get(path);
        LOGGER.info("文件管理根目录: {}", ROOT_DIR.toAbsolutePath());
    }
    
    public static void handle(ChannelHandlerContext ctx, FullHttpRequest req) {
        String uri = req.uri();
        HttpMethod method = req.method();
        
        try {
            if (uri.startsWith("/api/files/list")) {
                handleListDir(ctx, req);
            } else if (uri.startsWith("/api/files/read")) {
                handleReadFile(ctx, req);
            } else if (uri.equals("/api/files/write") && method == HttpMethod.POST) {
                handleWriteFile(ctx, req);
            } else if (uri.startsWith("/api/files/delete") && method == HttpMethod.DELETE) {
                handleDeleteFile(ctx, req);
            } else if (uri.equals("/api/files/mkdir") && method == HttpMethod.POST) {
                handleMkdir(ctx, req);
            } else if (uri.equals("/api/files/upload") && method == HttpMethod.POST) {
                handleUploadFile(ctx, req);
            } else {
                sendJson(ctx, HttpResponseStatus.NOT_FOUND, "{\"error\": \"Unknown API\"}");
            }
        } catch (SecurityException e) {
            sendJson(ctx, HttpResponseStatus.FORBIDDEN, "{\"error\": \"Access denied\"}");
        } catch (Exception e) {
            LOGGER.error("API 处理失败", e);
            sendJson(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    
    private static void handleListDir(ChannelHandlerContext ctx, FullHttpRequest req) throws IOException {
        String pathParam = getQueryParam(req.uri(), "path");
        Path dir = resolveSafePath(pathParam);
        
        if (!Files.isDirectory(dir)) {
            sendJson(ctx, HttpResponseStatus.BAD_REQUEST, "{\"error\": \"Not a directory\"}");
            return;
        }
        
        StringBuilder json = new StringBuilder("[");
        var files = Files.list(dir).collect(Collectors.toList());
        for (int i = 0; i < files.size(); i++) {
            Path f = files.get(i);
            if (i > 0) json.append(",");
            json.append("{\"name\":\"").append(escapeJson(f.getFileName().toString())).append("\",");
            json.append("\"isDir\":").append(Files.isDirectory(f)).append(",");
            json.append("\"size\":").append(Files.isRegularFile(f) ? Files.size(f) : 0).append("}");
        }
        json.append("]");
        
        sendJson(ctx, HttpResponseStatus.OK, json.toString());
    }
    
    private static void handleReadFile(ChannelHandlerContext ctx, FullHttpRequest req) throws IOException {
        String pathParam = getQueryParam(req.uri(), "path");
        Path file = resolveSafePath(pathParam);
        
        if (!Files.isRegularFile(file)) {
            sendJson(ctx, HttpResponseStatus.BAD_REQUEST, "{\"error\": \"Not a file\"}");
            return;
        }
        
        // BUG #9 修复: 检测二进制文件
        String fileName = file.getFileName().toString().toLowerCase();
        if (isBinaryFile(fileName)) {
            sendJson(ctx, HttpResponseStatus.BAD_REQUEST, "{\"error\": \"Binary file not supported for preview\"}");
            return;
        }
        
        // 限制文件大小 (10MB)
        if (Files.size(file) > 10 * 1024 * 1024) {
            sendJson(ctx, HttpResponseStatus.BAD_REQUEST, "{\"error\": \"File too large\"}");
            return;
        }
        
        String content = Files.readString(file, StandardCharsets.UTF_8);
        sendJson(ctx, HttpResponseStatus.OK, "{\"content\":\"" + escapeJson(content) + "\"}");
    }
    
    /**
     * 检测是否为二进制文件 (根据扩展名)
     */
    private static boolean isBinaryFile(String fileName) {
        String[] binaryExtensions = {
            ".jar", ".zip", ".tar", ".gz", ".7z", ".rar",
            ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".ico", ".webp",
            ".mp3", ".wav", ".ogg", ".flac",
            ".mp4", ".avi", ".mkv", ".mov", ".webm",
            ".exe", ".dll", ".so", ".class", ".o",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx",
            ".nbt", ".dat", ".mca", ".db", ".sqlite"
        };
        for (String ext : binaryExtensions) {
            if (fileName.endsWith(ext)) return true;
        }
        return false;
    }
    
    private static void handleWriteFile(ChannelHandlerContext ctx, FullHttpRequest req) throws IOException {
        String body = req.content().toString(StandardCharsets.UTF_8);
        // 简单 JSON 解析 (生产环境应使用 Gson/Jackson)
        String pathParam = extractJsonValue(body, "path");
        String content = extractJsonValue(body, "content");
        
        Path file = resolveSafePath(pathParam);
        Files.writeString(file, content, StandardCharsets.UTF_8);
        
        sendJson(ctx, HttpResponseStatus.OK, "{\"success\": true}");
        LOGGER.info("文件已保存: {}", file);
    }
    
    private static void handleDeleteFile(ChannelHandlerContext ctx, FullHttpRequest req) throws IOException {
        String pathParam = getQueryParam(req.uri(), "path");
        Path file = resolveSafePath(pathParam);
        
        Files.deleteIfExists(file);
        sendJson(ctx, HttpResponseStatus.OK, "{\"success\": true}");
        LOGGER.info("文件已删除: {}", file);
    }
    
    private static void handleMkdir(ChannelHandlerContext ctx, FullHttpRequest req) throws IOException {
        String body = req.content().toString(StandardCharsets.UTF_8);
        String pathParam = extractJsonValue(body, "path");
        
        Path dir = resolveSafePath(pathParam);
        Files.createDirectories(dir);
        
        sendJson(ctx, HttpResponseStatus.OK, "{\"success\": true}");
        LOGGER.info("目录已创建: {}", dir);
    }
    
    /**
     * 处理文件上传 (#12 文件上传)
     */
    private static void handleUploadFile(ChannelHandlerContext ctx, FullHttpRequest req) throws IOException {
        String body = req.content().toString(StandardCharsets.UTF_8);
        String pathParam = extractJsonValue(body, "path");
        String content = extractJsonValue(body, "content");
        String encoding = extractJsonValue(body, "encoding");
        
        Path file = resolveSafePath(pathParam);
        
        if ("base64".equals(encoding)) {
            // Base64 解码并写入二进制文件
            byte[] data = java.util.Base64.getDecoder().decode(content);
            Files.write(file, data);
        } else {
            // 纯文本写入
            Files.writeString(file, content, StandardCharsets.UTF_8);
        }
        
        sendJson(ctx, HttpResponseStatus.OK, "{\"success\": true}");
        LOGGER.info("文件已上传: {}", file);
    }
    
    /**
     * 安全路径解析 - 防止目录遍历攻击
     */
    private static Path resolveSafePath(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return ROOT_DIR;
        }
        
        // 移除开头的 /
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        
        Path resolved = ROOT_DIR.resolve(relativePath).normalize();
        
        // 确保解析后的路径仍在根目录下
        if (!resolved.startsWith(ROOT_DIR)) {
            throw new SecurityException("Path traversal detected: " + relativePath);
        }
        
        return resolved;
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
        // 简易 JSON 提取 (生产环境用 Gson)
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
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
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
