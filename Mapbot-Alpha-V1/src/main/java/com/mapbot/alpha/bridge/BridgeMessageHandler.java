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
            var data = com.mapbot.alpha.utils.JsonUtils.fromJson(msg, java.util.Map.class);
            if (data == null) return;
            
            String type = String.valueOf(data.get("type"));
            
            switch (type) {
                case "register":
                    handleRegister(ctx, data);
                    break;
                case "heartbeat":
                    handleHeartbeat(ctx, data);
                    break;
                case "event":
                    handleEvent(ctx, data);
                    break;
                case "chat":
                    handleChat(data);
                    break;
                case "file_response":
                    handleFileResponse(data);
                    break;
                case "status_update":
                    handleStatusUpdate(data);
                    break;
                case "proxy_response":
                    handleProxyResponse(data);
                    break;
                default:
                    LOGGER.warn("未知消息类型: {}", type);
            }
        } catch (Exception e) {
            LOGGER.error("处理 Bridge 消息失败: " + msg, e);
        }
    }
    
    private void handleRegister(ChannelHandlerContext ctx, java.util.Map<String, Object> data) {
        serverId = String.valueOf(data.get("serverId"));
        String version = String.valueOf(data.getOrDefault("version", "1.0"));
        
        ServerRegistry.INSTANCE.register(serverId, ctx.channel());
        
        LOGGER.info("服务器已注册: {} (版本: {})", serverId, version);
        ctx.writeAndFlush("{\"type\":\"register_ack\",\"success\":true}\n");
        
        String notifyMsg = "[服务器] " + serverId + " 已启动";
        long groupId = AlphaConfig.getPlayerGroupId();
        com.mapbot.alpha.network.OneBotClient.INSTANCE.sendGroupMessage(groupId, notifyMsg);
    }
    
    private void handleHeartbeat(ChannelHandlerContext ctx, java.util.Map<String, Object> data) {
        if (serverId != null) {
            String players = String.valueOf(data.getOrDefault("players", "0"));
            String tps = String.valueOf(data.getOrDefault("tps", "20.0"));
            String memory = String.valueOf(data.getOrDefault("memory", "0MB"));
            
            ServerRegistry.INSTANCE.updateStatus(serverId, players, tps, memory);
        }
        ctx.writeAndFlush("{\"type\":\"heartbeat_ack\"}\n");
    }
    
    private void handleEvent(ChannelHandlerContext ctx, java.util.Map<String, Object> data) {
        String event = String.valueOf(data.get("event"));
        LOGGER.info("[{}] 事件: {}", serverId, event);
        com.mapbot.alpha.network.LogWebSocketHandler.broadcast("[" + serverId + "] " + event);
    }
    
    /**
     * 处理来自 MC 的聊天消息 -> QQ 群
     */
    private void handleChat(java.util.Map<String, Object> data) {
        String player = String.valueOf(data.get("player"));
        String content = String.valueOf(data.get("content"));
        if (player == null || content == null) return;
        
        long groupId = AlphaConfig.getPlayerGroupId();
        String format = AlphaConfig.INSTANCE.getBridgeIngameMsgFormat();
        String qqMsg = format.replace("{player}", player).replace("{content}", content);
        com.mapbot.alpha.network.OneBotClient.INSTANCE.sendGroupMessage(groupId, qqMsg);
    }
    
    private void handleFileResponse(java.util.Map<String, Object> data) {
        String requestId = String.valueOf(data.get("requestId"));
        String error = String.valueOf(data.getOrDefault("error", ""));
        
        // 文件列表可能是 "files" 字段 (Array)
        Object filesObj = data.get("files");
        String result;
        if (filesObj != null) {
            result = com.mapbot.alpha.utils.JsonUtils.toJson(filesObj);
        } else {
            result = String.valueOf(data.getOrDefault("content", ""));
        }
        
        com.mapbot.alpha.bridge.BridgeFileProxy.completeRequest(requestId, result, error);
    }
    
    private void handleStatusUpdate(java.util.Map<String, Object> data) {
        if (serverId == null) return;
        
        String players = String.valueOf(data.get("players"));
        String tps = String.valueOf(data.get("tps"));
        String memory = String.valueOf(data.get("memory"));
        
        ServerRegistry.INSTANCE.updateStatus(serverId, players, tps, memory);
    }
    
    /**
     * 处理 Bridge 代理响应
     * STEP 13: 用于命令执行结果返回
     */
    private void handleProxyResponse(java.util.Map<String, Object> data) {
        String requestId = String.valueOf(data.get("requestId"));
        String result = String.valueOf(data.get("result"));
        
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
}
