package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.bridge.BridgeProxy;
import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.data.DataManager;
import com.mapbot.alpha.logic.SignManager;

/**
 * 签到命令 (Task #022 Redis 版)
 * #sign / #签到
 */
public class SignCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        var dm = DataManager.INSTANCE;
        var signMgr = SignManager.INSTANCE;
        
        // 绑定检查
        String uuid = dm.getBinding(senderQQ);
        if (uuid == null) {
            return "[签到失败] 请先使用 #id 绑定账号";
        }
        
        // 获取玩家名
        String playerName = getPlayerName(senderQQ);
        
        // 已签到检查
        if (signMgr.hasSignedInToday(senderQQ)) {
            int days = signMgr.getSignInDays(senderQQ);
            return String.format("%s 今日已签到\n您累计已签到 %d 天\n[提示] 今日已领取，明天再来吧",
                playerName, days);
        }
        
        // 请求 Mod 端抽奖
        String itemJson = BridgeProxy.INSTANCE.rollLoot();
        if (itemJson == null || itemJson.isEmpty()) {
            return "[错误] 奖池配置异常或服务器离线";
        }
        
        // 解析物品信息
        try {
            var json = com.google.gson.JsonParser.parseString(itemJson).getAsJsonObject();
            String rarity = json.get("rarity").getAsString();
            String name = json.get("name").getAsString();
            int count = json.get("count").getAsInt();
            String rarityMsg = json.has("rarityMsg") ? json.get("rarityMsg").getAsString() : "";
            
            // 记录签到并保存奖励
            signMgr.recordSignIn(senderQQ);
            signMgr.setPendingReward(senderQQ, itemJson);
            
            int days = signMgr.getSignInDays(senderQQ);
            
            // 检查在线状态
            boolean isOnline = BridgeProxy.INSTANCE.isPlayerOnline(uuid);
            
            StringBuilder result = new StringBuilder();
            result.append(playerName).append(" 今日已签到\n");
            result.append("您累计已签到 ").append(days).append(" 天\n");
            result.append("获得物品: [").append(rarity).append("] ").append(name).append(" x").append(count).append("\n");
            result.append(rarityMsg);
            
            if (isOnline) {
                result.append("\n若确认背包有空间，请输入 #accept 指令来确认物品发放");
            } else {
                result.append("\n您当前未在线");
                result.append("\n请私聊机器人输入 #cdk 来获取兑换码");
                result.append("\n上线后使用 /mapbot cdk [your-cdkey] 来兑换物品");
            }
            
            return result.toString();
            
        } catch (Exception e) {
            return "[错误] 签到失败: " + e.getMessage();
        }
    }
    
    private String getPlayerName(long qq) {
        // 尝试从已连接的服务器获取玩家名
        String uuid = DataManager.INSTANCE.getBinding(qq);
        if (uuid == null) return "未知玩家";
        
        // 向 Mod 请求玩家名 (简化：直接返回 UUID 前缀)
        return uuid.substring(0, 8);
    }
    
    @Override
    public String getHelp() {
        return "每日签到抽奖";
    }
}
