package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.data.DataManager;

/**
 * 解禁命令
 * #unmute <QQ号/@>
 */
public class UnmuteCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        String target = args.trim().replaceAll("[^0-9]", "");
        if (target.isEmpty()) {
            return "[错误] 用法: #unmute <QQ号/@提及>";
        }
        
        long targetQQ = Long.parseLong(target);
        String uuid = DataManager.INSTANCE.getBinding(targetQQ);
        if (uuid == null) {
            return "[错误] 该QQ未绑定游戏账号";
        }
        
        DataManager.INSTANCE.unmute(uuid);
        return String.format("[解禁成功] QQ %d 已解除禁言", targetQQ);
    }
    
    @Override
    public String getHelp() {
        return "解除禁言: #unmute <QQ>";
    }
    
    @Override
    public boolean requiresAdmin() {
        return true;
    }
}
