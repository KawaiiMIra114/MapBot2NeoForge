package com.mapbot.alpha.network;

import com.mapbot.alpha.process.ProcessManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocket 日志推送处理器
 * STEP 5: 实时日志广播
 */
public class LogWebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Network/WS");
    
    // 全局活跃通道组
    private static final ChannelGroup CLIENTS = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        CLIENTS.add(channel);
        LOGGER.info("Web 控制台已连接: {} (当前连接数: {})", channel.remoteAddress(), CLIENTS.size());
        
        // 发送历史日志
        for (String line : ProcessManager.INSTANCE.getLogHistory()) {
            channel.writeAndFlush(new TextWebSocketFrame(line));
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        LOGGER.info("Web 控制台断开: {} (剩余连接数: {})", ctx.channel().remoteAddress(), CLIENTS.size());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        String text = frame.text().trim();
        LOGGER.debug("收到控制台指令: {}", text);
        
        // 处理从 Web 控制台发来的指令
        if (!text.isEmpty()) {
            // 以 / 开头的是 Alpha 内置指令，其他发送给 MC 服务器
            if (text.startsWith("/")) {
                String cmd = text.substring(1); // 移除开头的 /
                String result = ConsoleCommandHandler.handle(cmd);
                if (result != null && !result.isEmpty()) {
                    ctx.channel().writeAndFlush(new TextWebSocketFrame("[Alpha] " + result));
                }
            } else {
                // 问题 #6 修复: 发送给 Bridge 服务器而非本地 ProcessManager
                var servers = com.mapbot.alpha.bridge.ServerRegistry.INSTANCE.getAllServers();
                if (!servers.isEmpty()) {
                    var server = servers.iterator().next();
                    String json = String.format(
                        "{\"type\":\"execute_command\",\"requestId\":\"%s\",\"arg1\":\"%s\"}",
                        System.currentTimeMillis(), text.replace("\\", "\\\\").replace("\"", "\\\""));
                    server.channel.writeAndFlush(json + "\n");
                    ctx.channel().writeAndFlush(new TextWebSocketFrame("[发送到 " + server.serverId + "] " + text));
                } else {
                    // 没有 Bridge 服务器，尝试发给本地 ProcessManager
                    ProcessManager.INSTANCE.sendCommand(text);
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("WebSocket 异常", cause);
        ctx.close();
    }

    /**
     * 广播日志行给所有已连接的客户端
     */
    public static void broadcast(String line) {
        if (!CLIENTS.isEmpty()) {
            CLIENTS.writeAndFlush(new TextWebSocketFrame(line));
        }
    }
    
    /**
     * 获取当前连接数
     */
    public static int getConnectionCount() {
        return CLIENTS.size();
    }
}
