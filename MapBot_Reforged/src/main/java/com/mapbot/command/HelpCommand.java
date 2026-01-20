package com.mapbot.command;

import com.mapbot.logic.InboundHandler;
import com.mapbot.logic.ServerStatusManager;

/**
 * 帮助菜单命令
 * #help / #菜单
 */
public class HelpCommand implements ICommand {
    @Override
    public String getDescription() {
        return "显示命令帮助菜单: #help";
    }

    @Override
    public void execute(String args, long senderQQ, long sourceGroupId) {
        InboundHandler.sendReplyToQQ(sourceGroupId, ServerStatusManager.getHelp());
    }
}
