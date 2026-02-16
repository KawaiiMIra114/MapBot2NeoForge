package com.mapbot.alpha.config;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Alpha 配置 Schema 校验器 (Step-04 B2)
 * 实现 unknown-key fail-closed 和类型/范围严格校验。
 *
 * 所有 alpha.properties 允许的键必须在此白名单中注册。
 * 未知键、非法类型、超出范围的值都会导致校验失败。
 */
public class ConfigSchema {

    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/ConfigSchema");

    /** 校验条目：键名 → 校验规则 */
    private static final Map<String, SchemaEntry> SCHEMA = new LinkedHashMap<>();

    static {
        // === 连接配置 ===
        str("connection.wsUrl", true);
        str("connection.wsToken", false);  // OneBot access_token (NapCat 鉴权)
        intRange("connection.reconnectInterval", 1, 300, true);
        intRange("connection.listenPort", 1, 65535, true);
        // legacy 别名
        intRange("server.listenPort", 1, 65535, false);
        intRange("connection.bridgePort", 1, 65535, false);

        // === Bridge 配置 ===
        intRange("bridge.listenPort", 1, 65535, true);

        // === Minecraft 目标配置 ===
        str("minecraft.targetHost", true);
        intRange("minecraft.targetPort", 1, 65535, true);
        // legacy 别名
        str("server.targetMcHost", false);
        intRange("server.targetMcPort", 1, 65535, false);

        // === Redis 配置 ===
        str("redis.host", true);
        intRange("redis.port", 1, 65535, true);
        str("redis.password", true);  // 允许空字符串
        intRange("redis.database", 0, 15, true);
        bool("redis.enabled", true);

        // === 消息配置 ===
        longRange("messaging.playerGroupId", 0, Long.MAX_VALUE, true);
        longRange("messaging.adminGroupId", 0, Long.MAX_VALUE, true);
        str("messaging.adminQQs", true);
        longRange("messaging.botQQ", 0, Long.MAX_VALUE, true);

        // === 调试 ===
        bool("debug.debugMode", true);

        // === 安全配置 (AuthManager 管理) ===
        str("auth.bridge.token", false);
        str("auth.bridge.sharedToken", false);
        str("auth.consoleToken", false);  // legacy
        str("auth.bridge.allowedServerIds", false);
        str("auth.allowedServerIds", false);  // legacy
        str("auth.tokenSecret", false);
        bool("auth.bootstrapAdmin.enabled", false);
        str("auth.bootstrapAdmin.username", false);
        str("auth.bootstrapAdmin.password", false);
        str("auth.bootstrapAdmin.role", false);

        // === 消息格式 ===
        str("bridge.ingameMsgFormat", false);
    }

    /**
     * 校验 Properties 是否符合 schema。
     * @return 校验结果，包含所有错误。
     */
    public static ValidationResult validate(Properties props) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (props == null) {
            errors.add("配置对象为 null");
            return new ValidationResult(false, errors, warnings);
        }

        // 1. 检查未知键 (fail-closed)
        for (String key : props.stringPropertyNames()) {
            if (!SCHEMA.containsKey(key)) {
                errors.add(String.format("未知配置键: '%s' (fail-closed: 不允许未注册的键)", key));
            }
        }

        // 2. 检查已注册键的类型和范围
        for (Map.Entry<String, SchemaEntry> entry : SCHEMA.entrySet()) {
            String key = entry.getKey();
            SchemaEntry schema = entry.getValue();
            String value = props.getProperty(key);

            // 非必填且未设置 → 跳过
            if (value == null || value.isBlank()) {
                continue;
            }

            value = value.trim();

            switch (schema.type) {
                case INT -> {
                    try {
                        int intVal = Integer.parseInt(value);
                        if (intVal < schema.minInt || intVal > schema.maxInt) {
                            errors.add(String.format("配置键 '%s' 值超出范围: %d (允许 %d~%d)",
                                    key, intVal, schema.minInt, schema.maxInt));
                        }
                    } catch (NumberFormatException e) {
                        errors.add(String.format("配置键 '%s' 类型错误: '%s' 不是有效的整数", key, value));
                    }
                }
                case LONG -> {
                    try {
                        long longVal = Long.parseLong(value);
                        if (longVal < schema.minLong || longVal > schema.maxLong) {
                            errors.add(String.format("配置键 '%s' 值超出范围: %d (允许 %d~%d)",
                                    key, longVal, schema.minLong, schema.maxLong));
                        }
                    } catch (NumberFormatException e) {
                        errors.add(String.format("配置键 '%s' 类型错误: '%s' 不是有效的长整数", key, value));
                    }
                }
                case BOOLEAN -> {
                    String normalized = value.toLowerCase(Locale.ROOT);
                    if (!"true".equals(normalized) && !"false".equals(normalized)) {
                        errors.add(String.format("配置键 '%s' 类型错误: '%s' 不是有效的布尔值 (true/false)", key, value));
                    }
                }
                case STRING -> {
                    // 字符串类型无特殊校验
                }
            }
        }

        boolean passed = errors.isEmpty();
        if (!passed) {
            for (String err : errors) {
                LOGGER.error("[ConfigSchema] {}", err);
            }
        }

        return new ValidationResult(passed, errors, warnings);
    }

    // === Schema 注册辅助方法 ===

    private static void str(String key, boolean required) {
        SCHEMA.put(key, new SchemaEntry(ValueType.STRING, required));
    }

    private static void bool(String key, boolean required) {
        SCHEMA.put(key, new SchemaEntry(ValueType.BOOLEAN, required));
    }

    private static void intRange(String key, int min, int max, boolean required) {
        SchemaEntry e = new SchemaEntry(ValueType.INT, required);
        e.minInt = min;
        e.maxInt = max;
        SCHEMA.put(key, e);
    }

    private static void longRange(String key, long min, long max, boolean required) {
        SchemaEntry e = new SchemaEntry(ValueType.LONG, required);
        e.minLong = min;
        e.maxLong = max;
        SCHEMA.put(key, e);
    }

    // === 内部类型 ===

    public enum ValueType { STRING, INT, LONG, BOOLEAN }

    private static class SchemaEntry {
        final ValueType type;
        final boolean required;
        int minInt = Integer.MIN_VALUE;
        int maxInt = Integer.MAX_VALUE;
        long minLong = Long.MIN_VALUE;
        long maxLong = Long.MAX_VALUE;

        SchemaEntry(ValueType type, boolean required) {
            this.type = type;
            this.required = required;
        }
    }

    /**
     * 校验结果
     */
    public record ValidationResult(boolean passed, List<String> errors, List<String> warnings) {

        /** 格式化为人类可读的摘要 */
        public String toSummary() {
            if (passed) return "配置校验通过";
            StringBuilder sb = new StringBuilder("配置校验失败 (").append(errors.size()).append(" 个错误):\n");
            for (int i = 0; i < errors.size(); i++) {
                sb.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
            }
            return sb.toString().trim();
        }
    }

    /** 获取所有已注册的合法键名（用于审计） */
    public static Set<String> getRegisteredKeys() {
        return Collections.unmodifiableSet(SCHEMA.keySet());
    }
}
