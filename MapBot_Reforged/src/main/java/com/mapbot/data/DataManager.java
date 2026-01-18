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
     */
    private void load() {
        lock.writeLock().lock();
        try {
            String json = Files.readString(dataPath);
            DataModel loaded = GSON.fromJson(json, DataModel.class);
            
            if (loaded != null) {
                this.data = loaded;
                LOGGER.info("数据加载成功: {} 管理员, {} 绑定", 
                    data.admins.size(), data.playerBindings.size());
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
        lock.readLock().lock();
        try {
            String json = GSON.toJson(data);
            Files.writeString(dataPath, json);
            LOGGER.debug("数据已保存");
        } catch (IOException e) {
            LOGGER.error("保存数据文件失败: {}", e.getMessage());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    // ================== 管理员管理 ==================
    
    /**
     * 检查指定 QQ 是否为管理员
     * 
     * @param qq QQ 号
     * @return 是否为管理员
     */
    public boolean isAdmin(long qq) {
        lock.readLock().lock();
        try {
            return data.admins.contains(qq);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 添加管理员
     * 
     * @param qq QQ 号
     * @return 是否添加成功 (false 表示已存在)
     */
    public boolean addAdmin(long qq) {
        lock.writeLock().lock();
        try {
            if (data.admins.contains(qq)) {
                return false;
            }
            data.admins.add(qq);
            save();
            LOGGER.info("已添加管理员: {}", qq);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 移除管理员
     * 
     * @param qq QQ 号
     * @return 是否移除成功 (false 表示不存在)
     */
    public boolean removeAdmin(long qq) {
        lock.writeLock().lock();
        try {
            boolean removed = data.admins.remove(qq);
            if (removed) {
                save();
                LOGGER.info("已移除管理员: {}", qq);
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 获取管理员列表 (只读副本)
     * 
     * @return 管理员 QQ 列表
     */
    public List<Long> getAdmins() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(data.admins);
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
        /** 管理员 QQ 列表 */
        List<Long> admins = new ArrayList<>();
        
        /** 玩家绑定映射 (QQ → UUID) */
        Map<Long, String> playerBindings = new HashMap<>();
        
        /** 玩家在线时长记录 (UUID → PlaytimeRecord) */
        Map<String, PlaytimeRecord> playerPlaytime = new HashMap<>();
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
