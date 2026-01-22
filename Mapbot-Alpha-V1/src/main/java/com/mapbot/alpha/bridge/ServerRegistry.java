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
 */
public class ServerRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Bridge/Registry");
    public static final ServerRegistry INSTANCE = new ServerRegistry();
    
    private final Map<String, ServerInfo> servers = new ConcurrentHashMap<>();
    
    public void register(String serverId, Channel channel) {
        servers.put(serverId, new ServerInfo(serverId, channel));
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
     * 服务器信息
     */
    public static class ServerInfo {
        public final String serverId;
        public final Channel channel;
        public final long connectedAt;
        public long lastHeartbeat;
        
        public ServerInfo(String serverId, Channel channel) {
            this.serverId = serverId;
            this.channel = channel;
            this.connectedAt = System.currentTimeMillis();
            this.lastHeartbeat = this.connectedAt;
        }
        
        public boolean isOnline() {
            return channel.isActive();
        }
    }
}
