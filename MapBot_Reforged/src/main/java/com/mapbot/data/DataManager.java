/*
 * MapBot Reforged - 数据管理器
 * 
 * 遵从 .ai_rules.md 中定义的治理规则。
 * 使用 Gson 实现简单的 JSON 文件持久化。
 * 
 * 功能:
 * - 管理员 QQ 列表管理
 * - 玩家绑定 (QQ → UUID) 管理
 * 
 * 存储路径: config/mapbot_data.json
 */

package com.mapbot.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 数据管理器 (单例)
 * 负责管理管理员列表和玩家绑定的持久化存储
 */
public class DataManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/Data");
    
    /** 单例实例 */
    public static final DataManager INSTANCE = new DataManager();
    
    /** 数据文件路径 */
    private static final String DATA_FILE_NAME = "mapbot_data.json";
    
    /** Gson 实例 (美化输出) */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /** 权限等级常量 */
    public static final int PERMISSION_LEVEL_USER = 0;
    public static final int PERMISSION_LEVEL_MOD = 1;
    public static final int PERMISSION_LEVEL_ADMIN = 2;
    
    /** 读写锁，保证线程安全 */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    /** 数据模型 */
    private DataModel data;
    
    /** 数据文件路径 */
    private Path dataPath;
    
    private DataManager() {
        this.data = new DataModel();
    }
    
    /**
     * 初始化数据管理器
     * 应在 FMLCommonSetupEvent 或服务器启动时调用
     */
    public void init() {
        try {
            // 获取配置目录路径
            Path configDir = FMLPaths.CONFIGDIR.get();
            this.dataPath = configDir.resolve(DATA_FILE_NAME);
            
            LOGGER.info("数据文件路径: {}", dataPath);
            
            // 尝试加载现有数据
            if (Files.exists(dataPath)) {
                load();
            } else {
                // 创建默认数据文件
                save();
                LOGGER.info("已创建默认数据文件");
            }
        } catch (Exception e) {
            LOGGER.error("初始化数据管理器失败: {}", e.getMessage());
        }
    }
    
    /**
     * 从文件加载数据
     * 包含数据迁移逻辑 (旧版 admins -> 新版 permissions)
     */
    private void load() {
        lock.writeLock().lock();
        try {
            String json = Files.readString(dataPath);
            DataModel loaded = GSON.fromJson(json, DataModel.class);
            
            if (loaded != null) {
                this.data = loaded;
                
                // 数据迁移: 将旧版 admins 列表迁移到权限系统
                if (!data.admins.isEmpty() && data.userPermissions.isEmpty()) {
                    LOGGER.info("正在迁移旧版管理员数据 ({} 个)...", data.admins.size());
                    for (Long adminQQ : data.admins) {
                        data.userPermissions.put(adminQQ, PERMISSION_LEVEL_ADMIN);
                    }
                    data.admins.clear(); // 清空旧列表
                    save(); // 保存迁移结果
                    LOGGER.info("数据迁移完成");
                }
                
                LOGGER.info("数据加载成功: {} 权限记录, {} 绑定, {} 禁言", 
                    data.userPermissions.size(), data.playerBindings.size(), data.mutedPlayers.size());
            }
        } catch (IOException e) {
            LOGGER.error("读取数据文件失败: {}", e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 保存数据到文件
     */
    public void save() {
        // Fix #4: 使用 writeLock 代替 readLock，因为文件写入是写操作
        lock.writeLock().lock();
        try {
            String json = GSON.toJson(data);
            Files.writeString(dataPath, json);
            LOGGER.debug("数据已保存");
        } catch (IOException e) {
            LOGGER.error("保存数据文件失败: {}", e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    // ================== 权限管理 ==================
    
    /**
     * 获取用户权限等级
     * 
     * @param qq QQ 号
     * @return 权限等级 (0=User, 1=Mod, 2=Admin)
     */
    public int getPermissionLevel(long qq) {
        lock.readLock().lock();
        try {
            return data.userPermissions.getOrDefault(qq, PERMISSION_LEVEL_USER);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 设置用户权限等级
     * 
     * @param qq QQ 号
     * @param level 权限等级
     */
    public void setPermissionLevel(long qq, int level) {
        lock.writeLock().lock();
        try {
            if (level == PERMISSION_LEVEL_USER) {
                data.userPermissions.remove(qq); // User 等级无需存储
            } else {
                data.userPermissions.put(qq, level);
            }
            save();
            LOGGER.info("设置权限: QQ {} -> Level {}", qq, level);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 检查是否为管理员 (Level >= 2)
     * 兼容旧版接口
     */
    public boolean isAdmin(long qq) {
        return getPermissionLevel(qq) >= PERMISSION_LEVEL_ADMIN;
    }
    
    /**
     * 检查是否为协管员 (Level >= 1)
     */
    public boolean isModerator(long qq) {
        return getPermissionLevel(qq) >= PERMISSION_LEVEL_MOD;
    }
    
    /**
     * 添加管理员 (兼容旧版接口)
     */
    public boolean addAdmin(long qq) {
        if (isAdmin(qq)) return false;
        setPermissionLevel(qq, PERMISSION_LEVEL_ADMIN);
        return true;
    }
    
    /**
     * 移除管理员 (降级为普通用户)
     */
    public boolean removeAdmin(long qq) {
        if (getPermissionLevel(qq) == PERMISSION_LEVEL_USER) return false;
        setPermissionLevel(qq, PERMISSION_LEVEL_USER);
        return true;
    }
    
    /**
     * 获取管理员列表 (兼容旧版接口)
     * 返回所有 Level >= 2 的用户
     */
    public List<Long> getAdmins() {
        lock.readLock().lock();
        try {
            List<Long> admins = new ArrayList<>();
            for (Map.Entry<Long, Integer> entry : data.userPermissions.entrySet()) {
                if (entry.getValue() >= PERMISSION_LEVEL_ADMIN) {
                    admins.add(entry.getKey());
                }
            }
            return admins;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // ================== 禁言管理 ==================
    
    /**
     * 禁言玩家
     * 
     * @param uuid 玩家 UUID
     * @param durationMillis 禁言时长 (毫秒)，-1 表示永久
     */
    public void mute(String uuid, long durationMillis) {
        lock.writeLock().lock();
        try {
            long expiry = (durationMillis == -1) ? -1 : System.currentTimeMillis() + durationMillis;
            data.mutedPlayers.put(uuid, expiry);
            save();
            LOGGER.info("玩家被禁言: {} (到期: {})", uuid, expiry);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 解除禁言
     * 
     * @param uuid 玩家 UUID
     * @return 是否成功解除 (false 表示未被禁言)
     */
    public boolean unmute(String uuid) {
        lock.writeLock().lock();
        try {
            if (data.mutedPlayers.remove(uuid) != null) {
                save();
                LOGGER.info("玩家解除禁言: {}", uuid);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 检查是否被禁言
     * 
     * @param uuid 玩家 UUID
     * @return 是否处于禁言状态
     */
    public boolean isMuted(String uuid) {
        lock.readLock().lock();
        try {
            if (!data.mutedPlayers.containsKey(uuid)) {
                return false;
            }
            
            long expiry = data.mutedPlayers.get(uuid);
            // -1 为永久禁言
            if (expiry == -1) {
                return true;
            }
            
            // 检查是否过期
            if (System.currentTimeMillis() > expiry) {
                // Fix #5: 已过期，异步清理过期数据
                cleanExpiredMute(uuid);
                return false;
            }
            
            return true;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 异步清理过期禁言 (Fix #5)
     * 在读锁中发现过期后，使用写锁异步清理
     */
    private void cleanExpiredMute(String uuid) {
        new Thread(() -> {
            lock.writeLock().lock();
            try {
                Long expiry = data.mutedPlayers.get(uuid);
                if (expiry != null && expiry != -1 && System.currentTimeMillis() > expiry) {
                    data.mutedPlayers.remove(uuid);
                    save();
                    LOGGER.debug("已自动清理过期禁言: {}", uuid);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }, "MapBot-MuteCleanup").start();
    }
    
    /**
     * 获取禁言到期时间
     * 
     * @param uuid 玩家 UUID
     * @return 到期时间戳，-1 为永久，0 为未禁言
     */
    public long getMuteExpiry(String uuid) {
        lock.readLock().lock();
        try {
            return data.mutedPlayers.getOrDefault(uuid, 0L);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // ================== 玩家绑定管理 ==================
    
    /**
     * 绑定玩家 (QQ → UUID)
     * 
     * @param qq QQ 号
     * @param uuid 玩家 UUID
     * @return 是否绑定成功 (false 表示已绑定)
     */
    public boolean bind(long qq, String uuid) {
        lock.writeLock().lock();
        try {
            if (data.playerBindings.containsKey(qq)) {
                return false;
            }
            data.playerBindings.put(qq, uuid);
            save();
            LOGGER.info("已绑定玩家: {} -> {}", qq, uuid);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 解绑玩家
     * 
     * @param qq QQ 号
     * @return 是否解绑成功
     */
    public boolean unbind(long qq) {
        lock.writeLock().lock();
        try {
            String removed = data.playerBindings.remove(qq);
            if (removed != null) {
                save();
                LOGGER.info("已解绑玩家: {}", qq);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 获取玩家绑定的 UUID
     * 
     * @param qq QQ 号
     * @return UUID 字符串，未绑定返回 null
     */
    public String getBinding(long qq) {
        lock.readLock().lock();
        try {
            return data.playerBindings.get(qq);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 检查 QQ 是否已绑定
     * 
     * @param qq QQ 号
     * @return 是否已绑定
     */
    public boolean isBound(long qq) {
        lock.readLock().lock();
        try {
            return data.playerBindings.containsKey(qq);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 获取所有绑定 (只读副本)
     * 
     * @return QQ → UUID 映射表
     */
    public Map<Long, String> getAllBindings() {
        lock.readLock().lock();
        try {
            return new HashMap<>(data.playerBindings);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 检查 QQ 是否已绑定 (别名方法)
     * 
     * @param qq QQ 号
     * @return 是否已绑定
     */
    public boolean isQQBound(long qq) {
        return isBound(qq);
    }
    
    /**
     * 检查 UUID 是否已被绑定
     * 
     * @param uuid 玩家 UUID
     * @return 是否已被绑定
     */
    public boolean isUUIDBound(String uuid) {
        lock.readLock().lock();
        try {
            return data.playerBindings.containsValue(uuid);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 根据 UUID 获取绑定的 QQ 号
     * 
     * @param uuid 玩家 UUID
     * @return 绑定的 QQ 号，未绑定返回 -1
     */
    public long getQQByUUID(String uuid) {
        lock.readLock().lock();
        try {
            for (Map.Entry<Long, String> entry : data.playerBindings.entrySet()) {
                if (entry.getValue().equals(uuid)) {
                    return entry.getKey();
                }
            }
            return -1L;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // ================== 在线时长管理 ==================
    
    /**
     * 获取玩家的在线时长记录
     * 
     * @param uuidStr 玩家 UUID 字符串
     * @return PlaytimeRecord，未找到返回 null
     */
    public PlaytimeRecord getPlaytimeRecord(String uuidStr) {
        lock.readLock().lock();
        try {
            return data.playerPlaytime.get(uuidStr);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 保存玩家的在线时长记录
     * 
     * @param uuidStr 玩家 UUID 字符串
     * @param record 时长记录
     */
    public void savePlaytimeRecord(String uuidStr, PlaytimeRecord record) {
        lock.writeLock().lock();
        try {
            data.playerPlaytime.put(uuidStr, record);
            save();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    // ================== 内部数据模型 ==================
    
    /**
     * 数据模型类
     * 直接序列化为 JSON
     */
    private static class DataModel {
        /** 旧版管理员列表 (仅用于迁移) */
        List<Long> admins = new ArrayList<>();
        
        /** 用户权限映射 (QQ -> Level) */
        Map<Long, Integer> userPermissions = new HashMap<>();
        
        /** 禁言玩家列表 (UUID -> 到期时间戳) */
        Map<String, Long> mutedPlayers = new HashMap<>();
        
        /** 玩家绑定映射 (QQ → UUID) */
        Map<Long, String> playerBindings = new HashMap<>();
        
        /** 玩家在线时长记录 (UUID → PlaytimeRecord) */
        Map<String, PlaytimeRecord> playerPlaytime = new HashMap<>();
        
        /** 签到记录 (QQ -> yyyy-MM-dd) */
        Map<Long, String> lastSignIn = new HashMap<>();
        
        /** 累计签到天数 (QQ -> 天数) */
        Map<Long, Integer> signInStreak = new HashMap<>();
    }
    
    // ================== 签到管理 ==================
    
    /**
     * 检查今日是否已签到
     */
    public boolean hasSignedInToday(long qq) {
        lock.readLock().lock();
        try {
            String lastDate = data.lastSignIn.get(qq);
            if (lastDate == null) return false;
            
            String today = java.time.LocalDate.now().toString(); // yyyy-MM-dd
            return today.equals(lastDate);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 记录签到 (同时累加签到天数)
     */
    public void recordSignIn(long qq) {
        lock.writeLock().lock();
        try {
            String today = java.time.LocalDate.now().toString();
            data.lastSignIn.put(qq, today);
            // 累加签到天数
            int current = data.signInStreak.getOrDefault(qq, 0);
            data.signInStreak.put(qq, current + 1);
            save();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 获取累计签到天数
     */
    public int getSignInDays(long qq) {
        lock.readLock().lock();
        try {
            return data.signInStreak.getOrDefault(qq, 0);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 在线时长记录
     * Task #016-STEP2 新增
     */
    public static class PlaytimeRecord {
        /** 今日在线毫秒 */
        public long dailyMs = 0;
        
        /** 本周在线毫秒 */
        public long weeklyMs = 0;
        
        /** 本月在线毫秒 */
        public long monthlyMs = 0;
        
        /** 总计在线毫秒 */
        public long totalMs = 0;
        
        /** 上次重置日期 (YYYY-MM-DD 格式) */
        public String lastReset = null;
    }
}
