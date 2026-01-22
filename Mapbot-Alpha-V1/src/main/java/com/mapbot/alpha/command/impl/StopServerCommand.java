package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.bridge.BridgeProxy;
import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.data.DataManager;
import com.mapbot.alpha.network.OneBotClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 关闭服务器命令
 * #stopserver [秒数]
 */
public class StopServerCommand implements ICommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/Command/Stop");

    @Override
    public int requiredPermLevel() {
        return DataManager.PERMISSION_LEVEL_ADMIN;
    }

    @Override
    public String getHelp() {
        return "倒计时关闭服务器: #stopserver [秒数]";
    }

    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        int countdown = 0;
        if (!args.trim().isEmpty()) {
            try {
                countdown = Integer.parseInt(args.trim());
                if (countdown < 0 || countdown > 3600) {
                    OneBotClient.INSTANCE.sendGroupMessage(sourceGroupId, "[错误] 倒计时范围: 0-3600 秒");
                    return null;
                }
            } catch (NumberFormatException e) {
                OneBotClient.INSTANCE.sendGroupMessage(sourceGroupId, "[错误] 倒计时必须为数字");
                return null;
            }
        }

        LOGGER.warn("管理员 {} 执行关服命令, 倒计时: {}s", senderQQ, countdown);
        
        final int seconds = countdown;
        BridgeProxy.stopServer(seconds).thenAccept(result -> {
            if (result.startsWith("[错误]")) {
                OneBotClient.INSTANCE.sendGroupMessage(sourceGroupId, result);
            } else if (seconds == 0) {
                OneBotClient.INSTANCE.sendGroupMessage(sourceGroupId, "[系统] 正在立即关闭服务器...");
            } else {
                OneBotClient.INSTANCE.sendGroupMessage(sourceGroupId, 
                    String.format("[系统] 服务器将在 %d 秒后关闭", seconds));
            }
        });
        return null;
    }
}
