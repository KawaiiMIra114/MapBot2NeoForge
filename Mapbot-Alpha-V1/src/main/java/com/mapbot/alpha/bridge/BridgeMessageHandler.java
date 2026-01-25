package com.mapbot.alpha.bridge;

import com.mapbot.alpha.config.AlphaConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridge 消息处理器
 * 处理来自 MC 服务端的消息
 * STEP 13: 添加 proxy_response 处理
 */
public class BridgeMessageHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Bridge/Handler");
    
    private String serverId = null;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LOGGER.info("新的 Bridge 连接: {}", ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        LOGGER.debug("收到 Bridge 消息: {}", msg);
        
        try {
            String type = extractJsonValue(msg, "type");
            
            switch (type) {
                case "register":
                    handleRegister(ctx, msg);
                    break;
                case "heartbeat":
                    handleHeartbeat(ctx);
                    break;
                case "event":
                    handleEvent(ctx, msg);
                    break;
                case "chat":
                    handleChat(msg);
                    break;
                case "file_response":
                    handleFileResponse(msg);
                    break;
                case "status_update":
                    handleStatusUpdate(msg);
                    break;
                case "proxy_response":
                    handleProxyResponse(msg);
                    break;
                default:
                    LOGGER.warn("未知消息类型: {}", type);
            }
        } catch (Exception e) {
            LOGGER.error("处理 Bridge 消息失败", e);
        }
    }
    
    private void handleRegister(ChannelHandlerContext ctx, String msg) {
        serverId = extractJsonValue(msg, "serverId");
        String version = extractJsonValue(msg, "version");
        
        ServerRegistry.INSTANCE.register(serverId, ctx.channel());
        
        LOGGER.info("服务器已注册: {} (版本: {})", serverId, version);
        ctx.writeAndFlush("{\"type\":\"register_ack\",\"success\":true}\n");
    }
    
    private void handleHeartbeat(ChannelHandlerContext ctx) {
        if (serverId != null) {
            ServerRegistry.INSTANCE.updateHeartbeat(serverId);
        }
        ctx.writeAndFlush("{\"type\":\"heartbeat_ack\"}\n");
    }
    
    private void handleEvent(ChannelHandlerContext ctx, String msg) {
        String event = extractJsonValue(msg, "event");
        LOGGER.info("[{}] 事件: {}", serverId, event);
        com.mapbot.alpha.network.LogWebSocketHandler.broadcast("[" + serverId + "] " + event);
    }
    
    /**
     * 处理来自 MC 的聊天消息 -> QQ 群
     */
    private void handleChat(String msg) {
        String player = extractJsonValue(msg, "player");
        String content = extractJsonValue(msg, "content");
        
        if (player.isEmpty() || content.isEmpty()) return;
        
        String formattedMsg = String.format("[%s] %s: %s", serverId, player, content);
        LOGGER.info("[MC->QQ] {}", formattedMsg);
        
        // 发送到玩家群
        long groupId = AlphaConfig.getPlayerGroupId();
        com.mapbot.alpha.network.OneBotClient.INSTANCE.sendGroupMessage(groupId, formattedMsg);
        
        // 广播到 Web 控制台
        com.mapbot.alpha.network.LogWebSocketHandler.broadcast(formattedMsg);
    }
    
    private void handleFileResponse(String msg) {
        String requestId = extractJsonValue(msg, "requestId");
        String error = extractJsonValue(msg, "error");
        
        // 文件列表使用 files 字段 (JSON 数组), 文件内容使用 content 字段
        String content = extractJsonValue(msg, "content");
        String files = extractJsonArray(msg, "files");
        
        // 优先使用 files 字段
        String result = (files != null && !files.isEmpty()) ? files : content;
        BridgeFileProxy.completeRequest(requestId, result, error);
    }
    
    /**
     * 提取 JSON 数组字段 (不带引号的值)
     */
    private String extractJsonArray(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        
        // 跳过空白
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        
        if (start >= json.length() || json.charAt(start) != '[') return null;
        
        // 找到匹配的 ]
        int depth = 1;
        int end = start + 1;
        while (end < json.length() && depth > 0) {
            char c = json.charAt(end);
            if (c == '[') depth++;
            else if (c == ']') depth--;
            end++;
        }
        
        return json.substring(start, end);
    }
    
    private void handleStatusUpdate(String msg) {
        if (serverId == null) return;
        
        String players = extractJsonValue(msg, "players");
        String tps = extractJsonValue(msg, "tps");
        String memory = extractJsonValue(msg, "memory");
        
        ServerRegistry.INSTANCE.updateStatus(serverId, players, tps, memory);
    }
    
    /**
     * 处理 Bridge 代理响应
     * STEP 13: 用于命令执行结果返回
     */
    private void handleProxyResponse(String msg) {
        String requestId = extractJsonValue(msg, "requestId");
        String result = extractJsonValue(msg, "result");
        
        // 传递给 BridgeProxy 完成请求
        BridgeProxy.INSTANCE.completeRequest(requestId, result);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (serverId != null) {
            ServerRegistry.INSTANCE.unregister(serverId);
            LOGGER.info("服务器断开连接: {}", serverId);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent e) {
            if (e.state() == IdleState.READER_IDLE) {
                LOGGER.warn("Bridge 连接超时，断开: {}", serverId);
                ctx.close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("Bridge 连接异常: " + serverId, cause);
        ctx.close();
    }
    
    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) {
            search = "\"" + key + "\":";
            start = json.indexOf(search);
            if (start == -1) return "";
            start += search.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return json.substring(start, end).trim();
        }
        start += search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }
}
