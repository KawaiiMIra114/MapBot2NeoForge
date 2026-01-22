package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.bridge.BridgeProxy;
import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.data.DataManager;
import com.mapbot.alpha.network.OneBotClient;

/**
 * 取消关服命令
 * #cancelstop
 */
public class CancelStopCommand implements ICommand {
    @Override
    public int requiredPermLevel() {
        return DataManager.PERMISSION_LEVEL_ADMIN;
    }

    @Override
    public String getHelp() {
        return "取消正在进行的关服倒计时: #cancelstop";
    }

    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        BridgeProxy.cancelStop().thenAccept(result -> {
            OneBotClient.INSTANCE.sendGroupMessage(sourceGroupId, result);
        });
        return null;
    }
}
