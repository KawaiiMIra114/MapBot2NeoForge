package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.bridge.BridgeProxy;
import com.mapbot.alpha.command.ICommand;

/**
 * 背包查询命令
 * #inv <玩家名>
 */
public class InventoryCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        String playerName = args.trim();
        if (playerName.isEmpty()) {
            return "[错误] 用法: #inv <玩家名>";
        }
        
        return BridgeProxy.INSTANCE.getPlayerInventory(playerName);
    }
    
    @Override
    public String getHelp() {
        return "查看玩家背包";
    }
    
    @Override
    public boolean adminGroupOnly() {
        return true;
    }
}
