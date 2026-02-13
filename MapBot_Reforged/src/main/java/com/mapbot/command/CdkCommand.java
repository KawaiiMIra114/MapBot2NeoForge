package com.mapbot.command;

import com.mapbot.logic.InboundHandler;
import com.mapbot.logic.SignManager;

/**
 * 获取兑换码命令
 * #cdk
 */
public class CdkCommand implements ICommand {
    @Override
    public String getDescription() {
        return "获取签到奖励兑换码: #cdk";
    }

    @Override
    public void execute(String args, long senderQQ, long sourceGroupId) {
        if (!SignManager.INSTANCE.hasPendingReward(senderQQ)) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[提示] 您当前没有待领取的奖励，请先输入 #sign");
            return;
        }

        String code = SignManager.INSTANCE.generateCdk(senderQQ);
        if (code != null) {
            // Fix #2: 兑换码通过私聊发送，避免在群聊中泄露
            InboundHandler.sendPrivateMessage(senderQQ, 
                String.format("您的兑换码: %s\n请进服输入: /mapbot cdk %s", code, code));
            InboundHandler.sendReplyToQQ(sourceGroupId, 
                String.format("[CQ:at,qq=%d] 兑换码已通过私聊发送，请查看私信。", senderQQ));
        } else {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 生成失败");
        }
    }
}
