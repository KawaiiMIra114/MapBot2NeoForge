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

        // 0.2. 初始化 Redis
        com.mapbot.alpha.database.RedisManager.INSTANCE.init();
        
        // 0.5. 初始化认证管理器
        com.mapbot.alpha.security.AuthManager.INSTANCE.init();
        LOGGER.info("[AUTH] 认证管理器已初始化");
        if (!com.mapbot.alpha.security.AuthManager.INSTANCE.isBridgeAuthEnabled()) {
            LOGGER.warn("[SECURITY] Bridge 鉴权未就绪：默认拒绝所有 Bridge 注册，请配置 auth.bridge.token 与 auth.bridge.allowedServerIds");
        }
        
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

        int listenPort = AlphaConfig.getListenPort();
        int bridgePort = AlphaConfig.getBridgeListenPort();
        LOGGER.info("[NETWORK] 智能分流端口: {}", listenPort);
        LOGGER.info("[NETWORK] Bridge 端口: {}", bridgePort);
        LOGGER.info("[NETWORK] MC 转发目标: {}:{}", AlphaConfig.getTargetMcHost(), AlphaConfig.getTargetMcPort());

        // 4. 启动 Bridge 服务器
        BridgeServer.INSTANCE.start(bridgePort);
        LOGGER.info("[BRIDGE] Bridge 服务器已启动: {}", bridgePort);
        
        // 4.3. 初始化指标存储并加载历史数据
        com.mapbot.alpha.metrics.MetricsStorage.INSTANCE.init();
        LOGGER.info("[METRICS] 指标存储已加载");
        
        // 4.5. 启动性能指标收集器
        com.mapbot.alpha.metrics.MetricsCollector.INSTANCE.start();
        LOGGER.info("[METRICS] 性能指标收集器已启动");
        
        // 5. (可选) 启动 MC 服务器进程
        // ProcessManager.INSTANCE.startServer("./MapBot_Reforged/run", "java -Xmx2G -jar server.jar nogui");

        // 5.2. 控制台 stop/exit 触发优雅关闭
        startConsoleStopWatcher();

        // 5.5. 添加退出钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("[SYSTEM] 正在关闭，保存数据...");
            long playerGroupId = AlphaConfig.getPlayerGroupId();
            if (playerGroupId > 0) {
                boolean sent = OneBotClient.INSTANCE.sendGroupMessageBlocking(playerGroupId, "吃饱睡觉。", 1200);
                if (!sent) {
                    LOGGER.warn("[SYSTEM] 关机通知发送失败或超时");
                }
            }
            com.mapbot.alpha.metrics.MetricsStorage.INSTANCE.save();
            com.mapbot.alpha.security.AuthManager.INSTANCE.saveUsers();
            com.mapbot.alpha.database.RedisManager.INSTANCE.shutdown();
        }));

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

            LOGGER.info("[SYSTEM] 核心已就绪，监听端口: {}", listenPort);
            LOGGER.info("==============================================");
            ChannelFuture f = b.bind(listenPort).sync();
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            LOGGER.error("[ERROR] 核心崩溃: {}", e.getMessage());
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    private static void startConsoleStopWatcher() {
        Thread consoleWatcher = new Thread(() -> {
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(System.in))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String cmd = line.trim();
                    if ("stop".equalsIgnoreCase(cmd) || "exit".equalsIgnoreCase(cmd)) {
                        LOGGER.info("[SYSTEM] 收到控制台停止指令: {}", cmd);
                        System.exit(0);
                        return;
                    }
                }
            } catch (Exception ignored) {
            }
        }, "Mapbot-Alpha-ConsoleWatcher");
        consoleWatcher.setDaemon(true);
        consoleWatcher.start();
    }
}
