package com.mapbot.alpha.bridge;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Bridge 服务器
 * STEP 7: 接受 MC 服务端的 Bridge Mod 连接
 */
public class BridgeServer {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Bridge");
    public static final BridgeServer INSTANCE = new BridgeServer();
    
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    
    public void start(int port) {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        // 心跳检测 (60秒无活动则断开)
                        p.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
                        // 行分隔解码
                        p.addLast(new LineBasedFrameDecoder(65536));
                        // 帧超限时返回结构化错误并断连
                        p.addLast(new BridgeFrameSizeGuardHandler());
                        p.addLast(new StringDecoder(StandardCharsets.UTF_8));
                        p.addLast(new StringEncoder(StandardCharsets.UTF_8));
                        // 首帧 register 强制鉴权，通过后再放行到 BridgeMessageHandler
                        p.addLast(new BridgeRegistrationAuthHandler());
                        // Bridge 消息处理
                        p.addLast(new BridgeMessageHandler());
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
            
            serverChannel = b.bind(port).sync().channel();
            LOGGER.info("Bridge 服务器已启动，监听端口: {}", port);
        } catch (Exception e) {
            LOGGER.error("Bridge 服务器启动失败", e);
        }
    }
    
    public void stop() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        LOGGER.info("Bridge 服务器已停止");
    }

    private static class BridgeFrameSizeGuardHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (cause instanceof TooLongFrameException) {
                LOGGER.warn("Bridge 入站帧超限: from={}", ctx.channel().remoteAddress());
                String response = com.mapbot.alpha.utils.JsonUtils.toJson(
                    BridgeErrorMapper.registerAckFailurePayload(
                        "frame_too_large",
                        BridgeErrorMapper.BRG_VALIDATION_205,
                        false
                    )
                ) + "\n";
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                return;
            }
            ctx.fireExceptionCaught(cause);
        }
    }

    private static class BridgeRegistrationAuthHandler extends SimpleChannelInboundHandler<String> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) {
            String payload = msg == null ? "" : msg.trim();
            if (payload.isEmpty()) {
                reject(ctx, null, "empty_message", BridgeErrorMapper.BRG_VALIDATION_202);
                return;
            }

            java.util.Map<String, Object> data = com.mapbot.alpha.utils.JsonUtils.fromJson(payload, java.util.Map.class);
            if (data == null) {
                reject(ctx, null, "invalid_json", BridgeErrorMapper.BRG_VALIDATION_202);
                return;
            }

            String type = String.valueOf(data.get("type"));
            if (!"register".equals(type)) {
                reject(ctx, null, "register_required", BridgeErrorMapper.BRG_VALIDATION_201);
                return;
            }

            String serverId = toSafeString(data.get("serverId"));
            String token = firstNonBlank(
                    toSafeString(data.get("token")),
                    toSafeString(data.get("authToken")),
                    toSafeString(data.get("bridgeToken")),
                    toSafeString(data.get("secret"))
            );
            if (!com.mapbot.alpha.security.AuthManager.INSTANCE.isBridgeRegistrationAuthorized(serverId, token)) {
                reject(ctx, serverId, "unauthorized", BridgeErrorMapper.BRG_AUTH_101);
                return;
            }

            LOGGER.info("Bridge 注册鉴权通过: {} ({})", serverId, ctx.channel().remoteAddress());
            ctx.pipeline().remove(this);
            ctx.fireChannelRead(msg);
        }

        private static void reject(ChannelHandlerContext ctx, String serverId, String reason, String explicitErrorCode) {
            LOGGER.warn("拒绝未授权 Bridge 注册: serverId={}, from={}, reason={}",
                    serverId, ctx.channel().remoteAddress(), reason);
            String response = com.mapbot.alpha.utils.JsonUtils.toJson(
                BridgeErrorMapper.registerAckFailurePayload(reason, explicitErrorCode, false)
            ) + "\n";
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }

        private static String toSafeString(Object value) {
            if (value == null) return null;
            String s = String.valueOf(value).trim();
            if (s.isEmpty() || "null".equalsIgnoreCase(s)) return null;
            return s;
        }

        private static String firstNonBlank(String... values) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
            return null;
        }
    }
}
