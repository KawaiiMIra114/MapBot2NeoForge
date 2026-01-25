package com.mapbot.alpha.network;

import com.mapbot.alpha.bridge.ServerRegistry;
import com.mapbot.alpha.process.ProcessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web 控制台命令处理器
 * 处理以 / 开头的 Alpha 内置指令
 */
public class ConsoleCommandHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Console");
    
    /**
     * 处理控制台命令
     * @return 命令执行结果，null 表示无响应
     */
    public static String handle(String cmd) {
        String[] parts = cmd.split("\\s+", 2);
        String name = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";
        
        LOGGER.debug("处理控制台命令: {} {}", name, args);
        
        return switch (name) {
            case "status" -> getStatus();
            case "help" -> getHelp();
            case "servers" -> getServers();
            case "broadcast" -> broadcast(args);
            default -> "未知命令: " + name + "。输入 /help 查看帮助。";
        };
    }
    
    private static String getStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Alpha Core 状态 ===\n");
        sb.append("MC 进程: ").append(ProcessManager.INSTANCE.isRunning() ? "运行中" : "未启动").append("\n");
        sb.append("Bridge 连接: ").append(ServerRegistry.INSTANCE.getServerCount()).append(" 台\n");
        sb.append("WebSocket: ").append(LogWebSocketHandler.getConnectionCount()).append(" 个连接");
        return sb.toString();
    }
    
    private static String getHelp() {
        return """
            === Alpha 控制台帮助 ===
            /status    - 查看系统状态
            /help      - 显示帮助
            /servers   - 列出已连接服务器
            /broadcast <消息> - 向所有服务器广播
            
            提示: 不以 / 开头的命令将发送给 MC 服务器""";
    }
    
    private static String getServers() {
        var servers = ServerRegistry.INSTANCE.getAllServers();
        if (servers.isEmpty()) {
            return "暂无已连接的服务器";
        }
        
        StringBuilder sb = new StringBuilder("=== 已连接服务器 ===\n");
        for (var s : servers) {
            sb.append("• ").append(s.serverId)
              .append(" | 玩家: ").append(s.players)
              .append(" | TPS: ").append(s.tps)
              .append(" | ").append(s.isOnline() ? "在线" : "离线")
              .append("\n");
        }
        return sb.toString().trim();
    }
    
    private static String broadcast(String message) {
        if (message.isEmpty()) {
            return "用法: /broadcast <消息>";
        }
        // 修复 #1: 使用正确的 JSON 格式发送给 Reforged
        String json = String.format("{\"type\":\"broadcast\",\"requestId\":\"%s\",\"arg1\":\"%s\"}",
                System.currentTimeMillis(), escapeJson(message));
        ServerRegistry.INSTANCE.broadcast(json);
        return "已广播消息给 " + ServerRegistry.INSTANCE.getServerCount() + " 台服务器";
    }
    
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
