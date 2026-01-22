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
    
    // 群配置
    private long playerGroupId = 875585697L;
    private long adminGroupId = 885810515L;
    private long botQQ = 2133782376L;
    
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
                playerGroupId = Long.parseLong(props.getProperty("messaging.playerGroupId", String.valueOf(playerGroupId)));
                adminGroupId = Long.parseLong(props.getProperty("messaging.adminGroupId", String.valueOf(adminGroupId)));
                botQQ = Long.parseLong(props.getProperty("messaging.botQQ", String.valueOf(botQQ)));
                debugMode = Boolean.parseBoolean(props.getProperty("debug.debugMode", "true"));
                
                LOGGER.info("配置已加载: playerGroup={}, adminGroup={}, botQQ={}", playerGroupId, adminGroupId, botQQ);
            } else {
                save();
                LOGGER.info("已创建默认配置文件: {}", configPath);
            }
        } catch (Exception e) {
            LOGGER.error("加载配置失败", e);
        }
    }
    
    public void save() {
        try {
            props.setProperty("connection.wsUrl", wsUrl);
            props.setProperty("connection.reconnectInterval", String.valueOf(reconnectInterval));
            props.setProperty("messaging.playerGroupId", String.valueOf(playerGroupId));
            props.setProperty("messaging.adminGroupId", String.valueOf(adminGroupId));
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
    
    // Setters
    public void setPlayerGroupId(long id) { this.playerGroupId = id; save(); }
    public void setAdminGroupId(long id) { this.adminGroupId = id; save(); }
}
