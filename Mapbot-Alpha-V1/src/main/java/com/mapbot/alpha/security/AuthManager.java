package com.mapbot.alpha.security;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final String SECRET_KEY = "MapBot-Alpha-Secret-2026";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // 用户存储文件
    private Path usersFile = Paths.get("config", "users.json");
    
    // 用户数据库
    private final Map<String, User> users = new ConcurrentHashMap<>();
    
    // 已颁发的有效 Token
    private final Map<String, TokenInfo> validTokens = new ConcurrentHashMap<>();
    
    /**
     * 初始化
     */
    public void init() {
        loadUsers();
        if (users.isEmpty()) {
            // 创建默认管理员
            createUser("admin", "admin123", Role.ADMIN);
            LOGGER.info("已创建默认管理员账户: admin / admin123");
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
        
        if (!user.passwordHash.equals(hashPassword(password))) {
            LOGGER.warn("登录失败: 密码错误 {}", username);
            return null;
        }
        
        String token = generateToken(username);
        validTokens.put(token, new TokenInfo(username, user.role, System.currentTimeMillis() + TOKEN_EXPIRE_MS));
        LOGGER.info("用户登录成功: {} (角色: {})", username, user.role);
        return token;
    }
    
    /**
     * 验证 Token
     */
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) return false;
        
        TokenInfo info = validTokens.get(token);
        if (info == null) return false;
        
        if (System.currentTimeMillis() > info.expireAt) {
            validTokens.remove(token);
            return false;
        }
        
        return true;
    }
    
    /**
     * 验证 Token 并检查权限
     */
    public boolean hasPermission(String token, Role requiredRole) {
        TokenInfo info = validTokens.get(token);
        if (info == null) return false;
        return info.role.ordinal() >= requiredRole.ordinal();
    }
    
    /**
     * 登出
     */
    public void logout(String token) {
        validTokens.remove(token);
    }
    
    /**
     * 获取 Token 对应的用户名
     */
    public String getUsername(String token) {
        TokenInfo info = validTokens.get(token);
        return info != null ? info.username : null;
    }
    
    /**
     * 获取 Token 对应的角色
     */
    public Role getRole(String token) {
        TokenInfo info = validTokens.get(token);
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
        users.put(username, new User(username, hashPassword(password), role));
        saveUsers();
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
            saveUsers();
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
        
        user.passwordHash = hashPassword(newPassword);
        saveUsers();
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
            byte[] hash = md.digest((data + SECRET_KEY).getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash).substring(0, 32);
        } catch (Exception e) {
            return "";
        }
    }
    
    private static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            return password;
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
}
