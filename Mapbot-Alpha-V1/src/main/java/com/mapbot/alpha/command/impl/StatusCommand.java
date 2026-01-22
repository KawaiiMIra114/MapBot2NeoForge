package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.bridge.BridgeProxy;
import com.mapbot.alpha.command.ICommand;

/**
 * 服务器状态命令
 * #status / #tps / #状态
 */
public class StatusCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        return BridgeProxy.INSTANCE.getServerStatus();
    }
    
    @Override
    public String getHelp() {
        return "查看服务器状态";
    }
}
