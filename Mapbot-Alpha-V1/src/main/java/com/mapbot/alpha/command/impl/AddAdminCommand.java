package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.data.DataManager;

/**
 * 添加管理员命令
 * #addadmin <QQ号> [等级]
 * 
 * 优化：
 * 1. 首次自动成功：若系统无管理员，第一次执行自动成功
 * 2. 语法增强：支持设置权限等级
 *    - #addadmin @用户 → 添加为 Admin (超级管理员)
 *    - #addadmin @用户 1 → 设置为 Level 1 (受信用户)
 *    - #addadmin @用户 2 → 设置为 Level 2 (普通管理员)
 *    - #addadmin @用户 admin → 添加为 Admin (超级管理员)
 */
public class AddAdminCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        String[] parts = args.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return "[错误] 用法: #addadmin <QQ号> [等级]\n" +
                   "等级: 1=受信用户, 2=管理员, admin=超级管理员 (默认)";
        }
        
        // 解析目标 QQ
        String target = parts[0].replaceAll("[^0-9]", "");
        if (target.isEmpty()) {
            return "[错误] 无效的 QQ 号";
        }
        long targetQQ = Long.parseLong(target);
        
        // 首次自动成功：若系统无管理员，自动添加
        if (DataManager.INSTANCE.getAdmins().isEmpty()) {
            DataManager.INSTANCE.addAdmin(targetQQ);
            return String.format("[系统] 系统无管理员，已自动添加 QQ %d 为首位管理员", targetQQ);
        }
        
        // 解析权限等级
        boolean setAsAdmin = true;
        int permLevel = -1;
        
        if (parts.length > 1) {
            String levelArg = parts[1].toLowerCase();
            if ("admin".equals(levelArg)) {
                setAsAdmin = true;
            } else if ("1".equals(levelArg)) {
                setAsAdmin = false;
                permLevel = 1;
            } else if ("2".equals(levelArg)) {
                setAsAdmin = false;
                permLevel = 2;
            } else {
                return "[错误] 无效的等级参数\n" +
                       "可用值: 1 (受信用户), 2 (管理员), admin (超级管理员)";
            }
        }
        
        // 执行操作
        if (setAsAdmin) {
            DataManager.INSTANCE.addAdmin(targetQQ);
            return String.format("[成功] 已将 QQ %d 设为超级管理员", targetQQ);
        } else {
            DataManager.INSTANCE.setPermission(targetQQ, permLevel);
            String levelName = (permLevel == 1) ? "受信用户" : "管理员";
            return String.format("[成功] 已将 QQ %d 设为 Level %d (%s)", targetQQ, permLevel, levelName);
        }
    }
    
    @Override
    public String getHelp() {
        return "添加管理员/设置权限: #addadmin <QQ> [等级]";
    }
    
    @Override
    public boolean requiresAdmin() {
        // 注意：首次自动成功由 CommandRegistry 在“无管理员”场景下特殊放行。
        // 系统已有管理员后，本命令必须由超级管理员执行。
        return true;
    }
    
    @Override
    public int requiredPermLevel() {
        return 0;
    }
}
