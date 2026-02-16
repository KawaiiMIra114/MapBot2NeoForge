package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.bridge.BridgeProxy;
import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.network.OneBotClient;
import com.mapbot.alpha.security.ContractRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 关闭服务器命令
 * #stopserver [秒数] [serverId]
 */
public class StopServerCommand implements ICommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/Command/Stop");

    @Override
    public ContractRole requiredRole() {
        return ContractRole.ADMIN;
    }

    @Override
    public String getHelp() {
        return "倒计时关闭服务器: #stopserver [秒数] [serverId]（或 #stopserver [serverId] [秒数]）";
    }

    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        String serverId = null;
        int countdown = 0;
        String trimmed = args == null ? "" : args.trim();
        if (!trimmed.isEmpty()) {
            String[] tokens = trimmed.split("\\s+");
            if (tokens.length == 1) {
                if (isInteger(tokens[0])) {
                    countdown = Integer.parseInt(tokens[0]);
                } else {
                    serverId = tokens[0];
                }
            } else if (tokens.length == 2) {
                boolean firstIsInt = isInteger(tokens[0]);
                boolean secondIsInt = isInteger(tokens[1]);
                if (firstIsInt == secondIsInt) {
                    return "[错误] 参数格式: #stopserver [秒数] [serverId]";
                }
                if (firstIsInt) {
                    countdown = Integer.parseInt(tokens[0]);
                    serverId = tokens[1];
                } else {
                    serverId = tokens[0];
                    countdown = Integer.parseInt(tokens[1]);
                }
            } else {
                return "[错误] 参数过多，格式: #stopserver [秒数] [serverId]";
            }
        }

        if (countdown < 0 || countdown > 3600) {
            return "[错误] 倒计时范围: 0-3600 秒";
        }

        LOGGER.warn("管理员 {} 执行关服命令, 倒计时: {}s, target={}",
            senderQQ, countdown, serverId == null ? "<auto>" : serverId);
        
        final int seconds = countdown;
        BridgeProxy.stopServer(seconds, serverId).thenAccept(result -> {
            if (result.startsWith("[错误]")) {
                sendContextReply(senderQQ, sourceGroupId, result);
            } else if (seconds == 0) {
                sendContextReply(senderQQ, sourceGroupId, "[系统] 正在立即关闭服务器...");
            } else {
                sendContextReply(senderQQ, sourceGroupId, String.format("[系统] 服务器将在 %d 秒后关闭", seconds));
            }
        });
        return null;
    }

    private static boolean isInteger(String text) {
        if (text == null || text.isEmpty()) return false;
        try {
            Integer.parseInt(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void sendContextReply(long senderQQ, long sourceGroupId, String message) {
        if (sourceGroupId > 0) {
            OneBotClient.INSTANCE.sendGroupMessage(sourceGroupId, message);
        } else {
            OneBotClient.INSTANCE.sendPrivateMessage(senderQQ, message);
        }
    }
}
