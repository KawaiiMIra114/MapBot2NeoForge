package com.mapbot.command;

import com.mapbot.data.DataManager;
import com.mapbot.logic.InboundHandler;

/**
 * 移除管理员命令 (兼容旧版)
 * #removeadmin <QQ>
 */
public class RemoveAdminCommand implements ICommand {
    @Override
    public int getRequiredLevel() {
        return DataManager.PERMISSION_LEVEL_ADMIN;
    }

    @Override
    public String getDescription() {
        return "移除管理员权限: #removeadmin <QQ>";
    }

    @Override
    public void execute(String args, long senderQQ, long sourceGroupId) {
        if (args.trim().isEmpty()) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 用法: #removeadmin <QQ>");
            return;
        }

        try {
            long targetQQ = Long.parseLong(args.trim());
            if (targetQQ == senderQQ) {
                InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 不能移除自己的权限");
                return;
            }
            if (DataManager.INSTANCE.removeAdmin(targetQQ)) {
                InboundHandler.sendReplyToQQ(sourceGroupId, "[成功] 已移除管理员权限: " + targetQQ);
            } else {
                InboundHandler.sendReplyToQQ(sourceGroupId, "[提示] 该用户不是管理员");
            }
        } catch (NumberFormatException e) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] QQ号格式错误");
        }
    }
}
