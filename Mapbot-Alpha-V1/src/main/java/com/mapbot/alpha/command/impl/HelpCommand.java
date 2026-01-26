package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.command.CommandRegistry;
import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.config.AlphaConfig;
import com.mapbot.alpha.data.DataManager;

import java.util.Map;

/**
 * 帮助命令
 * #help / #菜单
 * 
 * 优化：分群权限显示
 * - 玩家群：仅显示 Level 0 命令
 * - 管理群：显示当前用户权限可执行的命令
 * - #help all：显示全部命令并标注权限要求
 */
public class HelpCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        boolean showAll = "all".equalsIgnoreCase(args.trim());
        boolean isPlayerGroup = (sourceGroupId == AlphaConfig.getPlayerGroupId());
        boolean isAdminGroup = (sourceGroupId == AlphaConfig.getAdminGroupId());
        
        int userLevel = DataManager.INSTANCE.getPermission(senderQQ);
        boolean isAdmin = DataManager.INSTANCE.isAdmin(senderQQ);
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== MapBot 命令帮助 ===\n");
        
        if (showAll) {
            // 显示全部命令并标注权限
            sb.append("\n[可用命令]\n");
            for (Map.Entry<String, ICommand> e : CommandRegistry.getCommands().entrySet()) {
                ICommand cmd = e.getValue();
                if (canExecute(cmd, userLevel, isAdmin)) {
                    sb.append("#").append(e.getKey());
                    String help = cmd.getHelp();
                    if (help != null && !help.isEmpty()) {
                        sb.append(" - ").append(help);
                    }
                    sb.append("\n");
                }
            }
            
            sb.append("\n[需更高权限]\n");
            for (Map.Entry<String, ICommand> e : CommandRegistry.getCommands().entrySet()) {
                ICommand cmd = e.getValue();
                if (!canExecute(cmd, userLevel, isAdmin)) {
                    sb.append("#").append(e.getKey());
                    String help = cmd.getHelp();
                    if (help != null && !help.isEmpty()) {
                        sb.append(" - ").append(help);
                    }
                    
                    // 标注权限要求
                    if (cmd.requiresAdmin()) {
                        sb.append(" [需 Admin]");
                    } else if (cmd.requiredPermLevel() > 0) {
                        sb.append(" [需 Level ").append(cmd.requiredPermLevel()).append("]");
                    }
                    sb.append("\n");
                }
            }
        } else if (isPlayerGroup) {
            // 玩家群：仅显示 Level 0 命令
            for (Map.Entry<String, ICommand> e : CommandRegistry.getCommands().entrySet()) {
                ICommand cmd = e.getValue();
                if (cmd.requiredPermLevel() == 0 && !cmd.requiresAdmin() && !cmd.adminGroupOnly()) {
                    sb.append("#").append(e.getKey());
                    String help = cmd.getHelp();
                    if (help != null && !help.isEmpty()) {
                        sb.append(" - ").append(help);
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n[提示] 输入 #help all 查看全部命令");
        } else if (isAdminGroup) {
            // 管理群：显示当前用户可执行的命令
            for (Map.Entry<String, ICommand> e : CommandRegistry.getCommands().entrySet()) {
                ICommand cmd = e.getValue();
                if (canExecute(cmd, userLevel, isAdmin)) {
                    sb.append("#").append(e.getKey());
                    String help = cmd.getHelp();
                    if (help != null && !help.isEmpty()) {
                        sb.append(" - ").append(help);
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n[提示] 输入 #help all 查看全部命令");
        } else {
            // 其他群：显示基础命令
            return CommandRegistry.getHelpText();
        }
        
        return sb.toString().trim();
    }
    
    private boolean canExecute(ICommand cmd, int userLevel, boolean isAdmin) {
        if (cmd.requiresAdmin() && !isAdmin) return false;
        if (userLevel < cmd.requiredPermLevel()) return false;
        return true;
    }
    
    @Override
    public String getHelp() {
        return "显示命令帮助 (#help all 查看全部)";
    }
}
