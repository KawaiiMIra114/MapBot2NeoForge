package com.mapbot.command;

import com.mapbot.data.DataManager;
import com.mapbot.logic.InboundHandler;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * 账号绑定命令
 * #id <游戏ID> / #bind <游戏ID>
 */
public class BindCommand implements ICommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/Command/Bind");
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");

    @Override
    public String getDescription() {
        return "绑定游戏ID并加入白名单: #id <游戏ID>";
    }

    @Override
    public void execute(String args, long senderQQ, long sourceGroupId) {
        String playerName = args.trim();
        
        if (playerName.isEmpty()) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 用法: #id <游戏ID>\n例如: #id Steve");
            return;
        }
        
        if (!PLAYER_NAME_PATTERN.matcher(playerName).matches()) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 无效的游戏ID格式\n只能包含字母、数字、下划线，长度3-16位");
            return;
        }
        
        if (DataManager.INSTANCE.isQQBound(senderQQ)) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[绑定失败] 该QQ已绑定其他账号，请先解绑。");
            return;
        }
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 服务器未就绪");
            return;
        }
        
        LOGGER.info("处理绑定请求: QQ {} -> 玩家 {}", senderQQ, playerName);
        
        boolean isOnlineMode = server.usesAuthentication();
        
        CompletableFuture.supplyAsync(() -> resolveGameProfile(server, playerName, isOnlineMode))
            .thenAcceptAsync(profile -> {
                if (profile == null) {
                    InboundHandler.sendReplyToQQ(sourceGroupId, "[绑定失败] 玩家不存在或未加入过服务器。");
                    return;
                }
                
                String uuid = profile.getId().toString();
                
                if (DataManager.INSTANCE.isUUIDBound(uuid)) {
                    InboundHandler.sendReplyToQQ(sourceGroupId, "[绑定失败] 该游戏ID已被其他QQ绑定，请联系管理员。");
                    return;
                }
                
                if (DataManager.INSTANCE.bind(senderQQ, uuid)) {
                    try {
                        UserWhiteList whitelist = server.getPlayerList().getWhiteList();
                        if (!whitelist.isWhiteListed(profile)) {
                            whitelist.add(new UserWhiteListEntry(profile));
                            whitelist.save();
                        }
                        InboundHandler.sendReplyToQQ(sourceGroupId, String.format("[绑定成功]\nQQ <-> %s\n白名单已同步。", profile.getName()));
                    } catch (Exception e) {
                        LOGGER.error("添加白名单失败", e);
                        InboundHandler.sendReplyToQQ(sourceGroupId, "[警告] 绑定成功，但同步白名单失败。");
                    }
                } else {
                    InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 数据库写入失败");
                }
            }, server);
    }

    private GameProfile resolveGameProfile(MinecraftServer server, String playerName, boolean onlineMode) {
        try {
            if (onlineMode) {
                Optional<GameProfile> cached = server.getProfileCache().get(playerName);
                if (cached.isPresent()) return cached.get();
                ServerPlayer onlinePlayer = server.getPlayerList().getPlayerByName(playerName);
                if (onlinePlayer != null) return onlinePlayer.getGameProfile();
                return null;
            } else {
                return new GameProfile(UUIDUtil.createOfflinePlayerUUID(playerName), playerName);
            }
        } catch (Exception e) {
            return null;
        }
    }
}
