package com.mapbot.alpha;

import com.mapbot.alpha.bridge.BridgeServer;
import com.mapbot.alpha.config.AlphaConfig;
import com.mapbot.alpha.data.DataManager;
import com.mapbot.alpha.logic.InboundHandler;
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
 * Mapbot Alpha V1 - 多服中枢
 * STEP 13: 完整版 - 从 Reforged 移植所有功能
 */
public class MapbotAlpha {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Alpha");
    private static final int LISTEN_PORT = 25560;

    public static void main(String[] args) {
        // 修复中文乱码 (问题 #1)
        try {
            System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
            System.setErr(new java.io.PrintStream(System.err, true, "UTF-8"));
        } catch (Exception ignored) {}
        
        LOGGER.info("==============================================");
        LOGGER.info("  Mapbot Alpha V1 - 多服中枢");
        LOGGER.info("  STEP 13: Reforged 功能完整移植版");
        LOGGER.info("==============================================");
        
        // 0. 加载配置
        AlphaConfig.INSTANCE.load();
        LOGGER.info("[CONFIG] 配置已加载");
        
        // 1. 初始化数据管理器
        DataManager.INSTANCE.init();
        LOGGER.info("[DATA] 数据管理器已初始化");
        
        // 2. 初始化命令系统 (通过加载 InboundHandler 类触发 static 块)
        try {
            Class.forName("com.mapbot.alpha.logic.InboundHandler");
            LOGGER.info("[COMMAND] 命令系统已初始化");
        } catch (ClassNotFoundException e) {
            LOGGER.error("命令系统初始化失败", e);
        }
        
        // 3. 启动 OneBot 客户端
        String wsUrl = AlphaConfig.getWsUrl();
        OneBotClient.INSTANCE.connect(wsUrl);
        LOGGER.info("[ONEBOT] 正在连接: {}", wsUrl);

        // 4. 启动 Bridge 服务器
        BridgeServer.INSTANCE.start(25561);
        LOGGER.info("[BRIDGE] Bridge 服务器已启动: 25561");
        
        // 5. (可选) 启动 MC 服务器进程
        // ProcessManager.INSTANCE.startServer("./MapBot_Reforged/run", "java -Xmx2G -jar server.jar nogui");

        // 6. 启动 Netty 分流服务器 (主线程阻塞)
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

            LOGGER.info("[SYSTEM] 核心已就绪，监听端口: {}", LISTEN_PORT);
            LOGGER.info("==============================================");
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
