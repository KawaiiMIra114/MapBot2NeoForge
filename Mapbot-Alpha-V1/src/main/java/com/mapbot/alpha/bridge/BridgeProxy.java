package com.mapbot.alpha.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

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
    // 多服 fan-out 总超时 (秒)
    private static final int FANOUT_TIMEOUT = 10;
    private static final AtomicLong REQUEST_SEQ = new AtomicLong(0);
    
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
        String qLower = q.toLowerCase(java.util.Locale.ROOT);

        var servers = ServerRegistry.INSTANCE.getAllServers();
        if (servers.isEmpty()) return null;

        // 1) 完整匹配
        var exact = ServerRegistry.INSTANCE.getServer(q);
        if (exact != null) return exact.serverId;
        for (var s : servers) {
            if (s.serverId != null && s.serverId.equalsIgnoreCase(q)) {
                return s.serverId;
            }
        }

        // 2) 唯一前缀匹配
        String matched = null;
        for (var s : servers) {
            if (s.serverId != null && s.serverId.toLowerCase(java.util.Locale.ROOT).startsWith(qLower)) {
                if (matched != null) {
                    return null; // 多个匹配，拒绝自动选择
                }
                matched = s.serverId;
            }
        }
        return matched;
    }

    private static String nextRequestId(String action, String serverId) {
        long seq = REQUEST_SEQ.incrementAndGet();
        return action + "_" + System.currentTimeMillis() + "_" + seq + "_" + (serverId == null ? "unknown" : serverId);
    }

    private static Set<String> getOnlineServerIds() {
        Set<String> ids = new HashSet<>();
        for (var s : ServerRegistry.INSTANCE.getAllServers()) {
            if (s == null || s.serverId == null || s.serverId.isEmpty()) continue;
            if (s.channel != null && s.channel.isActive()) {
                ids.add(s.serverId);
            }
        }
        return ids;
    }

    private static FanoutBatchResult fanOutRequests(Collection<String> serverIds, String action, String arg1, String arg2, int totalTimeoutSeconds) {
        if (serverIds == null || serverIds.isEmpty()) {
            return new FanoutBatchResult(Collections.emptyMap(), 0);
        }

        Map<String, CompletableFuture<String>> futureMap = new LinkedHashMap<>();
        for (String serverId : serverIds) {
            futureMap.put(serverId, sendRequestAsyncToServer(serverId, action, arg1, arg2));
        }

        CompletableFuture<?>[] futures = futureMap.values().toArray(new CompletableFuture[0]);
        CompletableFuture.allOf(futures)
            .completeOnTimeout(null, totalTimeoutSeconds, TimeUnit.SECONDS)
            .join();

        Map<String, String> responses = new LinkedHashMap<>();
        int timeoutCount = 0;
        for (Map.Entry<String, CompletableFuture<String>> entry : futureMap.entrySet()) {
            CompletableFuture<String> future = entry.getValue();
            if (!future.isDone()) {
                timeoutCount++;
                continue;
            }
            try {
                responses.put(entry.getKey(), future.getNow(null));
            } catch (CompletionException e) {
                LOGGER.debug("fan-out 子请求异常: action={}, server={}", action, entry.getKey(), e);
            }
        }

        if (timeoutCount > 0) {
            LOGGER.warn("fan-out 总超时: action={}, 完成 {}/{}, 超时 {}",
                action, responses.size(), futureMap.size(), timeoutCount);
        } else {
            LOGGER.info("fan-out 完成: action={}, 完成 {}/{}",
                action, responses.size(), futureMap.size());
        }
        return new FanoutBatchResult(responses, timeoutCount);
    }

    private static final class FanoutBatchResult {
        private final Map<String, String> responses;
        private final int timeoutCount;

        private FanoutBatchResult(Map<String, String> responses, int timeoutCount) {
            this.responses = responses;
            this.timeoutCount = timeoutCount;
        }
    }

    private record ResolvedIdentity(String uuid, String name) {}
    
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
        List<String> serverIds = getSortedOnlineServerIds();
        if (serverIds.isEmpty()) {
            return "[在线] 当前无在线服务器";
        }

        FanoutBatchResult batch = fanOutRequests(serverIds, "get_players", null, null, FANOUT_TIMEOUT);
        StringBuilder sb = new StringBuilder("[在线列表]");
        int responded = 0;

        for (String serverId : serverIds) {
            sb.append("\n\n[").append(serverId).append("]");
            String result = batch.responses.get(serverId);
            if (result == null || result.isBlank()) {
                sb.append("\n[错误] 请求超时或无响应");
                continue;
            }
            responded++;
            sb.append("\n").append(result.trim());
        }

        if (responded == 0) {
            sb.append("\n\n[错误] 所有服务器均未响应");
        }
        return sb.toString();
    }
    
    /**
     * 解析玩家并绑定
     */
    public String resolveAndBind(String playerName, long senderQQ) {
        String normalizedName = playerName == null ? "" : playerName.trim();
        if (normalizedName.isEmpty()) {
            return "[错误] 玩家名为空";
        }
        List<String> onlineServers = getSortedOnlineServerIds();
        if (onlineServers.isEmpty()) {
            return "[错误] 当前无在线服务器";
        }

        ResolvedIdentity identity = resolveIdentityByName(normalizedName, onlineServers);
        if (identity == null) {
            return "[绑定失败] 玩家不存在或服务器无法解析该玩家";
        }

        String uuid = identity.uuid();
        String name = identity.name();

        // P0: 绑定数据统一存储在 Alpha（避免 Reforged 端写本地数据）
        if (DataManager.INSTANCE.isUUIDBound(uuid)) {
            Long occupier = DataManager.INSTANCE.getQQByUUID(uuid);
            if (occupier != null && occupier != senderQQ) {
                return "FAIL:OCCUPIED:" + occupier;
            }
        }

        boolean ok = DataManager.INSTANCE.bind(senderQQ, uuid, name);
        if (!ok) {
            Long occupier = DataManager.INSTANCE.getQQByUUID(uuid);
            if (occupier != null && occupier != senderQQ) {
                return "FAIL:OCCUPIED:" + occupier;
            }
            return "[绑定失败] 绑定写入失败（可能已绑定或冲突）";
        }

        String syncResult = syncWhitelistAddToServers(onlineServers, name);
        LOGGER.info("绑定完成: qq={}, uuid={}, name={}", senderQQ, uuid, name);
        return "[绑定成功] " + name + " (" + uuid + ")\n" + syncResult;
    }

    /**
     * 新服务器注册后，将当前绑定快照同步到该服白名单（尽力而为，不阻断注册流程）
     */
    public void syncWhitelistSnapshotToServer(String serverId) {
        if (serverId == null || serverId.isBlank()) return;
        var target = ServerRegistry.INSTANCE.getServer(serverId);
        if (target == null || target.channel == null || !target.channel.isActive()) return;

        Map<Long, String> allBindings = DataManager.INSTANCE.getAllBindings();
        if (allBindings.isEmpty()) return;

        int success = 0;
        int skipped = 0;
        int failed = 0;
        for (String uuid : allBindings.values()) {
            if (uuid == null || uuid.isBlank()) continue;
            String name = DataManager.INSTANCE.getPlayerName(uuid);
            if (name == null || name.isBlank()) {
                name = resolveNameByUuid(uuid);
            }
            if (name == null || name.isBlank()) {
                skipped++;
                continue;
            }
            DataManager.INSTANCE.updatePlayerName(uuid, name);
            String result = sendRequestToServer(serverId, "whitelist_add", name.trim(), null);
            if (isWhitelistSyncSuccess(result)) {
                success++;
            } else {
                failed++;
            }
        }
        LOGGER.info("白名单快照同步完成: serverId={}, success={}, skipped={}, failed={}",
            serverId, success, skipped, failed);
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
     * 重载所有在线子服的 MapBot 配置/数据
     */
    public String reloadSubServerConfigs() {
        List<String> serverIds = getSortedOnlineServerIds();
        if (serverIds.isEmpty()) {
            return "[子服重载] 无在线服务器";
        }

        FanoutBatchResult batch = fanOutRequests(serverIds, "reload_config", null, null, FANOUT_TIMEOUT);
        int success = 0;
        java.util.List<String> failedServers = new java.util.ArrayList<>();

        for (String serverId : serverIds) {
            String result = batch.responses.get(serverId);
            if (isSuccess(result)) {
                success++;
                continue;
            }
            if (result == null || result.isBlank()) {
                failedServers.add(serverId + "(超时)");
            } else {
                failedServers.add(serverId + "(" + result.trim() + ")");
            }
        }

        StringBuilder summary = new StringBuilder();
        summary.append("[子服重载] 成功 ").append(success).append("/").append(serverIds.size());
        if (!failedServers.isEmpty()) {
            summary.append("\n[子服重载] 失败: ").append(String.join(", ", failedServers));
        }
        return summary.toString();
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
        java.util.List<String> targets = new java.util.ArrayList<>(getOnlineServerIds());
        if (targets.isEmpty()) return null;
        java.util.Collections.sort(targets);

        String normalizedName = playerName.trim();
        for (String serverId : targets) {
            String uuid = sendRequestToServer(serverId, "resolve_uuid", normalizedName, null);
            if (uuid != null && !uuid.isBlank()) {
                return uuid.trim();
            }
        }
        return null;
    }

    /**
     * 通过 UUID 反查玩家名（用于绑定 UUID 自愈刷新）
     */
    public String resolveNameByUuid(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) return null;
        java.util.List<String> targets = getSortedOnlineServerIds();
        if (targets.isEmpty()) return null;

        String normalizedUuid = uuid.trim();
        for (String serverId : targets) {
            String name = sendRequestToServer(serverId, "resolve_name", normalizedUuid, null);
            if (name != null && !name.isBlank()) {
                return name.trim();
            }
        }
        return null;
    }

    private static List<String> getSortedOnlineServerIds() {
        List<String> targets = new java.util.ArrayList<>(getOnlineServerIds());
        Collections.sort(targets);
        return targets;
    }

    private ResolvedIdentity resolveIdentityByName(String playerName, List<String> onlineServers) {
        for (String serverId : onlineServers) {
            String uuid = sendRequestToServer(serverId, "resolve_uuid", playerName, null);
            if (uuid == null || uuid.isBlank()) continue;
            String normalizedUuid = uuid.trim();

            String resolvedName = sendRequestToServer(serverId, "resolve_name", normalizedUuid, null);
            if (resolvedName == null || resolvedName.isBlank()) {
                resolvedName = playerName;
            } else {
                resolvedName = resolvedName.trim();
            }
            return new ResolvedIdentity(normalizedUuid, resolvedName);
        }
        return null;
    }

    private String syncWhitelistAddToServers(List<String> serverIds, String playerName) {
        if (serverIds == null || serverIds.isEmpty()) {
            return "[白名单同步] 无在线服务器";
        }
        FanoutBatchResult batch = fanOutRequests(serverIds, "whitelist_add", playerName, null, FANOUT_TIMEOUT);

        int success = 0;
        java.util.List<String> failedServers = new java.util.ArrayList<>();
        for (String serverId : serverIds) {
            String result = batch.responses.get(serverId);
            if (isWhitelistSyncSuccess(result)) {
                success++;
                continue;
            }
            if (result == null || result.isBlank()) {
                failedServers.add(serverId + "(超时)");
            } else {
                failedServers.add(serverId + "(" + result + ")");
            }
        }

        StringBuilder summary = new StringBuilder();
        summary.append("[白名单同步] 成功 ").append(success).append("/").append(serverIds.size());
        if (failedServers.isEmpty()) {
            LOGGER.info("白名单同步成功: player={}, success={}/{}", playerName, success, serverIds.size());
            return summary.toString();
        }
        summary.append("\n[白名单同步] 失败: ").append(String.join(", ", failedServers));
        LOGGER.warn("白名单同步部分失败: player={}, success={}/{}, failed={}",
            playerName, success, serverIds.size(), failedServers);
        return summary.toString();
    }

    private boolean isWhitelistSyncSuccess(String result) {
        if (result == null) return false;
        String trimmed = result.trim();
        return trimmed.equalsIgnoreCase("SUCCESS")
            || trimmed.equalsIgnoreCase("SUCCESS:ALREADY")
            || trimmed.startsWith("SUCCESS:");
    }

    private boolean isSuccess(String result) {
        if (result == null) return false;
        String trimmed = result.trim();
        return trimmed.equalsIgnoreCase("SUCCESS") || trimmed.startsWith("SUCCESS:");
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
        Set<String> targets = findOnlineServersForPlayer(uuid);
        if (targets.isEmpty()) {
            return "FAIL:OFFLINE";
        }

        FanoutBatchResult batch = fanOutRequests(targets, "give_item", uuid, itemJson, FANOUT_TIMEOUT);
        int successCount = 0;
        boolean anyInventoryFull = false;
        String firstError = null;

        for (String serverId : targets) {
            String result = batch.responses.get(serverId);
            if (result == null || result.isEmpty()) {
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
            if (result.startsWith("[错误]")) {
                firstError = firstError == null ? ("FAIL:" + result) : firstError;
                continue;
            }
            // 未知返回
            firstError = firstError == null ? ("FAIL:" + result) : firstError;
        }

        LOGGER.info("多服发奖结果: targets={}, success={}, timeout={}, firstError={}",
            targets.size(), successCount, batch.timeoutCount, firstError);

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
    public Set<String> findOnlineServersForPlayer(String uuid) {
        Set<String> onlineServers = new HashSet<>();
        if (uuid == null || uuid.isEmpty()) return onlineServers;

        Set<String> targets = getOnlineServerIds();
        if (targets.isEmpty()) return onlineServers;

        FanoutBatchResult batch = fanOutRequests(targets, "has_player", uuid, null, FANOUT_TIMEOUT);
        for (String serverId : targets) {
            String result = batch.responses.get(serverId);
            if (result == null) continue;
            if ("YES".equalsIgnoreCase(result) || "TRUE".equalsIgnoreCase(result) || "ONLINE".equalsIgnoreCase(result)) {
                onlineServers.add(serverId);
            }
        }

        if (batch.timeoutCount > 0) {
            LOGGER.warn("玩家在线查询部分超时: uuid={}, online={}, timeout={}", uuid, onlineServers.size(), batch.timeoutCount);
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
        return stopServer(countdown, null);
    }

    public static CompletableFuture<String> stopServer(int countdown, String serverId) {
        return sendRequestAsync("stop_server", String.valueOf(countdown), null, serverId);
    }
    
    /**
     * 取消关服 (异步)
     */
    public static CompletableFuture<String> cancelStop() {
        return cancelStop(null);
    }

    public static CompletableFuture<String> cancelStop(String serverId) {
        return sendRequestAsync("cancel_stop", null, null, serverId);
    }
    
    /**
     * 异步发送请求
     */
    private static CompletableFuture<String> sendRequestAsync(String action, String arg1, String arg2, String explicitServerId) {
        String targetServerId = null;
        String serverQuery = explicitServerId == null ? "" : explicitServerId.trim();

        if (!serverQuery.isEmpty()) {
            targetServerId = resolveServerId(serverQuery);
            if (targetServerId == null) {
                return CompletableFuture.completedFuture("[错误] 未找到或匹配不唯一的服务器: " + serverQuery + "；在线服务器: " + listServerIds());
            }
        } else {
            Set<String> onlineServers = getOnlineServerIds();
            if (onlineServers.isEmpty()) {
                LOGGER.warn("无可用服务器");
                return CompletableFuture.completedFuture("[错误] 无可用服务器");
            }
            if (onlineServers.size() > 1) {
                return CompletableFuture.completedFuture("[错误] 检测到多服在线，请显式指定 serverId。在线服务器: " + listServerIds());
            }
            targetServerId = onlineServers.iterator().next();
        }

        LOGGER.info("异步请求下发: action={}, target={}", action, targetServerId);
        return sendRequestAsyncToServer(targetServerId, action, arg1, arg2);
    }

    /**
     * 异步发送请求到指定服务器
     */
    public static CompletableFuture<String> sendRequestAsyncToServer(String serverId, String action, String arg1, String arg2) {
        var server = ServerRegistry.INSTANCE.getServer(serverId);
        if (server == null || server.channel == null || !server.channel.isActive()) {
            return CompletableFuture.completedFuture("[错误] 服务器离线: " + serverId);
        }

        String requestId = nextRequestId(action, serverId);
        
        CompletableFuture<String> future = new CompletableFuture<>();
        INSTANCE.pendingRequests.put(requestId, future);
        
        Map<String, Object> req = new HashMap<>();
        req.put("type", action);
        req.put("requestId", requestId);
        if (arg1 != null) req.put("arg1", arg1);
        if (arg2 != null) req.put("arg2", arg2);
        
        String json = com.mapbot.alpha.utils.JsonUtils.toJson(req);
        if (BridgeErrorMapper.isFrameTooLarge(json)) {
            INSTANCE.pendingRequests.remove(requestId);
            LOGGER.warn("异步请求被发送前门禁拒绝: action={}, serverId={}, requestId={}, errorCode={}",
                action, serverId, requestId, BridgeErrorMapper.BRG_VALIDATION_205);
            return CompletableFuture.completedFuture(
                BridgeErrorMapper.toCompatErrorResult(
                    BridgeErrorMapper.BRG_VALIDATION_205,
                    "frame_too_large",
                    false
                )
            );
        }
        server.channel.writeAndFlush(json + "\n");
        
        return future.orTimeout(TIMEOUT, TimeUnit.SECONDS)
            .exceptionally(e -> {
                INSTANCE.pendingRequests.remove(requestId);
                LOGGER.warn("异步请求超时: action={}, serverId={}, requestId={}", action, serverId, requestId);
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

        String requestId = nextRequestId(action, serverId);
        
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);
        
        Map<String, Object> req = new HashMap<>();
        req.put("type", action);
        req.put("requestId", requestId);
        if (arg1 != null) req.put("arg1", arg1);
        if (arg2 != null) req.put("arg2", arg2);

        String json = com.mapbot.alpha.utils.JsonUtils.toJson(req);
        if (BridgeErrorMapper.isFrameTooLarge(json)) {
            pendingRequests.remove(requestId);
            LOGGER.warn("同步请求被发送前门禁拒绝: action={}, serverId={}, requestId={}, errorCode={}",
                action, serverId, requestId, BridgeErrorMapper.BRG_VALIDATION_205);
            return BridgeErrorMapper.toCompatErrorResult(
                BridgeErrorMapper.BRG_VALIDATION_205,
                "frame_too_large",
                false
            );
        }

        server.channel.writeAndFlush(json + "\n");
        
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
