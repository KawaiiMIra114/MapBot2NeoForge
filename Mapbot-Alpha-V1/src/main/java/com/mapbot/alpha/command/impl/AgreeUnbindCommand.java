package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.data.DataManager;
import com.mapbot.alpha.security.ContractRole;

/**
 * 管理员强制解绑命令
 * #agreeunbind <QQ号>
 * 
 * 用途：处理绑定冲突，管理员核实后强制解除指定 QQ 的绑定
 * 
 * 流程：
 * 1. 玩家 A 发现账号被 QQ B 占用
 * 2. 玩家 A 联系管理员
 * 3. 管理员核实后执行 #agreeunbind <B的QQ>
 * 4. 解绑成功，玩家 A 可重新绑定
 */
public class AgreeUnbindCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        String target = args.trim().replaceAll("[^0-9]", "");
        if (target.isEmpty()) {
            return "[错误] 用法: #agreeunbind <QQ号>\n" +
                   "示例: #agreeunbind 123456789";
        }
        
        long targetQQ = Long.parseLong(target);
        
        // 检查是否已绑定
        String uuid = DataManager.INSTANCE.getBinding(targetQQ);
        if (uuid == null) {
            return String.format("[提示] QQ %d 未绑定任何账号", targetQQ);
        }
        
        // 执行解绑
        boolean success = DataManager.INSTANCE.unbind(targetQQ);
        if (success) {
            return String.format("[成功] 已强制解绑 QQ %d (UUID: %s)", targetQQ, uuid);
        } else {
            return "[错误] 解绑失败";
        }
    }
    
    @Override
    public String getHelp() {
        return "管理员强制解绑: #agreeunbind <QQ号>";
    }
    
    @Override
    public ContractRole requiredRole() {
        return ContractRole.ADMIN;
    }
}
