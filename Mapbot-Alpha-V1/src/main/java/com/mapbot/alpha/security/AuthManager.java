package com.mapbot.alpha.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JWT 认证管理器
 * 简化实现: 使用 HMAC-SHA256 签名
 */
public enum AuthManager {
    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthManager.class);
    private static final long TOKEN_EXPIRE_MS = 24 * 60 * 60 * 1000; // 24 小时
    private static final String SECRET_KEY = "MapBot-Alpha-Secret-" + System.currentTimeMillis();
    
    // 已颁发的有效 Token (简化实现，生产环境应使用 Redis)
    private final Map<String, TokenInfo> validTokens = new ConcurrentHashMap<>();
    
    // 默认用户 (后续可从配置文件读取)
    private String adminUsername = "admin";
    private String adminPasswordHash = hashPassword("admin123");
    
    /**
     * 用户登录
     * @return JWT Token, 失败返回 null
     */
    public String login(String username, String password) {
        if (!adminUsername.equals(username)) {
            LOGGER.warn("登录失败: 用户不存在 {}", username);
            return null;
        }
        
        if (!adminPasswordHash.equals(hashPassword(password))) {
            LOGGER.warn("登录失败: 密码错误 {}", username);
            return null;
        }
        
        String token = generateToken(username);
        validTokens.put(token, new TokenInfo(username, System.currentTimeMillis() + TOKEN_EXPIRE_MS));
        LOGGER.info("用户登录成功: {}", username);
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
     * 生成 CSRF Token
     */
    public String generateCsrfToken() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * 设置管理员凭据 (从配置加载)
     */
    public void setCredentials(String username, String password) {
        this.adminUsername = username;
        this.adminPasswordHash = hashPassword(password);
        LOGGER.info("管理员凭据已更新");
    }
    
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
    
    private static class TokenInfo {
        final String username;
        final long expireAt;
        
        TokenInfo(String username, long expireAt) {
            this.username = username;
            this.expireAt = expireAt;
        }
    }
}
