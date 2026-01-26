package com.mapbot.alpha.network;

import com.mapbot.alpha.process.ProcessManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.AttributeKey;
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

    private static final AttributeKey<String> SELECTED_SERVER_ID = AttributeKey.valueOf("mapbot.console.selectedServerId");

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        CLIENTS.add(channel);
        channel.attr(SELECTED_SERVER_ID).set(null);
        LOGGER.info("Web 控制台已连接: {} (当前连接数: {})", channel.remoteAddress(), CLIENTS.size());
        
        // 发送历史日志
        for (String line : ProcessManager.INSTANCE.getLogHistory()) {
            channel.writeAndFlush(new TextWebSocketFrame(line));
        }

        channel.writeAndFlush(new TextWebSocketFrame(
            "[Alpha] 控制台已连接。输入 /server <服务器名> 切换到指定服务器，输入 /back 返回 Alpha 控制台。"
        ));
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
            handleConsoleInput(ctx, text);
        }
    }

    private void handleConsoleInput(ChannelHandlerContext ctx, String text) {
        String selectedServerId = ctx.channel().attr(SELECTED_SERVER_ID).get();

        // 全局控制指令：/server 与 /back 永远优先处理
        if (text.startsWith("/server")) {
            handleSwitchServer(ctx, text);
            return;
        }
        if (text.equals("/back")) {
            ctx.channel().attr(SELECTED_SERVER_ID).set(null);
            ctx.channel().writeAndFlush(new TextWebSocketFrame("[Alpha] 已返回 Alpha 控制台"));
            return;
        }

        // Alpha 控制台（默认）：只处理以 / 开头的 Alpha 内置指令；普通文本不转发
        if (selectedServerId == null || selectedServerId.isEmpty()) {
            if (!text.startsWith("/")) {
                ctx.channel().writeAndFlush(new TextWebSocketFrame(
                    "[Alpha] 当前为 Alpha 控制台。输入 /server <服务器名> 切换到服务器控制台。"
                ));
                return;
            }

            String cmd = text.substring(1);
            String result = ConsoleCommandHandler.handle(cmd);
            if (result != null && !result.isEmpty()) {
                ctx.channel().writeAndFlush(new TextWebSocketFrame("[Alpha] " + result));
            }
            return;
        }

        // 服务器控制台：自动补全 / 前缀并转发到指定服务器，回显执行结果
        String serverCommand = text.startsWith("/") ? text : "/" + text;
        ctx.channel().writeAndFlush(new TextWebSocketFrame("[发送到 " + selectedServerId + "] " + serverCommand));

        com.mapbot.alpha.bridge.BridgeProxy.sendRequestAsyncToServer(selectedServerId, "execute_command", serverCommand, null)
            .whenComplete((result, error) -> {
                if (error != null) {
                    ctx.channel().writeAndFlush(new TextWebSocketFrame("[错误] " + error.getMessage()));
                    return;
                }
                if (result == null || result.isEmpty()) {
                    ctx.channel().writeAndFlush(new TextWebSocketFrame("[返回] (空)"));
                    return;
                }
                ctx.channel().writeAndFlush(new TextWebSocketFrame("[返回] " + result));
            });
    }

    private void handleSwitchServer(ChannelHandlerContext ctx, String text) {
        String[] parts = text.split("\\s+", 2);
        if (parts.length < 2 || parts[1].trim().isEmpty()) {
            ctx.channel().writeAndFlush(new TextWebSocketFrame(
                "[Alpha] 用法: /server <服务器名> | /back\n" +
                "当前在线服务器: " + com.mapbot.alpha.bridge.BridgeProxy.listServerIds()
            ));
            return;
        }

        String query = parts[1].trim();
        String resolved = com.mapbot.alpha.bridge.BridgeProxy.resolveServerId(query);
        if (resolved == null) {
            ctx.channel().writeAndFlush(new TextWebSocketFrame(
                "[Alpha] 未找到服务器: " + query + "\n当前在线服务器: " + com.mapbot.alpha.bridge.BridgeProxy.listServerIds()
            ));
            return;
        }

        ctx.channel().attr(SELECTED_SERVER_ID).set(resolved);
        ctx.channel().writeAndFlush(new TextWebSocketFrame("[Alpha] 已切换到服务器控制台: " + resolved + "（输入 /back 返回）"));
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
