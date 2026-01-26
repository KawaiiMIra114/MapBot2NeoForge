package com.mapbot.alpha.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Alpha Core 数据管理器
 * 从 Reforged DataManager 移植
 * 管理: QQ-MC绑定、禁言、权限等级
 */
public class DataManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Data");
    public static final DataManager INSTANCE = new DataManager();
    
    // 权限等级常量
    public static final int PERMISSION_LEVEL_USER = 0;
    public static final int PERMISSION_LEVEL_VIP = 1;
    public static final int PERMISSION_LEVEL_ADMIN = 2;
    
    private final Path dataDir = Paths.get("data");
    
    // QQ -> UUID 绑定
    private final ConcurrentHashMap<Long, String> bindings = new ConcurrentHashMap<>();
    // UUID -> 禁言到期时间 (-1=永久, 0=未禁言)
    private final ConcurrentHashMap<String, Long> mutes = new ConcurrentHashMap<>();
    // QQ -> 权限等级 (0=普通, 1=VIP, 2=OP)
    private final ConcurrentHashMap<Long, Integer> permissions = new ConcurrentHashMap<>();
    // 管理员 QQ 列表
    private final Set<Long> admins = ConcurrentHashMap.newKeySet();
    
    private static final String REDIS_SYNC_CHANNEL = "mapbot:sync";
    private static final String REDIS_KEY_BINDINGS = "mapbot:bindings";
    private static final String REDIS_KEY_MUTES = "mapbot:mutes";
    private static final String REDIS_KEY_PERMS = "mapbot:permissions";
    private static final String REDIS_KEY_ADMINS = "mapbot:admins";

    private DataManager() {}
    
    public void init() {
        try {
            Files.createDirectories(dataDir);
            
            // 1. 先从本地加载 (作为备用或初始状态)
            loadBindings();
            loadMutes();
            loadPermissions();
            loadAdmins();

            // 2. 如果开启了 Redis，则从 Redis 同步并启动订阅
            var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
            if (redis.isEnabled()) {
                syncFromRedis();
                redis.subscribe(REDIS_SYNC_CHANNEL, this::handleRedisSync);
                LOGGER.info("Redis 数据同步已开启");
            }

            LOGGER.info("数据管理器初始化完成: {} 绑定, {} 禁言, {} 管理员", 
                bindings.size(), mutes.size(), admins.size());
        } catch (Exception e) {
            LOGGER.error("数据管理器初始化失败", e);
        }
    }

    private void syncFromRedis() {
        var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
        redis.execute(jedis -> {
            // 同步绑定
            Map<String, String> b = jedis.hgetAll(REDIS_KEY_BINDINGS);
            b.forEach((k, v) -> bindings.put(Long.parseLong(k), v));

            // 同步禁言
            Map<String, String> m = jedis.hgetAll(REDIS_KEY_MUTES);
            m.forEach((k, v) -> mutes.put(k, Long.parseLong(v)));

            // 同步权限
            Map<String, String> p = jedis.hgetAll(REDIS_KEY_PERMS);
            p.forEach((k, v) -> permissions.put(Long.parseLong(k), Integer.parseInt(v)));

            // 同步管理员
            Set<String> a = jedis.smembers(REDIS_KEY_ADMINS);
            a.forEach(k -> admins.add(Long.parseLong(k)));
            
            return null;
        });
    }

    private void handleRedisSync(String message) {
        // 收到同步指令，重新从 Redis 加载数据 (简单实现：全量刷新)
        // 或者是解析消息只更新变动项
        LOGGER.debug("收到 Redis 同步请求: {}", message);
        syncFromRedis();
    }

    private void broadcastSync() {
        var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
        if (redis.isEnabled()) {
            redis.publish(REDIS_SYNC_CHANNEL, "refresh");
        }
    }
    
    // ==================== 绑定 ====================
    
    public boolean bind(long qq, String uuid) {
        // 检查 QQ 是否已绑定
        if (bindings.containsKey(qq)) return false;
        // 检查 UUID 是否已被其他 QQ 绑定
        if (isUUIDBound(uuid)) return false;
        
        bindings.put(qq, uuid);
        saveBindings();
        
        var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
        if (redis.isEnabled()) {
            redis.execute(jedis -> jedis.hset(REDIS_KEY_BINDINGS, String.valueOf(qq), uuid));
            broadcastSync();
        }
        return true;
    }
    
    public boolean unbind(long qq) {
        if (bindings.remove(qq) != null) {
            saveBindings();
            
            var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
            if (redis.isEnabled()) {
                redis.execute(jedis -> jedis.hdel(REDIS_KEY_BINDINGS, String.valueOf(qq)));
                broadcastSync();
            }
            return true;
        }
        return false;
    }
    
    public String getBinding(long qq) {
        return bindings.get(qq);
    }
    
    public Long getQQByUUID(String uuid) {
        for (Map.Entry<Long, String> e : bindings.entrySet()) {
            if (e.getValue().equals(uuid)) return e.getKey();
        }
        return null;
    }
    
    public boolean isBound(long qq) {
        return bindings.containsKey(qq);
    }
    
    public boolean isUUIDBound(String uuid) {
        return bindings.containsValue(uuid);
    }
    
    // ==================== 禁言 ====================
    
    public void mute(String uuid, long expiryMs) {
        mutes.put(uuid, expiryMs);
        saveMutes();
        
        var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
        if (redis.isEnabled()) {
            redis.execute(jedis -> jedis.hset(REDIS_KEY_MUTES, uuid, String.valueOf(expiryMs)));
            broadcastSync();
        }
    }
    
    public void unmute(String uuid) {
        mutes.remove(uuid);
        saveMutes();
        
        var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
        if (redis.isEnabled()) {
            redis.execute(jedis -> jedis.hdel(REDIS_KEY_MUTES, uuid));
            broadcastSync();
        }
    }
    
    public boolean isMuted(String uuid) {
        Long expiry = mutes.get(uuid);
        if (expiry == null) return false;
        if (expiry == -1) return true; // 永久
        if (System.currentTimeMillis() > expiry) {
            mutes.remove(uuid);
            saveMutes();
            return false;
        }
        return true;
    }
    
    public long getMuteExpiry(String uuid) {
        return mutes.getOrDefault(uuid, 0L);
    }
    
    // ==================== 权限 ====================
    
    public void setPermission(long qq, int level) {
        permissions.put(qq, level);
        savePermissions();
        
        var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
        if (redis.isEnabled()) {
            redis.execute(jedis -> jedis.hset(REDIS_KEY_PERMS, String.valueOf(qq), String.valueOf(level)));
            broadcastSync();
        }
    }
    
    public int getPermission(long qq) {
        return permissions.getOrDefault(qq, 0);
    }
    
    // ==================== 管理员 ====================
    
    public void addAdmin(long qq) {
        admins.add(qq);
        saveAdmins();
        
        var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
        if (redis.isEnabled()) {
            redis.execute(jedis -> jedis.sadd(REDIS_KEY_ADMINS, String.valueOf(qq)));
            broadcastSync();
        }
    }
    
    public void removeAdmin(long qq) {
        admins.remove(qq);
        saveAdmins();
        
        var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
        if (redis.isEnabled()) {
            redis.execute(jedis -> jedis.srem(REDIS_KEY_ADMINS, String.valueOf(qq)));
            broadcastSync();
        }
    }
    
    public boolean isAdmin(long qq) {
        return admins.contains(qq);
    }
    
    public Set<Long> getAdmins() {
        return Collections.unmodifiableSet(admins);
    }

    /**
     * 获取全部绑定数据快照 (QQ -> UUID)
     */
    public Map<Long, String> getAllBindings() {
        return Map.copyOf(bindings);
    }

    /**
     * 获取全部权限数据快照 (QQ -> Level)
     */
    public Map<Long, Integer> getAllPermissions() {
        return Map.copyOf(permissions);
    }

    /**
     * 获取全部禁言数据快照 (UUID -> expiry)
     */
    public Map<String, Long> getAllMutes() {
        return Map.copyOf(mutes);
    }
    
    // ==================== 持久化 ====================
    
    private void loadBindings() throws IOException {
        Path file = dataDir.resolve("bindings.txt");
        if (!Files.exists(file)) return;
        for (String line : Files.readAllLines(file)) {
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                bindings.put(Long.parseLong(parts[0]), parts[1]);
            }
        }
    }
    
    private void saveBindings() {
        try {
            List<String> lines = new ArrayList<>();
            bindings.forEach((qq, uuid) -> lines.add(qq + "=" + uuid));
            Files.write(dataDir.resolve("bindings.txt"), lines);
        } catch (Exception e) {
            LOGGER.error("保存绑定数据失败", e);
        }
    }
    
    private void loadMutes() throws IOException {
        Path file = dataDir.resolve("mutes.txt");
        if (!Files.exists(file)) return;
        for (String line : Files.readAllLines(file)) {
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                mutes.put(parts[0], Long.parseLong(parts[1]));
            }
        }
    }
    
    private void saveMutes() {
        try {
            List<String> lines = new ArrayList<>();
            mutes.forEach((uuid, exp) -> lines.add(uuid + "=" + exp));
            Files.write(dataDir.resolve("mutes.txt"), lines);
        } catch (Exception e) {
            LOGGER.error("保存禁言数据失败", e);
        }
    }
    
    private void loadPermissions() throws IOException {
        Path file = dataDir.resolve("permissions.txt");
        if (!Files.exists(file)) return;
        for (String line : Files.readAllLines(file)) {
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                permissions.put(Long.parseLong(parts[0]), Integer.parseInt(parts[1]));
            }
        }
    }
    
    private void savePermissions() {
        try {
            List<String> lines = new ArrayList<>();
            permissions.forEach((qq, lvl) -> lines.add(qq + "=" + lvl));
            Files.write(dataDir.resolve("permissions.txt"), lines);
        } catch (Exception e) {
            LOGGER.error("保存权限数据失败", e);
        }
    }
    
    private void loadAdmins() throws IOException {
        Path file = dataDir.resolve("admins.txt");
        if (!Files.exists(file)) return;
        for (String line : Files.readAllLines(file)) {
            if (!line.trim().isEmpty()) {
                admins.add(Long.parseLong(line.trim()));
            }
        }
    }
    
    private void saveAdmins() {
        try {
            List<String> lines = new ArrayList<>();
            admins.forEach(qq -> lines.add(String.valueOf(qq)));
            Files.write(dataDir.resolve("admins.txt"), lines);
        } catch (Exception e) {
            LOGGER.error("保存管理员数据失败", e);
        }
    }
}
