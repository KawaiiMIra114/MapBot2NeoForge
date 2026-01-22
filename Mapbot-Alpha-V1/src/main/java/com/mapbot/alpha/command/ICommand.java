package com.mapbot.alpha.command;

/**
 * 命令接口
 * 从 Reforged 移植
 */
public interface ICommand {
    
    /**
     * 执行命令
     * @param args 命令参数
     * @param senderQQ 发送者 QQ
     * @param sourceGroupId 来源群号
     * @return 执行结果消息 (null 表示无需回复)
     */
    String execute(String args, long senderQQ, long sourceGroupId);
    
    /**
     * 获取命令帮助
     */
    String getHelp();
    
    /**
     * 是否需要管理员权限
     */
    default boolean requiresAdmin() {
        return false;
    }
    
    /**
     * 是否仅限管理群使用
     */
    default boolean adminGroupOnly() {
        return false;
    }
    
    /**
     * 最低权限等级 (0=普通, 1=VIP, 2=OP)
     */
    default int requiredPermLevel() {
        return 0;
    }
}
