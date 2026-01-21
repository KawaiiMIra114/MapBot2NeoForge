package com.mapbot.command;

import com.mapbot.data.DataManager;
import com.mapbot.event.MapBotSignInEvent;
import com.mapbot.logic.InboundHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

package com.mapbot.command;

import com.mapbot.data.DataManager;
import com.mapbot.data.loot.LootConfig;
import com.mapbot.logic.InboundHandler;
import com.mapbot.logic.SignManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

/**
 * 每日签到命令 (v2)
 * #sign / #签到
 */
public class SignCommand implements ICommand {
    @Override
    public String getDescription() {
        return "每日签到 (抽奖): #sign";
    }

    @Override
    public void execute(String args, long senderQQ, long sourceGroupId) {
        // 1. 绑定检查
        String uuidStr = DataManager.INSTANCE.getBinding(senderQQ);
        if (uuidStr == null) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[签到失败] 请先使用 #id 绑定账号");
            return;
        }

        // 2. 每日冷却检查
        // 注意: 如果有待领取的奖励 (hasPendingReward)，允许再次输入 #sign 查看状态
        if (DataManager.INSTANCE.hasSignedInToday(senderQQ) && !SignManager.INSTANCE.hasPendingReward(senderQQ)) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[提示] 今天已签到，明天再来吧");
            return;
        }

        // 3. 执行抽奖 (或获取暂存结果)
        LootConfig.LootItem item = SignManager.INSTANCE.rollSignReward(senderQQ);
        if (item == null) {
            InboundHandler.sendReplyToQQ(sourceGroupId, "[错误] 奖池配置异常，无法抽奖");
            return;
        }

        // 4. 构建回复
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[签到] 成功 (QQ: %d)\n", senderQQ));
        sb.append("----------------\n");
        sb.append(String.format("物品: [%s] %s x%d\n", item.rarity, item.name, item.count));
        sb.append(LootConfig.INSTANCE.getRarityMessage(item.rarity)).append("\n");
        sb.append("----------------\n");

        // 5. 检查在线状态，决定引导流程
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        boolean isOnline = false;
        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(uuidStr));
            isOnline = (player != null);
        }

        if (isOnline) {
            sb.append("[状态] 检测到当前在线\n");
            sb.append("[操作] 请输入 #accept 领取奖励");
        } else {
            sb.append("[状态] 检测到当前离线\n");
            sb.append("[操作] 请私聊机器人输入 #cdk 获取兑换码\n");
            sb.append("[操作] 上线后使用 /mapbot cdk [兑换码] 领取");
        }

        InboundHandler.sendReplyToQQ(sourceGroupId, sb.toString());
    }
}
