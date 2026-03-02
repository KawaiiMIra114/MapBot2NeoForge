package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.bridge.BridgeProxy;
import com.mapbot.alpha.command.ICommand;

/**
 * 在线时长查询命令 (Task #03 重构: Bridge 转发模式)
 * #playtime <玩家名> [模式]
 * 
 * Alpha 不再本地读取 PlaytimeStore，全部转发给 Reforged 端处理。
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
        
        // 转发给 Reforged 端查询在线时长
        String result = BridgeProxy.INSTANCE.getPlaytime(targetName, mode);
        if (result == null || result.isEmpty()) {
            return "[错误] 服务器离线或无响应";
        }
        return result;
    }
    
    @Override
    public String getHelp() {
        return "查询在线时长: #playtime <玩家>";
    }
}
