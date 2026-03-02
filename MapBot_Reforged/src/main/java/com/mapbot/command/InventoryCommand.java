package com.mapbot.command;

import com.mapbot.data.DataManager;
import com.mapbot.security.CommandCategory;
import com.mapbot.logic.InboundHandler;
import com.mapbot.logic.InventoryManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * 玩家库存查询命令
 * #inv <玩家> [-e]
 */
public class InventoryCommand implements ICommand {
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.OPS_WRITE;
    }

    @Override
    public String getDescription() {
        return "查询玩家背包/末影箱: #inv <玩家名> [-e]";
    }

    @Override
    public void execute(String args, long senderQQ, long sourceGroupId) {
        if (args.trim().isEmpty()) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 用法: #inv <玩家名> [-e]");
            return;
        }

        boolean ender = args.contains("-e") || args.contains("-E");
        String targetName = args.replace("-e", "").replace("-E", "").trim();

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 服务器未就绪");
            return;
        }

        server.execute(() -> {
            ServerPlayer target = server.getPlayerList().getPlayerByName(targetName);
            if (target == null) {
                InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 玩家 " + targetName + " 不在线，无法查询实时背包。");
                return;
            }

            String result;
            if (ender) {
                result = InventoryManager.getPlayerEnderChest(target);
            } else {
                result = InventoryManager.getPlayerInventory(target);
            }
            InboundHandler.sendReplyToQQ(sourceGroupId, result);
        });
    }
}
