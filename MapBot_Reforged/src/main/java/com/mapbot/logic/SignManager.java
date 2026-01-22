package com.mapbot.logic;

import com.mapbot.data.DataManager;
import com.mapbot.data.loot.LootConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 签到状态管理器
 * 管理签到流程: 抽奖 -> 暂存 -> (在线领 | CDK兑换)
 */
public class SignManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/Sign");
    public static final SignManager INSTANCE = new SignManager();

    // 暂存的抽奖结果 (QQ -> LootItem)
    // 注意: 这只是临时缓存，服务器重启会丢失 (符合逻辑，必须当天领)
    private final Map<Long, LootConfig.LootItem> pendingRewards = new ConcurrentHashMap<>();
    
    // CDK 映射 (Code -> RewardContext)
    private final Map<String, CdkContext> activeCdks = new ConcurrentHashMap<>();
    
    private record CdkContext(long qq, LootConfig.LootItem item, long expiry) {}

    /**
     * 执行签到抽奖
     * @param qq 玩家QQ
     * @return 抽到的物品
     */
    public LootConfig.LootItem rollSignReward(long qq) {
        // 如果已经抽过了但没领，返回旧结果
        if (pendingRewards.containsKey(qq)) {
            return pendingRewards.get(qq);
        }
        
        // 抽新奖
        LootConfig.LootItem item = LootConfig.INSTANCE.roll();
        if (item != null) {
            pendingRewards.put(qq, item);
            // 标记 DataManager 今日已签 (防止重复刷)
            DataManager.INSTANCE.recordSignIn(qq);
        }
        return item;
    }

    /**
     * 生成兑换码 (针对离线玩家)
     * @param qq 玩家QQ
     * @return 兑换码字符串 (如 MP-8X2A)
     */
    public String generateCdk(long qq) {
        LootConfig.LootItem item = pendingRewards.get(qq);
        if (item == null) return null;

        // 生成简单易读的 4 位随机码
        String code = "MP-" + randomString(4);
        
        // 存入 CDK 库 (有效期 24 小时)
        activeCdks.put(code, new CdkContext(qq, item, System.currentTimeMillis() + 86400000L));
        
        // 从暂存区移除 (进入 CDK 流程)
        pendingRewards.remove(qq);
        
        return code;
    }

    /**
     * 在线直接领取
     * @param qq 玩家QQ
     * @return 是否成功发放
     */
    public boolean claimOnline(long qq) {
        LootConfig.LootItem item = pendingRewards.remove(qq);
        if (item == null) return false;

        String uuidStr = DataManager.INSTANCE.getBinding(qq);
        if (uuidStr == null) return false;

        return distributeItem(uuidStr, item);
    }

    /**
     * 使用 CDK 兑换
     * @param uuidStr 玩家UUID
     * @param code 兑换码
     * @return 兑换结果描述
     */
    public String redeemCdk(String uuidStr, String code) {
        CdkContext ctx = activeCdks.remove(code);
        
        if (ctx == null) return "无效的兑换码";
        if (System.currentTimeMillis() > ctx.expiry) return "兑换码已过期";
        
        // 校验使用者是否为绑定的账号 (防止被盗用)
        long boundQQ = DataManager.INSTANCE.getQQByUUID(uuidStr);
        if (boundQQ != ctx.qq) {
            // 放回池子 (防止误操作导致失效)
            activeCdks.put(code, ctx); 
            return "此兑换码不属于您绑定的账号";
        }

        if (distributeItem(uuidStr, ctx.item)) {
            return "兑换成功！获得: " + ctx.item.name + " x" + ctx.item.count;
        } else {
            // 发放失败 (如背包满)，放回
            activeCdks.put(code, ctx);
            return "背包空间不足，兑换失败";
        }
    }

    /**
     * 核心发放逻辑 (给予物品)
     */
    private boolean distributeItem(String uuidStr, LootConfig.LootItem item) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return false;

        ServerPlayer player = server.getPlayerList().getPlayer(UUID.fromString(uuidStr));
        if (player == null) return false;

        try {
            // 解析物品 ID
            net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.parse(item.id);
            net.minecraft.world.item.Item mcItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
            
            if (mcItem == null || mcItem == net.minecraft.world.item.Items.AIR) {
                LOGGER.error("未知物品ID: {}", item.id);
                return false;
            }

            ItemStack stack = new ItemStack(mcItem, item.count);
            
            // 尝试塞入背包
            if (player.getInventory().add(stack)) {
                return true;
            } else {
                // 背包满，尝试生成掉落物
                player.drop(stack, false);
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("发放物品失败", e);
            return false;
        }
    }
    
    public boolean hasPendingReward(long qq) {
        return pendingRewards.containsKey(qq);
    }

    private String randomString(int length) {
        String chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
