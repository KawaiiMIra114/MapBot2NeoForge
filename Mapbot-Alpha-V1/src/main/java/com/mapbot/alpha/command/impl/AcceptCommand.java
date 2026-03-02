package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.bridge.BridgeProxy;
import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.data.DataManager;

/**
 * 领取奖励命令 (Task #03 重构: Bridge 转发模式)
 * #accept / #领取
 * 
 * Alpha 不再本地管理待领取奖励，全部转发给 Reforged 端处理。
 */
public class AcceptCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        // 绑定检查
        String uuid = DataManager.INSTANCE.getBinding(senderQQ);
        if (uuid == null) {
            return "[领取失败] 请先使用 #id 绑定账号";
        }
        
        // 转发给 Reforged 端处理领取业务
        String result = BridgeProxy.INSTANCE.acceptReward(senderQQ);
        if (result == null || result.isEmpty()) {
            return "[错误] 服务器离线或无响应";
        }
        return result;
    }
    
    @Override
    public String getHelp() {
        return "领取签到奖励";
    }
}
