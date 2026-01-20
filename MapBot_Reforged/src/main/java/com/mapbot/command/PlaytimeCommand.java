package com.mapbot.command;

import com.mapbot.data.DataManager;
import com.mapbot.data.PlaytimeManager;
import com.mapbot.logic.InboundHandler;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

/**
 * 在线时长查询命令
 * #playtime <玩家> [模式]
 */
public class PlaytimeCommand implements ICommand {
    @Override
    public String getDescription() {
        return "查询玩家在线时长: #playtime <玩家名> [0=天, 1=周, 2=月, 3=总]";
    }

    @Override
    public void execute(String args, long senderQQ, long sourceGroupId) {
        String[] parts = args.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 用法: #playtime <玩家名> [时段]");
            return;
        }

        String targetName = parts[0];
        int mode = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        server.execute(() -> {
            UUID uuid = server.getProfileCache().get(targetName)
                    .map(com.mojang.authlib.GameProfile::getId)
                    .orElse(null);

            if (uuid == null) {
                InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 找不到玩家: " + targetName);
                return;
            }

            long mins = PlaytimeManager.INSTANCE.getPlaytimeMinutes(uuid, mode);
            String time = PlaytimeManager.formatDuration(mins);
            InboundHandler.sendReplyToQQ(sourceGroupId, String.format("[在线时长] %s\n时长: %s", targetName, time));
        });
    }
}
