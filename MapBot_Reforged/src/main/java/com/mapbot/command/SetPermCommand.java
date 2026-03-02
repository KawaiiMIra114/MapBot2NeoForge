package com.mapbot.command;

import com.mapbot.data.DataManager;
import com.mapbot.security.CommandCategory;
import com.mapbot.logic.InboundHandler;

/**
 * 设置权限命令
 * #setperm <QQ> <Level>
 */
public class SetPermCommand implements ICommand {

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.GOVERNANCE; // 权限治理: 仅 OWNER
    }

    @Override
    public String getDescription() {
        return "设置权限: #setperm <QQ> <Level 0-2>";
    }

    @Override
    public void execute(String args, long senderQQ, long sourceGroupId) {
        String[] parts = args.trim().split("\\s+");
        
        if (parts.length < 2) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 用法: #setperm <QQ> <Level>\nLevels: 0=User, 1=Mod, 2=Admin");
            return;
        }
        
        try {
            long targetQQ = Long.parseLong(parts[0]);
            int level = Integer.parseInt(parts[1]);
            
            if (level < 0 || level > 2) {
                InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 权限等级必须在 0-2 之间");
                return;
            }
            
            // 防止自我操作
            if (targetQQ == senderQQ) {
                InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 不能修改自己的权限");
                return;
            }
            
            DataManager.INSTANCE.setPermissionLevel(targetQQ, level);
            
            String roleName = switch (level) {
                case 0 -> "普通用户";
                case 1 -> "协管员 (Mod)";
                case 2 -> "管理员 (Admin)";
                default -> "未知";
            };
            
            InboundHandler.sendReplyToQQ(sourceGroupId, String.format("[操作成功] 已将 %d 设置为 %s (Level %d)", targetQQ, roleName, level));
            
        } catch (NumberFormatException e) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 参数格式错误，请输入数字");
        }
    }
}
