package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.bridge.BridgeProxy;
import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.security.ContractRole;

/**
 * 位置查询命令
 * #location <玩家名> / #位置
 */
public class LocationCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        String playerName = args.trim();
        if (playerName.isEmpty()) {
            return "[错误] 用法: #location <玩家名>";
        }
        
        return BridgeProxy.INSTANCE.getPlayerLocation(playerName);
    }
    
    @Override
    public String getHelp() {
        return "查看玩家位置";
    }
    
    @Override
    public boolean adminGroupOnly() {
        return true;
    }

    @Override
    public ContractRole requiredRole() {
        return ContractRole.ADMIN;
    }
}
