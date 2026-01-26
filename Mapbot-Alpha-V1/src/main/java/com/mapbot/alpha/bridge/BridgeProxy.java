package com.mapbot.alpha.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.*;

import com.mapbot.alpha.data.DataManager;

/**
 * Bridge 代理
 * 用于 Alpha Core 向 MC 服务器发送请求并获取响应
 */
public class BridgeProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Bridge/Proxy");
    public static final BridgeProxy INSTANCE = new BridgeProxy();
    
    // 请求超时 (秒)
    private static final int TIMEOUT = 10;
    
    // 待处理请求: requestId -> CompletableFuture
    private final ConcurrentHashMap<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();
    
    private BridgeProxy() {}

    /**
     * 控制台/面板专用：列出在线服务器 ID
     */
    public static String listServerIds() {
        var servers = ServerRegistry.INSTANCE.getAllServers();
        if (servers.isEmpty()) {
            return "(无)";
        }
        return servers.stream().map(s -> s.serverId).sorted().reduce((a, b) -> a + ", " + b).orElse("(无)");
    }

    /**
     * 控制台/面板专用：解析服务器 ID（支持完整匹配与唯一前缀匹配）
     */
    public static String resolveServerId(String query) {
        if (query == null) return null;
        String q = query.trim();
        if (q.isEmpty()) return null;

        var servers = ServerRegistry.INSTANCE.getAllServers();
        if (servers.isEmpty()) return null;

        // 1) 完整匹配
        var exact = ServerRegistry.INSTANCE.getServer(q);
        if (exact != null) return exact.serverId;

        // 2) 唯一前缀匹配
        String matched = null;
        for (var s : servers) {
            if (s.serverId != null && s.serverId.startsWith(q)) {
                if (matched != null) {
                    return null; // 多个匹配，拒绝自动选择
                }
                matched = s.serverId;
            }
        }
        return matched;
    }
    
    /**
     * 完成请求 (由 BridgeMessageHandler 调用)
     */
    public void completeRequest(String requestId, String result) {
        CompletableFuture<String> future = pendingRequests.remove(requestId);
        if (future != null) {
            future.complete(result);
        }
    }
    
    /**
     * 获取在线玩家列表
     */
    public String getOnlinePlayerList() {
        String result = sendRequest("get_players", null, null);
        if (result == null || result.isEmpty()) {
            return "[服务器] 无法获取在线列表";
        }
        return result;
    }
    
    /**
     * 解析玩家并绑定
     */
    public String resolveAndBind(String playerName, long senderQQ) {
        String result = sendRequest("bind_player", playerName, String.valueOf(senderQQ));
        if (result == null) {
            return "[错误] 服务器无响应";
        }
        
        if (result.startsWith("SUCCESS:")) {
            String[] parts = result.split(":", 3);
            if (parts.length >= 3) {
                String uuid = parts[1];
                String name = parts[2];

                // P0: 绑定数据统一存储在 Alpha（避免 Reforged 端写本地数据）
                if (DataManager.INSTANCE.isUUIDBound(uuid)) {
                    Long occupier = DataManager.INSTANCE.getQQByUUID(uuid);
                    if (occupier != null && occupier != senderQQ) {
                        return "FAIL:OCCUPIED:" + occupier;
                    }
                }

                boolean ok = DataManager.INSTANCE.bind(senderQQ, uuid);
                if (!ok) {
                    Long occupier = DataManager.INSTANCE.getQQByUUID(uuid);
                    if (occupier != null && occupier != senderQQ) {
                        return "FAIL:OCCUPIED:" + occupier;
                    }
                    return "[绑定失败] 绑定写入失败（可能已绑定或冲突）";
                }

                return "[绑定成功] " + name;
            }
        } else if (result.startsWith("FAIL:")) {
            return result.substring(5);
        }
        
        return result;
    }
    
    /**
     * 获取服务器状态
     */
    public String getServerStatus() {
        String result = sendRequest("get_status", null, null);
        if (result == null || result.isEmpty()) {
            return "[服务器] 无法获取状态";
        }
        return result;
    }
    
    /**
     * 获取玩家背包
     */
    public String getPlayerInventory(String playerName) {
        String result = sendRequest("get_inventory", playerName, null);
        if (result == null) {
            return "[错误] 无法获取背包";
        }
        return result;
    }
    
    /**
     * 获取玩家位置
     */
    public String getPlayerLocation(String playerName) {
        String result = sendRequest("get_location", playerName, null);
        if (result == null) {
            return "[错误] 无法获取位置";
        }
        return result;
    }

    /**
     * P0: 解析玩家名为 UUID（用于在线时长等需要 UUID 的查询）
     */
    public String resolveUuidByName(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) return null;
        var servers = ServerRegistry.INSTANCE.getAllServers();
        if (servers.isEmpty()) return null;
        var server = servers.iterator().next();

        String uuid = sendRequestToServer(server.serverId, "resolve_uuid", playerName.trim(), null);
        return (uuid == null || uuid.isEmpty()) ? null : uuid;
    }
    
    /**
     * 执行游戏指令
     */
    public String executeCommand(String command) {
        String result = sendRequest("execute_command", command, null);
        if (result == null) {
            return "[错误] 执行失败";
        }
        return result;
    }
    
    /**
     * 广播消息
     */
    public void broadcast(String message) {
        sendRequest("broadcast", message, null);
    }
    
    /**
     * 签到抽奖
     */
    public String signIn(long senderQQ) {
        String result = sendRequest("sign_in", String.valueOf(senderQQ), null);
        if (result == null) {
            return "[错误] 签到失败";
        }
        return result;
    }
    
    /**
     * 领取签到奖励
     */
    public String acceptReward(long senderQQ) {
        String result = sendRequest("accept_reward", String.valueOf(senderQQ), null);
        if (result == null) {
            return "[错误] 领取失败";
        }
        return result;
    }
    
    /**
     * 获取在线时长
     */
    public String getPlaytime(String playerName, int mode) {
        String result = sendRequest("get_playtime", playerName, String.valueOf(mode));
        if (result == null) {
            return "[错误] 无法获取在线时长";
        }
        return result;
    }
    
    /**
     * 获取 CDK 兑换码 (旧版，转发到 Mod)
     */
    public String getCdk(long senderQQ) {
        String result = sendRequest("get_cdk", String.valueOf(senderQQ), null);
        if (result == null) {
            return "[错误] 获取兑换码失败";
        }
        return result;
    }
    
    // ==================== Task #022: Redis 迁移新接口 ====================
    
    /**
     * 请求 Mod 端抽奖
     * @return Item JSON (如 {"id":"minecraft:diamond","count":5,"name":"钻石","rarity":"SR"})
     */
    public String rollLoot() {
        return sendRequest("roll_loot", null, null);
    }
    
    /**
     * 请求 Mod 端发放物品
     * @param uuid 玩家 UUID
     * @param itemJson 物品 JSON
     * @return "SUCCESS" 或 "FAIL:原因"
     */
    public String giveItem(String uuid, String itemJson) {
        return sendRequest("give_item", uuid, itemJson);
    }

    /**
     * P3: 多服联合发放逻辑
     * - 玩家在线：发放到该玩家当前所有在线的服务器
     * - 玩家全服离线：返回 FAIL:OFFLINE，由上层触发 CDK 机制
     *
     * @return SUCCESS[:发放服务器数/目标服务器数] 或 FAIL:OFFLINE / FAIL:INVENTORY_FULL / FAIL:原因
     */
    public String giveItemToOnlineServers(String uuid, String itemJson) {
        java.util.Set<String> targets = findOnlineServersForPlayer(uuid);
        if (targets.isEmpty()) {
            return "FAIL:OFFLINE";
        }

        int successCount = 0;
        boolean anyInventoryFull = false;
        String firstError = null;

        for (String serverId : targets) {
            String result = sendRequestToServer(serverId, "give_item", uuid, itemJson);
            if (result == null) {
                firstError = firstError == null ? ("服务器无响应: " + serverId) : firstError;
                continue;
            }
            if (result.startsWith("SUCCESS")) {
                successCount++;
                continue;
            }
            if (result.startsWith("FAIL:INVENTORY_FULL")) {
                anyInventoryFull = true;
                firstError = firstError == null ? result : firstError;
                continue;
            }
            if (result.startsWith("FAIL:")) {
                firstError = firstError == null ? result : firstError;
                continue;
            }
            // 未知返回
            firstError = firstError == null ? ("FAIL:" + result) : firstError;
        }

        if (successCount > 0) {
            return "SUCCESS:" + successCount + "/" + targets.size();
        }
        if (anyInventoryFull) {
            return "FAIL:INVENTORY_FULL";
        }
        return firstError != null ? firstError : "FAIL:发放失败";
    }

    /**
     * P3: 判断玩家是否在任意已连接服务器在线
     */
    public boolean isPlayerOnline(String uuid) {
        return !findOnlineServersForPlayer(uuid).isEmpty();
    }

    /**
     * P3: 获取玩家在线服务器集合
     */
    public java.util.Set<String> findOnlineServersForPlayer(String uuid) {
        java.util.Set<String> onlineServers = new java.util.HashSet<>();
        if (uuid == null || uuid.isEmpty()) return onlineServers;

        for (var s : ServerRegistry.INSTANCE.getAllServers()) {
            String result = sendRequestToServer(s.serverId, "has_player", uuid, null);
            if (result == null) continue;
            if ("YES".equalsIgnoreCase(result) || "TRUE".equalsIgnoreCase(result) || "ONLINE".equalsIgnoreCase(result)) {
                onlineServers.add(s.serverId);
            }
        }

        return onlineServers;
    }
    
    /**
     * 请求 Alpha 验证并兑换 CDK (由 Mod 端调用)
     * @param code 兑换码
     * @param uuid 玩家 UUID
     * @return "VALID:{itemJson}" 或 "INVALID:原因"
     */
    public String redeemCdk(String code, String uuid) {
        var signManager = com.mapbot.alpha.logic.SignManager.INSTANCE;
        var dataManager = DataManager.INSTANCE;
        
        String cdkJson = signManager.getCdkInfo(code);
        if (cdkJson == null) {
            return "INVALID:无效的兑换码";
        }
        
        // 解析 CDK JSON
        try {
            var json = com.google.gson.JsonParser.parseString(cdkJson).getAsJsonObject();
            long cdkQQ = json.get("qq").getAsLong();
            long expiry = json.get("expiry").getAsLong();
            String itemJson = json.get("item").toString();
            
            // 检查过期
            if (System.currentTimeMillis() > expiry) {
                signManager.removeCdk(code);
                return "INVALID:兑换码已过期";
            }
            
            // 检查归属
            Long boundQQ = dataManager.getQQByUUID(uuid);
            if (boundQQ == null || boundQQ != cdkQQ) {
                return "INVALID:此兑换码不属于您绑定的账号";
            }
            
            // 有效，删除 CDK 并返回物品
            signManager.removeCdk(code);
            return "VALID:" + itemJson;
            
        } catch (Exception e) {
            LOGGER.error("CDK 解析失败", e);
            return "INVALID:CDK 数据损坏";
        }
    }
    
    /**
     * 关闭服务器 (异步)
     */
    public static CompletableFuture<String> stopServer(int countdown) {
        return sendRequestAsync("stop_server", String.valueOf(countdown), null);
    }
    
    /**
     * 取消关服 (异步)
     */
    public static CompletableFuture<String> cancelStop() {
        return sendRequestAsync("cancel_stop", null, null);
    }
    
    /**
     * 异步发送请求
     */
    private static CompletableFuture<String> sendRequestAsync(String action, String arg1, String arg2) {
        var servers = ServerRegistry.INSTANCE.getAllServers();
        if (servers.isEmpty()) {
            LOGGER.warn("无可用服务器");
            return CompletableFuture.completedFuture("[错误] 无可用服务器");
        }
        
        ServerRegistry.ServerInfo server = servers.iterator().next();
        return sendRequestAsyncToServer(server.serverId, action, arg1, arg2);
    }

    /**
     * 异步发送请求到指定服务器
     */
    public static CompletableFuture<String> sendRequestAsyncToServer(String serverId, String action, String arg1, String arg2) {
        var server = ServerRegistry.INSTANCE.getServer(serverId);
        if (server == null || server.channel == null || !server.channel.isActive()) {
            return CompletableFuture.completedFuture("[错误] 服务器离线: " + serverId);
        }

        String requestId = action + "_" + System.currentTimeMillis();
        
        CompletableFuture<String> future = new CompletableFuture<>();
        INSTANCE.pendingRequests.put(requestId, future);
        
        Map<String, Object> req = new HashMap<>();
        req.put("type", action);
        req.put("requestId", requestId);
        if (arg1 != null) req.put("arg1", arg1);
        if (arg2 != null) req.put("arg2", arg2);
        
        String json = com.mapbot.alpha.utils.JsonUtils.toJson(req);
        server.channel.writeAndFlush(json + "\n");
        
        return future.orTimeout(TIMEOUT, TimeUnit.SECONDS)
            .exceptionally(e -> {
                INSTANCE.pendingRequests.remove(requestId);
                return "[错误] 请求超时";
            });
    }
    
    /**
     * 发送请求到第一个可用的服务器
     */
    private String sendRequest(String action, String arg1, String arg2) {
        var servers = ServerRegistry.INSTANCE.getAllServers();
        if (servers.isEmpty()) {
            LOGGER.warn("无可用服务器");
            return null;
        }
        
        ServerRegistry.ServerInfo server = servers.iterator().next();
        return sendRequestToServer(server.serverId, action, arg1, arg2);
    }

    /**
     * 同步发送请求到指定服务器
     */
    public String sendRequestToServer(String serverId, String action, String arg1, String arg2) {
        var server = ServerRegistry.INSTANCE.getServer(serverId);
        if (server == null || server.channel == null || !server.channel.isActive()) {
            LOGGER.warn("服务器离线: {}", serverId);
            return null;
        }

        String requestId = action + "_" + System.currentTimeMillis();
        
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);
        
        Map<String, Object> req = new HashMap<>();
        req.put("type", action);
        req.put("requestId", requestId);
        if (arg1 != null) req.put("arg1", arg1);
        if (arg2 != null) req.put("arg2", arg2);
        
        server.channel.writeAndFlush(com.mapbot.alpha.utils.JsonUtils.toJson(req) + "\n");
        
        try {
            return future.get(TIMEOUT, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            pendingRequests.remove(requestId);
            LOGGER.warn("请求超时: {}", action);
            return null;
        } catch (Exception e) {
            pendingRequests.remove(requestId);
            LOGGER.error("请求失败: {}", action, e);
            return null;
        }
    }
}
