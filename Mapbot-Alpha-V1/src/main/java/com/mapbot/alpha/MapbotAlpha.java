package com.mapbot.alpha;

import com.mapbot.alpha.network.OneBotClient;
import com.mapbot.alpha.network.ProtocolDetector;
import com.mapbot.alpha.process.ProcessManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mapbot Alpha V1 - 独立控制台与守护进程
 */
public class MapbotAlpha {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Alpha");
    private static final int LISTEN_PORT = 25560;

    public static void main(String[] args) {
        LOGGER.info("[SYSTEM] Mapbot Alpha V1 正在启动...");
        
        // 1. 启动 OneBot 客户端
        OneBotClient.INSTANCE.connect("ws://127.0.0.1:8080");

        // 2. 启动 MC 服务器进程 (异步)
        ProcessManager.INSTANCE.startServer("./MapBot_Reforged/run", "java -Xmx2G -jar ../build/libs/mapbot-5.0.0-REF.jar nogui");

        // 3. 启动 Netty 分流服务器 (主线程阻塞)
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     ch.pipeline().addLast(new ProtocolDetector());
                 }
             });

            LOGGER.info("[SYSTEM] 核心已就绪，正在监听分流端口: {}", LISTEN_PORT);
            ChannelFuture f = b.bind(LISTEN_PORT).sync();
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            LOGGER.error("[ERROR] 核心崩溃: {}", e.getMessage());
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
