package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.command.QqRoleResolver;
import com.mapbot.alpha.data.DataManager;
import com.mapbot.alpha.security.ContractRole;

import java.util.Optional;

/**
 * 添加管理角色命令
 * #addadmin <QQ号> [admin|owner]
 */
public class AddAdminCommand implements ICommand {

    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        String[] parts = args == null ? new String[0] : args.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return "[错误] 用法: #addadmin <QQ号> [admin|owner]";
        }

        String target = parts[0].replaceAll("[^0-9]", "");
        if (target.isEmpty()) {
            return "[错误] 无效的 QQ 号";
        }
        long targetQQ = Long.parseLong(target);

        // 首次初始化：系统无 owner 时，允许直接创建首位 owner。
        if (DataManager.INSTANCE.getAdmins().isEmpty()) {
            QqRoleResolver.applyRole(targetQQ, ContractRole.OWNER);
            return String.format("[系统] 系统无 owner，已自动添加 QQ %d 为首位 owner", targetQQ);
        }

        ContractRole targetRole = ContractRole.ADMIN;
        if (parts.length > 1) {
            Optional<ContractRole> parsed = QqRoleResolver.parseRoleToken(parts[1]);
            if (parsed.isEmpty() || parsed.get() == ContractRole.USER) {
                return "[错误] 仅支持 admin 或 owner";
            }
            targetRole = parsed.get();
        }

        QqRoleResolver.applyRole(targetQQ, targetRole);
        return String.format("[成功] QQ %d 已设为 %s", targetQQ, QqRoleResolver.roleLabel(targetRole));
    }

    @Override
    public String getHelp() {
        return "添加管理角色: #addadmin <QQ> [admin|owner]";
    }

    @Override
    public ContractRole requiredRole() {
        return ContractRole.OWNER;
    }
}
