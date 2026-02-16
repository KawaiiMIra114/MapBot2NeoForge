package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.data.DataManager;
import com.mapbot.alpha.security.ContractRole;

/**
 * 禁言命令
 * #mute <QQ号/@> [时长分钟]
 */
public class MuteCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        String[] parts = args.trim().split("\\s+");
        if (parts.length < 1 || parts[0].isEmpty()) {
            return "[错误] 用法: #mute <QQ号/@提及> [时长分钟]\n不填时长为永久禁言";
        }
        
        // 解析目标 QQ
        String target = parts[0].replaceAll("[^0-9]", "");
        if (target.isEmpty()) {
            return "[错误] 无效的目标QQ";
        }
        long targetQQ = Long.parseLong(target);
        
        // 获取绑定的 UUID
        String uuid = DataManager.INSTANCE.getBinding(targetQQ);
        if (uuid == null) {
            return "[错误] 该QQ未绑定游戏账号";
        }
        
        // 解析时长
        long expiryMs = -1; // 永久
        if (parts.length >= 2) {
            try {
                int minutes = Integer.parseInt(parts[1]);
                expiryMs = System.currentTimeMillis() + minutes * 60 * 1000L;
            } catch (NumberFormatException e) {
                return "[错误] 时长必须是数字(分钟)";
            }
        }
        
        DataManager.INSTANCE.mute(uuid, expiryMs);
        
        String timeStr = (expiryMs == -1) ? "永久" : parts[1] + "分钟";
        return String.format("[禁言成功] QQ %d 已被禁言 %s", targetQQ, timeStr);
    }
    
    @Override
    public String getHelp() {
        return "禁言玩家: #mute <QQ> [分钟]";
    }
    
    @Override
    public ContractRole requiredRole() {
        return ContractRole.ADMIN;
    }
}
