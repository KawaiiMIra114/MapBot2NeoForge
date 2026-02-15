package com.mapbot.alpha.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Base64;
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
            BridgeErrorMapper.ErrorMeta meta = BridgeErrorMapper.map(
                BridgeErrorMapper.BRG_TRANSPORT_301,
                "Server not connected: " + serverId,
                true
            );
            return new FileResponse(null, meta.rawError, meta.errorCode, meta.rawError, meta.retryable, meta.mappingConflict);
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

            if ("file_upload".equals(action)) {
                String encoding = String.valueOf(req.getOrDefault("encoding", "")).trim().toLowerCase();
                if ("base64".equals(encoding)) {
                    String content = String.valueOf(req.getOrDefault("content", ""));
                    byte[] raw;
                    try {
                        raw = Base64.getDecoder().decode(content);
                    } catch (IllegalArgumentException ex) {
                        BridgeErrorMapper.ErrorMeta meta = BridgeErrorMapper.map(
                            BridgeErrorMapper.BRG_VALIDATION_202,
                            "invalid_base64_payload",
                            false
                        );
                        return new FileResponse(null, meta.rawError, meta.errorCode, meta.rawError, meta.retryable, meta.mappingConflict);
                    }
                    if (raw.length > BridgeErrorMapper.BASE64_RAW_MAX_BYTES) {
                        BridgeErrorMapper.ErrorMeta meta = BridgeErrorMapper.map(
                            BridgeErrorMapper.BRG_VALIDATION_205,
                            "base64_raw_size_exceeded",
                            false
                        );
                        return new FileResponse(null, meta.rawError, meta.errorCode, meta.rawError, meta.retryable, meta.mappingConflict);
                    }
                }
            }

            String json = com.mapbot.alpha.utils.JsonUtils.toJson(req);
            if (BridgeErrorMapper.isFrameTooLarge(json)) {
                BridgeErrorMapper.ErrorMeta meta = BridgeErrorMapper.map(
                    BridgeErrorMapper.BRG_VALIDATION_205,
                    "frame_too_large",
                    false
                );
                return new FileResponse(null, meta.rawError, meta.errorCode, meta.rawError, meta.retryable, meta.mappingConflict);
            }

            server.channel.writeAndFlush(json + "\n");
            
            // 等待响应
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            LOGGER.warn("文件请求超时: {} -> {}", serverId, path);
            BridgeErrorMapper.ErrorMeta meta = BridgeErrorMapper.map(
                BridgeErrorMapper.BRG_TIMEOUT_501,
                "Request timeout",
                true
            );
            return new FileResponse(null, meta.rawError, meta.errorCode, meta.rawError, meta.retryable, meta.mappingConflict);
        } catch (Exception e) {
            LOGGER.error("文件请求失败", e);
            BridgeErrorMapper.ErrorMeta meta = BridgeErrorMapper.map(null, e.getMessage(), false);
            return new FileResponse(null, meta.rawError, meta.errorCode, meta.rawError, meta.retryable, meta.mappingConflict);
        } finally {
            pendingRequests.remove(requestId);
        }
    }
    
    /**
     * 完成请求 (由 BridgeMessageHandler 调用)
     */
    public static void completeRequest(
        String requestId,
        String content,
        String error,
        String errorCode,
        String rawError,
        boolean retryable,
        boolean mappingConflict
    ) {
        CompletableFuture<FileResponse> future = pendingRequests.get(requestId);
        if (future != null) {
            if (error != null && !error.isEmpty()) {
                BridgeErrorMapper.ErrorMeta meta = BridgeErrorMapper.map(errorCode, rawError == null ? error : rawError, retryable);
                future.complete(new FileResponse(null, error, meta.errorCode, meta.rawError, meta.retryable, mappingConflict || meta.mappingConflict));
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
        public final String errorCode;
        public final String rawError;
        public final boolean retryable;
        public final boolean mappingConflict;
        
        public FileResponse(String content, String error) {
            this(content, error, null, null, false, false);
        }

        public FileResponse(
            String content,
            String error,
            String errorCode,
            String rawError,
            boolean retryable,
            boolean mappingConflict
        ) {
            this.content = content;
            this.error = error;
            this.errorCode = errorCode;
            this.rawError = rawError;
            this.retryable = retryable;
            this.mappingConflict = mappingConflict;
        }
        
        public boolean isSuccess() {
            return error == null || error.isEmpty();
        }
    }
}
