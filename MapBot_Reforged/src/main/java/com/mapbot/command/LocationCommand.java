package com.mapbot.command;

import com.mapbot.data.DataManager;
import com.mapbot.security.CommandCategory;
import com.mapbot.logic.InboundHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * 玩家坐标查询命令
 * #location <玩家>
 */
public class LocationCommand implements ICommand {
    @Override
    public CommandCategory getCategory() {
        return CommandCategory.OPS_WRITE;
    }

    @Override
    public String getDescription() {
        return "查询玩家坐标: #location <玩家名>";
    }

    @Override
    public void execute(String args, long senderQQ, long sourceGroupId) {
        String targetName = args.trim();
        if (targetName.isEmpty()) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 用法: #location <玩家名>");
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 服务器未就绪");
            return;
        }

        server.execute(() -> {
            ServerPlayer target = server.getPlayerList().getPlayerByName(targetName);
            if (target == null) {
                InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 玩家 " + targetName + " 不在线");
                return;
            }

            String dim = getDimName(target.level().dimension().location().toString());
            int x = target.getBlockX();
            int y = target.getBlockY();
            int z = target.getBlockZ();
            
            String msg = String.format("[位置] %s\n维度: %s\n坐标: %d, %d, %d", targetName, dim, x, y, z);
            InboundHandler.sendReplyToQQ(sourceGroupId, msg);
        });
    }

    private String getDimName(String dimId) {
        return switch (dimId) {
            case "minecraft:overworld" -> "主世界";
            case "minecraft:the_nether" -> "下界";
            case "minecraft:the_end" -> "末地";
            default -> dimId;
        };
    }
}
