package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.data.DataManager;

/**
 * 强制解绑命令
 * #adminunbind <QQ号>
 */
public class ForceUnbindCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        String target = args.trim().replaceAll("[^0-9]", "");
        if (target.isEmpty()) {
            return "[错误] 用法: #adminunbind <QQ号>";
        }
        
        long targetQQ = Long.parseLong(target);
        
        if (DataManager.INSTANCE.getBinding(targetQQ) == null) {
            return "[错误] 该QQ未绑定任何账号";
        }
        
        if (DataManager.INSTANCE.unbind(targetQQ)) {
            return String.format("[成功] 已强制解绑 QQ %d", targetQQ);
        }
        
        return "[错误] 解绑失败";
    }
    
    @Override
    public String getHelp() {
        return "强制解绑: #adminunbind <QQ>";
    }
    
    @Override
    public boolean requiresAdmin() {
        return true;
    }
}
