package com.mapbot.alpha.command.impl;

import com.mapbot.alpha.bridge.BridgeProxy;
import com.mapbot.alpha.command.ICommand;
import com.mapbot.alpha.data.DataManager;

/**
 * 绑定命令
 * #id <游戏ID> / #bind <游戏ID>
 * 
 * 注意: 白名单操作通过 Bridge 代理到 MC 服务器
 */
public class BindCommand implements ICommand {
    
    @Override
    public String execute(String args, long senderQQ, long sourceGroupId) {
        String playerName = args.trim();
        
        if (playerName.isEmpty()) {
            return "[错误] 用法: #id <游戏ID>\n例如: #id Steve";
        }
        
        if (!playerName.matches("^[a-zA-Z0-9_]{3,16}$")) {
            return "[错误] 无效的游戏ID格式\n只能包含字母、数字、下划线，长度3-16位";
        }
        
        if (DataManager.INSTANCE.getBinding(senderQQ) != null) {
            return "[绑定失败] 该QQ已绑定其他账号，请先 #unbind 解绑";
        }
        
        // 通过 Bridge 获取 UUID 并添加白名单
        String result = BridgeProxy.INSTANCE.resolveAndBind(playerName, senderQQ);
        return result;
    }
    
    @Override
    public String getHelp() {
        return "绑定游戏ID: #id <玩家名>";
    }
}
