package com.mapbot.command;

import com.mapbot.data.DataManager;
import com.mapbot.logic.InboundHandler;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 解绑账号命令
 * #unbind
 */
public class UnbindCommand implements ICommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/Command/Unbind");

    @Override
    public String getDescription() {
        return "解绑我的游戏账号: #unbind";
    }

    @Override
    public void execute(String args, long senderQQ, long sourceGroupId) {
        if (!DataManager.INSTANCE.isQQBound(senderQQ)) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[操作失败] 未找到绑定记录。");
            return;
        }
        
        String uuid = DataManager.INSTANCE.getBinding(senderQQ);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 服务器未就绪");
            return;
        }
        
        server.execute(() -> {
            if (DataManager.INSTANCE.unbind(senderQQ)) {
                try {
                    com.mojang.authlib.GameProfile profile = server.getProfileCache().get(java.util.UUID.fromString(uuid))
                            .orElse(new com.mojang.authlib.GameProfile(java.util.UUID.fromString(uuid), "Unknown"));
                    server.getPlayerList().getWhiteList().remove(profile);
                    server.getPlayerList().getWhiteList().save();
                    InboundHandler.sendReplyToQQ(sourceGroupId, "[解绑成功] 已移除白名单。");
                    LOGGER.info("用户 {} 已解绑 (UUID: {})", senderQQ, uuid);
                } catch (Exception e) {
                    InboundHandler.sendReplyToQQ(sourceGroupId, "[警告] 解绑成功，但白名单操作异常。");
                }
            } else {
                InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 解绑失败");
            }
        });
    }
}
