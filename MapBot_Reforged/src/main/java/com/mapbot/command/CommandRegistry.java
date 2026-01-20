package com.mapbot.command;

import com.mapbot.data.DataManager;
import com.mapbot.logic.InboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 命令注册表
 * 负责命令的分发和权限检查
 */
public class CommandRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/Command");
    private static final Map<String, ICommand> commands = new HashMap<>();
    
    /**
     * 注册命令
     * 
     * @param name 命令名称 (小写)
     * @param command 命令执行器
     */
    public static void register(String name, ICommand command) {
        commands.put(name.toLowerCase(), command);
    }
    
    /**
     * 注册命令别名
     * 
     * @param alias 别名
     * @param targetCommand 目标命令名称
     */
    public static void registerAlias(String alias, String targetCommand) {
        ICommand cmd = commands.get(targetCommand.toLowerCase());
        if (cmd != null) {
            commands.put(alias.toLowerCase(), cmd);
        } else {
            LOGGER.warn("无法注册别名 {}: 目标命令 {} 不存在", alias, targetCommand);
        }
    }
    
    /**
     * 分发命令
     * 
     * @param commandName 命令名称
     * @param args 参数字符串
     * @param senderQQ 发送者 QQ
     * @param sourceGroupId 来源群号
     * @return 是否成功找到并执行命令
     */
    public static boolean dispatch(String commandName, String args, long senderQQ, long sourceGroupId) {
        ICommand cmd = commands.get(commandName.toLowerCase());
        
        if (cmd == null) {
            return false;
        }
        
        // 权限检查
        int userLevel = DataManager.INSTANCE.getPermissionLevel(senderQQ);
        int requiredLevel = cmd.getRequiredLevel();
        
        if (userLevel < requiredLevel) {
            InboundHandler.sendReplyToQQ(sourceGroupId, 
                String.format("[权限拒绝] 此命令需要 Level %d 权限 (当前: %d)", requiredLevel, userLevel));
            return true;
        }
        
        try {
            cmd.execute(args, senderQQ, sourceGroupId);
        } catch (Exception e) {
            LOGGER.error("命令执行出错: {}", commandName, e);
            InboundHandler.sendReplyToQQ(sourceGroupId, "[系统错误] 命令执行发生异常");
        }
        
        return true;
    }
    
    /**
     * 获取所有注册的命令名称
     */
    public static Set<String> getCommandNames() {
        return commands.keySet();
    }
}
