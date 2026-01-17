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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 服务器状态管理器
 * 提供服务器信息查询功能
 */
public class ServerStatusManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/Status");

    /**
     * 获取在线玩家列表
     * 
     * @return 格式化的玩家列表字符串
     */
    public static String getList() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        
        if (server == null) {
            return "❌ 服务器未就绪";
        }
        
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        int online = players.size();
        int max = server.getMaxPlayers();
        
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("📊 在线玩家: %d/%d\n", online, max));
        
        if (online == 0) {
            sb.append("(暂无玩家在线)");
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
            return "❌ 服务器未就绪";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("📊 服务器状态\n");
        sb.append("─────────\n");
        
        // MSPT (Milliseconds Per Tick)
        // 1.21.1: 从 tickTimes 数组手动计算 (纳秒 -> 毫秒)
        double mspt = Arrays.stream(server.tickTimes).average().orElse(0) * 1.0E-6D;
        double tps = Math.min(20.0, 1000.0 / Math.max(mspt, 1.0));
        
        sb.append(String.format("⏱️ MSPT: %.2f ms\n", mspt));
        sb.append(String.format("📈 TPS: %.1f\n", tps));
        
        // 内存使用
        Runtime runtime = Runtime.getRuntime();
        long usedMB = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMB = runtime.maxMemory() / 1024 / 1024;
        double memPercent = (double) usedMB / maxMB * 100;
        
        sb.append(String.format("💾 内存: %d/%d MB (%.1f%%)\n", usedMB, maxMB, memPercent));
        
        // 在线人数
        int online = server.getPlayerCount();
        int max = server.getMaxPlayers();
        sb.append(String.format("👥 玩家: %d/%d\n", online, max));
        
        // 世界信息
        String levelName = server.getWorldData().getLevelName();
        sb.append(String.format("🌍 世界: %s", levelName));
        
        return sb.toString();
    }

    /**
     * 获取帮助信息
     * 
     * @return 格式化的命令帮助字符串
     */
    public static String getHelp() {
        return """
            📖 MapBot Reforged 命令帮助
            ─────────────────────
            #id <游戏ID> - 绑定QQ与游戏账号
            #unbind - 解绑游戏账号
            #list / #在线 - 查看在线玩家
            #tps / #status - 查看服务器状态
            #inv <玩家名> - 查看玩家背包
            #help / #菜单 - 显示此帮助
            ─────────────────────
            ⚙️ 管理员命令:
            #stopserver - 关闭服务器
            #addadmin <QQ> - 添加管理员
            #removeadmin <QQ> - 移除管理员
            #adminunbind <QQ> - 强制解绑
            #reload - 重载配置
            ─────────────────────
            普通消息将转发到游戏内""";
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
