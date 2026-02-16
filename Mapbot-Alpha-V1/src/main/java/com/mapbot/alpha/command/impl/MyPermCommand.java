package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.command.QqRoleResolver;
import com.mapbot.alpha.data.DataManager;
import com.mapbot.alpha.security.ContractRole;

/**
 * 查看权限命令
 * #myperm
 */
public class MyPermCommand implements ICommand {

    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        ContractRole role = QqRoleResolver.resolveRole(senderQQ);
        int legacyLevel = DataManager.INSTANCE.getPermission(senderQQ);
        boolean legacyAdminFlag = DataManager.INSTANCE.isAdmin(senderQQ);
        String binding = DataManager.INSTANCE.getBinding(senderQQ);

        StringBuilder sb = new StringBuilder();
        sb.append("[权限信息]\n");
        sb.append("QQ: ").append(senderQQ).append("\n");
        sb.append("角色: ").append(QqRoleResolver.roleLabel(role)).append("\n");
        sb.append("兼容字段: level=").append(legacyLevel)
            .append(", adminFlag=").append(legacyAdminFlag ? "1" : "0").append("\n");
        sb.append("绑定状态: ").append(binding != null ? "已绑定" : "未绑定");
        return sb.toString();
    }

    @Override
    public String getHelp() {
        return "查看我的角色与权限";
    }
}
