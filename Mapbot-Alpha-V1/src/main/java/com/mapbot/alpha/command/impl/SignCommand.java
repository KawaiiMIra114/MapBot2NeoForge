package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.bridge.BridgeProxy;
import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.data.DataManager;

/**
 * 签到命令
 * #sign / #签到
 */
public class SignCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        // 绑定检查
        if (DataManager.INSTANCE.getBinding(senderQQ) == null) {
            return "[签到失败] 请先使用 #id 绑定账号";
        }
        
        // 通过 Bridge 执行签到
        return BridgeProxy.INSTANCE.signIn(senderQQ);
    }
    
    @Override
    public String getHelp() {
        return "每日签到抽奖";
    }
}
