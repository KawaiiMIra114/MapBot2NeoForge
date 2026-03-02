package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.bridge.BridgeProxy;
import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.data.DataManager;

/**
 * CDK 兑换码命令 (Task #03 重构: Bridge 转发模式)
 * #cdk
 * 
 * Alpha 不再本地生成 CDK，全部转发给 Reforged 端处理。
 */
public class CdkCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        // 绑定检查
        if (DataManager.INSTANCE.getBinding(senderQQ) == null) {
            return "[错误] 请先使用 #id 绑定账号";
        }
        
        // 转发给 Reforged 端处理 CDK 生成
        String result = BridgeProxy.INSTANCE.getCdk(senderQQ);
        if (result == null || result.isEmpty()) {
            return "[错误] 服务器离线或无响应";
        }
        return result;
    }
    
    @Override
    public String getHelp() {
        return "获取签到兑换码";
    }
}
