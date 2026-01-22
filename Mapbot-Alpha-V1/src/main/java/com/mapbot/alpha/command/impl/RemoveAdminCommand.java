package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.data.DataManager;

/**
 * 移除管理员命令
 * #removeadmin <QQ号>
 */
public class RemoveAdminCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        String target = args.trim().replaceAll("[^0-9]", "");
        if (target.isEmpty()) {
            return "[错误] 用法: #removeadmin <QQ号>";
        }
        
        long targetQQ = Long.parseLong(target);
        DataManager.INSTANCE.removeAdmin(targetQQ);
        return String.format("[成功] 已移除 QQ %d 的管理员权限", targetQQ);
    }
    
    @Override
    public String getHelp() {
        return "移除管理员";
    }
    
    @Override
    public boolean requiresAdmin() {
        return true;
    }
}
