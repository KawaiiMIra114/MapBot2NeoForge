package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.command.CommandRegistry;
import com.mapbot.alpha.command.ICommand;

/**
 * 帮助命令
 * #help / #菜单
 */
public class HelpCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        return CommandRegistry.getHelpText();
    }
    
    @Override
    public String getHelp() {
        return "显示命令帮助";
    }
}
