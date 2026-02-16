package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.command.QqRoleResolver;
import com.mapbot.alpha.data.DataManager;
import com.mapbot.alpha.security.ContractRole;

import java.util.Optional;

/**
 * 设置角色命令（兼容旧命令名 #setperm）
 * #setperm <QQ号> <user|admin|owner>
 */
public class SetPermCommand implements ICommand {

    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        String[] parts = args == null ? new String[0] : args.trim().split("\\s+");
        if (parts.length < 2) {
            return "[错误] 用法: #setperm <QQ号> <user|admin|owner>";
        }

        String target = parts[0].replaceAll("[^0-9]", "");
        if (target.isEmpty()) {
            return "[错误] 无效的 QQ 号";
        }

        Optional<ContractRole> roleOpt = QqRoleResolver.parseRoleToken(parts[1]);
        if (roleOpt.isEmpty()) {
            return "[错误] 角色必须是 user/admin/owner (兼容: 0/1/2/3)";
        }

        long targetQQ = Long.parseLong(target);
        ContractRole role = roleOpt.get();

        if (role == ContractRole.USER && DataManager.INSTANCE.isAdmin(targetQQ)
            && DataManager.INSTANCE.getAdmins().size() <= 1) {
            return "[错误] 不能降级最后一个 owner，请先添加新的 owner";
        }

        QqRoleResolver.applyRole(targetQQ, role);
        return String.format("[成功] QQ %d 的角色已设为 %s", targetQQ, QqRoleResolver.roleLabel(role));
    }

    @Override
    public String getHelp() {
        return "设置角色: #setperm <QQ> <user|admin|owner>";
    }

    @Override
    public ContractRole requiredRole() {
        return ContractRole.OWNER;
    }
}
