package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.bridge.BridgeProxy;
import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.config.AlphaConfig;
import com.mapbot.alpha.security.AuthManager;

/**
 * 重载配置命令 (Step-04 B2: 事务闭环)
 * #reload
 *
 * 流程: parse → validate → staging → atomic swap → audit → rollback
 */
public class ReloadCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        // 事务式重载 Alpha 配置
        AlphaConfig.ReloadResult configResult = AlphaConfig.INSTANCE.reload();

        // 安全配置重载（独立于主配置事务）
        AuthManager.INSTANCE.reloadSecurityConfig();
        String authSummary = AuthManager.INSTANCE.getBridgeAuthSummary();

        // 子服务器配置重载
        String childReloadSummary = BridgeProxy.INSTANCE.reloadSubServerConfigs();

        StringBuilder sb = new StringBuilder();
        sb.append(configResult.toSummary()).append("\n");
        sb.append("配置版本: v").append(AlphaConfig.INSTANCE.getConfigVersion()).append("\n");
        sb.append(authSummary).append("\n");
        sb.append(childReloadSummary);
        return sb.toString().trim();
    }
    
    @Override
    public String getHelp() {
        return "事务式重载配置文件 (含校验与回滚)";
    }
    
    @Override
    public boolean requiresAdmin() {
        return true;
    }
}
