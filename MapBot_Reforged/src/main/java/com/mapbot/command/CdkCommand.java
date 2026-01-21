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
            // 注意: 这里应该尝试私聊发送，但 InboundHandler 暂无 sendPrivateMessage 封装
            // 这里为了简单，先发群临时消息 (go-cqhttp 支持群里发At)
            // 或者直接明文发群里 (如果是私聊触发的命令)
            // 改进: 提示用户这是兑换码
            InboundHandler.sendReplyToQQ(sourceGroupId, 
                String.format("[CQ:at,qq=%d] 您的兑换码: %s\n请进服输入: /mapbot cdk %s", senderQQ, code, code));
        } else {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 生成失败");
        }
    }
}
