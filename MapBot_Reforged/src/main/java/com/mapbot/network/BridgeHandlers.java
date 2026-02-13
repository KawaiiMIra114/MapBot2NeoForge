/*
 * MapBot Reforged - Bridge 消息处理器集合
 * 
 * R4 重构: 从 BridgeClient 拆分出全部 handler 方法
 * 所有方法接受预解析的 JsonObject，避免重复 JSON 解析
 */
package com.mapbot.network;

import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * BridgeClient 消息处理器
 * 将各类型消息的处理逻辑集中管理
 */
public final class BridgeHandlers {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/Bridge");
    
    private BridgeHandlers() {}

    // ==================== 便捷工具方法 ====================

    private static String getString(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : json.get(key).toString();
        }
        return "";
    }

    private static MinecraftServer getServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    // ==================== 指令 ====================

    static void handleCommand(JsonObject json, BridgeClient client) {
        String cmd = getString(json, "cmd");
        String requestId = getString(json, "requestId");
        LOGGER.info("[Bridge] 收到指令: {}", cmd);
        
        MinecraftServer server = getServer();
        if (server != null) {
            server.execute(() -> server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack(), cmd));
            client.sendProxyResponse(requestId, "指令已执行");
        } else {
            client.sendProxyResponse(requestId, "服务器未就绪");
        }
    }

    // ==================== STEP 13: Alpha 代理请求 ====================

    static void handleGetPlayers(JsonObject json, BridgeClient client) {
        String requestId = getString(json, "requestId");
        MinecraftServer server = getServer();
        
        if (server == null) { client.sendProxyResponse(requestId, "[服务器] 未就绪"); return; }
        
        var players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) { client.sendProxyResponse(requestId, "[在线] 当前无玩家在线"); return; }
        
        StringBuilder sb = new StringBuilder();
        sb.append("[在线] ").append(players.size()).append(" 人\n");
        for (var p : players) sb.append("• ").append(p.getName().getString()).append("\n");
        client.sendProxyResponse(requestId, sb.toString().trim());
    }

    static void handleHasPlayer(JsonObject json, BridgeClient client) {
        String requestId = getString(json, "requestId");
        String uuidStr = getString(json, "arg1");
        MinecraftServer server = getServer();

        if (server == null || uuidStr.isEmpty()) { client.sendProxyResponse(requestId, "NO"); return; }

        server.execute(() -> {
            try {
                var player = server.getPlayerList().getPlayer(UUID.fromString(uuidStr));
                client.sendProxyResponse(requestId, player != null ? "YES" : "NO");
            } catch (Exception e) {
                client.sendProxyResponse(requestId, "NO");
            }
        });
    }

    static void handleGetStatus(JsonObject json, BridgeClient client) {
        String requestId = getString(json, "requestId");
        MinecraftServer server = getServer();
        
        if (server == null) { client.sendProxyResponse(requestId, "[服务器] 未就绪"); return; }
        
        double tps = com.mapbot.logic.ServerStatusManager.getCurrentTPS();
        Runtime rt = Runtime.getRuntime();
        long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        long maxMB = rt.maxMemory() / 1024 / 1024;
        int playerCount = server.getPlayerList().getPlayers().size();
        
        client.sendProxyResponse(requestId, String.format(
            "[状态] %s\n在线: %d 人\nTPS: %.1f\n内存: %dMB / %dMB",
            com.mapbot.config.BotConfig.getServerId(), playerCount, tps, usedMB, maxMB));
    }

    static void handleBindPlayer(JsonObject json, BridgeClient client) {
        String requestId = getString(json, "requestId");
        String playerName = getString(json, "arg1");
        MinecraftServer server = getServer();
        
        if (server == null) { client.sendProxyResponse(requestId, "[错误] 服务器未就绪"); return; }
        
        try {
            java.util.Optional<com.mojang.authlib.GameProfile> profile = server.getProfileCache().get(playerName);
            
            if (profile.isEmpty()) {
                if (!server.usesAuthentication()) {
                    var uuid = net.minecraft.core.UUIDUtil.createOfflinePlayerUUID(playerName);
                    var whitelist = server.getPlayerList().getWhiteList();
                    var gp = new com.mojang.authlib.GameProfile(uuid, playerName);
                    if (!whitelist.isWhiteListed(gp)) {
                        whitelist.add(new net.minecraft.server.players.UserWhiteListEntry(gp));
                        whitelist.save();
                    }
                    client.sendProxyResponse(requestId, "SUCCESS:" + uuid + ":" + playerName);
                    return;
                }
                client.sendProxyResponse(requestId, "FAIL:[绑定失败] 玩家不存在");
                return;
            }
            
            String uuid = profile.get().getId().toString();
            var whitelist = server.getPlayerList().getWhiteList();
            if (!whitelist.isWhiteListed(profile.get())) {
                whitelist.add(new net.minecraft.server.players.UserWhiteListEntry(profile.get()));
                whitelist.save();
            }
            client.sendProxyResponse(requestId, "SUCCESS:" + uuid + ":" + profile.get().getName());
            
        } catch (Exception e) {
            LOGGER.error("绑定失败", e);
            client.sendProxyResponse(requestId, "[错误] 绑定失败: " + e.getMessage());
        }
    }

    static void handleResolveUuid(JsonObject json, BridgeClient client) {
        String requestId = getString(json, "requestId");
        String playerName = getString(json, "arg1");
        MinecraftServer server = getServer();

        if (server == null || playerName.isEmpty()) { client.sendProxyResponse(requestId, ""); return; }

        try {
            var profile = server.getProfileCache().get(playerName);
            if (profile.isPresent()) {
                client.sendProxyResponse(requestId, profile.get().getId().toString());
                return;
            }
            if (!server.usesAuthentication()) {
                var uuid = net.minecraft.core.UUIDUtil.createOfflinePlayerUUID(playerName);
                client.sendProxyResponse(requestId, uuid.toString());
                return;
            }
            client.sendProxyResponse(requestId, "");
        } catch (Exception e) {
            client.sendProxyResponse(requestId, "");
        }
    }

    static void handleSignIn(JsonObject json, BridgeClient client) {
        String requestId = getString(json, "requestId");
        String qqStr = getString(json, "arg1");
        
        try {
            long qq = Long.parseLong(qqStr);
            var dm = com.mapbot.data.DataManager.INSTANCE;
            
            String uuid = dm.getBinding(qq);
            String playerName = "未知玩家";
            boolean isOnline = false;
            
            MinecraftServer server = getServer();
            if (server != null && uuid != null) {
                var player = server.getPlayerList().getPlayer(UUID.fromString(uuid));
                if (player != null) {
                    playerName = player.getName().getString();
                    isOnline = true;
                } else {
                    var profile = server.getProfileCache().get(UUID.fromString(uuid));
                    if (profile.isPresent()) playerName = profile.get().getName();
                }
            }
            
            if (dm.hasSignedInToday(qq)) {
                int days = dm.getSignInDays(qq);
                client.sendProxyResponse(requestId, String.format(
                    "%s 今日已签到\n您累计已签到 %d 天\n[提示] 今日已领取，明天再来吧", playerName, days));
                return;
            }
            
            var item = com.mapbot.logic.SignManager.INSTANCE.rollSignReward(qq);
            if (item == null) { client.sendProxyResponse(requestId, "[错误] 奖池配置异常"); return; }
            
            int days = dm.getSignInDays(qq);
            String rarityMsg = com.mapbot.data.loot.LootConfig.INSTANCE.getRarityMessage(item.rarity);
            
            StringBuilder result = new StringBuilder();
            result.append(playerName).append(" 今日已签到\n");
            result.append("您累计已签到 ").append(days).append(" 天\n");
            result.append("获得物品: [").append(item.rarity).append("] ").append(item.name).append(" x").append(item.count).append("\n");
            result.append(rarityMsg);
            
            if (isOnline) {
                result.append("\n若确认背包有空间，请输入 #accept 指令来确认物品发放");
            } else {
                result.append("\n您当前未在线");
                result.append("\n请私聊机器人输入 #cdk 来获取兑换码");
                result.append("\n上线后使用 /mapbot cdk [your-cdkey] 来兑换物品");
            }
            
            client.sendProxyResponse(requestId, result.toString());
            
        } catch (Exception e) {
            LOGGER.error("签到失败", e);
            client.sendProxyResponse(requestId, "[错误] 签到失败: " + e.getMessage());
        }
    }

    static void handleAcceptReward(JsonObject json, BridgeClient client) {
        String requestId = getString(json, "requestId");
        String qqStr = getString(json, "arg1");
        
        try {
            long qq = Long.parseLong(qqStr);
            var signManager = com.mapbot.logic.SignManager.INSTANCE;
            var dm = com.mapbot.data.DataManager.INSTANCE;
            
            if (!signManager.hasPendingReward(qq)) {
                client.sendProxyResponse(requestId, "[领取失败] 无待领取奖励\n[提示] 请先使用 #sign 签到");
                return;
            }
            
            String uuid = dm.getBinding(qq);
            if (uuid == null) {
                client.sendProxyResponse(requestId, "[领取失败] 账号未绑定\n[提示] 请使用 #id 绑定游戏账号");
                return;
            }
            
            MinecraftServer server = getServer();
            if (server != null) {
                var player = server.getPlayerList().getPlayer(UUID.fromString(uuid));
                if (player == null) {
                    client.sendProxyResponse(requestId, "[领取失败] 玩家不在线\n[提示] 请使用 #cdk 获取兑换码");
                    return;
                }
            }
            
            boolean success = signManager.claimOnline(qq);
            if (success) {
                client.sendProxyResponse(requestId, "[领取成功] 物品已发放到背包");
            } else {
                client.sendProxyResponse(requestId, "[领取失败] 背包空间不足\n[提示] 请清理背包后重试");
            }
        } catch (Exception e) {
            LOGGER.error("领取失败", e);
            client.sendProxyResponse(requestId, "[错误] 领取失败: " + e.getMessage());
        }
    }

    static void handleGetInventory(JsonObject json, BridgeClient client) {
        String requestId = getString(json, "requestId");
        String playerName = getString(json, "arg1");
        MinecraftServer server = getServer();
        
        if (server == null) { client.sendProxyResponse(requestId, "[错误] 服务器未就绪"); return; }
        
        var player = server.getPlayerList().getPlayerByName(playerName);
        if (player == null) { client.sendProxyResponse(requestId, "[错误] 玩家不在线: " + playerName); return; }
        
        client.sendProxyResponse(requestId, com.mapbot.logic.InventoryManager.getPlayerInventory(player));
    }

    static void handleGetLocation(JsonObject json, BridgeClient client) {
        String requestId = getString(json, "requestId");
        String playerName = getString(json, "arg1");
        MinecraftServer server = getServer();
        
        if (server == null) { client.sendProxyResponse(requestId, "[错误] 服务器未就绪"); return; }
        
        var player = server.getPlayerList().getPlayerByName(playerName);
        if (player == null) { client.sendProxyResponse(requestId, "[错误] 玩家不在线: " + playerName); return; }
        
        String world = player.level().dimension().location().toString();
        client.sendProxyResponse(requestId, String.format("[位置] %s\n世界: %s\n坐标: %d, %d, %d",
            playerName, world, (int)player.getX(), (int)player.getY(), (int)player.getZ()));
    }

    static void handleExecuteCommand(JsonObject json, BridgeClient client) {
        String requestId = getString(json, "requestId");
        String command = getString(json, "arg1");
        MinecraftServer server = getServer();
        
        if (server == null) { client.sendProxyResponse(requestId, "[错误] 服务器未就绪"); return; }
        
        server.execute(() -> server.getCommands().performPrefixedCommand(
            server.createCommandSourceStack(), command));
        client.sendProxyResponse(requestId, "[成功] 指令已执行: " + command);
    }

    static void handleBroadcast(JsonObject json, BridgeClient client) {
        String requestId = getString(json, "requestId");
        String message = getString(json, "arg1");
        MinecraftServer server = getServer();
        
        if (server != null) {
            server.execute(() -> server.getPlayerList().broadcastSystemMessage(
                net.minecraft.network.chat.Component.literal(message), false));
        }
        client.sendProxyResponse(requestId, "OK");
    }

    static void handleGetPlaytime(JsonObject json, BridgeClient client) {
        String requestId = getString(json, "requestId");
        String playerName = getString(json, "arg1");
        String modeStr = getString(json, "arg2");
        MinecraftServer server = getServer();
        
        if (server == null) { client.sendProxyResponse(requestId, "[错误] 服务器未就绪"); return; }
        
        int mode = 0;
        try { mode = Integer.parseInt(modeStr); } catch (Exception ignored) {}
        
        final int finalMode = mode;
        server.execute(() -> {
            UUID uuid = server.getProfileCache().get(playerName)
                .map(com.mojang.authlib.GameProfile::getId).orElse(null);
            
            if (uuid == null) { client.sendProxyResponse(requestId, "[错误] 玩家不存在: " + playerName); return; }
            
            long mins = com.mapbot.data.PlaytimeManager.INSTANCE.getPlaytimeMinutes(uuid, finalMode);
            String time = com.mapbot.data.PlaytimeManager.formatDuration(mins);
            String[] periods = {"日", "周", "月", "总"};
            String period = (finalMode >= 0 && finalMode < periods.length) ? periods[finalMode] : "总";
            client.sendProxyResponse(requestId, String.format("[在线时长] %s (%s)\n时长: %s", playerName, period, time));
        });
    }

    static void handleGetCdk(JsonObject json, BridgeClient client) {
        String requestId = getString(json, "requestId");
        String qqStr = getString(json, "arg1");
        
        try {
            long qq = Long.parseLong(qqStr);
            if (!com.mapbot.logic.SignManager.INSTANCE.hasPendingReward(qq)) {
                client.sendProxyResponse(requestId, "[提示] 您没有待领取的奖励，请先 #sign");
                return;
            }
            String code = com.mapbot.logic.SignManager.INSTANCE.generateCdk(qq);
            if (code != null) {
                client.sendProxyResponse(requestId, String.format("[兑换码] %s\n进服输入: /mapbot cdk %s", code, code));
            } else {
                client.sendProxyResponse(requestId, "[错误] 生成兑换码失败");
            }
        } catch (Exception e) {
            client.sendProxyResponse(requestId, "[错误] " + e.getMessage());
        }
    }

    // ==================== Task #022: Redis 迁移新接口 ====================

    static void handleRollLoot(JsonObject json, BridgeClient client) {
        String requestId = getString(json, "requestId");
        try {
            var item = com.mapbot.data.loot.LootConfig.INSTANCE.roll();
            if (item == null) { client.sendProxyResponse(requestId, ""); return; }
            
            String rarityMsg = com.mapbot.data.loot.LootConfig.INSTANCE.getRarityMessage(item.rarity);
            String result = String.format(
                "{\"id\":\"%s\",\"count\":%d,\"name\":\"%s\",\"rarity\":\"%s\",\"rarityMsg\":\"%s\"}",
                item.id, item.count, item.name, item.rarity, client.escapeJson(rarityMsg));
            client.sendProxyResponse(requestId, result);
        } catch (Exception e) {
            LOGGER.error("抽奖失败", e);
            client.sendProxyResponse(requestId, "");
        }
    }

    static void handleGiveItem(JsonObject json, BridgeClient client) {
        String requestId = getString(json, "requestId");
        String uuid = getString(json, "arg1");
        String itemJson = getString(json, "arg2");
        
        try {
            MinecraftServer server = getServer();
            if (server == null) { client.sendProxyResponse(requestId, "FAIL:服务器未就绪"); return; }
            
            var player = server.getPlayerList().getPlayer(UUID.fromString(uuid));
            if (player == null) { client.sendProxyResponse(requestId, "FAIL:OFFLINE"); return; }
            
            var itemObj = com.google.gson.JsonParser.parseString(itemJson).getAsJsonObject();
            String itemId = itemObj.get("id").getAsString();
            int count = itemObj.get("count").getAsInt();
            
            var id = net.minecraft.resources.ResourceLocation.parse(itemId);
            var mcItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
            
            if (mcItem == null || mcItem == net.minecraft.world.item.Items.AIR) {
                client.sendProxyResponse(requestId, "FAIL:未知物品");
                return;
            }
            
            var stack = new net.minecraft.world.item.ItemStack(mcItem, count);
            if (player.getInventory().add(stack)) {
                client.sendProxyResponse(requestId, "SUCCESS");
            } else {
                player.drop(stack, false);
                client.sendProxyResponse(requestId, "SUCCESS");
            }
        } catch (Exception e) {
            LOGGER.error("发放物品失败", e);
            client.sendProxyResponse(requestId, "FAIL:" + e.getMessage());
        }
    }

    // ==================== 代理响应 ====================

    static void handleProxyResponseFromAlpha(JsonObject json, BridgeClient client) {
        String requestId = getString(json, "requestId");
        String result = getString(json, "result");
        client.completePendingRequest(requestId, result);
    }

    // ==================== 停服/取消 ====================

    static void handleStopServer(JsonObject json, BridgeClient client) {
        String requestId = getString(json, "requestId");
        String countdownStr = getString(json, "arg1");
        MinecraftServer server = getServer();
        
        if (server == null) { client.sendProxyResponse(requestId, "[错误] 服务器未就绪"); return; }
        
        int countdown = 0;
        try { countdown = Integer.parseInt(countdownStr); } catch (Exception ignored) {}
        
        if (countdown == 0) {
            client.sendProxyResponse(requestId, "[系统] 正在立即关闭服务器...");
            LOGGER.warn("收到立即关服指令");
            server.execute(() -> server.halt(false));
        } else {
            final int seconds = countdown;
            com.mapbot.logic.ServerStatusManager.setStopCancelled(false);
            client.sendProxyResponse(requestId, String.format("[系统] 服务器将在 %d 秒后关闭", seconds));
            LOGGER.warn("收到倒计时关服指令: {}s", seconds);
            
            new Thread(() -> {
                try {
                    int remaining = seconds;
                    while (remaining > 0) {
                        if (com.mapbot.logic.ServerStatusManager.isStopCancelled()) return;
                        if (remaining <= 10 || remaining == 30 || remaining == 60) {
                            final int r = remaining;
                            server.execute(() -> server.getPlayerList().getPlayers().forEach(p ->
                                p.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                    "§c[警告] 服务器将在 " + r + " 秒后关闭"))));
                        }
                        Thread.sleep(1000);
                        remaining--;
                    }
                    if (!com.mapbot.logic.ServerStatusManager.isStopCancelled()) {
                        server.execute(() -> server.halt(false));
                    }
                } catch (InterruptedException e) {
                    LOGGER.error("关服线程中断", e);
                }
            }, "MapBot-StopCountdown").start();
        }
    }

    static void handleCancelStop(JsonObject json, BridgeClient client) {
        String requestId = getString(json, "requestId");
        com.mapbot.logic.ServerStatusManager.setStopCancelled(true);
        client.sendProxyResponse(requestId, "[系统] 已发送取消请求");
    }

    // ==================== QQ 消息 ====================

    static void handleQQMessage(JsonObject json, BridgeClient client) {
        String sender = getString(json, "sender");
        String content = getString(json, "content");
        if (sender.isEmpty() || content.isEmpty()) return;
        
        String formattedMsg = String.format("[QQ] %s: %s", sender, content);
        LOGGER.info("[QQ->MC] {}", formattedMsg);
        
        MinecraftServer server = getServer();
        if (server == null) { LOGGER.warn("收到 QQ 消息但服务器未就绪，丢弃: {}", formattedMsg); return; }
        
        server.execute(() -> {
            MinecraftServer s = getServer();
            if (s != null) {
                s.getPlayerList().broadcastSystemMessage(
                    net.minecraft.network.chat.Component.literal(formattedMsg), false);
            }
        });
    }

    // ==================== 文件操作 ====================

    static void handleFileRequest(JsonObject json, BridgeClient client) {
        String type = getString(json, "type");
        String requestId = getString(json, "requestId");
        String path = getString(json, "path");
        
        if (!path.isEmpty() && !client.isPathSafe(path)) {
            client.send(String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"error\":\"Access denied: path outside server directory\"}", requestId));
            LOGGER.warn("拒绝不安全的文件操作路径: {}", path);
            return;
        }
        
        try {
            String response;
            switch (type) {
                case "file_list":  response = handleFileList(requestId, path, client); break;
                case "file_read":  response = handleFileRead(requestId, path, client); break;
                case "file_write": response = handleFileWrite(requestId, path, getString(json, "content"), client); break;
                case "file_delete":response = handleFileDelete(requestId, path, client); break;
                default:
                    response = String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"error\":\"Unknown action\"}", requestId);
            }
            client.send(response);
        } catch (Exception e) {
            client.send(String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"error\":\"%s\"}", requestId, e.getMessage()));
        }
    }

    private static String handleFileList(String requestId, String path, BridgeClient client) throws IOException {
        File dir = new File(path.isEmpty() ? "." : path);
        if (!dir.isDirectory()) {
            return String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"error\":\"Not a directory\"}", requestId);
        }
        StringBuilder sb = new StringBuilder("[");
        File[] files = dir.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (i > 0) sb.append(",");
                File f = files[i];
                sb.append("{\"name\":\"").append(client.escapeJson(f.getName())).append("\",");
                sb.append("\"isDir\":").append(f.isDirectory()).append(",");
                sb.append("\"size\":").append(f.isFile() ? f.length() : 0).append("}");
            }
        }
        sb.append("]");
        return String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"files\":%s}", requestId, sb);
    }

    private static String handleFileRead(String requestId, String path, BridgeClient client) throws IOException {
        File file = new File(path);
        if (!file.isFile()) {
            return String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"error\":\"Not a file\"}", requestId);
        }
        String content = java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8);
        return String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"content\":\"%s\"}", requestId, client.escapeJson(content));
    }

    private static String handleFileWrite(String requestId, String path, String content, BridgeClient client) throws IOException {
        File file = new File(path);
        java.nio.file.Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
        return String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"content\":\"ok\"}", requestId);
    }

    private static String handleFileDelete(String requestId, String path, BridgeClient client) throws IOException {
        File file = new File(path);
        boolean deleted = file.delete();
        if (deleted) {
            return String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"content\":\"ok\"}", requestId);
        } else {
            return String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"error\":\"Delete failed\"}", requestId);
        }
    }
}
