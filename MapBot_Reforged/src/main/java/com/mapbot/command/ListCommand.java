package com.mapbot.command;

import com.mapbot.logic.InboundHandler;
import com.mapbot.logic.ServerStatusManager;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * 在线玩家列表命令
 * #list / #在线
 */
public class ListCommand implements ICommand {
    @Override
    public String getDescription() {
        return "查看在线玩家列表: #list";
    }

    @Override
    public void execute(String args, long senderQQ, long sourceGroupId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 服务器未就绪");
            return;
        }
        
        server.execute(() -> {
            String result = ServerStatusManager.getList();
            InboundHandler.sendReplyToQQ(sourceGroupId, result);
        });
    }
}
