package com.mapbot.command;

import com.mapbot.data.DataManager;
import com.mapbot.data.loot.LootConfig;
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
            // Fix #11: 同时重载所有可重载的配置
            DataManager.INSTANCE.init();
            LootConfig.INSTANCE.init();
            InboundHandler.sendReplyToQQ(sourceGroupId, "[系统] 配置和数据已重载 (DataManager + LootConfig)");
        } catch (Exception e) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 重载失败: " + e.getMessage());
        }
    }
}
