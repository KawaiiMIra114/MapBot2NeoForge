package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.data.DataManager;

/**
 * 解绑命令
 * #unbind / #解绑
 */
public class UnbindCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        if (DataManager.INSTANCE.getBinding(senderQQ) == null) {
            return "[解绑失败] 你还没有绑定任何账号";
        }
        
        if (DataManager.INSTANCE.unbind(senderQQ)) {
            return "[解绑成功] 已解除绑定\n注意: 白名单不会自动移除";
        }
        
        return "[错误] 解绑失败";
    }
    
    @Override
    public String getHelp() {
        return "解绑游戏账号";
    }
}
