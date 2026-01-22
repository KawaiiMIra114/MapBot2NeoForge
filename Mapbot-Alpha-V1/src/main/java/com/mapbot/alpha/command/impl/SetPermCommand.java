package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.data.DataManager;

/**
 * 设置权限命令
 * #setperm <QQ号> <等级>
 */
public class SetPermCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        String[] parts = args.trim().split("\\s+");
        if (parts.length < 2) {
            return "[错误] 用法: #setperm <QQ号> <等级>\n等级: 0=普通, 1=VIP, 2=OP";
        }
        
        String target = parts[0].replaceAll("[^0-9]", "");
        if (target.isEmpty()) {
            return "[错误] 无效的QQ号";
        }
        
        int level;
        try {
            level = Integer.parseInt(parts[1]);
            if (level < 0 || level > 2) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            return "[错误] 等级必须是 0-2 的数字";
        }
        
        long targetQQ = Long.parseLong(target);
        DataManager.INSTANCE.setPermission(targetQQ, level);
        
        String[] levelNames = {"普通", "VIP", "OP"};
        return String.format("[成功] QQ %d 的权限等级已设为 %d (%s)", targetQQ, level, levelNames[level]);
    }
    
    @Override
    public String getHelp() {
        return "设置权限: #setperm <QQ> <0-2>";
    }
    
    @Override
    public boolean requiresAdmin() {
        return true;
    }
}
