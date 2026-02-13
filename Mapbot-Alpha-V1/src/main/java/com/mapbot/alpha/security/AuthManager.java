package com.mapbot.alpha.security;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 多用户认证管理器
 * 支持：多用户、角色权限、JSON 持久化
 */
public enum AuthManager {
    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthManager.class);
    private static final long TOKEN_EXPIRE_MS = 24 * 60 * 60 * 1000; // 24 小时
    private static final String DEFAULT_TOKEN_SECRET = "MapBot-Alpha-Secret-2026";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PASSWORD_HASH_PREFIX = "pbkdf2$";
    private static final String PASSWORD_KDF = "PBKDF2WithHmacSHA256";
    private static final int PASSWORD_ITERATIONS = 120_000;
    private static final int PASSWORD_KEY_BITS = 256;
    private static final int PASSWORD_SALT_BYTES = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    // 用户存储文件
    private Path usersFile = Paths.get("config", "users.json");
    
    // 用户数据库
    private final Map<String, User> users = new ConcurrentHashMap<>();
    
    // 已颁发的有效 Token
    private final Map<String, TokenInfo> validTokens = new ConcurrentHashMap<>();

    private static final String REDIS_SYNC_CHANNEL = "mapbot:auth:sync";
    private static final String REDIS_KEY_USERS = "mapbot:web:users";
    private static final String REDIS_KEY_TOKENS = "mapbot:web:tokens";
    private static final Path ALPHA_CONFIG_FILE = Paths.get("config", "alpha.properties");

    private static final String KEY_BOOTSTRAP_ENABLED = "auth.bootstrapAdmin.enabled";
    private static final String KEY_BOOTSTRAP_USERNAME = "auth.bootstrapAdmin.username";
    private static final String KEY_BOOTSTRAP_PASSWORD = "auth.bootstrapAdmin.password";
    private static final String KEY_BOOTSTRAP_ROLE = "auth.bootstrapAdmin.role";

    private static final String KEY_BRIDGE_TOKEN = "auth.bridge.token";
    private static final String KEY_BRIDGE_TOKEN_ALT = "auth.bridge.sharedToken";
    private static final String KEY_BRIDGE_TOKEN_LEGACY = "auth.consoleToken";
    private static final String KEY_BRIDGE_ALLOWED = "auth.bridge.allowedServerIds";
    private static final String KEY_BRIDGE_ALLOWED_LEGACY = "auth.allowedServerIds";
    private static final String KEY_TOKEN_SECRET = "auth.tokenSecret";

    private static final Set<String> WEAK_PASSWORDS = Set.of(
            "admin123", "admin", "123456", "12345678", "password", "change_me_now", "qwerty"
    );
    private static final Set<String> WEAK_BRIDGE_TOKENS = Set.of(
            "change_me_now", "changeme", "default", "mapbot", "admin123"
    );

    private volatile BridgeAuthConfig bridgeAuthConfig = BridgeAuthConfig.disabled();
    private volatile BootstrapAdminConfig bootstrapAdminConfig = BootstrapAdminConfig.disabled();
    private volatile String tokenSecret = DEFAULT_TOKEN_SECRET;
    
    /**
     * 初始化
     */
    public void init() {
        loadSecurityConfig();
        loadUsers();

        // Redis 同步
        var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
        if (redis.isEnabled()) {
            syncFromRedis();
            redis.subscribe(REDIS_SYNC_CHANNEL, msg -> syncFromRedis());
            LOGGER.info("AuthManager Redis 同步已开启");
        }

        if (users.isEmpty()) {
            bootstrapAdminIfConfigured();
        }
    }

    private void loadSecurityConfig() {
        Properties props = loadAlphaProperties();
        boolean updated = ensureSecurityProperties(props);
        if (updated) {
            saveAlphaProperties(props, "auto-seeded security defaults");
        }

        bridgeAuthConfig = parseBridgeAuthConfig(props);
        bootstrapAdminConfig = parseBootstrapAdminConfig(props);
        tokenSecret = parseTokenSecret(props);

        if (bridgeAuthConfig.enabled) {
            LOGGER.info("Bridge 鉴权已启用，允许 serverId 数量: {}", bridgeAuthConfig.allowedServerIds.size());
        } else {
            LOGGER.warn("Bridge 鉴权未启用或配置不完整（默认拒绝所有 Bridge 注册）");
        }
    }

    private Properties loadAlphaProperties() {
        Properties props = new Properties();
        if (Files.exists(ALPHA_CONFIG_FILE)) {
            try (InputStream in = Files.newInputStream(ALPHA_CONFIG_FILE)) {
                props.load(in);
            } catch (Exception e) {
                LOGGER.error("读取安全配置失败: {}", ALPHA_CONFIG_FILE, e);
            }
        }
        return props;
    }

    private void saveAlphaProperties(Properties props, String reason) {
        try {
            Path parent = ALPHA_CONFIG_FILE.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (OutputStream out = Files.newOutputStream(ALPHA_CONFIG_FILE)) {
                props.store(out, "MapBot Alpha Core Configuration");
            }
            LOGGER.info("安全配置已写入 {} ({})", ALPHA_CONFIG_FILE, reason);
        } catch (Exception e) {
            LOGGER.error("写入安全配置失败: {}", ALPHA_CONFIG_FILE, e);
        }
    }

    private boolean ensureSecurityProperties(Properties props) {
        boolean updated = false;

        // 兼容旧键：若新键缺失但旧键存在，回填到新键
        String legacyToken = trimToNull(props.getProperty(KEY_BRIDGE_TOKEN_LEGACY));
        String currentBridgeToken = trimToNull(props.getProperty(KEY_BRIDGE_TOKEN));
        if (currentBridgeToken == null && legacyToken != null) {
            props.setProperty(KEY_BRIDGE_TOKEN, legacyToken);
            updated = true;
        }

        String legacyAllowed = trimToNull(props.getProperty(KEY_BRIDGE_ALLOWED_LEGACY));
        String currentAllowed = trimToNull(props.getProperty(KEY_BRIDGE_ALLOWED));
        if (currentAllowed == null && legacyAllowed != null) {
            props.setProperty(KEY_BRIDGE_ALLOWED, legacyAllowed);
            updated = true;
        }

        if (trimToNull(props.getProperty(KEY_BRIDGE_TOKEN)) == null) {
            props.setProperty(KEY_BRIDGE_TOKEN, generateSecureToken(24));
            updated = true;
            LOGGER.warn("检测到缺失 {}, 已自动生成并写入配置文件。", KEY_BRIDGE_TOKEN);
        }
        if (trimToNull(props.getProperty(KEY_BRIDGE_ALLOWED)) == null) {
            props.setProperty(KEY_BRIDGE_ALLOWED, "default");
            updated = true;
            LOGGER.warn("检测到缺失 {}, 已默认写入 default。", KEY_BRIDGE_ALLOWED);
        }
        if (trimToNull(props.getProperty(KEY_TOKEN_SECRET)) == null) {
            props.setProperty(KEY_TOKEN_SECRET, generateSecureToken(24));
            updated = true;
            LOGGER.warn("检测到缺失 {}, 已自动生成并写入配置文件。", KEY_TOKEN_SECRET);
        }

        if (!props.containsKey(KEY_BOOTSTRAP_ENABLED)) {
            props.setProperty(KEY_BOOTSTRAP_ENABLED, "false");
            updated = true;
        }
        if (!props.containsKey(KEY_BOOTSTRAP_USERNAME)) {
            props.setProperty(KEY_BOOTSTRAP_USERNAME, "admin");
            updated = true;
        }
        if (!props.containsKey(KEY_BOOTSTRAP_PASSWORD)) {
            props.setProperty(KEY_BOOTSTRAP_PASSWORD, "");
            updated = true;
        }
        if (!props.containsKey(KEY_BOOTSTRAP_ROLE)) {
            props.setProperty(KEY_BOOTSTRAP_ROLE, "ADMIN");
            updated = true;
        }

        return updated;
    }

    private String parseTokenSecret(Properties props) {
        String configured = firstNonBlank(props.getProperty(KEY_TOKEN_SECRET));
        if (configured == null || configured.trim().length() < 16) {
            return DEFAULT_TOKEN_SECRET;
        }
        return configured.trim();
    }

    private BridgeAuthConfig parseBridgeAuthConfig(Properties props) {
        String token = firstNonBlank(
                props.getProperty(KEY_BRIDGE_TOKEN),
                props.getProperty(KEY_BRIDGE_TOKEN_ALT),
                props.getProperty(KEY_BRIDGE_TOKEN_LEGACY)
        );
        if (token != null) {
            token = token.trim();
        }

        if (token == null || token.isEmpty() || isWeakBridgeToken(token)) {
            return BridgeAuthConfig.disabled();
        }

        Set<String> allowed = parseCsvSet(firstNonBlank(
                props.getProperty(KEY_BRIDGE_ALLOWED),
                props.getProperty(KEY_BRIDGE_ALLOWED_LEGACY)
        ));
        if (allowed.isEmpty()) {
            return BridgeAuthConfig.disabled();
        }
        return new BridgeAuthConfig(true, token, allowed);
    }

    private BootstrapAdminConfig parseBootstrapAdminConfig(Properties props) {
        boolean enabled = Boolean.parseBoolean(props.getProperty(KEY_BOOTSTRAP_ENABLED, "false"));
        String username = props.getProperty(KEY_BOOTSTRAP_USERNAME, "admin");
        String password = props.getProperty(KEY_BOOTSTRAP_PASSWORD, "");
        String roleName = props.getProperty(KEY_BOOTSTRAP_ROLE, "ADMIN");
        Role role = Role.ADMIN;
        try {
            role = Role.valueOf(roleName.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {}
        return new BootstrapAdminConfig(enabled, username, password, role);
    }

    private void bootstrapAdminIfConfigured() {
        BootstrapAdminConfig cfg = bootstrapAdminConfig;
        if (!cfg.enabled) {
            LOGGER.warn("当前无任何管理用户，且未启用 bootstrap 管理员创建。请配置 auth.bootstrapAdmin.* 后重启。");
            return;
        }

        String username = cfg.username == null ? "" : cfg.username.trim();
        String password = cfg.password == null ? "" : cfg.password.trim();
        if (username.isEmpty()) {
            LOGGER.error("bootstrap 管理员创建失败: 用户名为空");
            return;
        }
        if (!isStrongPassword(password)) {
            LOGGER.error("bootstrap 管理员创建失败: 密码强度不足（至少 10 位，且不能是弱口令）");
            return;
        }

        if (createUser(username, password, cfg.role)) {
            LOGGER.info("已创建 bootstrap 管理员账户: {} (role={})", username, cfg.role);
        }
    }

    public boolean isBridgeRegistrationAuthorized(String serverId, String presentedToken) {
        BridgeAuthConfig cfg = bridgeAuthConfig;
        if (!cfg.enabled) return false;
        if (!isValidServerId(serverId)) return false;
        if (presentedToken == null || presentedToken.isBlank()) return false;
        if (!secureEquals(cfg.sharedToken, presentedToken.trim())) return false;
        return cfg.allowedServerIds.contains(serverId);
    }

    public boolean isBridgeAuthEnabled() {
        return bridgeAuthConfig.enabled;
    }

    private void syncFromRedis() {
        var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
        redis.execute(jedis -> {
            Map<String, String> u = jedis.hgetAll(REDIS_KEY_USERS);
            u.forEach((name, json) -> {
                User user = GSON.fromJson(json, User.class);
                if (user != null) users.put(name, user);
            });
            
            // Token 我们选择实时查 Redis 而不是缓存到本地 Map，以保证绝对的一致性
            return null;
        });
    }

    private void broadcastSync() {
        var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
        if (redis.isEnabled()) {
            redis.publish(REDIS_SYNC_CHANNEL, "refresh");
        }
    }
    
    /**
     * 用户登录
     */
    public String login(String username, String password) {
        User user = users.get(username);
        if (user == null) {
            LOGGER.warn("登录失败: 用户不存在 {}", username);
            return null;
        }
        
        if (!verifyPassword(password, user.passwordHash)) {
            LOGGER.warn("登录失败: 密码错误 {}", username);
            return null;
        }

        // 兼容迁移：旧版 SHA-256 登录成功后升级为 PBKDF2 存储
        if (!isModernPasswordHash(user.passwordHash)) {
            user.passwordHash = hashPassword(password);
            saveUsers();
            var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
            if (redis.isEnabled()) {
                redis.execute(jedis -> jedis.hset(REDIS_KEY_USERS, username, GSON.toJson(user)));
                broadcastSync();
            }
            LOGGER.info("用户密码哈希已升级到 PBKDF2: {}", username);
        }
        
        String token = generateToken(username);
        TokenInfo info = new TokenInfo(username, user.role, System.currentTimeMillis() + TOKEN_EXPIRE_MS);
        
        validTokens.put(token, info);
        
        // 同步到 Redis
        var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
        if (redis.isEnabled()) {
            redis.execute(jedis -> jedis.hset(REDIS_KEY_TOKENS, token, GSON.toJson(info)));
        }
        
        LOGGER.info("用户登录成功: {} (角色: {})", username, user.role);
        return token;
    }
    
    /**
     * 验证 Token
     */
    public boolean validateToken(String token) {
        return getTokenInfo(token) != null;
    }
    
    /**
     * 验证 Token 并检查权限
     */
    public boolean hasPermission(String token, Role requiredRole) {
        TokenInfo info = getTokenInfo(token);
        if (info == null || requiredRole == null) return false;
        return info.role.ordinal() >= requiredRole.ordinal();
    }
    
    /**
     * 登出
     */
    public void logout(String token) {
        validTokens.remove(token);
        var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
        if (redis.isEnabled()) {
            redis.execute(jedis -> jedis.hdel(REDIS_KEY_TOKENS, token));
        }
    }
    
    /**
     * 获取 Token 对应的用户名
     */
    public String getUsername(String token) {
        TokenInfo info = getTokenInfo(token);
        return info != null ? info.username : null;
    }
    
    /**
     * 获取 Token 对应的角色
     */
    public Role getRole(String token) {
        TokenInfo info = getTokenInfo(token);
        return info != null ? info.role : null;
    }
    
    // === 用户管理 API ===
    
    /**
     * 创建用户
     */
    public boolean createUser(String username, String password, Role role) {
        if (users.containsKey(username)) {
            return false;
        }
        if (!isStrongPassword(password)) {
            LOGGER.warn("创建用户失败: 密码强度不足 username={}", username);
            return false;
        }
        User user = new User(username, hashPassword(password), role);
        users.put(username, user);
        saveUsers();
        
        var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
        if (redis.isEnabled()) {
            redis.execute(jedis -> jedis.hset(REDIS_KEY_USERS, username, GSON.toJson(user)));
            broadcastSync();
        }
        
        LOGGER.info("用户已创建: {} (角色: {})", username, role);
        return true;
    }
    
    /**
     * 删除用户
     */
    public boolean deleteUser(String username) {
        if ("admin".equals(username)) {
            return false; // 不允许删除 admin
        }
        User removed = users.remove(username);
        if (removed != null) {
            invalidateTokensByUsername(username);
            saveUsers();
            
            var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
            if (redis.isEnabled()) {
                redis.execute(jedis -> jedis.hdel(REDIS_KEY_USERS, username));
                broadcastSync();
            }
            
            LOGGER.info("用户已删除: {}", username);
            return true;
        }
        return false;
    }
    
    /**
     * 修改密码
     */
    public boolean changePassword(String username, String newPassword) {
        User user = users.get(username);
        if (user == null) return false;
        if (!isStrongPassword(newPassword)) {
            LOGGER.warn("修改密码失败: 密码强度不足 username={}", username);
            return false;
        }
        
        user.passwordHash = hashPassword(newPassword);
        invalidateTokensByUsername(username);
        saveUsers();
        
        var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
        if (redis.isEnabled()) {
            redis.execute(jedis -> jedis.hset(REDIS_KEY_USERS, username, GSON.toJson(user)));
            broadcastSync();
        }
        
        LOGGER.info("用户密码已修改: {}", username);
        return true;
    }
    
    /**
     * 修改角色
     */
    public boolean changeRole(String username, Role newRole) {
        User user = users.get(username);
        if (user == null) return false;
        
        user.role = newRole;
        saveUsers();
        
        var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
        if (redis.isEnabled()) {
            redis.execute(jedis -> jedis.hset(REDIS_KEY_USERS, username, GSON.toJson(user)));
            broadcastSync();
        }
        
        LOGGER.info("用户角色已修改: {} -> {}", username, newRole);
        return true;
    }
    
    /**
     * 获取所有用户列表 (不含密码)
     */
    public List<UserInfo> listUsers() {
        List<UserInfo> list = new ArrayList<>();
        for (User user : users.values()) {
            list.add(new UserInfo(user.username, user.role));
        }
        return list;
    }
    
    // === 持久化 ===
    
    private void loadUsers() {
        try {
            if (Files.exists(usersFile)) {
                String json = Files.readString(usersFile, StandardCharsets.UTF_8);
                Map<String, User> loaded = GSON.fromJson(json, new TypeToken<Map<String, User>>(){}.getType());
                if (loaded != null) {
                    users.putAll(loaded);
                    LOGGER.info("已加载 {} 个用户", users.size());
                }
            }
        } catch (Exception e) {
            LOGGER.error("加载用户数据失败", e);
        }
    }
    
    public void saveUsers() {
        try {
            Files.createDirectories(usersFile.getParent());
            Files.writeString(usersFile, GSON.toJson(users), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.error("保存用户数据失败", e);
        }
    }
    
    // === Token 生成 ===
    
    private String generateToken(String username) {
        String payload = username + ":" + System.currentTimeMillis() + ":" + UUID.randomUUID();
        return base64Encode(payload) + "." + sign(payload);
    }
    
    private String sign(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((data + tokenSecret).getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash).substring(0, 32);
        } catch (Exception e) {
            return "";
        }
    }
    
    private static String hashPassword(String password) {
        if (password == null) {
            password = "";
        }
        try {
            byte[] salt = new byte[PASSWORD_SALT_BYTES];
            SECURE_RANDOM.nextBytes(salt);
            byte[] digest = pbkdf2(password.toCharArray(), salt, PASSWORD_ITERATIONS, PASSWORD_KEY_BITS);
            return PASSWORD_HASH_PREFIX
                    + PASSWORD_ITERATIONS + "$"
                    + Base64.getEncoder().encodeToString(salt) + "$"
                    + Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            LOGGER.error("PBKDF2 密码哈希失败，回退 legacy SHA-256", e);
            return hashPasswordLegacy(password);
        }
    }

    private static boolean verifyPassword(String plainPassword, String storedHash) {
        if (storedHash == null || storedHash.isBlank()) {
            return false;
        }
        if (isModernPasswordHash(storedHash)) {
            try {
                String[] parts = storedHash.split("\\$");
                if (parts.length != 4) return false;
                int iterations = Integer.parseInt(parts[1]);
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] expected = Base64.getDecoder().decode(parts[3]);
                byte[] actual = pbkdf2((plainPassword == null ? "" : plainPassword).toCharArray(), salt, iterations, expected.length * 8);
                return MessageDigest.isEqual(expected, actual);
            } catch (Exception e) {
                LOGGER.warn("校验 PBKDF2 密码失败: {}", e.getMessage());
                return false;
            }
        }
        return secureEquals(hashPasswordLegacy(plainPassword == null ? "" : plainPassword), storedHash);
    }

    private static boolean isModernPasswordHash(String storedHash) {
        return storedHash != null && storedHash.startsWith(PASSWORD_HASH_PREFIX);
    }

    private static String hashPasswordLegacy(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            return password;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyBits) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyBits);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PASSWORD_KDF);
            return skf.generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }
    
    private static String base64Encode(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }
    
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void invalidateTokensByUsername(String username) {
        if (username == null || username.isBlank()) return;

        List<String> localTokens = new ArrayList<>();
        for (Map.Entry<String, TokenInfo> entry : validTokens.entrySet()) {
            TokenInfo info = entry.getValue();
            if (info != null && username.equals(info.username)) {
                localTokens.add(entry.getKey());
            }
        }
        localTokens.forEach(validTokens::remove);

        var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
        if (redis.isEnabled()) {
            redis.execute(jedis -> {
                Map<String, String> all = jedis.hgetAll(REDIS_KEY_TOKENS);
                if (all == null || all.isEmpty()) return null;
                List<String> toDelete = new ArrayList<>();
                all.forEach((token, json) -> {
                    try {
                        TokenInfo info = GSON.fromJson(json, TokenInfo.class);
                        if (info != null && username.equals(info.username)) {
                            toDelete.add(token);
                        }
                    } catch (Exception ignored) {}
                });
                if (!toDelete.isEmpty()) {
                    jedis.hdel(REDIS_KEY_TOKENS, toDelete.toArray(new String[0]));
                }
                return null;
            });
        }
    }

    private TokenInfo getTokenInfo(String token) {
        if (token == null || token.isBlank()) return null;
        TokenInfo info = validTokens.get(token);

        var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
        if (info == null && redis.isEnabled()) {
            String json = redis.execute(jedis -> jedis.hget(REDIS_KEY_TOKENS, token));
            if (json != null && !json.isEmpty()) {
                info = GSON.fromJson(json, TokenInfo.class);
                if (info != null) {
                    validTokens.put(token, info);
                }
            }
        }

        if (info == null) return null;

        if (System.currentTimeMillis() > info.expireAt) {
            validTokens.remove(token);
            if (redis.isEnabled()) {
                redis.execute(jedis -> jedis.hdel(REDIS_KEY_TOKENS, token));
            }
            return null;
        }
        return info;
    }

    private static boolean isStrongPassword(String password) {
        if (password == null) return false;
        String p = password.trim();
        if (p.length() < 10) return false;
        return !WEAK_PASSWORDS.contains(p.toLowerCase(Locale.ROOT));
    }

    private static boolean isWeakBridgeToken(String token) {
        String normalized = token.toLowerCase(Locale.ROOT);
        return normalized.length() < 16 || WEAK_BRIDGE_TOKENS.contains(normalized);
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String v = value.trim();
        return v.isEmpty() ? null : v;
    }

    private static String generateSecureToken(int bytes) {
        byte[] random = new byte[Math.max(bytes, 16)];
        SECURE_RANDOM.nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
    }

    private static Set<String> parseCsvSet(String csv) {
        if (csv == null || csv.isBlank()) {
            return Collections.emptySet();
        }
        Set<String> set = new HashSet<>();
        for (String entry : csv.split(",")) {
            String serverId = entry.trim();
            if (isValidServerId(serverId)) {
                set.add(serverId);
            }
        }
        return set;
    }

    private static boolean isValidServerId(String serverId) {
        if (serverId == null || serverId.isBlank()) return false;
        return serverId.matches("[A-Za-z0-9._-]{1,64}");
    }

    private static boolean secureEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }
    
    // === 数据类 ===
    
    public enum Role {
        VIEWER,     // 只读
        OPERATOR,   // 操作员 (可执行命令)
        ADMIN       // 管理员 (完全控制)
    }
    
    public static class User {
        public String username;
        public String passwordHash;
        public Role role;
        
        public User() {}
        
        public User(String username, String passwordHash, Role role) {
            this.username = username;
            this.passwordHash = passwordHash;
            this.role = role;
        }
    }
    
    public static class UserInfo {
        public String username;
        public Role role;
        
        public UserInfo(String username, Role role) {
            this.username = username;
            this.role = role;
        }
    }
    
    private static class TokenInfo {
        final String username;
        final Role role;
        final long expireAt;
        
        TokenInfo(String username, Role role, long expireAt) {
            this.username = username;
            this.role = role;
            this.expireAt = expireAt;
        }
    }

    private record BridgeAuthConfig(boolean enabled, String sharedToken, Set<String> allowedServerIds) {
        private static BridgeAuthConfig disabled() {
            return new BridgeAuthConfig(false, "", Collections.emptySet());
        }
    }

    private record BootstrapAdminConfig(boolean enabled, String username, String password, Role role) {
        private static BootstrapAdminConfig disabled() {
            return new BootstrapAdminConfig(false, "admin", "", Role.ADMIN);
        }
    }
}
