package com.mapbot.command;

import com.mapbot.data.DataManager;
import com.mapbot.logic.InboundHandler;

/**
 * 重载配置命令
 * #reload
 */
public class ReloadCommand implements ICommand {
    @Override
    public int getRequiredLevel() {
        return DataManager.PERMISSION_LEVEL_ADMIN;
    }

    @Override
    public String getDescription() {
        return "重载插件配置文件和数据: #reload";
    }

    @Override
    public void execute(String args, long senderQQ, long sourceGroupId) {
        try {
            DataManager.INSTANCE.init();
            InboundHandler.sendReplyToQQ(sourceGroupId, "[系统] 配置和数据已重载");
        } catch (Exception e) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 重载失败: " + e.getMessage());
        }
    }
}
