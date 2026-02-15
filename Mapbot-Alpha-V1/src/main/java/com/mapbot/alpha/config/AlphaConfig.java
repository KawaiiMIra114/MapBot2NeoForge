package com.mapbot.alpha.config;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.Locale;
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

    private static final int RESERVED_PORT_MIN = 25560;
    private static final int RESERVED_PORT_MAX = 25566;
    private static final int SMART_GATEWAY_PORT = 25560;
    private static final int DEFAULT_BRIDGE_PORT = 25661;
    private static final int DEFAULT_TARGET_MC_PORT = 25570;
    
    private final Properties props = new Properties();
    private final Path configPath = Paths.get("config", "alpha.properties");
    
    // 连接配置
    private String wsUrl = "ws://127.0.0.1:7000";
    private int reconnectInterval = 5;
    private int listenPort = SMART_GATEWAY_PORT;
    private int bridgeListenPort = DEFAULT_BRIDGE_PORT;
    private String targetMcHost = "127.0.0.1";
    private int targetMcPort = DEFAULT_TARGET_MC_PORT;
    
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

    // 事务管理 (Step-04 B2)
    private volatile int configVersion = 0;
    private volatile Properties lastValidProps = null;
    
    private AlphaConfig() {}
    
    public void load() {
        try {
            Files.createDirectories(configPath.getParent());
            
            if (Files.exists(configPath)) {
                Properties newProps = new Properties();
                try (InputStream in = Files.newInputStream(configPath)) {
                    newProps.load(in);
                }

                // Step-04 B2: Schema 校验 (fail-closed)
                ConfigSchema.ValidationResult validation = ConfigSchema.validate(newProps);
                if (!validation.passed()) {
                    LOGGER.error("配置校验失败，中止加载。保持上一个有效配置。\n{}", validation.toSummary());
                    return;
                }

                props.clear();
                props.putAll(newProps);
                
                wsUrl = props.getProperty("connection.wsUrl", wsUrl);
                reconnectInterval = parseIntProperty("connection.reconnectInterval", reconnectInterval);
                listenPort = parseIntProperty(
                    "connection.listenPort",
                    parseIntProperty("server.listenPort", listenPort)
                );
                bridgeListenPort = parseIntProperty(
                    "bridge.listenPort",
                    parseIntProperty("connection.bridgePort", bridgeListenPort)
                );
                targetMcHost = readStringProperty("minecraft.targetHost", readStringProperty("server.targetMcHost", targetMcHost));
                targetMcPort = parseIntProperty(
                    "minecraft.targetPort",
                    parseIntProperty("server.targetMcPort", targetMcPort)
                );
                boolean portsChanged = normalizePorts();
                
                redisHost = props.getProperty("redis.host", redisHost);
                redisPort = parseIntProperty("redis.port", redisPort);
                redisPassword = props.getProperty("redis.password", redisPassword);
                redisDatabase = parseIntProperty("redis.database", redisDatabase);
                redisEnabled = parseBooleanProperty("redis.enabled", redisEnabled);
                
                playerGroupId = parseLongProperty("messaging.playerGroupId", playerGroupId);
                adminGroupId = parseLongProperty("messaging.adminGroupId", adminGroupId);
                adminQQs = props.getProperty("messaging.adminQQs", adminQQs);
                botQQ = parseLongProperty("messaging.botQQ", botQQ);
                debugMode = parseBooleanProperty("debug.debugMode", debugMode);
                
                // 同步管理员到 DataManager
                syncAdminsToDataManager();
                if (portsChanged) {
                    save();
                    LOGGER.info("检测到端口配置异常，已自动修正并写回 {}", configPath);
                }

                // 标记为有效配置
                lastValidProps = new Properties();
                lastValidProps.putAll(props);
                configVersion++;
                
                LOGGER.info("配置已加载: version={} playerGroup={} adminGroup={} botQQ={} redisEnabled={}", 
                    configVersion, playerGroupId, adminGroupId, botQQ, redisEnabled);
            } else {
                save();
                lastValidProps = new Properties();
                lastValidProps.putAll(props);
                configVersion = 1;
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
            Files.createDirectories(configPath.getParent());

            Properties merged = new Properties();
            if (Files.exists(configPath)) {
                try (InputStream in = Files.newInputStream(configPath)) {
                    merged.load(in);
                }
            }

            merged.setProperty("connection.wsUrl", wsUrl);
            merged.setProperty("connection.reconnectInterval", String.valueOf(reconnectInterval));
            merged.setProperty("connection.listenPort", String.valueOf(listenPort));
            merged.setProperty("bridge.listenPort", String.valueOf(bridgeListenPort));
            merged.setProperty("minecraft.targetHost", targetMcHost);
            merged.setProperty("minecraft.targetPort", String.valueOf(targetMcPort));

            merged.setProperty("redis.host", redisHost);
            merged.setProperty("redis.port", String.valueOf(redisPort));
            merged.setProperty("redis.password", redisPassword);
            merged.setProperty("redis.database", String.valueOf(redisDatabase));
            merged.setProperty("redis.enabled", String.valueOf(redisEnabled));

            merged.setProperty("messaging.playerGroupId", String.valueOf(playerGroupId));
            merged.setProperty("messaging.adminGroupId", String.valueOf(adminGroupId));
            merged.setProperty("messaging.adminQQs", adminQQs);
            merged.setProperty("messaging.botQQ", String.valueOf(botQQ));
            merged.setProperty("debug.debugMode", String.valueOf(debugMode));

            try (OutputStream out = Files.newOutputStream(configPath)) {
                merged.store(out, "MapBot Alpha Core Configuration");
            }

            props.clear();
            props.putAll(merged);
        } catch (Exception e) {
            LOGGER.error("保存配置失败", e);
        }
    }
    
    // Getters
    public static String getWsUrl() { return INSTANCE.wsUrl; }
    public static int getReconnectInterval() { return INSTANCE.reconnectInterval; }
    public static int getListenPort() { return INSTANCE.listenPort; }
    public static int getBridgeListenPort() { return INSTANCE.bridgeListenPort; }
    public static String getTargetMcHost() { return INSTANCE.targetMcHost; }
    public static int getTargetMcPort() { return INSTANCE.targetMcPort; }
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
     * 事务式热重载配置 (Step-04 B2)
     * 流程: parse → validate → staging → atomic swap → audit → rollback
     * @return 重载结果
     */
    public ReloadResult reload() {
        int prevVersion = configVersion;
        String timestamp = Instant.now().toString();
        LOGGER.info("[RELOAD-AUDIT] 开始热重载 prevVersion={} time={}", prevVersion, timestamp);

        // 1. Parse: 读取新配置
        Properties stagingProps = new Properties();
        try {
            if (!Files.exists(configPath)) {
                String msg = "配置文件不存在: " + configPath;
                LOGGER.error("[RELOAD-AUDIT] 解析失败: {}", msg);
                return ReloadResult.failure(prevVersion, msg);
            }
            try (InputStream in = Files.newInputStream(configPath)) {
                stagingProps.load(in);
            }
        } catch (Exception e) {
            String msg = "解析配置文件失败: " + e.getMessage();
            LOGGER.error("[RELOAD-AUDIT] {}", msg, e);
            return ReloadResult.failure(prevVersion, msg);
        }

        // 2. Validate: Schema 校验 (staging 中，不影响运行态)
        ConfigSchema.ValidationResult validation = ConfigSchema.validate(stagingProps);
        if (!validation.passed()) {
            String msg = validation.toSummary();
            LOGGER.error("[RELOAD-AUDIT] 校验失败 (rollback 到 v{}): {}", prevVersion, msg);
            return ReloadResult.failure(prevVersion, msg);
        }

        // 3. Staging: 保存旧配置快照用于回滚
        Properties rollbackSnapshot = new Properties();
        rollbackSnapshot.putAll(props);

        // 保存旧字段值
        String oldWsUrl = wsUrl;
        int oldReconnectInterval = reconnectInterval;
        int oldListenPort = listenPort;
        int oldBridgeListenPort = bridgeListenPort;
        String oldTargetMcHost = targetMcHost;
        int oldTargetMcPort = targetMcPort;
        String oldRedisHost = redisHost;
        int oldRedisPort = redisPort;
        String oldRedisPassword = redisPassword;
        int oldRedisDatabase = redisDatabase;
        boolean oldRedisEnabled = redisEnabled;
        long oldPlayerGroupId = playerGroupId;
        long oldAdminGroupId = adminGroupId;
        String oldAdminQQs = adminQQs;
        long oldBotQQ = botQQ;
        boolean oldDebugMode = debugMode;

        // 4. Atomic Swap: 应用新配置
        try {
            props.clear();
            props.putAll(stagingProps);

            wsUrl = props.getProperty("connection.wsUrl", wsUrl);
            reconnectInterval = parseIntProperty("connection.reconnectInterval", reconnectInterval);
            listenPort = parseIntProperty(
                "connection.listenPort",
                parseIntProperty("server.listenPort", listenPort)
            );
            bridgeListenPort = parseIntProperty(
                "bridge.listenPort",
                parseIntProperty("connection.bridgePort", bridgeListenPort)
            );
            targetMcHost = readStringProperty("minecraft.targetHost", readStringProperty("server.targetMcHost", targetMcHost));
            targetMcPort = parseIntProperty(
                "minecraft.targetPort",
                parseIntProperty("server.targetMcPort", targetMcPort)
            );
            normalizePorts();

            redisHost = props.getProperty("redis.host", redisHost);
            redisPort = parseIntProperty("redis.port", redisPort);
            redisPassword = props.getProperty("redis.password", redisPassword);
            redisDatabase = parseIntProperty("redis.database", redisDatabase);
            redisEnabled = parseBooleanProperty("redis.enabled", redisEnabled);

            playerGroupId = parseLongProperty("messaging.playerGroupId", playerGroupId);
            adminGroupId = parseLongProperty("messaging.adminGroupId", adminGroupId);
            adminQQs = props.getProperty("messaging.adminQQs", adminQQs);
            botQQ = parseLongProperty("messaging.botQQ", botQQ);
            debugMode = parseBooleanProperty("debug.debugMode", debugMode);

            syncAdminsToDataManager();

            // 更新版本和有效快照
            lastValidProps = new Properties();
            lastValidProps.putAll(props);
            configVersion++;

            // 5. Audit: 记录成功
            LOGGER.info("[RELOAD-AUDIT] 热重载成功 v{} -> v{} time={}", prevVersion, configVersion, timestamp);
            return ReloadResult.success(prevVersion, configVersion);

        } catch (Exception e) {
            // 6. Rollback: 恢复旧配置
            LOGGER.error("[RELOAD-AUDIT] 应用配置异常，执行回滚到 v{}", prevVersion, e);
            props.clear();
            props.putAll(rollbackSnapshot);
            wsUrl = oldWsUrl;
            reconnectInterval = oldReconnectInterval;
            listenPort = oldListenPort;
            bridgeListenPort = oldBridgeListenPort;
            targetMcHost = oldTargetMcHost;
            targetMcPort = oldTargetMcPort;
            redisHost = oldRedisHost;
            redisPort = oldRedisPort;
            redisPassword = oldRedisPassword;
            redisDatabase = oldRedisDatabase;
            redisEnabled = oldRedisEnabled;
            playerGroupId = oldPlayerGroupId;
            adminGroupId = oldAdminGroupId;
            adminQQs = oldAdminQQs;
            botQQ = oldBotQQ;
            debugMode = oldDebugMode;

            LOGGER.info("[RELOAD-AUDIT] 回滚完成，当前仍为 v{}", configVersion);
            return ReloadResult.failure(prevVersion, "应用配置异常 (已回滚): " + e.getMessage());
        }
    }

    /** 获取当前配置版本号 */
    public int getConfigVersion() { return configVersion; }

    /**
     * 热重载结果 (Step-04 B2)
     */
    public record ReloadResult(boolean success, int prevVersion, int newVersion, String message) {
        public static ReloadResult success(int prev, int next) {
            return new ReloadResult(true, prev, next, "配置版本 v" + prev + " -> v" + next);
        }
        public static ReloadResult failure(int prev, String reason) {
            return new ReloadResult(false, prev, prev, reason);
        }
        public String toSummary() {
            return success
                    ? "[成功] " + message
                    : "[失败] 保持 v" + prevVersion + " — " + message;
        }
    }

    private int parseIntProperty(String key, int fallback) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) return fallback;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            LOGGER.warn("配置项 {} 值非法: {}, 使用默认值 {}", key, value, fallback);
            return fallback;
        }
    }

    private long parseLongProperty(String key, long fallback) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) return fallback;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            LOGGER.warn("配置项 {} 值非法: {}, 使用默认值 {}", key, value, fallback);
            return fallback;
        }
    }

    private boolean parseBooleanProperty(String key, boolean fallback) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) return fallback;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized)) return true;
        if ("false".equals(normalized)) return false;
        LOGGER.warn("配置项 {} 值非法: {}, 使用默认值 {}", key, value, fallback);
        return fallback;
    }

    private String readStringProperty(String key, String fallback) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) return fallback;
        return value.trim();
    }

    private boolean normalizePorts() {
        boolean changed = false;
        if (listenPort < 1 || listenPort > 65535) {
            LOGGER.warn("listenPort={} 非法，已回退到 {}", listenPort, SMART_GATEWAY_PORT);
            listenPort = SMART_GATEWAY_PORT;
            changed = true;
        }

        if (listenPort >= RESERVED_PORT_MIN && listenPort <= RESERVED_PORT_MAX && listenPort != SMART_GATEWAY_PORT) {
            LOGGER.warn("listenPort={} 位于保留段且非智能分流口 25560，已回退到 {}", listenPort, SMART_GATEWAY_PORT);
            listenPort = SMART_GATEWAY_PORT;
            changed = true;
        }

        int sanitizedBridgePort = sanitizeServicePort("bridge.listenPort", bridgeListenPort, DEFAULT_BRIDGE_PORT);
        if (sanitizedBridgePort != bridgeListenPort) changed = true;
        bridgeListenPort = sanitizedBridgePort;
        int sanitizedTargetMcPort = sanitizeServicePort("minecraft.targetPort", targetMcPort, DEFAULT_TARGET_MC_PORT);
        if (sanitizedTargetMcPort != targetMcPort) changed = true;
        targetMcPort = sanitizedTargetMcPort;

        if (targetMcHost == null || targetMcHost.isBlank()) {
            targetMcHost = "127.0.0.1";
            changed = true;
        }

        if (bridgeListenPort == listenPort) {
            int adjusted = findAvailableServicePort(DEFAULT_BRIDGE_PORT, listenPort, targetMcPort);
            LOGGER.warn("bridge.listenPort 与 listenPort 冲突，已调整为 {}", adjusted);
            bridgeListenPort = adjusted;
            changed = true;
        }
        if (targetMcPort == listenPort || targetMcPort == bridgeListenPort) {
            int adjusted = findAvailableServicePort(DEFAULT_TARGET_MC_PORT, listenPort, bridgeListenPort);
            LOGGER.warn("minecraft.targetPort 与现有端口冲突，已调整为 {}", adjusted);
            targetMcPort = adjusted;
            changed = true;
        }
        return changed;
    }

    private int sanitizeServicePort(String key, int configured, int fallback) {
        int port = configured;
        if (port < 1 || port > 65535) {
            LOGGER.warn("{}={} 非法，已回退到 {}", key, port, fallback);
            return fallback;
        }
        if (port >= RESERVED_PORT_MIN && port <= RESERVED_PORT_MAX) {
            LOGGER.warn("{}={} 命中保留端口段 25560-25566，已回退到 {}", key, port, fallback);
            return fallback;
        }
        return port;
    }

    private int findAvailableServicePort(int preferred, int... occupiedPorts) {
        int port = Math.max(1, preferred);
        while (port <= 65535) {
            if (isValidServicePort(port, occupiedPorts)) {
                return port;
            }
            port++;
        }
        // 理论上不会触发，兜底返回默认值
        return preferred;
    }

    private boolean isValidServicePort(int port, int... occupiedPorts) {
        if (port < 1 || port > 65535) return false;
        if (port >= RESERVED_PORT_MIN && port <= RESERVED_PORT_MAX) return false;
        for (int occupied : occupiedPorts) {
            if (port == occupied) return false;
        }
        return true;
    }
}
