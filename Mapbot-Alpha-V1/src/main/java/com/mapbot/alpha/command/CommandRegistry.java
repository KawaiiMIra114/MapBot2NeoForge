package com.mapbot.alpha.command;

import com.mapbot.alpha.config.AlphaConfig;
import com.mapbot.alpha.data.DataManager;
import com.mapbot.alpha.network.OneBotClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 命令注册中心
 * 从 Reforged 移植到 Alpha Core
 */
public class CommandRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Command");
    
    private static final Map<String, ICommand> commands = new HashMap<>();
    private static final Map<String, String> aliases = new HashMap<>();
    
    /**
     * 注册命令
     */
    public static void register(String name, ICommand cmd) {
        commands.put(name.toLowerCase(), cmd);
        LOGGER.debug("注册命令: #{}", name);
    }
    
    /**
     * 注册别名
     */
    public static void registerAlias(String alias, String target) {
        aliases.put(alias.toLowerCase(), target.toLowerCase());
    }
    
    /**
     * 分发命令
     * @return true 如果命令存在并被处理
     */
    public static boolean dispatch(String cmdName, String args, long senderQQ, long sourceGroupId) {
        String name = cmdName.toLowerCase();
        
        // 解析别名
        if (aliases.containsKey(name)) {
            name = aliases.get(name);
        }
        
        ICommand cmd = commands.get(name);
        if (cmd == null) {
            return false;
        }
        
        // 权限检查: 管理群专属
        if (cmd.adminGroupOnly() && sourceGroupId != AlphaConfig.getAdminGroupId()) {
            sendReply(sourceGroupId, "[权限] 此命令仅限管理群使用");
            return true;
        }
        
        // 权限检查: 管理员
        if (cmd.requiresAdmin() && !DataManager.INSTANCE.isAdmin(senderQQ)) {
            sendReply(sourceGroupId, "[权限] 此命令需要管理员权限");
            return true;
        }
        
        // 权限检查: 等级
        int userLevel = DataManager.INSTANCE.getPermission(senderQQ);
        if (userLevel < cmd.requiredPermLevel()) {
            sendReply(sourceGroupId, "[权限] 权限不足，需要等级 " + cmd.requiredPermLevel());
            return true;
        }
        
        try {
            String result = cmd.execute(args, senderQQ, sourceGroupId);
            if (result != null && !result.isEmpty()) {
                sendReply(sourceGroupId, result);
            }
        } catch (Exception e) {
            LOGGER.error("命令执行异常: #{} {}", name, args, e);
            sendReply(sourceGroupId, "[错误] 命令执行失败: " + e.getMessage());
        }
        
        return true;
    }
    
    /**
     * 发送回复到 QQ 群
     */
    public static void sendReply(long groupId, String message) {
        OneBotClient.INSTANCE.sendGroupMessage(groupId, message);
    }
    
    /**
     * 获取所有命令
     */
    public static Map<String, ICommand> getCommands() {
        return commands;
    }
    
    /**
     * 获取命令帮助文本
     */
    public static String getHelpText() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== MapBot 命令帮助 ===\n");
        for (Map.Entry<String, ICommand> e : commands.entrySet()) {
            sb.append("#").append(e.getKey());
            String help = e.getValue().getHelp();
            if (help != null && !help.isEmpty()) {
                sb.append(" - ").append(help);
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }
}
