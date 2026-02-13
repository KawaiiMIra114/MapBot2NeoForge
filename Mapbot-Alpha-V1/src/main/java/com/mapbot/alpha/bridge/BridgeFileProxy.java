package com.mapbot.alpha.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Bridge 文件代理
 * STEP 9: 跨服文件管理
 */
public class BridgeFileProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Bridge/FileProxy");
    
    // 请求ID -> CompletableFuture 映射
    private static final Map<String, CompletableFuture<FileResponse>> pendingRequests = new ConcurrentHashMap<>();
    
    // 请求超时时间 (秒)
    private static final int TIMEOUT_SECONDS = 10;
    
    /**
     * 列出远程服务器目录
     */
    public static FileResponse listDir(String serverId, String path) {
        return sendRequest(serverId, "file_list", path, null);
    }
    
    /**
     * 读取远程服务器文件
     */
    public static FileResponse readFile(String serverId, String path) {
        return sendRequest(serverId, "file_read", path, null);
    }
    
    /**
     * 写入远程服务器文件
     */
    public static FileResponse writeFile(String serverId, String path, String content) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("content", content != null ? content : "");
        return sendRequest(serverId, "file_write", path, fields);
    }
    
    /**
     * 删除远程服务器文件
     */
    public static FileResponse deleteFile(String serverId, String path) {
        return sendRequest(serverId, "file_delete", path, null);
    }

    /**
     * 创建远程目录
     */
    public static FileResponse mkdir(String serverId, String path) {
        return sendRequest(serverId, "file_mkdir", path, null);
    }

    /**
     * 上传远程文件
     */
    public static FileResponse uploadFile(String serverId, String path, String content, String encoding) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("content", content != null ? content : "");
        if (encoding != null && !encoding.isEmpty()) {
            fields.put("encoding", encoding);
        }
        return sendRequest(serverId, "file_upload", path, fields);
    }
    
    private static FileResponse sendRequest(String serverId, String action, String path, Map<String, Object> fields) {
        ServerRegistry.ServerInfo server = ServerRegistry.INSTANCE.getServer(serverId);
        if (server == null || !server.isOnline()) {
            return new FileResponse(null, "Server not connected: " + serverId);
        }
        
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        CompletableFuture<FileResponse> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);
        
        try {
            // 构建请求消息
            Map<String, Object> req = new HashMap<>();
            req.put("type", action);
            req.put("requestId", requestId);
            req.put("path", path != null ? path : "");
            if (fields != null && !fields.isEmpty()) {
                req.putAll(fields);
            }
            
            server.channel.writeAndFlush(com.mapbot.alpha.utils.JsonUtils.toJson(req) + "\n");
            
            // 等待响应
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            LOGGER.warn("文件请求超时: {} -> {}", serverId, path);
            return new FileResponse(null, "Request timeout");
        } catch (Exception e) {
            LOGGER.error("文件请求失败", e);
            return new FileResponse(null, e.getMessage());
        } finally {
            pendingRequests.remove(requestId);
        }
    }
    
    /**
     * 完成请求 (由 BridgeMessageHandler 调用)
     */
    public static void completeRequest(String requestId, String content, String error) {
        CompletableFuture<FileResponse> future = pendingRequests.get(requestId);
        if (future != null) {
            if (error != null && !error.isEmpty()) {
                future.complete(new FileResponse(null, error));
            } else {
                future.complete(new FileResponse(content, null));
            }
        }
    }
    
    /**
     * 文件操作响应
     */
    public static class FileResponse {
        public final String content;
        public final String error;
        
        public FileResponse(String content, String error) {
            this.content = content;
            this.error = error;
        }
        
        public boolean isSuccess() {
            return error == null || error.isEmpty();
        }
    }
}
