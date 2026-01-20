package com.mapbot.command;

import com.mapbot.data.DataManager;
import com.mapbot.event.MapBotSignInEvent;
import com.mapbot.logic.InboundHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

/**
 * 签到命令 (Task #020 预留)
 * #sign / #签到
 */
public class SignCommand implements ICommand {
    @Override
    public String getDescription() {
        return "每日签到领取奖励: #sign";
    }

    @Override
    public void execute(String args, long senderQQ, long sourceGroupId) {
        String uuidStr = DataManager.INSTANCE.getBinding(senderQQ);
        if (uuidStr == null) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[签到失败] 请先使用 #id 绑定账号");
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(uuidStr));
            if (player == null) {
                InboundHandler.sendReplyToQQ(sourceGroupId, "[签到失败] 您必须在线才能签到并领取奖励");
                return;
            }

            // --- 核心: 触发 NeoForge 事件，供 KubeJS 联动 ---
            MapBotSignInEvent event = new MapBotSignInEvent(player, senderQQ);
            NeoForge.EVENT_BUS.post(event);

            // 基础反馈 (具体奖励逻辑由 KubeJS 处理)
            InboundHandler.sendReplyToQQ(sourceGroupId, "[签到成功] 奖励已发放，请在游戏内查收！");
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a[MapBot] 签到成功！感谢支持。"));
        });
    }
}
