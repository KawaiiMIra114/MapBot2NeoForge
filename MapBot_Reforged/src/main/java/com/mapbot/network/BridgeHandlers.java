/*
 * MapBot Reforged - Bridge 消息处理器集合
 * 
 * R4 重构: 从 BridgeClient 拆分出全部 handler 方法
 * 所有方法接受预解析的 JsonObject，避免重复 JSON 解析
 */
package com.mapbot.network;

import com.mapbot.common.protocol.BridgeErrorMapper;

import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.Locale;
import java.util.Base64;
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

    private static UUID parseUuidLenient(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;

        try {
            return UUID.fromString(s);
        } catch (Exception ignored) {
        }

        // 兼容无连字符 UUID
        if (s.matches("[0-9a-fA-F]{32}")) {
            String dashed = s.replaceFirst(
                "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})",
                "$1-$2-$3-$4-$5"
            );
            try {
                return UUID.fromString(dashed);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    // ==================== 指令 ====================

    static void handleCommand(JsonObject json, BridgeClient client) {
        String cmd = getString(json, "cmd");
        String requestId = getString(json, "requestId");
        LOGGER.info("[Bridge] 收到指令: {}", cmd);
        
        MinecraftServer server = getServer();
        if (server == null) {
            client.sendProxyResponse(requestId, "服务器未就绪");
            return;
        }

        String normalized = normalizeCommand(cmd);
        if (normalized.isEmpty()) {
            client.sendProxyResponse(requestId, "指令为空");
            return;
        }

        CompletableFuture<String> outcome = new CompletableFuture<>();
        server.execute(() -> {
            boolean ok = tryExecuteFromConsole(server, normalized);
            outcome.complete(ok ? "指令已执行" : "指令执行失败或语法错误");
        });

        try {
            client.sendProxyResponse(requestId, outcome.get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            client.sendProxyResponse(requestId, "指令执行超时");
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
                UUID uuid = parseUuidLenient(uuidStr);
                if (uuid == null) {
                    client.sendProxyResponse(requestId, "NO");
                    return;
                }
                var player = server.getPlayerList().getPlayer(uuid);
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

        String normalizedName = playerName == null ? "" : playerName.trim();
        if (normalizedName.isEmpty()) {
            client.sendProxyResponse(requestId, "FAIL:[绑定失败] 玩家名为空");
            return;
        }

        CompletableFuture<String> outcome = new CompletableFuture<>();
        server.execute(() -> {
            try {
                java.util.Optional<com.mojang.authlib.GameProfile> profile = server.getProfileCache().get(normalizedName);
                if (profile.isPresent()) {
                    addToWhitelist(server, profile.get());
                    outcome.complete("SUCCESS:" + profile.get().getId() + ":" + profile.get().getName());
                    return;
                }

                if (!server.usesAuthentication()) {
                    var uuid = net.minecraft.core.UUIDUtil.createOfflinePlayerUUID(normalizedName);
                    var gp = new com.mojang.authlib.GameProfile(uuid, normalizedName);
                    addToWhitelist(server, gp);
                    outcome.complete("SUCCESS:" + uuid + ":" + normalizedName);
                    return;
                }
                outcome.complete("FAIL:[绑定失败] 玩家不存在");
            } catch (Exception e) {
                LOGGER.error("绑定失败", e);
                outcome.complete("FAIL:[错误] 绑定失败: " + e.getMessage());
            }
        });

        try {
            client.sendProxyResponse(requestId, outcome.get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            client.sendProxyResponse(requestId, "FAIL:[错误] 绑定请求超时");
        }
    }

    static void handleWhitelistAdd(JsonObject json, BridgeClient client) {
        String requestId = getString(json, "requestId");
        String playerName = getString(json, "arg1");
        MinecraftServer server = getServer();

        if (server == null) {
            client.sendProxyResponse(requestId, "FAIL:服务器未就绪");
            return;
        }
        if (playerName == null || playerName.isBlank()) {
            client.sendProxyResponse(requestId, "FAIL:玩家名为空");
            return;
        }
        if (!hasRootCommand(server, "whitelist")) {
            client.sendProxyResponse(requestId, "FAIL:当前服务端未启用 whitelist 命令");
            return;
        }

        String normalizedName = playerName.trim();
        if (!normalizedName.matches("^[A-Za-z0-9_]{3,16}$")) {
            client.sendProxyResponse(requestId, "FAIL:玩家名格式非法");
            return;
        }

        CompletableFuture<String> outcome = new CompletableFuture<>();
        server.execute(() -> {
            try {
                boolean ok = tryExecuteFromConsole(server, "whitelist add " + normalizedName);
                outcome.complete(ok ? "SUCCESS" : "FAIL:命令执行失败");
            } catch (Exception e) {
                LOGGER.error("白名单同步失败: name={}", normalizedName, e);
                outcome.complete("FAIL:" + e.getMessage());
            }
        });

        try {
            client.sendProxyResponse(requestId, outcome.get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            client.sendProxyResponse(requestId, "FAIL:白名单同步超时");
        }
    }

    static void handleWhitelistRemove(JsonObject json, BridgeClient client) {
        String requestId = getString(json, "requestId");
        String playerName = getString(json, "arg1");
        MinecraftServer server = getServer();

        if (server == null) {
            client.sendProxyResponse(requestId, "FAIL:服务器未就绪");
            return;
        }
        if (playerName == null || playerName.isBlank()) {
            client.sendProxyResponse(requestId, "FAIL:玩家名为空");
            return;
        }
        if (!hasRootCommand(server, "whitelist")) {
            client.sendProxyResponse(requestId, "FAIL:当前服务端未启用 whitelist 命令");
            return;
        }

        String normalizedName = playerName.trim();
        if (!normalizedName.matches("^[A-Za-z0-9_]{3,16}$")) {
            client.sendProxyResponse(requestId, "FAIL:玩家名格式非法");
            return;
        }

        CompletableFuture<String> outcome = new CompletableFuture<>();
        server.execute(() -> {
            try {
                boolean ok = tryExecuteFromConsole(server, "whitelist remove " + normalizedName);
                outcome.complete(ok ? "SUCCESS" : "FAIL:命令执行失败");
            } catch (Exception e) {
                LOGGER.error("白名单移除失败: name={}", normalizedName, e);
                outcome.complete("FAIL:" + e.getMessage());
            }
        });

        try {
            client.sendProxyResponse(requestId, outcome.get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            client.sendProxyResponse(requestId, "FAIL:白名单移除超时");
        }
    }

    static void handleReloadConfig(JsonObject json, BridgeClient client) {
        String requestId = getString(json, "requestId");
        MinecraftServer server = getServer();
        if (server == null) {
            client.sendProxyResponse(requestId, "FAIL:服务器未就绪");
            return;
        }

        CompletableFuture<String> outcome = new CompletableFuture<>();
        server.execute(() -> {
            try {
                com.mapbot.data.DataManager.INSTANCE.init();
                com.mapbot.data.loot.LootConfig.INSTANCE.init();
                outcome.complete("SUCCESS");
            } catch (Exception e) {
                LOGGER.error("子服重载失败", e);
                outcome.complete("FAIL:" + e.getMessage());
            }
        });

        try {
            client.sendProxyResponse(requestId, outcome.get(8, TimeUnit.SECONDS));
        } catch (Exception e) {
            client.sendProxyResponse(requestId, "FAIL:子服重载超时");
        }
    }

    static void handleResolveUuid(JsonObject json, BridgeClient client) {
        String requestId = getString(json, "requestId");
        String playerName = getString(json, "arg1");
        MinecraftServer server = getServer();

        if (server == null || playerName.isEmpty()) { client.sendProxyResponse(requestId, ""); return; }

        CompletableFuture<String> outcome = new CompletableFuture<>();
        String normalizedName = playerName.trim();
        server.execute(() -> {
            try {
                var profile = server.getProfileCache().get(normalizedName);
                if (profile.isPresent()) {
                    outcome.complete(profile.get().getId().toString());
                    return;
                }
                if (!server.usesAuthentication()) {
                    var uuid = net.minecraft.core.UUIDUtil.createOfflinePlayerUUID(normalizedName);
                    outcome.complete(uuid.toString());
                    return;
                }
                outcome.complete("");
            } catch (Exception ignored) {
                outcome.complete("");
            }
        });

        try {
            client.sendProxyResponse(requestId, outcome.get(3, TimeUnit.SECONDS));
        } catch (Exception e) {
            client.sendProxyResponse(requestId, "");
        }
    }

    static void handleResolveName(JsonObject json, BridgeClient client) {
        String requestId = getString(json, "requestId");
        String uuidStr = getString(json, "arg1");
        MinecraftServer server = getServer();

        if (server == null || uuidStr.isEmpty()) {
            client.sendProxyResponse(requestId, "");
            return;
        }

        CompletableFuture<String> outcome = new CompletableFuture<>();
        server.execute(() -> {
            try {
                UUID uuid = parseUuidLenient(uuidStr);
                if (uuid == null) {
                    outcome.complete("");
                    return;
                }

                var online = server.getPlayerList().getPlayer(uuid);
                if (online != null) {
                    outcome.complete(online.getName().getString());
                    return;
                }

                var profile = server.getProfileCache().get(uuid);
                if (profile.isPresent()) {
                    outcome.complete(profile.get().getName());
                    return;
                }
                outcome.complete("");
            } catch (Exception ignored) {
                outcome.complete("");
            }
        });

        try {
            client.sendProxyResponse(requestId, outcome.get(3, TimeUnit.SECONDS));
        } catch (Exception e) {
            client.sendProxyResponse(requestId, "");
        }
    }

    static void handleSwitchServer(JsonObject json, BridgeClient client) {
        String requestId = getString(json, "requestId");
        String playerName = getString(json, "arg1");
        String transferEndpoint = getString(json, "arg2");
        MinecraftServer server = getServer();

        if (server == null) {
            client.sendProxyResponse(requestId, "FAIL:服务器未就绪");
            return;
        }
        if (playerName == null || playerName.isBlank()) {
            client.sendProxyResponse(requestId, "FAIL:玩家名为空");
            return;
        }
        if (transferEndpoint == null || transferEndpoint.isBlank()) {
            client.sendProxyResponse(requestId, "FAIL:目标转移地址为空");
            return;
        }

        HostPort hp = parseHostPort(transferEndpoint.trim());
        if (hp == null) {
            client.sendProxyResponse(requestId, "FAIL:目标转移地址格式错误，应为 host:port");
            return;
        }
        if (!hasRootCommand(server, "transfer")) {
            client.sendProxyResponse(requestId, "FAIL:当前服务端未安装原生 /transfer 命令");
            return;
        }

        CompletableFuture<String> outcome = new CompletableFuture<>();
        String normalizedPlayer = playerName.trim();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayerByName(normalizedPlayer);
            if (player == null) {
                outcome.complete("FAIL:玩家不在线，无法执行跨服");
                return;
            }
            boolean success = tryExecuteFromPlayer(server, player, "transfer " + hp.host + " " + hp.port);
            if (success) {
                outcome.complete("SUCCESS:已执行 /transfer " + hp.host + ":" + hp.port);
            } else {
                LOGGER.warn("跨服转移执行失败: player={}, endpoint={}:{}", normalizedPlayer, hp.host, hp.port);
                outcome.complete("FAIL:执行 /transfer 失败，请检查目标地址或指令兼容性");
            }
        });

        try {
            // Alpha 侧默认 10s 超时，这里留足余量避免上游误判超时
            String result = outcome.get(8, TimeUnit.SECONDS);
            client.sendProxyResponse(requestId, result);
        } catch (Exception e) {
            LOGGER.warn("跨服转移等待执行结果超时: player={}, endpoint={}", normalizedPlayer, transferEndpoint);
            client.sendProxyResponse(requestId, "FAIL:跨服执行超时，请稍后重试");
        }
    }

    private static boolean tryExecuteFromPlayer(MinecraftServer server, ServerPlayer player, String rawCommand) {
        try {
            var source = player.createCommandSourceStack().withSuppressedOutput().withPermission(4);
            var dispatcher = server.getCommands().getDispatcher();
            var parse = dispatcher.parse(rawCommand, source);
            if (parse.getReader().canRead() || !parse.getExceptions().isEmpty()) {
                return false;
            }
            int result = dispatcher.execute(parse);
            return result > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean tryExecuteFromConsole(MinecraftServer server, String rawCommand) {
        try {
            var source = server.createCommandSourceStack().withSuppressedOutput().withPermission(4);
            var dispatcher = server.getCommands().getDispatcher();
            var parse = dispatcher.parse(rawCommand, source);
            if (parse.getReader().canRead() || !parse.getExceptions().isEmpty()) {
                return false;
            }
            dispatcher.execute(parse);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String normalizeCommand(String rawCommand) {
        if (rawCommand == null) return "";
        String cmd = rawCommand.trim();
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1).trim();
        }
        return cmd;
    }

    private static boolean hasRootCommand(MinecraftServer server, String literal) {
        if (server == null || literal == null || literal.isBlank()) return false;
        try {
            return server.getCommands().getDispatcher().getRoot().getChild(literal) != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static HostPort parseHostPort(String endpoint) {
        if (endpoint == null) return null;
        String s = endpoint.trim();
        if (s.isEmpty()) return null;

        String host = s;
        Integer port = null;

        // 容错: 允许意外的 host:port:port，连续剥离尾部端口片段
        while (true) {
            int idx = host.lastIndexOf(':');
            if (idx <= 0 || idx >= host.length() - 1) break;

            String hostPart = host.substring(0, idx).trim();
            String portStr = host.substring(idx + 1).trim();
            try {
                int parsed = Integer.parseInt(portStr);
                if (hostPart.isEmpty() || parsed < 1 || parsed > 65535) {
                    break;
                }
                host = hostPart;
                port = parsed;
            } catch (NumberFormatException e) {
                break;
            }
        }

        if (port == null) return null;
        boolean bracketedIpv6 = host.startsWith("[") && host.endsWith("]") && host.length() > 2;
        if (bracketedIpv6) {
            host = host.substring(1, host.length() - 1);
        }
        if (host.isEmpty()) return null;
        if (host.toLowerCase(Locale.ROOT).contains(" ")) return null;
        if (!bracketedIpv6 && host.indexOf(':') >= 0) return null;
        String normalizedHost = bracketedIpv6 ? ("[" + host + "]") : host;
        return new HostPort(normalizedHost, port);
    }

    private static boolean addToWhitelist(MinecraftServer server, com.mojang.authlib.GameProfile profile) throws Exception {
        var whitelist = server.getPlayerList().getWhiteList();
        if (whitelist.isWhiteListed(profile)) {
            return false;
        }
        whitelist.add(new net.minecraft.server.players.UserWhiteListEntry(profile));
        whitelist.save();
        return true;
    }

    private record HostPort(String host, int port) {}

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

        String normalized = normalizeCommand(command);
        if (normalized.isEmpty()) {
            client.sendProxyResponse(requestId, "[错误] 指令为空");
            return;
        }

        CompletableFuture<String> outcome = new CompletableFuture<>();
        server.execute(() -> {
            boolean ok = tryExecuteFromConsole(server, normalized);
            if (ok) {
                outcome.complete("[成功] 指令已执行: " + normalized);
            } else {
                outcome.complete("[错误] 指令执行失败或语法错误: " + normalized);
            }
        });

        try {
            client.sendProxyResponse(requestId, outcome.get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
            client.sendProxyResponse(requestId, "[错误] 指令执行超时: " + normalized);
        }
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
        if (sender.isEmpty()) return;

        // Alpha 已经完成了所有名字解析 (sender=游戏ID, content中@QQ→@玩家名)
        // 直接使用 Alpha 发来的 content, 不再用 Reforged 的 CQCodeParser 重新解析
        String displayContent = content;
        if (displayContent.isEmpty()) return;

        // 提取 atPlayerNames (Alpha 已解析的玩家名列表, 用于直接匹配在线玩家)
        java.util.List<String> atPlayerNames = new java.util.ArrayList<>();
        if (json.has("atPlayerNames") && json.get("atPlayerNames").isJsonArray()) {
            for (com.google.gson.JsonElement e : json.getAsJsonArray("atPlayerNames")) {
                try {
                    String name = e.getAsString();
                    if (name != null && !name.isEmpty()) atPlayerNames.add(name);
                } catch (Exception ignored) {}
            }
        }

        // 提取 atList (QQ号列表, 回退用)
        java.util.List<Long> atQQList = new java.util.ArrayList<>();
        if (json.has("atList") && json.get("atList").isJsonArray()) {
            for (com.google.gson.JsonElement e : json.getAsJsonArray("atList")) {
                try { atQQList.add(e.getAsLong()); } catch (Exception ignored) {}
            }
        }

        String formattedMsg = String.format("\u00A7b[QQ]\u00A7r <%s> %s", sender, displayContent);
        LOGGER.info("[QQ->MC] {} (atNames={}, atQQ={})", formattedMsg, atPlayerNames, atQQList);

        // 调用个性化消息（被@玩家: 高亮+标题+提示音）
        com.mapbot.logic.InboundHandler.sendPersonalizedMessage(formattedMsg, atQQList, atPlayerNames, sender);
    }

    // ==================== 文件操作 ====================

    static void handleFileRequest(JsonObject json, BridgeClient client) {
        String type = getString(json, "type");
        String requestId = getString(json, "requestId");
        String path = getString(json, "path");

        if (!path.isEmpty() && !client.isPathSafe(path)) {
            sendFileResponse(requestId, buildFileErrorResponse(
                    requestId,
                    BridgeErrorMapper.BRG_AUTH_102,
                    "Access denied: path outside server directory",
                    false,
                    client
            ), client);
            LOGGER.warn("拒绝不安全的文件操作路径: {}", path);
            return;
        }

        if (("file_write".equals(type)
                || "file_delete".equals(type)
                || "file_mkdir".equals(type)
                || "file_upload".equals(type))
                && !client.isMutationPathAllowed(path)) {
            sendFileResponse(requestId, buildFileErrorResponse(
                    requestId,
                    BridgeErrorMapper.BRG_AUTH_102,
                    "Access denied: path not in mutation whitelist",
                    false,
                    client
            ), client);
            LOGGER.warn("拒绝变更操作，路径不在白名单: type={}, path={}", type, path);
            return;
        }

        try {
            String response;
            switch (type) {
                case "file_list":
                    response = handleFileList(requestId, path, client);
                    break;
                case "file_read":
                    response = handleFileRead(requestId, path, client);
                    break;
                case "file_write":
                    response = handleFileWrite(requestId, path, getString(json, "content"), client);
                    break;
                case "file_delete":
                    response = handleFileDelete(requestId, path, client);
                    break;
                case "file_mkdir":
                    response = handleFileMkdir(requestId, path, client);
                    break;
                case "file_upload":
                    response = handleFileUpload(requestId, path, getString(json, "content"), getString(json, "encoding"), client);
                    break;
                default:
                    response = buildFileErrorResponse(
                            requestId,
                            BridgeErrorMapper.BRG_VALIDATION_202,
                            "unknown_file_action",
                            false,
                            client
                    );
            }
            sendFileResponse(requestId, response, client);
        } catch (Exception e) {
            sendFileResponse(requestId, buildFileErrorResponse(
                    requestId,
                    null,
                    e.getMessage(),
                    false,
                    client
            ), client);
        }
    }

    private static void sendFileResponse(String requestId, String response, BridgeClient client) {
        if (client.send(response)) {
            return;
        }
        LOGGER.warn("file_response 发送被门禁拒绝，回退统一错误: requestId={}", requestId);
        client.send(buildFileErrorResponse(
                requestId,
                BridgeErrorMapper.BRG_VALIDATION_205,
                "frame_too_large",
                false,
                client
        ));
    }

    private static String buildFileErrorResponse(
            String requestId,
            String explicitErrorCode,
            String rawError,
            boolean retryable,
            BridgeClient client
    ) {
        BridgeErrorMapper.ErrorMeta meta = BridgeErrorMapper.map(explicitErrorCode, rawError, retryable);
        return String.format(
                "{\"type\":\"file_response\",\"requestId\":\"%s\",\"error\":\"%s\",\"errorCode\":\"%s\",\"rawError\":\"%s\",\"retryable\":%s,\"mappingConflict\":%s}",
                client.escapeJson(requestId),
                client.escapeJson(meta.rawError),
                meta.errorCode,
                client.escapeJson(meta.rawError),
                meta.retryable,
                meta.mappingConflict
        );
    }

    private static String handleFileList(String requestId, String path, BridgeClient client) throws IOException {
        File dir = client.resolveSafePath(path);
        if (!dir.isDirectory()) {
            return buildFileErrorResponse(
                    requestId,
                    BridgeErrorMapper.BRG_VALIDATION_202,
                    "Not a directory",
                    false,
                    client
            );
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
        return String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"files\":%s}",
                client.escapeJson(requestId), sb);
    }

    private static String handleFileRead(String requestId, String path, BridgeClient client) throws IOException {
        File file = client.resolveSafePath(path);
        if (!file.isFile()) {
            return buildFileErrorResponse(
                    requestId,
                    BridgeErrorMapper.BRG_VALIDATION_202,
                    "Not a file",
                    false,
                    client
            );
        }
        long maxBytes = client.getBridgeFileMaxBytes();
        if (file.length() > maxBytes) {
            return buildFileErrorResponse(
                    requestId,
                    BridgeErrorMapper.BRG_VALIDATION_205,
                    "size_limit_exceeded:file_read",
                    false,
                    client
            );
        }
        String content = java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8);
        return String.format(
                "{\"type\":\"file_response\",\"requestId\":\"%s\",\"content\":\"%s\"}",
                client.escapeJson(requestId), client.escapeJson(content)
        );
    }

    private static String handleFileWrite(String requestId, String path, String content, BridgeClient client) throws IOException {
        File file = client.resolveSafePath(path);
        String safeContent = content == null ? "" : content;
        byte[] contentBytes = safeContent.getBytes(StandardCharsets.UTF_8);
        long maxBytes = client.getBridgeFileMaxBytes();
        if (contentBytes.length > maxBytes) {
            return buildFileErrorResponse(
                    requestId,
                    BridgeErrorMapper.BRG_VALIDATION_205,
                    "size_limit_exceeded:file_write",
                    false,
                    client
            );
        }

        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return buildFileErrorResponse(
                    requestId,
                    BridgeErrorMapper.BRG_EXECUTION_402,
                    "Create parent directory failed",
                    false,
                    client
            );
        }
        java.nio.file.Files.write(file.toPath(), contentBytes);
        return String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"content\":\"ok\"}", client.escapeJson(requestId));
    }

    private static String handleFileDelete(String requestId, String path, BridgeClient client) throws IOException {
        File file = client.resolveSafePath(path);
        if (!file.isFile()) {
            return buildFileErrorResponse(
                    requestId,
                    BridgeErrorMapper.BRG_VALIDATION_202,
                    "Not a file",
                    false,
                    client
            );
        }
        boolean deleted = file.delete();
        if (deleted) {
            return String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"content\":\"ok\"}", client.escapeJson(requestId));
        } else {
            return buildFileErrorResponse(
                    requestId,
                    BridgeErrorMapper.BRG_EXECUTION_402,
                    "Delete failed",
                    false,
                    client
            );
        }
    }

    private static String handleFileMkdir(String requestId, String path, BridgeClient client) throws IOException {
        File dir = client.resolveSafePath(path);
        if (dir.exists() && !dir.isDirectory()) {
            return buildFileErrorResponse(
                    requestId,
                    BridgeErrorMapper.BRG_VALIDATION_202,
                    "Path exists but is not a directory",
                    false,
                    client
            );
        }
        if (!dir.exists() && !dir.mkdirs()) {
            return buildFileErrorResponse(
                    requestId,
                    BridgeErrorMapper.BRG_EXECUTION_402,
                    "Create directory failed",
                    false,
                    client
            );
        }
        return String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"content\":\"ok\"}", client.escapeJson(requestId));
    }

    private static String handleFileUpload(String requestId, String path, String content, String encoding, BridgeClient client) throws IOException {
        String normalizedEncoding = encoding == null ? "" : encoding.trim().toLowerCase();
        byte[] payload;
        if ("base64".equals(normalizedEncoding)) {
            try {
                payload = Base64.getDecoder().decode(content == null ? "" : content);
            } catch (IllegalArgumentException e) {
                return buildFileErrorResponse(
                        requestId,
                        BridgeErrorMapper.BRG_VALIDATION_202,
                        "Invalid base64 payload",
                        false,
                        client
                );
            }
            if (payload.length > client.getBridgeBase64RawMaxBytes()) {
                return buildFileErrorResponse(
                        requestId,
                        BridgeErrorMapper.BRG_VALIDATION_205,
                        "base64_raw_size_exceeded",
                        false,
                        client
                );
            }
        } else if (normalizedEncoding.isEmpty() || "utf-8".equals(normalizedEncoding) || "utf8".equals(normalizedEncoding)) {
            payload = (content == null ? "" : content).getBytes(StandardCharsets.UTF_8);
        } else {
            return buildFileErrorResponse(
                    requestId,
                    BridgeErrorMapper.BRG_VALIDATION_202,
                    "Unsupported encoding: " + normalizedEncoding,
                    false,
                    client
            );
        }

        long maxBytes = client.getBridgeFileMaxBytes();
        if (payload.length > maxBytes) {
            return buildFileErrorResponse(
                    requestId,
                    BridgeErrorMapper.BRG_VALIDATION_205,
                    "size_limit_exceeded:file_upload",
                    false,
                    client
            );
        }

        File file = client.resolveSafePath(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return buildFileErrorResponse(
                    requestId,
                    BridgeErrorMapper.BRG_EXECUTION_402,
                    "Create parent directory failed",
                    false,
                    client
            );
        }
        java.nio.file.Files.write(file.toPath(), payload);
        return String.format("{\"type\":\"file_response\",\"requestId\":\"%s\",\"content\":\"ok\"}", client.escapeJson(requestId));
    }
}
