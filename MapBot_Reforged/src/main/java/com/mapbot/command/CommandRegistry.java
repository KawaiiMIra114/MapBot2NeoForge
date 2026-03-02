package com.mapbot.command;

import com.mapbot.logic.InboundHandler;
import com.mapbot.security.AuthorizationEngine;
import com.mapbot.security.AuthorizationEngine.AuthResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 命令注册表
 * 负责命令的注册、分发和鉴权（通过 AuthorizationEngine 代理）。
 *
 * Task04 重构: 移除内联的 DataManager 权限数字比较，
 * 全部改为调用 AuthorizationEngine.authorize()。
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
     * 使用 AuthorizationEngine 进行鉴权判定，取代旧版魔法数字比较。
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
        
        // 通过 AuthorizationEngine 统一鉴权
        AuthResult result = AuthorizationEngine.INSTANCE.authorize(
                senderQQ, commandName.toLowerCase(), cmd.getCategory());

        if (!result.isAllowed()) {
            // 鉴权失败 — 返回 AUTH-403 拒绝消息
            InboundHandler.sendReplyToQQ(sourceGroupId, result.getMessage());
            return true; // 命令已识别，但被拒绝
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
