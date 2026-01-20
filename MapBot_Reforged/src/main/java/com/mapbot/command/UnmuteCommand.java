package com.mapbot.command;

import com.mapbot.data.DataManager;
import com.mapbot.logic.InboundHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * 解除禁言命令
 * #unmute <玩家>
 */
public class UnmuteCommand implements ICommand {

    @Override
    public int getRequiredLevel() {
        return DataManager.PERMISSION_LEVEL_MOD;
    }

    @Override
    public String getDescription() {
        return "解除禁言: #unmute <玩家>";
    }

    @Override
    public void execute(String args, long senderQQ, long sourceGroupId) {
        String targetName = args.trim();
        
        if (targetName.isEmpty()) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 用法: #unmute <玩家>");
            return;
        }
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 服务器未就绪");
            return;
        }
        
        server.execute(() -> {
            String uuidStr = null;
            ServerPlayer player = server.getPlayerList().getPlayerByName(targetName);
            
            if (player != null) {
                uuidStr = player.getUUID().toString();
            } else {
                var profile = server.getProfileCache().get(targetName);
                if (profile.isPresent()) {
                    uuidStr = profile.get().getId().toString();
                }
            }
            
            if (uuidStr == null) {
                InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 找不到玩家: " + targetName);
                return;
            }
            
            if (DataManager.INSTANCE.unmute(uuidStr)) {
                InboundHandler.sendReplyToQQ(sourceGroupId, "[操作成功] 已解除 " + targetName + " 的禁言");
                if (player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a你的禁言已被解除。"));
                }
            } else {
                InboundHandler.sendReplyToQQ(sourceGroupId, "[提示] 该玩家未被禁言");
            }
        });
    }
}
