package com.mapbot.command;

import com.mapbot.logic.InboundHandler;
import com.mapbot.logic.SignManager;

/**
 * 确认领取命令
 * #accept
 */
public class AcceptCommand implements ICommand {
    @Override
    public String getDescription() {
        return "确认在线领取签到奖励: #accept";
    }

    @Override
    public void execute(String args, long senderQQ, long sourceGroupId) {
        if (!SignManager.INSTANCE.hasPendingReward(senderQQ)) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[提示] 您当前没有待领取的奖励，请先输入 #sign");
            return;
        }

        if (SignManager.INSTANCE.claimOnline(senderQQ)) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[成功] 奖励已发放至背包！");
        } else {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[失败] 领取失败，请确保您在线且已绑定账号。");
        }
    }
}
