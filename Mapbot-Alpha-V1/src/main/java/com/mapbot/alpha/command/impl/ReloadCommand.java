package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.bridge.BridgeProxy;
import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.config.AlphaConfig;
import com.mapbot.alpha.security.AuthManager;

/**
 * 重载配置命令
 * #reload
 */
public class ReloadCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        AlphaConfig.INSTANCE.reload();
        AuthManager.INSTANCE.reloadSecurityConfig();
        String authSummary = AuthManager.INSTANCE.getBridgeAuthSummary();
        String childReloadSummary = BridgeProxy.INSTANCE.reloadSubServerConfigs();
        return "[成功] Alpha 配置已重新加载\n" + authSummary + "\n" + childReloadSummary;
    }
    
    @Override
    public String getHelp() {
        return "重载配置文件";
    }
    
    @Override
    public boolean requiresAdmin() {
        return true;
    }
}
