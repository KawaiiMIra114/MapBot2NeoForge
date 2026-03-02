package com.mapbot.command;

import com.mapbot.data.DataManager;
import com.mapbot.security.CommandCategory;
import com.mapbot.logic.InboundHandler;
import com.mapbot.logic.ServerStatusManager;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 关闭服务器命令
 * #stopserver [秒数]
 */
public class StopServerCommand implements ICommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/Command/Stop");
    
    // Fix #9: 使用 ScheduledExecutorService 替代裸线程，支持优雅取消
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "MapBot-StopCountdown");
        t.setDaemon(true);
        return t;
    });
    private static volatile ScheduledFuture<?> currentCountdown = null;

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.SENSITIVE_WRITE; // 高风险: 停服操作
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
            // Fix #9: 防止重复创建倒计时
            if (currentCountdown != null && !currentCountdown.isDone()) {
                InboundHandler.sendReplyToQQ(sourceGroupId, "[提示] 已有倒计时正在进行，请先 #cancelstop");
                return;
            }
            
            final int seconds = countdown;
            ServerStatusManager.setStopCancelled(false);
            InboundHandler.sendReplyToQQ(sourceGroupId, String.format("[系统] 服务器将在 %d 秒后关闭", seconds));
            LOGGER.warn("管理员 {} 启动了关服倒计时: {}s", senderQQ, seconds);

            AtomicInteger remaining = new AtomicInteger(seconds);
            currentCountdown = SCHEDULER.scheduleAtFixedRate(() -> {
                if (ServerStatusManager.isStopCancelled()) {
                    InboundHandler.sendReplyToQQ(sourceGroupId, "[系统] 关服倒计时已取消");
                    currentCountdown.cancel(false);
                    return;
                }

                int r = remaining.getAndDecrement();
                if (r <= 0) {
                    InboundHandler.sendReplyToQQ(sourceGroupId, "[系统] 正在关闭服务器...");
                    server.execute(() -> server.halt(false));
                    currentCountdown.cancel(false);
                    return;
                }

                if (r <= 10 || r == 30 || r == 60) {
                    final int rem = r;
                    server.execute(() -> {
                        server.getPlayerList().getPlayers().forEach(p -> 
                            p.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c[警告] 服务器将在 " + rem + " 秒后关闭"))
                        );
                    });
                    InboundHandler.sendReplyToQQ(sourceGroupId, "[倒计时] " + rem + " 秒");
                }
            }, 0, 1, TimeUnit.SECONDS);
        }
    }
}
