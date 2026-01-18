/*
 * MapBot Reforged - 服务器状态管理器
 * 
 * 遵从 .ai_rules.md 中定义的治理规则。
 * 提供服务器状态查询功能。
 * 
 * 参考: MapBotV4 的 ListPlayers.java, CheckTPS.java, ServerInfo.java
 */

package com.mapbot.logic;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mapbot.config.BotConfig;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 服务器状态管理器
 * 提供服务器信息查询功能
 */
public class ServerStatusManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/Status");
    
    // ================== 自定义 MSPT 监控 ==================
    
    /** Tick 时间缓冲区 (纳秒)，存储最近 100 个 tick 的执行时间 */
    private static final long[] localTickTimes = new long[100];
    
    /** 当前写入位置 (循环缓冲区) */
    private static int tickIndex = 0;
    
    /**
     * 记录一次 tick 的执行时间
     * 由 GameEventListener 的 ServerTickEvent 调用
     * 
     * @param durationNanos tick 执行耗时 (纳秒)
     */
    public static void recordTick(long durationNanos) {
        if (tickIndex >= localTickTimes.length) {
            tickIndex = 0;
        }
        localTickTimes[tickIndex++] = durationNanos;
    }
    
    /**
     * 获取平均 MSPT (毫秒/tick)
     * 从本地缓冲区计算
     * 
     * @return 平均 MSPT (毫秒)
     */
    public static double getAverageMSPT() {
        double averageNanos = Arrays.stream(localTickTimes).average().orElse(0);
        return averageNanos / 1_000_000.0; // 纳秒 -> 毫秒
    }
    
    /**
     * 获取当前 TPS (基于 MSPT 换算)
     * @return TPS 值 (最大 20.0)
     */
    public static double getCurrentTPS() {
        double mspt = getAverageMSPT();
        if (mspt <= 0) return 20.0;
        return Math.min(20.0, 1000.0 / mspt);
    }

    // ================== Task #016 STEP1: TPS 分级监控告警 ==================
    
    /** TPS 告警阈值 (从高到低) */
    private static final int[] TPS_THRESHOLDS = {18, 16, 14, 12, 10};
    
    /** 告警等级名称 */
    private static final String[] ALERT_LEVELS = {"轻微", "注意", "警告", "严重", "危急"};
    
    /** 每级别连续告警计数 */
    private static final int[] alertCounts = new int[5];
    
    /** 是否已暂停该级别告警 (连续3次后暂停) */
    private static final boolean[] alertPaused = new boolean[5];
    
    /** 上一次触发的告警等级 (-1 表示无告警) */
    private static int lastAlertLevel = -1;
    
    /** 定时任务执行器 */
    private static ScheduledExecutorService scheduler;
    
    /**
     * 启动 TPS 监控
     * 应在服务器启动后调用
     */
    public static void startTPSMonitor() {
        if (scheduler != null && !scheduler.isShutdown()) {
            LOGGER.warn("TPS 监控器已在运行");
            return;
        }
        
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MapBot-TPSMonitor");
            t.setDaemon(true);
            return t;
        });
        
        // 每 30 秒检测一次
        scheduler.scheduleAtFixedRate(ServerStatusManager::checkTPS, 60, 30, TimeUnit.SECONDS);
        LOGGER.info("TPS 监控器已启动 (间隔 30 秒)");
    }
    
    /**
     * 停止 TPS 监控
     * 应在服务器关闭时调用
     */
    public static void stopTPSMonitor() {
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
            LOGGER.info("TPS 监控器已停止");
        }
    }
    
    /**
     * 检测 TPS 并发送告警
     * Task #016 STEP1 核心逻辑
     */
    private static void checkTPS() {
        try {
            double tps = getCurrentTPS();
            
            // 找到触发的最高告警等级 (从等级5到等级1)
            int triggeredLevel = -1;
            for (int i = TPS_THRESHOLDS.length - 1; i >= 0; i--) {
                if (tps < TPS_THRESHOLDS[i]) {
                    triggeredLevel = i;
                    break;
                }
            }
            
            // TPS 正常，重置所有计数和暂停状态
            if (triggeredLevel == -1) {
                if (lastAlertLevel >= 0) {
                    // TPS 回升，重置状态
                    Arrays.fill(alertCounts, 0);
                    Arrays.fill(alertPaused, false);
                    lastAlertLevel = -1;
                    LOGGER.debug("TPS 回升至正常，重置告警状态");
                }
                return;
            }
            
            // 如果该级别已暂停，不发送
            if (alertPaused[triggeredLevel]) {
                return;
            }
            
            // 增加计数
            alertCounts[triggeredLevel]++;
            
            // 发送告警
            int level = triggeredLevel + 1; // 显示等级 1-5
            String levelName = ALERT_LEVELS[triggeredLevel];
            int threshold = TPS_THRESHOLDS[triggeredLevel];
            
            String message = String.format("[警告] TPS低 (%.1f < %d)\n等级: %s",
                    tps, threshold, levelName);
            
            // 发送到 OP 群
            long opGroupId = BotConfig.getOpGroupId();
            if (opGroupId > 0) {
                InboundHandler.sendReplyToQQ(opGroupId, message);
                LOGGER.warn("TPS 告警: {} (等级{})", message, level);
            }
            
            // 连续 3 次后暂停该等级
            if (alertCounts[triggeredLevel] >= 3) {
                alertPaused[triggeredLevel] = true;
                String pauseMsg = String.format("[系统] TPS已连续3次低于%d，暂停等级%d告警",
                        threshold, level);
                if (opGroupId > 0) {
                    InboundHandler.sendReplyToQQ(opGroupId, pauseMsg);
                }
                LOGGER.info("暂停等级 {} 告警", level);
            }
            
            lastAlertLevel = triggeredLevel;
            
        } catch (Exception e) {
            LOGGER.error("TPS 检测异常", e);
        }
    }

    // ================== 查询方法 ==================

    /**
     * 获取在线玩家列表
     * 
     * @return 格式化的玩家列表字符串
     */
    public static String getList() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        
        if (server == null) {
            return "[错误] 服务器未就绪";
        }
        
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        int online = players.size();
        int max = server.getMaxPlayers();
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[在线玩家] %d/%d\n", online, max));
        
        if (online == 0) {
            sb.append("-");
        } else {
            String names = players.stream()
                .map(p -> p.getName().getString())
                .collect(Collectors.joining(", "));
            sb.append(names);
        }
        
        return sb.toString();
    }

    /**
     * 获取服务器详细信息
     * 
     * @return 格式化的服务器状态字符串
     */
    public static String getServerInfo() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        
        if (server == null) {
            return "[错误] 服务器未就绪";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("[服务器状态]\n");
        
        // MSPT (Milliseconds Per Tick) - 使用自管理缓冲区
        double mspt = getAverageMSPT();
        double tps = Math.min(20.0, 1000.0 / Math.max(mspt, 1.0));
        
        sb.append(String.format("TPS: %.1f  |  MSPT: %.2fms\n", tps, mspt));
        
        // 内存使用
        Runtime runtime = Runtime.getRuntime();
        long usedMB = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMB = runtime.maxMemory() / 1024 / 1024;
        double memPercent = (double) usedMB / maxMB * 100;
        
        sb.append(String.format("内存: %d/%d MB (%.0f%%)\n", usedMB, maxMB, memPercent));
        
        // 在线人数
        int online = server.getPlayerCount();
        int max = server.getMaxPlayers();
        sb.append(String.format("玩家: %d/%d\n", online, max));
        
        // 世界信息
        String levelName = server.getWorldData().getLevelName();
        sb.append(String.format("世界: %s", levelName));
        
        return sb.toString();
    }

    /**
     * 获取帮助信息
     * 
     * @return 格式化的命令帮助字符串
     */
    public static String getHelp() {
        return """
            [MapBot 命令列表]
            ------------------
            #id <ID>     绑定游戏账号
            #unbind      解绑账号
            #list        在线玩家列表
            #status      服务器状态
            #playtime    查询在线时长
            #help        显示此列表
            
            [管理命令]
            #inv <ID> [-e]  查背包/末影箱
            #location <ID>  查坐标
            #stopserver     关闭服务器
            #addadmin <QQ>  添加管理员
            #reload         重载配置
            ------------------
            注: 普通消息自动转发至游戏""";
    }

    /**
     * 停止服务器
     * TODO: 实现权限检查 (BotConfig.adminQQ)
     * 
     * @return 操作结果消息
     */
    public static String stopServer() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        
        if (server == null) {
            return "❌ 服务器未就绪";
        }
        
        // TODO: 权限检查
        // if (!BotConfig.getAdminList().contains(senderQQ)) {
        //     return "❌ 权限不足";
        // }
        
        LOGGER.warn("收到远程停服命令，服务器即将关闭...");
        
        // 使用 server.halt(false) 优雅关闭
        // halt(true) = 强制关闭, halt(false) = 正常关闭
        server.halt(false);
        
        return "⏹️ 服务器正在关闭...";
    }
}
