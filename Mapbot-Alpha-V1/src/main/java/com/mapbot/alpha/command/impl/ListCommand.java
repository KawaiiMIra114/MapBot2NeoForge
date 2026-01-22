package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.bridge.BridgeProxy;
import com.mapbot.alpha.command.ICommand;

/**
 * 在线列表命令
 * #list / #在线
 */
public class ListCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        return BridgeProxy.INSTANCE.getOnlinePlayerList();
    }
    
    @Override
    public String getHelp() {
        return "查看在线玩家";
    }
}
