package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.bridge.BridgeProxy;
import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.data.DataManager;

/**
 * 签到命令 (Task #03 重构: Bridge 转发模式)
 * #sign / #签到
 * 
 * Alpha 不再本地计算签到逻辑，全部转发给 Reforged 端处理。
 */
public class SignCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        // 绑定检查（Alpha 作为网关仍需做前置鉴权）
        String uuid = DataManager.INSTANCE.getBinding(senderQQ);
        if (uuid == null) {
            return "[签到失败] 请先使用 #id 绑定账号";
        }
        
        // 转发给 Reforged 端处理签到业务
        String result = BridgeProxy.INSTANCE.signIn(senderQQ);
        if (result == null || result.isEmpty()) {
            return "[错误] 服务器离线或无响应";
        }
        return result;
    }
    
    @Override
    public String getHelp() {
        return "每日签到抽奖";
    }
}
