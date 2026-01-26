package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.logic.PlaytimeStore;

/**
 * 在线时长查询命令
 * #playtime <玩家名> [模式]
 */
public class PlaytimeCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        String[] parts = args.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return "[错误] 用法: #playtime <玩家名> [0=日, 1=周, 2=月, 3=总]";
        }
        
        String targetName = parts[0];
        int mode = 0;
        if (parts.length > 1) {
            try {
                mode = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return "[错误] 模式必须是 0-3 的数字";
            }
        }

        String uuid = com.mapbot.alpha.bridge.BridgeProxy.INSTANCE.resolveUuidByName(targetName);
        if (uuid == null) {
            return "[错误] 玩家不存在或无法解析UUID: " + targetName;
        }

        var record = PlaytimeStore.INSTANCE.getPlaytime(uuid);
        long ms = switch (mode) {
            case 0 -> record.dailyMs;
            case 1 -> record.weeklyMs;
            case 2 -> record.monthlyMs;
            case 3 -> record.totalMs;
            default -> record.totalMs;
        };

        String[] periods = {"日", "周", "月", "总"};
        String period = (mode >= 0 && mode < periods.length) ? periods[mode] : "总";
        return String.format("[在线时长] %s (%s)\n时长: %s", targetName, period, PlaytimeStore.formatDurationMs(ms));
    }
    
    @Override
    public String getHelp() {
        return "查询在线时长: #playtime <玩家>";
    }
}
