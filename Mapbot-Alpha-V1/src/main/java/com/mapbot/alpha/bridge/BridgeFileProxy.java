package com.mapbot.alpha.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        return sendRequest(serverId, "file_write", path, content);
    }
    
    /**
     * 删除远程服务器文件
     */
    public static FileResponse deleteFile(String serverId, String path) {
        return sendRequest(serverId, "file_delete", path, null);
    }
    
    private static FileResponse sendRequest(String serverId, String action, String path, String content) {
        ServerRegistry.ServerInfo server = ServerRegistry.INSTANCE.getServer(serverId);
        if (server == null || !server.isOnline()) {
            return new FileResponse(null, "Server not connected: " + serverId);
        }
        
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        CompletableFuture<FileResponse> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);
        
        try {
            // 构建请求消息
            StringBuilder json = new StringBuilder();
            json.append("{\"type\":\"").append(action).append("\",");
            json.append("\"requestId\":\"").append(requestId).append("\",");
            json.append("\"path\":\"").append(escapeJson(path)).append("\"");
            if (content != null) {
                json.append(",\"content\":\"").append(escapeJson(content)).append("\"");
            }
            json.append("}\n");
            
            server.channel.writeAndFlush(json.toString());
            
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
    
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
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
