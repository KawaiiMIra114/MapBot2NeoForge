package com.mapbot.alpha.bridge;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务器注册表
 * 管理所有已连接的 MC 服务器
 * Task #05: 新增 targetGroupId 支持动态群组绑定
 */
public class ServerRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Bridge/Registry");
    public static final ServerRegistry INSTANCE = new ServerRegistry();
    
    private final Map<String, ServerInfo> servers = new ConcurrentHashMap<>();

    public void register(String serverId, Channel channel) {
        register(serverId, channel, null, 0, 0);
    }

    public void register(String serverId, Channel channel, String transferHost, int transferPort) {
        register(serverId, channel, transferHost, transferPort, 0);
    }

    /**
     * 注册服务器（Task #05: 支持 targetGroupId）
     * @param targetGroupId 子服声明的目标QQ群号，0 表示未指定（回退全局默认）
     */
    public void register(String serverId, Channel channel, String transferHost, int transferPort, long targetGroupId) {
        servers.put(serverId, new ServerInfo(serverId, channel, transferHost, transferPort, targetGroupId));
        LOGGER.info("服务器已注册: {} (当前连接数: {})", serverId, servers.size());
    }
    
    public void unregister(String serverId) {
        servers.remove(serverId);
        LOGGER.info("服务器已注销: {} (剩余连接数: {})", serverId, servers.size());
    }
    
    public void updateHeartbeat(String serverId) {
        ServerInfo info = servers.get(serverId);
        if (info != null) {
            info.lastHeartbeat = System.currentTimeMillis();
        }
    }
    
    public ServerInfo getServer(String serverId) {
        return servers.get(serverId);
    }
    
    public Collection<ServerInfo> getAllServers() {
        return servers.values();
    }
    
    public int getServerCount() {
        return servers.size();
    }

    /**
     * 获取子服绑定的目标群号
     * @return 子服声明的 targetGroupId，0 表示未指定
     */
    public long getTargetGroupId(String serverId) {
        ServerInfo info = servers.get(serverId);
        return info != null ? info.targetGroupId : 0;
    }
    
    /**
     * 向指定服务器发送消息
     */
    public boolean sendToServer(String serverId, String message) {
        ServerInfo info = servers.get(serverId);
        if (info != null && info.channel.isActive()) {
            info.channel.writeAndFlush(message + "\n");
            return true;
        }
        return false;
    }
    
    /**
     * 向所有服务器广播消息
     */
    public void broadcast(String message) {
        for (ServerInfo info : servers.values()) {
            if (info.channel.isActive()) {
                info.channel.writeAndFlush(message + "\n");
            }
        }
    }
    
    /**
     * 更新服务器状态
     */
    public void updateStatus(String serverId, String players, String tps, String memory) {
        ServerInfo info = servers.get(serverId);
        if (info != null) {
            info.players = players;
            info.tps = tps;
            info.memory = memory;
            info.lastHeartbeat = System.currentTimeMillis();
        }
    }
    
    /**
     * 获取服务器列表 JSON
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (ServerInfo info : servers.values()) {
            if (!first) sb.append(",");
            first = false;
            sb.append(info.toJson());
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * 服务器信息
     * Task #05: 新增 targetGroupId 字段
     */
    public static class ServerInfo {
        public final String serverId;
        public final Channel channel;
        public final long connectedAt;
        public final String transferHost;
        public final int transferPort;
        public final long targetGroupId;
        public long lastHeartbeat;
        public String players = "0";
        public String tps = "20.0";
        public String memory = "0MB";
        
        public ServerInfo(String serverId, Channel channel, String transferHost, int transferPort, long targetGroupId) {
            this.serverId = serverId;
            this.channel = channel;
            this.connectedAt = System.currentTimeMillis();
            this.lastHeartbeat = this.connectedAt;
            this.transferHost = transferHost;
            this.transferPort = transferPort;
            this.targetGroupId = targetGroupId;
        }
        
        public boolean isOnline() {
            return channel.isActive();
        }
        
        public long getUptimeMs() {
            return System.currentTimeMillis() - connectedAt;
        }
        
        public String toJson() {
            return "{\"id\":\"" + serverId + "\"," +
                   "\"online\":" + isOnline() + "," +
                   "\"players\":\"" + players + "\"," +
                   "\"tps\":\"" + tps + "\"," +
                   "\"memory\":\"" + memory + "\"," +
                   "\"targetGroupId\":" + targetGroupId + "," +
                   "\"uptime\":" + getUptimeMs() + "}";
        }
    }
}
