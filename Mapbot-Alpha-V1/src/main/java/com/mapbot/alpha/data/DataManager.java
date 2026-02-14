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
    // UUID -> 玩家名
    private final ConcurrentHashMap<String, String> playerNames = new ConcurrentHashMap<>();
    // UUID -> 禁言到期时间 (-1=永久, 0=未禁言)
    private final ConcurrentHashMap<String, Long> mutes = new ConcurrentHashMap<>();
    // QQ -> 权限等级 (0=普通, 1=VIP, 2=OP)
    private final ConcurrentHashMap<Long, Integer> permissions = new ConcurrentHashMap<>();
    // 管理员 QQ 列表
    private final Set<Long> admins = ConcurrentHashMap.newKeySet();
    
    private static final String REDIS_SYNC_CHANNEL = "mapbot:sync";
    private static final String REDIS_KEY_BINDINGS = "mapbot:bindings";
    private static final String REDIS_KEY_PLAYER_NAMES = "mapbot:player_names";
    private static final String REDIS_KEY_MUTES = "mapbot:mutes";
    private static final String REDIS_KEY_PERMS = "mapbot:permissions";
    private static final String REDIS_KEY_ADMINS = "mapbot:admins";

    private DataManager() {}
    
    public void init() {
        try {
            Files.createDirectories(dataDir);
            
            // 1. 先从本地加载 (作为备用或初始状态)
            loadBindings();
            loadPlayerNames();
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

            LOGGER.info("数据管理器初始化完成: {} 绑定, {} 玩家名, {} 禁言, {} 管理员", 
                bindings.size(), playerNames.size(), mutes.size(), admins.size());
        } catch (Exception e) {
            LOGGER.error("数据管理器初始化失败", e);
        }
    }

    private void syncFromRedis() {
        var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
        redis.execute(jedis -> {
            // 先构造快照，再整体替换，避免“只 put 不 clear”导致脏数据残留
            Map<Long, String> newBindings = new HashMap<>();
            Map<String, String> newPlayerNames = new HashMap<>();
            Map<String, Long> newMutes = new HashMap<>();
            Map<Long, Integer> newPermissions = new HashMap<>();
            Set<Long> newAdmins = new HashSet<>();

            Map<String, String> b = jedis.hgetAll(REDIS_KEY_BINDINGS);
            b.forEach((k, v) -> {
                try {
                    newBindings.put(Long.parseLong(k), v);
                } catch (Exception e) {
                    LOGGER.warn("Redis 绑定数据解析失败: {}={}", k, v);
                }
            });

            Map<String, String> pn = jedis.hgetAll(REDIS_KEY_PLAYER_NAMES);
            pn.forEach((uuid, name) -> {
                if (uuid != null && !uuid.isBlank() && name != null && !name.isBlank()) {
                    newPlayerNames.put(uuid.trim(), name.trim());
                }
            });

            Map<String, String> m = jedis.hgetAll(REDIS_KEY_MUTES);
            m.forEach((k, v) -> {
                try {
                    newMutes.put(k, Long.parseLong(v));
                } catch (Exception e) {
                    LOGGER.warn("Redis 禁言数据解析失败: {}={}", k, v);
                }
            });

            Map<String, String> p = jedis.hgetAll(REDIS_KEY_PERMS);
            p.forEach((k, v) -> {
                try {
                    newPermissions.put(Long.parseLong(k), Integer.parseInt(v));
                } catch (Exception e) {
                    LOGGER.warn("Redis 权限数据解析失败: {}={}", k, v);
                }
            });

            Set<String> a = jedis.smembers(REDIS_KEY_ADMINS);
            a.forEach(k -> {
                try {
                    newAdmins.add(Long.parseLong(k));
                } catch (Exception e) {
                    LOGGER.warn("Redis 管理员数据解析失败: {}", k);
                }
            });

            bindings.clear();
            bindings.putAll(newBindings);
            playerNames.clear();
            playerNames.putAll(newPlayerNames);
            mutes.clear();
            mutes.putAll(newMutes);
            permissions.clear();
            permissions.putAll(newPermissions);
            admins.clear();
            admins.addAll(newAdmins);

            LOGGER.info("Redis 全量同步完成: bindings={}, names={}, mutes={}, perms={}, admins={}",
                bindings.size(), playerNames.size(), mutes.size(), permissions.size(), admins.size());
            
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
        return bind(qq, uuid, null);
    }

    public synchronized boolean bind(long qq, String uuid, String playerName) {
        if (uuid == null || uuid.isBlank()) return false;
        final String normalizedUuid = uuid.trim();
        final String normalizedName = normalizePlayerName(playerName);
        // 检查 QQ 是否已绑定
        if (bindings.containsKey(qq)) return false;
        // 检查 UUID 是否已被其他 QQ 绑定
        if (isUUIDBound(normalizedUuid)) return false;
        
        bindings.put(qq, normalizedUuid);
        if (normalizedName != null) {
            playerNames.put(normalizedUuid, normalizedName);
        }
        saveBindings();
        savePlayerNames();
        
        var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
        if (redis.isEnabled()) {
            final String nameToWrite = normalizedName;
            redis.execute(jedis -> {
                jedis.hset(REDIS_KEY_BINDINGS, String.valueOf(qq), normalizedUuid);
                if (nameToWrite != null) {
                    jedis.hset(REDIS_KEY_PLAYER_NAMES, normalizedUuid, nameToWrite);
                }
                return null;
            });
            broadcastSync();
        }
        return true;
    }

    /**
     * 更新指定 QQ 的绑定 UUID（用于 UUID 自愈刷新）
     */
    public synchronized boolean updateBinding(long qq, String newUuid) {
        if (newUuid == null || newUuid.isBlank()) return false;
        final String normalizedUuid = newUuid.trim();

        String oldUuid = bindings.get(qq);
        if (oldUuid == null || oldUuid.isBlank()) return false;
        if (oldUuid.equalsIgnoreCase(normalizedUuid)) return true;

        Long occupier = getQQByUUID(normalizedUuid);
        if (occupier != null && occupier != qq) {
            return false;
        }

        bindings.put(qq, normalizedUuid);
        if (!isUUIDStillBoundByOthers(oldUuid, qq)) {
            playerNames.remove(oldUuid);
        }
        saveBindings();
        savePlayerNames();

        var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
        if (redis.isEnabled()) {
            final String oldUuidFinal = oldUuid;
            redis.execute(jedis -> {
                jedis.hset(REDIS_KEY_BINDINGS, String.valueOf(qq), normalizedUuid);
                if (oldUuidFinal != null && !oldUuidFinal.isBlank() && !isUUIDBound(oldUuidFinal)) {
                    jedis.hdel(REDIS_KEY_PLAYER_NAMES, oldUuidFinal);
                }
                return null;
            });
            broadcastSync();
        }
        return true;
    }
    
    public synchronized boolean unbind(long qq) {
        String removedUuid = bindings.remove(qq);
        if (removedUuid != null) {
            boolean keepName = isUUIDBound(removedUuid);
            if (!keepName) {
                playerNames.remove(removedUuid);
            }
            saveBindings();
            savePlayerNames();
            
            var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
            if (redis.isEnabled()) {
                final boolean removeNameFromRedis = !keepName;
                redis.execute(jedis -> {
                    jedis.hdel(REDIS_KEY_BINDINGS, String.valueOf(qq));
                    if (removeNameFromRedis) {
                        jedis.hdel(REDIS_KEY_PLAYER_NAMES, removedUuid);
                    }
                    return null;
                });
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

    public String getPlayerName(String uuid) {
        if (uuid == null || uuid.isBlank()) return null;
        return playerNames.get(uuid.trim());
    }

    public void updatePlayerName(String uuid, String playerName) {
        if (uuid == null || uuid.isBlank()) return;
        String normalizedName = normalizePlayerName(playerName);
        if (normalizedName == null) return;
        String normalizedUuid = uuid.trim();
        String old = playerNames.get(normalizedUuid);
        if (normalizedName.equals(old)) {
            return;
        }
        playerNames.put(normalizedUuid, normalizedName);
        savePlayerNames();
        var redis = com.mapbot.alpha.database.RedisManager.INSTANCE;
        if (redis.isEnabled()) {
            redis.execute(jedis -> jedis.hset(REDIS_KEY_PLAYER_NAMES, normalizedUuid, normalizedName));
            broadcastSync();
        }
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
     * 获取全部玩家名快照 (UUID -> 玩家名)
     */
    public Map<String, String> getAllPlayerNames() {
        return Map.copyOf(playerNames);
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
                try {
                    bindings.put(Long.parseLong(parts[0].trim()), parts[1]);
                } catch (NumberFormatException e) {
                    LOGGER.warn("忽略无效绑定数据: {}", line);
                }
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
                try {
                    mutes.put(parts[0], Long.parseLong(parts[1].trim()));
                } catch (NumberFormatException e) {
                    LOGGER.warn("忽略无效禁言数据: {}", line);
                }
            }
        }
    }

    private void loadPlayerNames() throws IOException {
        Path file = dataDir.resolve("player_names.txt");
        if (!Files.exists(file)) return;
        for (String line : Files.readAllLines(file)) {
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                String uuid = parts[0].trim();
                String name = normalizePlayerName(parts[1]);
                if (!uuid.isEmpty() && name != null) {
                    playerNames.put(uuid, name);
                }
            }
        }
    }

    private void savePlayerNames() {
        try {
            List<String> lines = new ArrayList<>();
            playerNames.forEach((uuid, name) -> lines.add(uuid + "=" + name));
            Files.write(dataDir.resolve("player_names.txt"), lines);
        } catch (Exception e) {
            LOGGER.error("保存玩家名数据失败", e);
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
                try {
                    permissions.put(Long.parseLong(parts[0].trim()), Integer.parseInt(parts[1].trim()));
                } catch (NumberFormatException e) {
                    LOGGER.warn("忽略无效权限数据: {}", line);
                }
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
                try {
                    admins.add(Long.parseLong(line.trim()));
                } catch (NumberFormatException e) {
                    LOGGER.warn("忽略无效管理员数据: {}", line);
                }
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

    private String normalizePlayerName(String name) {
        if (name == null) return null;
        String normalized = name.trim();
        if (normalized.isEmpty()) return null;
        if (!normalized.matches("^[A-Za-z0-9_]{3,16}$")) return null;
        return normalized;
    }

    private boolean isUUIDStillBoundByOthers(String uuid, long currentQq) {
        if (uuid == null || uuid.isBlank()) return false;
        for (Map.Entry<Long, String> e : bindings.entrySet()) {
            if (e.getKey() == currentQq) continue;
            if (uuid.equalsIgnoreCase(e.getValue())) return true;
        }
        return false;
    }
}
