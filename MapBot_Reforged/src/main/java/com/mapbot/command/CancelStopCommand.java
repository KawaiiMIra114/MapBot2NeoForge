package com.mapbot.command;

import com.mapbot.data.DataManager;
import com.mapbot.logic.InboundHandler;
import com.mapbot.logic.ServerStatusManager;

/**
 * 取消关服命令
 * #cancelstop
 */
public class CancelStopCommand implements ICommand {
    @Override
    public int getRequiredLevel() {
        return DataManager.PERMISSION_LEVEL_ADMIN;
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
