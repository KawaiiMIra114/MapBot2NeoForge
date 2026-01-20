package com.mapbot.command;

import com.mapbot.data.DataManager;
import com.mapbot.logic.InboundHandler;

/**
 * 添加管理员命令 (兼容旧版)
 * #addadmin <QQ>
 */
public class AddAdminCommand implements ICommand {
    @Override
    public int getRequiredLevel() {
        return DataManager.PERMISSION_LEVEL_ADMIN;
    }

    @Override
    public String getDescription() {
        return "添加管理员: #addadmin <QQ>";
    }

    @Override
    public void execute(String args, long senderQQ, long sourceGroupId) {
        if (args.trim().isEmpty()) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 用法: #addadmin <QQ>");
            return;
        }

        try {
            long targetQQ = Long.parseLong(args.trim());
            if (DataManager.INSTANCE.addAdmin(targetQQ)) {
                InboundHandler.sendReplyToQQ(sourceGroupId, "[成功] 已添加管理员: " + targetQQ);
            } else {
                InboundHandler.sendReplyToQQ(sourceGroupId, "[提示] 该用户已经是管理员");
            }
        } catch (NumberFormatException e) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] QQ号格式错误");
        }
    }
}
