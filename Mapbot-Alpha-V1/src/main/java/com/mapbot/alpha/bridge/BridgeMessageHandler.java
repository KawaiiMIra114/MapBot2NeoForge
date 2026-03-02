package com.mapbot.alpha.bridge;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridge 消息处理器 (Task #05 重构: 瘦身版)
 *
 * 仅保留 Netty 生命周期回调（连接/断开/超时/异常）。
 * 所有报文处理逻辑已迁移至 {@link BridgeRouteDispatcher}。
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
            @SuppressWarnings("unchecked")
            var data = com.mapbot.alpha.utils.JsonUtils.fromJson(msg, java.util.Map.class);
            if (data == null) return;
            
            String type = String.valueOf(data.get("type"));
            
            // 特殊处理: register 包需要捕获 serverId 到本 Handler 实例
            if ("register".equals(type)) {
                serverId = String.valueOf(data.get("serverId"));
            }
            
            // 委托给路由派发器
            BridgeRouteDispatcher.INSTANCE.dispatch(ctx, serverId, type, data);
            
        } catch (Exception e) {
            LOGGER.error("处理 Bridge 消息失败: " + msg, e);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (serverId != null) {
            // 下线通知发到该子服绑定的群（Task #05: 动态群组）
            long groupId = BridgeRouteDispatcher.resolveGroupId(serverId);

            ServerRegistry.INSTANCE.unregister(serverId);
            LOGGER.info("服务器断开连接: {}", serverId);
            com.mapbot.alpha.network.OneBotClient.INSTANCE.sendGroupMessage(groupId, "[服务器] " + serverId + " 断开连接");
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
