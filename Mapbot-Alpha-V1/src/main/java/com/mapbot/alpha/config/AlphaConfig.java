package com.mapbot.alpha.config;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Alpha Core 配置系统
 * 从 Reforged BotConfig 移植，使用 Properties 文件
 */
public class AlphaConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Config");
    public static final AlphaConfig INSTANCE = new AlphaConfig();
    
    private final Properties props = new Properties();
    private final Path configPath = Paths.get("config", "alpha.properties");
    
    // 连接配置
    private String wsUrl = "ws://127.0.0.1:7000";
    private int reconnectInterval = 5;
    
    // Redis 配置
    private String redisHost = "127.0.0.1";
    private int redisPort = 6379;
    private String redisPassword = "";
    private int redisDatabase = 0;
    private boolean redisEnabled = false;
    
    // 群配置
    private long playerGroupId = 875585697L;
    private long adminGroupId = 885810515L;
    private String adminQQs = ""; // 逗号分隔的管理员QQ
    private long botQQ = 2133782376L;
    
    // 消息格式配置
    private String bridgeIngameMsgFormat = "[群消息] {player}: {content}";
    
    // 调试
    private boolean debugMode = true;
    
    private AlphaConfig() {}
    
    public void load() {
        try {
            Files.createDirectories(configPath.getParent());
            
            if (Files.exists(configPath)) {
                try (InputStream in = Files.newInputStream(configPath)) {
                    props.load(in);
                }
                
                wsUrl = props.getProperty("connection.wsUrl", wsUrl);
                reconnectInterval = Integer.parseInt(props.getProperty("connection.reconnectInterval", "5"));
                
                redisHost = props.getProperty("redis.host", redisHost);
                redisPort = Integer.parseInt(props.getProperty("redis.port", String.valueOf(redisPort)));
                redisPassword = props.getProperty("redis.password", redisPassword);
                redisDatabase = Integer.parseInt(props.getProperty("redis.database", String.valueOf(redisDatabase)));
                redisEnabled = Boolean.parseBoolean(props.getProperty("redis.enabled", String.valueOf(redisEnabled)));
                
                playerGroupId = Long.parseLong(props.getProperty("messaging.playerGroupId", String.valueOf(playerGroupId)));
                adminGroupId = Long.parseLong(props.getProperty("messaging.adminGroupId", String.valueOf(adminGroupId)));
                adminQQs = props.getProperty("messaging.adminQQs", adminQQs);
                botQQ = Long.parseLong(props.getProperty("messaging.botQQ", String.valueOf(botQQ)));
                debugMode = Boolean.parseBoolean(props.getProperty("debug.debugMode", "true"));
                
                // 同步管理员到 DataManager
                syncAdminsToDataManager();
                
                LOGGER.info("配置已加载: playerGroup={}, adminGroup={}, botQQ={}, redisEnabled={}", 
                    playerGroupId, adminGroupId, botQQ, redisEnabled);
            } else {
                save();
                LOGGER.info("已创建默认配置文件: {}", configPath);
            }
        } catch (Exception e) {
            LOGGER.error("加载配置失败", e);
        }
    }
    
    private void syncAdminsToDataManager() {
        if (adminQQs != null && !adminQQs.isEmpty()) {
            for (String qqStr : adminQQs.split(",")) {
                try {
                    com.mapbot.alpha.data.DataManager.INSTANCE.addAdmin(Long.parseLong(qqStr.trim()));
                } catch (NumberFormatException ignored) {}
            }
        }
    }
    
    public void save() {
        try {
            props.setProperty("connection.wsUrl", wsUrl);
            props.setProperty("connection.reconnectInterval", String.valueOf(reconnectInterval));
            
            props.setProperty("redis.host", redisHost);
            props.setProperty("redis.port", String.valueOf(redisPort));
            props.setProperty("redis.password", redisPassword);
            props.setProperty("redis.database", String.valueOf(redisDatabase));
            props.setProperty("redis.enabled", String.valueOf(redisEnabled));
            
            props.setProperty("messaging.playerGroupId", String.valueOf(playerGroupId));
            props.setProperty("messaging.adminGroupId", String.valueOf(adminGroupId));
            props.setProperty("messaging.adminQQs", adminQQs);
            props.setProperty("messaging.botQQ", String.valueOf(botQQ));
            props.setProperty("debug.debugMode", String.valueOf(debugMode));
            
            try (OutputStream out = Files.newOutputStream(configPath)) {
                props.store(out, "MapBot Alpha Core Configuration");
            }
        } catch (Exception e) {
            LOGGER.error("保存配置失败", e);
        }
    }
    
    // Getters
    public static String getWsUrl() { return INSTANCE.wsUrl; }
    public static int getReconnectInterval() { return INSTANCE.reconnectInterval; }
    public static long getPlayerGroupId() { return INSTANCE.playerGroupId; }
    public static long getAdminGroupId() { return INSTANCE.adminGroupId; }
    public static long getOpGroupId() { return INSTANCE.adminGroupId; }
    public static long getBotQQ() { return INSTANCE.botQQ; }
    public static boolean isDebugMode() { return INSTANCE.debugMode; }
    public String getBridgeIngameMsgFormat() { return bridgeIngameMsgFormat; }
    public String getAdminQQs() { return adminQQs; }
    
    // Redis Getters
    public String getRedisHost() { return redisHost; }
    public int getRedisPort() { return redisPort; }
    public String getRedisPassword() { return redisPassword; }
    public int getRedisDatabase() { return redisDatabase; }
    public boolean isRedisEnabled() { return redisEnabled; }
    
    // Setters
    public void setPlayerGroupId(long id) { this.playerGroupId = id; save(); }
    public void setAdminGroupId(long id) { this.adminGroupId = id; save(); }
    public void setWsUrl(String url) { this.wsUrl = url; save(); }
    public void setBotQQ(long qq) { this.botQQ = qq; save(); }
    public void setAdminQQs(String qqs) { 
        this.adminQQs = qqs; 
        syncAdminsToDataManager();
        save(); 
    }
    public void setRedisEnabled(boolean enabled) { this.redisEnabled = enabled; save(); }
    public void setRedisConfig(String host, int port, String pass, int db) {
        this.redisHost = host;
        this.redisPort = port;
        this.redisPassword = pass;
        this.redisDatabase = db;
        save();
    }
    
    /**
     * 重新加载配置
     */
    public void reload() {
        load();
        LOGGER.info("配置已重新加载");
    }
}
