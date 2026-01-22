package com.mapbot.alpha.bridge;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridge 消息处理器
 * 处理来自 MC 服务端的消息
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
            // 简单 JSON 解析 (生产环境使用 Gson)
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
        
        // 注册到服务器管理器
        ServerRegistry.INSTANCE.register(serverId, ctx.channel());
        
        LOGGER.info("服务器已注册: {} (版本: {})", serverId, version);
        
        // 回复确认
        ctx.writeAndFlush("{\"type\":\"register_ack\",\"success\":true}\n");
    }
    
    private void handleHeartbeat(ChannelHandlerContext ctx) {
        // 更新最后活跃时间
        if (serverId != null) {
            ServerRegistry.INSTANCE.updateHeartbeat(serverId);
        }
        ctx.writeAndFlush("{\"type\":\"heartbeat_ack\"}\n");
    }
    
    private void handleEvent(ChannelHandlerContext ctx, String msg) {
        String event = extractJsonValue(msg, "event");
        String data = msg; // 完整消息传递给事件处理器
        
        LOGGER.info("[{}] 事件: {} - {}", serverId, event, data);
        
        // TODO: 转发给 OneBot / Web 控制台
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
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
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
            // 尝试非字符串值
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
