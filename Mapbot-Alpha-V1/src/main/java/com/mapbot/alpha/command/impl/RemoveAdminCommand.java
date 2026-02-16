package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.command.QqRoleResolver;
import com.mapbot.alpha.data.DataManager;
import com.mapbot.alpha.security.ContractRole;

/**
 * 移除管理角色命令
 * #removeadmin <QQ号>
 */
public class RemoveAdminCommand implements ICommand {

    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        String target = args == null ? "" : args.trim().replaceAll("[^0-9]", "");
        if (target.isEmpty()) {
            return "[错误] 用法: #removeadmin <QQ号>";
        }

        long targetQQ = Long.parseLong(target);
        if (DataManager.INSTANCE.isAdmin(targetQQ) && DataManager.INSTANCE.getAdmins().size() <= 1) {
            return "[错误] 不能移除最后一个 owner，请先添加新的 owner";
        }

        QqRoleResolver.applyRole(targetQQ, ContractRole.USER);
        return String.format("[成功] 已移除 QQ %d 的管理权限（当前角色: user）", targetQQ);
    }

    @Override
    public String getHelp() {
        return "移除管理角色: #removeadmin <QQ>";
    }

    @Override
    public ContractRole requiredRole() {
        return ContractRole.OWNER;
    }
}
