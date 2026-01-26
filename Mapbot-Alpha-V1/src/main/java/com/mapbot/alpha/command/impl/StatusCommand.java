package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.bridge.BridgeProxy;
import com.mapbot.alpha.command.ICommand;

/**
 * 服务器状态命令
 * #status / #tps / #状态
 */
public class StatusCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        String mode = args == null ? "" : args.trim().toLowerCase();
        boolean includePlayers = mode.equals("all") || mode.equals("players");

        var servers = com.mapbot.alpha.bridge.ServerRegistry.INSTANCE.getAllServers();
        if (servers.isEmpty()) {
            return "[状态] 无已连接服务器";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[状态] 在线服务器: ").append(servers.size()).append("\n");

        for (var s : servers) {
            sb.append("— ").append(s.serverId)
              .append(" | 在线: ").append(s.players)
              .append(" | TPS: ").append(s.tps)
              .append(" | 内存: ").append(s.memory)
              .append("\n");

            if (includePlayers) {
                String list = BridgeProxy.INSTANCE.sendRequestToServer(s.serverId, "get_players", null, null);
                if (list != null && !list.isEmpty()) {
                    for (String line : list.split("\\r?\\n")) {
                        sb.append("  ").append(line).append("\n");
                    }
                }
            }
        }

        if (!includePlayers) {
            sb.append("[提示] 输入 #status all 查看玩家分布");
        }

        return sb.toString().trim();
    }
    
    @Override
    public String getHelp() {
        return "查看服务器状态 (#status all 查看玩家分布)";
    }
}
