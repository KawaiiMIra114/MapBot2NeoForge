package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.bridge.BridgeProxy;
import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.data.DataManager;
import com.mapbot.alpha.logic.SignManager;

/**
 * 领取奖励命令 (Task #022 Redis 版)
 * #accept / #领取
 */
public class AcceptCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        var dm = DataManager.INSTANCE;
        var signMgr = SignManager.INSTANCE;
        
        // 绑定检查
        String uuid = dm.getBinding(senderQQ);
        if (uuid == null) {
            return "[领取失败] 请先使用 #id 绑定账号";
        }
        
        // 检查是否有待领取奖励
        String itemJson = signMgr.getPendingReward(senderQQ);
        if (itemJson == null) {
            return "[领取失败] 无待领取奖励\n[提示] 请先使用 #sign 签到";
        }
        
        // 请求 Mod 端发放物品
        String result = BridgeProxy.INSTANCE.giveItemToOnlineServers(uuid, itemJson);
        
        if (result == null) {
            return "[领取失败] 服务器无响应\n[提示] 请使用 #cdk 获取兑换码";
        }
        
        if (result.startsWith("SUCCESS")) {
            // 发放成功，删除待领取
            signMgr.removePendingReward(senderQQ);
            return result.contains(":") ?
                ("[领取成功] 物品已发放到背包 (" + result.substring(result.indexOf(':') + 1) + ")") :
                "[领取成功] 物品已发放到背包";
        } else if (result.startsWith("FAIL:OFFLINE")) {
            String code = signMgr.createCdk(senderQQ, itemJson);
            if (code == null) {
                return "[领取失败] 玩家不在线\n[提示] 暂无法生成兑换码，请稍后使用 #cdk 获取兑换码";
            }
            return "[领取失败] 玩家全服离线，已生成兑换码: " + code +
                "\n[提示] 请在游戏内使用 /mapbot cdk " + code + " 领取奖励";
        } else if (result.startsWith("FAIL:INVENTORY_FULL")) {
            return "[领取失败] 背包空间不足\n[提示] 请清理背包后重试";
        } else {
            return "[领取失败] " + result.replace("FAIL:", "");
        }
    }
    
    @Override
    public String getHelp() {
        return "领取签到奖励";
    }
}
