package com.mapbot.command;

import com.mapbot.data.DataManager;
import com.mapbot.logic.InboundHandler;

/**
 * 查看权限命令
 * #myperm
 */
public class MyPermCommand implements ICommand {

    @Override
    public String getDescription() {
        return "查看我的权限等级";
    }

    @Override
    public void execute(String args, long senderQQ, long sourceGroupId) {
        int level = DataManager.INSTANCE.getPermissionLevel(senderQQ);
        String roleName = switch (level) {
            case 0 -> "普通用户 (User)";
            case 1 -> "协管员 (Mod)";
            case 2 -> "管理员 (Admin)";
            default -> "未知";
        };
        
        InboundHandler.sendReplyToQQ(sourceGroupId, String.format("[权限信息]\nQQ: %d\n等级: %d\n角色: %s", senderQQ, level, roleName));
    }
}
