package com.mapbot.command;

import com.mapbot.data.DataManager;

/**
 * 命令执行器接口
 */
public interface ICommand {
    /**
     * 执行命令
     * 
     * @param args 命令参数
     * @param senderQQ 发送者 QQ
     * @param sourceGroupId 来源群号
     * @return 执行结果消息 (null 表示已自行处理回复)
     */
    void execute(String args, long senderQQ, long sourceGroupId);
    
    /**
     * 获取所需权限等级
     * 
     * @return 权限等级 (0=User, 1=Mod, 2=Admin)
     */
    default int getRequiredLevel() {
        return DataManager.PERMISSION_LEVEL_USER;
    }
    
    /**
     * 获取命令描述
     */
    String getDescription();
}
