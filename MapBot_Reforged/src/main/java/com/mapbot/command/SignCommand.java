package com.mapbot.command;

import com.mapbot.data.DataManager;
import com.mapbot.event.MapBotSignInEvent;
import com.mapbot.logic.InboundHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
        // 检查是否已签到
        if (DataManager.INSTANCE.hasSignedInToday(senderQQ)) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[提示] 今天已签到，明天再来吧");
            return;
        }

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

            // 记录签到
            DataManager.INSTANCE.recordSignIn(senderQQ);

            // 触发事件
            MapBotSignInEvent event = new MapBotSignInEvent(player, senderQQ);
            NeoForge.EVENT_BUS.post(event);

            // 如果事件未被取消 (说明 KubeJS 未处理)，发放保底奖励
            if (!event.isCanceled()) {
                ItemStack reward = new ItemStack(Items.GOLDEN_APPLE, 1);
                if (!player.getInventory().add(reward)) {
                    player.drop(reward, false);
                }
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§e[MapBot] 获得保底奖励: 金苹果 x1"));
            }

            // 基础反馈
            InboundHandler.sendReplyToQQ(sourceGroupId, "[签到成功] 奖励已发放，请在游戏内查收！");
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a[MapBot] 签到成功！"));
        });
    }
}
