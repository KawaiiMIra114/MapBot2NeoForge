package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.bridge.BridgeProxy;
import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.data.DataManager;

/**
 * CDK 兑换码命令
 * #cdk
 */
public class CdkCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        // 检查绑定
        if (DataManager.INSTANCE.getBinding(senderQQ) == null) {
            return "[错误] 请先使用 #id 绑定账号";
        }
        
        // 通过 Bridge 获取 CDK
        return BridgeProxy.INSTANCE.getCdk(senderQQ);
    }
    
    @Override
    public String getHelp() {
        return "获取签到兑换码";
    }
}
