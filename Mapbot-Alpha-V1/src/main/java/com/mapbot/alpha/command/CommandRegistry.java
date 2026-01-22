package com.mapbot.alpha.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * 指令注册表
 * STEP 6: 指令系统
 */
public class CommandRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Command");
    public static final CommandRegistry INSTANCE = new CommandRegistry();
    
    private final Map<String, CommandInfo> commands = new LinkedHashMap<>();
    
    public CommandRegistry() {
        registerBuiltinCommands();
    }
    
    private void registerBuiltinCommands() {
        // 帮助指令
        register("help", "显示所有可用指令", (args, ctx) -> {
            StringBuilder sb = new StringBuilder("可用指令:\n");
            for (CommandInfo cmd : commands.values()) {
                sb.append("  /").append(cmd.name).append(" - ").append(cmd.description).append("\n");
            }
            ctx.reply(sb.toString());
        });
        
        // 状态指令
        register("status", "显示系统状态", (args, ctx) -> {
            StringBuilder sb = new StringBuilder();
            sb.append("=== MapBot Alpha 状态 ===\n");
            sb.append("MC 进程: ").append(com.mapbot.alpha.process.ProcessManager.INSTANCE.isRunning() ? "运行中" : "未启动").append("\n");
            sb.append("WS 连接数: ").append(com.mapbot.alpha.network.LogWebSocketHandler.getConnectionCount()).append("\n");
            sb.append("Bridge 连接数: ").append(com.mapbot.alpha.bridge.ServerRegistry.INSTANCE.getServerCount()).append("\n");
            ctx.reply(sb.toString());
        });
        
        // 服务器列表
        register("servers", "列出所有已连接的服务器", (args, ctx) -> {
            var servers = com.mapbot.alpha.bridge.ServerRegistry.INSTANCE.getAllServers();
            if (servers.isEmpty()) {
                ctx.reply("没有已连接的服务器");
                return;
            }
            StringBuilder sb = new StringBuilder("已连接的服务器:\n");
            for (var server : servers) {
                sb.append("  - ").append(server.serverId)
                  .append(" (").append(server.isOnline() ? "在线" : "离线").append(")\n");
            }
            ctx.reply(sb.toString());
        });
        
        // 广播指令
        register("say", "向所有服务器广播消息", (args, ctx) -> {
            if (args.length == 0) {
                ctx.reply("用法: /say <消息>");
                return;
            }
            String message = String.join(" ", args);
            String cmd = "say [Alpha] " + message;
            com.mapbot.alpha.bridge.ServerRegistry.INSTANCE.broadcast("{\"type\":\"command\",\"cmd\":\"" + cmd + "\"}");
            ctx.reply("已广播: " + message);
        });
        
        LOGGER.info("已注册 {} 个内置指令", commands.size());
    }
    
    public void register(String name, String description, BiConsumer<String[], CommandContext> handler) {
        commands.put(name.toLowerCase(), new CommandInfo(name, description, handler));
    }
    
    public boolean execute(String input, CommandContext ctx) {
        if (input == null || input.isEmpty()) return false;
        
        // 移除开头的 /
        if (input.startsWith("/")) {
            input = input.substring(1);
        }
        
        String[] parts = input.split("\\s+", 2);
        String cmdName = parts[0].toLowerCase();
        String[] args = parts.length > 1 ? parts[1].split("\\s+") : new String[0];
        
        CommandInfo cmd = commands.get(cmdName);
        if (cmd == null) {
            ctx.reply("未知指令: " + cmdName + " (输入 /help 查看可用指令)");
            return false;
        }
        
        try {
            cmd.handler.accept(args, ctx);
            return true;
        } catch (Exception e) {
            LOGGER.error("执行指令失败: " + cmdName, e);
            ctx.reply("指令执行失败: " + e.getMessage());
            return false;
        }
    }
    
    public Collection<CommandInfo> getAllCommands() {
        return commands.values();
    }
    
    public static class CommandInfo {
        public final String name;
        public final String description;
        public final BiConsumer<String[], CommandContext> handler;
        
        public CommandInfo(String name, String description, BiConsumer<String[], CommandContext> handler) {
            this.name = name;
            this.description = description;
            this.handler = handler;
        }
    }
    
    public interface CommandContext {
        void reply(String message);
        String getSender();
    }
}
