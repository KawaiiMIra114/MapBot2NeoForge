package com.mapbot.alpha.bridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

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
     * 给予物品
     */
    public String giveItem(String playerName, String itemId, int count) {
        String result = sendRequest("give_item", playerName, itemId + ":" + count);
        if (result == null) {
            return "[错误] 给予物品失败";
        }
        return result;
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
     * 获取 CDK 兑换码
     */
    public String getCdk(long senderQQ) {
        String result = sendRequest("get_cdk", String.valueOf(senderQQ), null);
        if (result == null) {
            return "[错误] 获取兑换码失败";
        }
        return result;
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
        String requestId = action + "_" + System.currentTimeMillis();
        
        CompletableFuture<String> future = new CompletableFuture<>();
        INSTANCE.pendingRequests.put(requestId, future);
        
        StringBuilder json = new StringBuilder();
        json.append("{\"type\":\"").append(action).append("\"");
        json.append(",\"requestId\":\"").append(requestId).append("\"");
        if (arg1 != null) {
            json.append(",\"arg1\":\"").append(INSTANCE.escapeJson(arg1)).append("\"");
        }
        if (arg2 != null) {
            json.append(",\"arg2\":\"").append(INSTANCE.escapeJson(arg2)).append("\"");
        }
        json.append("}");
        
        server.channel.writeAndFlush(json.toString() + "\n");
        
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
        // 获取第一个在线服务器
        var servers = ServerRegistry.INSTANCE.getAllServers();
        if (servers.isEmpty()) {
            LOGGER.warn("无可用服务器");
            return null;
        }
        
        ServerRegistry.ServerInfo server = servers.iterator().next();
        String requestId = action + "_" + System.currentTimeMillis();
        
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);
        
        // 构建请求
        StringBuilder json = new StringBuilder();
        json.append("{\"type\":\"").append(action).append("\"");
        json.append(",\"requestId\":\"").append(requestId).append("\"");
        if (arg1 != null) {
            json.append(",\"arg1\":\"").append(escapeJson(arg1)).append("\"");
        }
        if (arg2 != null) {
            json.append(",\"arg2\":\"").append(escapeJson(arg2)).append("\"");
        }
        json.append("}");
        
        server.channel.writeAndFlush(json.toString() + "\n");
        
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
    
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}
