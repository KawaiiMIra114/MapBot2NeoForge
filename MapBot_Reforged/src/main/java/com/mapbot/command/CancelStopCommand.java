package com.mapbot.command;

import com.mapbot.data.DataManager;
import com.mapbot.security.CommandCategory;
import com.mapbot.logic.InboundHandler;
import com.mapbot.logic.ServerStatusManager;

/**
 * 取消关服命令
 * #cancelstop
 */
public class CancelStopCommand implements ICommand {
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.SENSITIVE_WRITE;
    }

    @Override
    public String getDescription() {
        return "取消正在进行的关服倒计时: #cancelstop";
    }

    @Override
    public void execute(String args, long senderQQ, long sourceGroupId) {
        ServerStatusManager.setStopCancelled(true);
        InboundHandler.sendReplyToQQ(sourceGroupId, "[系统] 已发送取消请求");
    }
}
