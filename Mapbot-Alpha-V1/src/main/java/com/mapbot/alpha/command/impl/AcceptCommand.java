package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.bridge.BridgeProxy;
import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.data.DataManager;

/**
 * 领取奖励命令
 * #accept / #领取
 */
public class AcceptCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        if (DataManager.INSTANCE.getBinding(senderQQ) == null) {
            return "[领取失败] 请先使用 #id 绑定账号";
        }
        
        return BridgeProxy.INSTANCE.acceptReward(senderQQ);
    }
    
    @Override
    public String getHelp() {
        return "领取签到奖励";
    }
}
