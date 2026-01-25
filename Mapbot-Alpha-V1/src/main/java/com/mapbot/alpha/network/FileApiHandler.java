package com.mapbot.alpha.network;

import com.google.gson.JsonObject;
import com.mapbot.alpha.utils.JsonUtils;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 文件管理 API 处理器
 * 已重构：使用 Gson 替换手工 JSON 拼接
 */
public class FileApiHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Network/FileApi");
    
    // 允许访问的根目录
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
            } else if (uri.equals("/api/files/unzip") && method == HttpMethod.POST) {
                handleUnzip(ctx, req);
            } else if (uri.equals("/api/files/batch-delete") && method == HttpMethod.POST) {
                handleBatchDelete(ctx, req);
            } else {
                sendJson(ctx, HttpResponseStatus.NOT_FOUND, JsonUtils.error("Unknown API"));
            }
        } catch (SecurityException e) {
            sendJson(ctx, HttpResponseStatus.FORBIDDEN, JsonUtils.error("Access denied"));
        } catch (Exception e) {
            LOGGER.error("API 处理失败", e);
            sendJson(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, JsonUtils.error(e.getMessage()));
        }
    }
    
    // === 文件列表 ===
    private static void handleListDir(ChannelHandlerContext ctx, FullHttpRequest req) throws IOException {
        String pathParam = getQueryParam(req.uri(), "path");
        Path dir = resolveSafePath(pathParam);
        
        if (!Files.isDirectory(dir)) {
            sendJson(ctx, HttpResponseStatus.BAD_REQUEST, JsonUtils.error("Not a directory"));
            return;
        }
        
        List<FileInfo> files = Files.list(dir)
                .map(f -> new FileInfo(
                        f.getFileName().toString(),
                        Files.isDirectory(f),
                        Files.isRegularFile(f) ? getFileSize(f) : 0
                ))
                .collect(Collectors.toList());
        
        sendJson(ctx, HttpResponseStatus.OK, JsonUtils.toJson(files));
    }
    
    private static long getFileSize(Path f) {
        try { return Files.size(f); } catch (Exception e) { return 0; }
    }
    
    // === 读取文件 ===
    private static void handleReadFile(ChannelHandlerContext ctx, FullHttpRequest req) throws IOException {
        String pathParam = getQueryParam(req.uri(), "path");
        Path file = resolveSafePath(pathParam);
        
        if (!Files.isRegularFile(file)) {
            sendJson(ctx, HttpResponseStatus.BAD_REQUEST, JsonUtils.error("Not a file"));
            return;
        }
        
        String fileName = file.getFileName().toString().toLowerCase();
        if (isBinaryFile(fileName)) {
            sendJson(ctx, HttpResponseStatus.BAD_REQUEST, JsonUtils.error("Binary file not supported for preview"));
            return;
        }
        
        if (Files.size(file) > 10 * 1024 * 1024) {
            sendJson(ctx, HttpResponseStatus.BAD_REQUEST, JsonUtils.error("File too large (max 10MB)"));
            return;
        }
        
        String content = Files.readString(file, StandardCharsets.UTF_8);
        sendJson(ctx, HttpResponseStatus.OK, JsonUtils.content(content));
    }
    
    // === 写入文件 ===
    private static void handleWriteFile(ChannelHandlerContext ctx, FullHttpRequest req) throws IOException {
        String body = req.content().toString(StandardCharsets.UTF_8);
        WriteRequest request = JsonUtils.fromJson(body, WriteRequest.class);
        
        Path file = resolveSafePath(request.path);
        Files.writeString(file, request.content != null ? request.content : "", StandardCharsets.UTF_8);
        
        sendJson(ctx, HttpResponseStatus.OK, JsonUtils.success());
        LOGGER.info("文件已保存: {}", file);
    }
    
    // === 删除文件 ===
    private static void handleDeleteFile(ChannelHandlerContext ctx, FullHttpRequest req) throws IOException {
        String pathParam = getQueryParam(req.uri(), "path");
        Path file = resolveSafePath(pathParam);
        
        Files.deleteIfExists(file);
        sendJson(ctx, HttpResponseStatus.OK, JsonUtils.success());
        LOGGER.info("文件已删除: {}", file);
    }
    
    // === 创建目录 ===
    private static void handleMkdir(ChannelHandlerContext ctx, FullHttpRequest req) throws IOException {
        String body = req.content().toString(StandardCharsets.UTF_8);
        PathRequest request = JsonUtils.fromJson(body, PathRequest.class);
        
        Path dir = resolveSafePath(request.path);
        Files.createDirectories(dir);
        
        sendJson(ctx, HttpResponseStatus.OK, JsonUtils.success());
        LOGGER.info("目录已创建: {}", dir);
    }
    
    // === 上传文件 ===
    private static void handleUploadFile(ChannelHandlerContext ctx, FullHttpRequest req) throws IOException {
        String body = req.content().toString(StandardCharsets.UTF_8);
        UploadRequest request = JsonUtils.fromJson(body, UploadRequest.class);
        
        Path file = resolveSafePath(request.path);
        
        if ("base64".equals(request.encoding)) {
            byte[] data = java.util.Base64.getDecoder().decode(request.content);
            Files.write(file, data);
        } else {
            Files.writeString(file, request.content != null ? request.content : "", StandardCharsets.UTF_8);
        }
        
        sendJson(ctx, HttpResponseStatus.OK, JsonUtils.success());
        LOGGER.info("文件已上传: {}", file);
    }
    
    // === 解压 ZIP ===
    private static void handleUnzip(ChannelHandlerContext ctx, FullHttpRequest req) throws IOException {
        String body = req.content().toString(StandardCharsets.UTF_8);
        UnzipRequest request = JsonUtils.fromJson(body, UnzipRequest.class);
        
        Path zipFile = resolveSafePath(request.path);
        Path targetDir = request.target != null ? resolveSafePath(request.target) : zipFile.getParent();
        
        if (!Files.isRegularFile(zipFile)) {
            sendJson(ctx, HttpResponseStatus.BAD_REQUEST, JsonUtils.error("ZIP file not found"));
            return;
        }
        
        int extractedCount = 0;
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName()).normalize();
                
                // 安全检查：防止 zip slip 攻击
                if (!entryPath.startsWith(targetDir)) {
                    throw new SecurityException("ZIP entry outside target directory: " + entry.getName());
                }
                
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath);
                    extractedCount++;
                }
                zis.closeEntry();
            }
        }
        
        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty("extracted", extractedCount);
        sendJson(ctx, HttpResponseStatus.OK, result.toString());
        LOGGER.info("ZIP 已解压: {} -> {} ({} 个文件)", zipFile, targetDir, extractedCount);
    }
    
    // === 批量删除 ===
    private static void handleBatchDelete(ChannelHandlerContext ctx, FullHttpRequest req) throws IOException {
        String body = req.content().toString(StandardCharsets.UTF_8);
        BatchDeleteRequest request = JsonUtils.fromJson(body, BatchDeleteRequest.class);
        
        int deleted = 0;
        List<String> errors = new ArrayList<>();
        
        for (String path : request.paths) {
            try {
                Path file = resolveSafePath(path);
                if (Files.deleteIfExists(file)) {
                    deleted++;
                }
            } catch (Exception e) {
                errors.add(path + ": " + e.getMessage());
            }
        }
        
        JsonObject result = new JsonObject();
        result.addProperty("success", errors.isEmpty());
        result.addProperty("deleted", deleted);
        if (!errors.isEmpty()) {
            result.add("errors", JsonUtils.getGson().toJsonTree(errors));
        }
        sendJson(ctx, HttpResponseStatus.OK, result.toString());
        LOGGER.info("批量删除: {} 个成功, {} 个失败", deleted, errors.size());
    }
    
    // === 二进制文件检测 ===
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
    
    // === 安全路径解析 ===
    private static Path resolveSafePath(String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return ROOT_DIR;
        }
        
        if (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        
        Path resolved = ROOT_DIR.resolve(relativePath).normalize();
        
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
    
    private static void sendJson(ChannelHandlerContext ctx, HttpResponseStatus status, String json) {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, Unpooled.wrappedBuffer(bytes));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        HttpUtil.setContentLength(response, bytes.length);
        ctx.writeAndFlush(response);
    }
    
    // === DTO 类 ===
    
    public static class FileInfo {
        public String name;
        public boolean isDir;
        public long size;
        
        public FileInfo(String name, boolean isDir, long size) {
            this.name = name;
            this.isDir = isDir;
            this.size = size;
        }
    }
    
    public static class WriteRequest {
        public String path;
        public String content;
    }
    
    public static class PathRequest {
        public String path;
    }
    
    public static class UploadRequest {
        public String path;
        public String content;
        public String encoding;
    }
    
    public static class UnzipRequest {
        public String path;
        public String target;
    }
    
    public static class BatchDeleteRequest {
        public String[] paths;
    }
}
