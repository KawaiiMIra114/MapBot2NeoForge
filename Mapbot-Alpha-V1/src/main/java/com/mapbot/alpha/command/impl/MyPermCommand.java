package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.data.DataManager;

/**
 * 查看权限命令
 * #myperm
 */
public class MyPermCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        int level = DataManager.INSTANCE.getPermission(senderQQ);
        boolean isAdmin = DataManager.INSTANCE.isAdmin(senderQQ);
        String binding = DataManager.INSTANCE.getBinding(senderQQ);
        
        StringBuilder sb = new StringBuilder();
        sb.append("[权限信息]\n");
        sb.append("QQ: ").append(senderQQ).append("\n");
        sb.append("权限等级: ").append(level);
        switch (level) {
            case 0 -> sb.append(" (普通)");
            case 1 -> sb.append(" (VIP)");
            case 2 -> sb.append(" (OP)");
        }
        sb.append("\n");
        sb.append("管理员: ").append(isAdmin ? "是" : "否").append("\n");
        sb.append("绑定状态: ").append(binding != null ? "已绑定" : "未绑定");
        
        return sb.toString();
    }
    
    @Override
    public String getHelp() {
        return "查看我的权限";
    }
}
