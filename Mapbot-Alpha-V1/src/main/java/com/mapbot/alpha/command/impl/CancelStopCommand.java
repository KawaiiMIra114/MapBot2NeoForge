package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.bridge.BridgeProxy;
import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.data.DataManager;
import com.mapbot.alpha.network.OneBotClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 取消关服命令
 * #cancelstop
 */
public class CancelStopCommand implements ICommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/Command/CancelStop");

    @Override
    public int requiredPermLevel() {
        return DataManager.PERMISSION_LEVEL_ADMIN;
    }

    @Override
    public String getHelp() {
        return "取消正在进行的关服倒计时: #cancelstop [serverId]";
    }

    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        String trimmed = args == null ? "" : args.trim();
        String serverId = null;
        if (!trimmed.isEmpty()) {
            String[] tokens = trimmed.split("\\s+");
            if (tokens.length != 1) {
                return "[错误] 参数格式: #cancelstop [serverId]";
            }
            serverId = tokens[0];
        }

        LOGGER.warn("管理员 {} 执行取消关服命令, target={}",
            senderQQ, serverId == null ? "<auto>" : serverId);

        BridgeProxy.cancelStop(serverId).thenAccept(result -> sendContextReply(senderQQ, sourceGroupId, result));
        return null;
    }

    private static void sendContextReply(long senderQQ, long sourceGroupId, String message) {
        if (sourceGroupId > 0) {
            OneBotClient.INSTANCE.sendGroupMessage(sourceGroupId, message);
        } else {
            OneBotClient.INSTANCE.sendPrivateMessage(senderQQ, message);
        }
    }
}
