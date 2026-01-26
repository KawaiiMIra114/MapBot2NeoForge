package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.data.DataManager;
import com.mapbot.alpha.logic.SignManager;

/**
 * CDK 兑换码命令 (Task #022 Redis 版)
 * #cdk
 */
public class CdkCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        var dm = DataManager.INSTANCE;
        var signMgr = SignManager.INSTANCE;
        
        // 检查绑定
        if (dm.getBinding(senderQQ) == null) {
            return "[错误] 请先使用 #id 绑定账号";
        }
        
        // 检查是否有待领取奖励
        String itemJson = signMgr.getPendingReward(senderQQ);
        if (itemJson == null) {
            return "[错误] 无待领取奖励\n[提示] 请先使用 #sign 签到";
        }
        
        // 生成 CDK
        String code = signMgr.createCdk(senderQQ, itemJson);
        if (code == null) {
            return "[错误] CDK 生成失败";
        }
        
        return String.format("[兑换码] %s\n有效期 24 小时\n进服后输入 /mapbot cdk %s 领取", code, code);
    }
    
    @Override
    public String getHelp() {
        return "获取签到兑换码";
    }
}
