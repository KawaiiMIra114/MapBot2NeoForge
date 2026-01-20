package com.mapbot.command;

import com.mapbot.data.DataManager;
import com.mapbot.logic.InboundHandler;
import com.mapbot.logic.ServerStatusManager;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 关闭服务器命令
 * #stopserver [秒数]
 */
public class StopServerCommand implements ICommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/Command/Stop");

    @Override
    public int getRequiredLevel() {
        return DataManager.PERMISSION_LEVEL_ADMIN;
    }

    @Override
    public String getDescription() {
        return "倒计时关闭服务器: #stopserver [秒数]";
    }

    @Override
    public void execute(String args, long senderQQ, long sourceGroupId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 服务器未就绪");
            return;
        }

        int countdown = 0;
        if (!args.trim().isEmpty()) {
            try {
                countdown = Integer.parseInt(args.trim());
                if (countdown < 0 || countdown > 3600) {
                    InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 倒计时范围: 0-3600 秒");
                    return;
                }
            } catch (NumberFormatException e) {
                InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 倒计时必须为数字");
                return;
            }
        }

        if (countdown == 0) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[系统] 正在立即关闭服务器...");
            LOGGER.warn("管理员 {} 执行了立即停服命令", senderQQ);
            server.execute(() -> server.halt(false));
        } else {
            final int seconds = countdown;
            ServerStatusManager.setStopCancelled(false);
            InboundHandler.sendReplyToQQ(sourceGroupId, String.format("[系统] 服务器将在 %d 秒后关闭", seconds));
            LOGGER.warn("管理员 {} 启动了关服倒计时: {}s", senderQQ, seconds);

            new Thread(() -> {
                try {
                    int remaining = seconds;
                    while (remaining > 0) {
                        if (ServerStatusManager.isStopCancelled()) {
                            InboundHandler.sendReplyToQQ(sourceGroupId, "[系统] 关服倒计时已取消");
                            return;
                        }

                        if (remaining <= 10 || remaining == 30 || remaining == 60) {
                            final int r = remaining;
                            server.execute(() -> {
                                server.getPlayerList().getPlayers().forEach(p -> 
                                    p.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c[警告] 服务器将在 " + r + " 秒后关闭"))
                                );
                            });
                            InboundHandler.sendReplyToQQ(sourceGroupId, "[倒计时] " + r + " 秒");
                        }
                        
                        Thread.sleep(1000);
                        remaining--;
                    }
                    
                    if (!ServerStatusManager.isStopCancelled()) {
                        InboundHandler.sendReplyToQQ(sourceGroupId, "[系统] 正在关闭服务器...");
                        server.execute(() -> server.halt(false));
                    }
                } catch (InterruptedException e) {
                    LOGGER.error("关服线程中断", e);
                }
            }, "MapBot-StopCountdown").start();
        }
    }
}
