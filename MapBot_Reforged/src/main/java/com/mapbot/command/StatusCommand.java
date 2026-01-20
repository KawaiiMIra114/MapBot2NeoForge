package com.mapbot.command;

import com.mapbot.logic.InboundHandler;
import com.mapbot.logic.ServerStatusManager;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * 服务器状态命令
 * #status / #tps / #状态
 */
public class StatusCommand implements ICommand {
    @Override
    public String getDescription() {
        return "查看服务器TPS和运行状态: #status";
    }

    @Override
    public void execute(String args, long senderQQ, long sourceGroupId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 服务器未就绪");
            return;
        }
        
        server.execute(() -> {
            String result = ServerStatusManager.getServerInfo();
            InboundHandler.sendReplyToQQ(sourceGroupId, result);
        });
    }
}
