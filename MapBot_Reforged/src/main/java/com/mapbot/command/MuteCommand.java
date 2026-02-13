package com.mapbot.command;

import com.mapbot.data.DataManager;
import com.mapbot.logic.InboundHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

/**
 * 禁言命令
 * #mute <玩家> [时长] [理由]
 */
public class MuteCommand implements ICommand {
    
    @Override
    public int getRequiredLevel() {
        return DataManager.PERMISSION_LEVEL_MOD; // 协管员及以上
    }
    
    @Override
    public String getDescription() {
        return "禁言玩家: #mute <玩家> [时长] [理由]";
    }

    @Override
    public void execute(String args, long senderQQ, long sourceGroupId) {
        // 解析参数: <玩家> [时长]
        String[] parts = args.trim().split("\\s+", 3);
        
        if (parts.length < 1 || parts[0].isEmpty()) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 用法: #mute <玩家> [时长] [理由]\n时长示例: 10m, 1h, forever");
            return;
        }
        
        String targetName = parts[0];
        String durationStr = parts.length > 1 ? parts[1] : "forever";
        // String reason = parts.length > 2 ? parts[2] : "违反规定"; // 暂未存储理由
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 服务器未就绪");
            return;
        }
        
        server.execute(() -> {
            // 查找玩家 UUID (在线优先，离线查缓存)
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
            
            // 解析时长
            long durationMillis = parseDuration(durationStr);
            if (durationMillis == 0) {
                 InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 无效的时长格式 (支持 s/m/h/d)");
                 return;
            }
            
            // 执行禁言
            DataManager.INSTANCE.mute(uuidStr, durationMillis);
            
            // 反馈
            String expiryStr = (durationMillis == -1) ? "永久" : durationStr;
            InboundHandler.sendReplyToQQ(sourceGroupId, String.format("[操作成功] 已禁言 %s (%s)", targetName, expiryStr));
            
            // 如果在线，通知玩家
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§c你已被管理员禁言！时长: " + expiryStr
                ));
            }
        });
    }
    
    /**
     * 解析时长字符串
     * @return 毫秒数，-1 表示永久，0 表示格式错误
     */
    private long parseDuration(String str) {
        if ("forever".equalsIgnoreCase(str) || "perm".equalsIgnoreCase(str)) {
            return -1;
        }
        
        try {
            char unit = str.charAt(str.length() - 1);
            long val = Long.parseLong(str.substring(0, str.length() - 1));
            
            // Fix #14: 防止数值溢出，限制最大 365 天
            if (val <= 0 || val > 365 * 24 * 60) {
                return switch (Character.toLowerCase(unit)) {
                    case 's', 'm', 'h', 'd' -> val <= 0 ? 0 : -1; // 超大值视为永久
                    default -> 0;
                };
            }
            
            return switch (Character.toLowerCase(unit)) {
                case 's' -> val * 1000;
                case 'm' -> val * 60 * 1000;
                case 'h' -> val * 60 * 60 * 1000;
                case 'd' -> val * 24 * 60 * 60 * 1000;
                default -> 0;
            };
        } catch (Exception e) {
            return 0;
        }
    }
}
