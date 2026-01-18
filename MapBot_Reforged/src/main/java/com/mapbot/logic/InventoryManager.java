/*
 * MapBot Reforged - 库存管理器
 * 
 * 遵从 .ai_rules.md 中定义的治理规则。
 * 使用 NeoForge 1.21.1 DataComponents API (非 NBT)。
 * 
 * 参考: ./Project_Docs/Architecture/Migration_1.21.md
 */

package com.mapbot.logic;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mapbot.utils.ChineseTranslator;

import java.util.ArrayList;
import java.util.List;

/**
 * 库存管理器
 * 提供玩家库存查询功能，使用 DataComponents API
 * 
 * ⚠️ 重要：此类严格遵循 1.21 迁移规范
 * - 不使用 ItemStack.getTag()
 * - 不使用 CompoundTag
 * - 使用 stack.get(DataComponents.XXX) 获取数据
 */
public class InventoryManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/Inventory");

    /**
     * 获取玩家库存的可读字符串
     * 
     * @param player 目标玩家
     * @return 格式化的库存信息
     */
    public static String getPlayerInventory(ServerPlayer player) {
        if (player == null) {
            return "❌ 玩家不存在或已离线";
        }

        List<String> lines = new ArrayList<>();
        lines.add(String.format("📦 %s 的背包:", player.getName().getString()));

        var inventory = player.getInventory();
        int itemCount = 0;

        // 遍历主背包 (36格: 0-8 快捷栏, 9-35 背包)
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            ItemStack stack = inventory.items.get(slot);

            if (stack.isEmpty()) {
                continue;
            }

            itemCount++;
            String itemInfo = formatItemStack(slot, stack);
            lines.add(itemInfo);
        }

        // 检查副手
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            itemCount++;
            lines.add(formatItemStack(-1, offhand) + " [副手]");
        }

        // 检查装备栏
        int armorSlot = 100;
        for (ItemStack armor : player.getArmorSlots()) {
            if (!armor.isEmpty()) {
                itemCount++;
                lines.add(formatItemStack(armorSlot, armor) + " [装备]");
            }
            armorSlot++;
        }

        if (itemCount == 0) {
            lines.add("(空)");
        } else {
            lines.add(String.format("--- 共 %d 个物品 ---", itemCount));
        }

        return String.join("\n", lines);
    }

    /**
     * 获取玩家末影箱的可读字符串
     * Task #012-STEP5 新增
     * 
     * @param player 目标玩家
     * @return 格式化的末影箱信息
     */
    public static String getPlayerEnderChest(ServerPlayer player) {
        if (player == null) {
            return "❌ 玩家不存在或已离线";
        }

        List<String> lines = new ArrayList<>();
        lines.add(String.format("🟣 %s 的末影箱:", player.getName().getString()));

        var enderChest = player.getEnderChestInventory();
        int itemCount = 0;

        // 末影箱固定 27 格 (3行 x 9列)
        for (int slot = 0; slot < enderChest.getContainerSize(); slot++) {
            ItemStack stack = enderChest.getItem(slot);

            if (stack.isEmpty()) {
                continue;
            }

            itemCount++;
            String itemInfo = formatItemStack(slot, stack);
            lines.add(itemInfo);
        }

        if (itemCount == 0) {
            lines.add("(空)");
        } else {
            lines.add(String.format("--- 共 %d 个物品 ---", itemCount));
        }

        return String.join("\n", lines);
    }

    /**
     * 格式化单个物品堆
     * 使用 DataComponents API 获取附魔和耐久信息
     * 
     * @param slot 槽位ID
     * @param stack 物品堆
     * @return 格式化字符串
     */
    private static String formatItemStack(int slot, ItemStack stack) {
        StringBuilder sb = new StringBuilder();

        // 槽位
        if (slot >= 0) {
            sb.append(String.format("[%02d] ", slot));
        } else {
            sb.append("[--] ");
        }

        // 物品名称 (优先使用中文翻译)
        String translationKey = stack.getDescriptionId();
        String itemName = ChineseTranslator.INSTANCE.translate(translationKey);
        
        // 如果翻译键没有找到中文，回退到 getHoverName
        if (itemName.equals(translationKey)) {
            itemName = stack.getHoverName().getString();
        }
        sb.append(itemName);

        // 数量
        if (stack.getCount() > 1) {
            sb.append(" x").append(stack.getCount());
        }

        // 耐久信息 (使用 DataComponents.DAMAGE 和 DataComponents.MAX_DAMAGE)
        Integer damage = stack.get(DataComponents.DAMAGE);
        Integer maxDamage = stack.get(DataComponents.MAX_DAMAGE);
        
        if (damage != null && maxDamage != null && maxDamage > 0) {
            int remaining = maxDamage - damage;
            sb.append(String.format(" [%d/%d]", remaining, maxDamage));
        }

        // 附魔信息 (使用 DataComponents.ENCHANTMENTS)
        ItemEnchantments enchantments = stack.get(DataComponents.ENCHANTMENTS);
        if (enchantments != null && !enchantments.isEmpty()) {
            List<String> enchantNames = new ArrayList<>();
            
            enchantments.entrySet().forEach(entry -> {
                Holder<Enchantment> enchantHolder = entry.getKey();
                int level = entry.getIntValue();
                
                // 获取附魔名称 (优先使用中文翻译)
                String enchantKey = enchantHolder.getRegisteredName();
                // 构建标准翻译键: enchantment.minecraft.xxx
                String enchantTranslationKey = "enchantment." + enchantKey.replace(":", ".");
                String enchantName = ChineseTranslator.INSTANCE.translate(enchantTranslationKey);
                
                // 如果没找到中文，回退到原版名称
                if (enchantName.equals(enchantTranslationKey)) {
                    enchantName = enchantHolder.value().description().getString();
                }
                
                if (level > 1) {
                    enchantNames.add(enchantName + " " + toRoman(level));
                } else {
                    enchantNames.add(enchantName);
                }
            });
            
            if (!enchantNames.isEmpty()) {
                sb.append(" (").append(String.join(", ", enchantNames)).append(")");
            }
        }

        return sb.toString();
    }

    /**
     * 将数字转换为罗马数字
     */
    private static String toRoman(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(num);
        };
    }
}