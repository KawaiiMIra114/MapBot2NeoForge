package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.config.AlphaConfig;

/**
 * 重载配置命令
 * #reload
 */
public class ReloadCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        AlphaConfig.INSTANCE.reload();
        return "[成功] 配置已重新加载";
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
