package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.data.DataManager;

/**
 * 添加管理员命令
 * #addadmin <QQ号>
 */
public class AddAdminCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        String target = args.trim().replaceAll("[^0-9]", "");
        if (target.isEmpty()) {
            return "[错误] 用法: #addadmin <QQ号>";
        }
        
        long targetQQ = Long.parseLong(target);
        DataManager.INSTANCE.addAdmin(targetQQ);
        return String.format("[成功] 已将 QQ %d 设为管理员", targetQQ);
    }
    
    @Override
    public String getHelp() {
        return "添加管理员";
    }
    
    @Override
    public boolean requiresAdmin() {
        return true;
    }
}
