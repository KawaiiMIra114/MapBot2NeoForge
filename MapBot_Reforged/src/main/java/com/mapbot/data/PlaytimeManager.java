/*
 * MapBot Reforged - 在线时长管理器
 * 
 * 遵从 .ai_rules.md 中定义的治理规则。
 * 追踪和管理玩家的在线时长统计。
 * 
 * Task #016-STEP2: 在线时长查询功能
 * - 登入/登出时间追踪
 * - 每日/每周/每月/总计时长统计
 * - 自动周期重置
 */

package com.mapbot.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线时长管理器 (单例)
 * 负责追踪玩家在线时长并提供查询功能
 */
public class PlaytimeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/Playtime");
    
    /** 单例实例 */
    public static final PlaytimeManager INSTANCE = new PlaytimeManager();
    
    /** 当前在线玩家的登录时间 (UUID -> 登录时间戳毫秒) */
    private final Map<UUID, Long> loginTimes = new ConcurrentHashMap<>();
    
    private PlaytimeManager() {}
    
    // ================== 事件钩子 ==================
    
    /**
     * 玩家登录时调用
     * 记录登录时间戳
     * 
     * @param uuid 玩家 UUID
     */
    public void onPlayerLogin(UUID uuid) {
        long now = System.currentTimeMillis();
        loginTimes.put(uuid, now);
        
        // 检查并执行周期重置
        checkAndResetPeriods(uuid);
        
        LOGGER.debug("玩家 {} 登录，时间: {}", uuid, now);
    }
    
    /**
     * 玩家登出时调用
     * 计算本次在线时长并累加到记录中
     * 
     * @param uuid 玩家 UUID
     */
    public void onPlayerLogout(UUID uuid) {
        Long loginTime = loginTimes.remove(uuid);
        if (loginTime == null) {
            LOGGER.warn("玩家 {} 登出但未找到登录记录", uuid);
            return;
        }
        
        long now = System.currentTimeMillis();
        long sessionMs = now - loginTime;
        
        // 累加到持久化存储
        addPlaytime(uuid, sessionMs);
        
        LOGGER.debug("玩家 {} 登出，本次在线: {} 分钟", uuid, sessionMs / 60000);
    }
    
    /**
     * 服务器关闭时调用
     * 保存所有在线玩家的当前时长
     */
    public void onServerStopping() {
        long now = System.currentTimeMillis();
        
        for (Map.Entry<UUID, Long> entry : loginTimes.entrySet()) {
            UUID uuid = entry.getKey();
            long sessionMs = now - entry.getValue();
            addPlaytime(uuid, sessionMs);
            LOGGER.info("服务器关闭，保存玩家 {} 在线时长: {} 分钟", uuid, sessionMs / 60000);
        }
        
        loginTimes.clear();
    }
    
    // ================== 时长管理 ==================
    
    /**
     * 累加玩家在线时长
     * 
     * @param uuid 玩家 UUID
     * @param milliseconds 要添加的毫秒数
     */
    private void addPlaytime(UUID uuid, long milliseconds) {
        DataManager.PlaytimeRecord record = DataManager.INSTANCE.getPlaytimeRecord(uuid.toString());
        
        if (record == null) {
            record = new DataManager.PlaytimeRecord();
        }
        
        // 累加到所有时段
        record.dailyMs += milliseconds;
        record.weeklyMs += milliseconds;
        record.monthlyMs += milliseconds;
        record.totalMs += milliseconds;
        
        // 更新最后活动日期
        record.lastReset = LocalDate.now(ZoneId.systemDefault()).toString();
        
        // 保存
        DataManager.INSTANCE.savePlaytimeRecord(uuid.toString(), record);
    }
    
    /**
     * 检查并重置周期数据
     * 
     * @param uuid 玩家 UUID
     */
    private void checkAndResetPeriods(UUID uuid) {
        DataManager.PlaytimeRecord record = DataManager.INSTANCE.getPlaytimeRecord(uuid.toString());
        
        if (record == null || record.lastReset == null) {
            return;
        }
        
        LocalDate lastReset = LocalDate.parse(record.lastReset);
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        
        boolean needsSave = false;
        
        // 检查日期变更 -> 重置每日
        if (!lastReset.equals(today)) {
            LOGGER.debug("玩家 {} 每日时长重置 (上次: {})", uuid, lastReset);
            record.dailyMs = 0;
            needsSave = true;
        }
        
        // 检查是否跨周 (周一重置)
        LocalDate lastMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (lastReset.isBefore(lastMonday)) {
            LOGGER.debug("玩家 {} 每周时长重置", uuid);
            record.weeklyMs = 0;
            needsSave = true;
        }
        
        // 检查是否跨月 (每月1号重置)
        if (lastReset.getMonth() != today.getMonth() || lastReset.getYear() != today.getYear()) {
            LOGGER.debug("玩家 {} 每月时长重置", uuid);
            record.monthlyMs = 0;
            needsSave = true;
        }
        
        if (needsSave) {
            record.lastReset = today.toString();
            DataManager.INSTANCE.savePlaytimeRecord(uuid.toString(), record);
        }
    }
    
    // ================== 查询接口 ==================
    
    /**
     * 获取玩家在线时长 (分钟)
     * 
     * @param uuid 玩家 UUID
     * @param mode 时段模式: 0=今天, 1=本周, 2=本月, 3=总计
     * @return 在线时长 (分钟)，未找到记录返回 0
     */
    public long getPlaytimeMinutes(UUID uuid, int mode) {
        DataManager.PlaytimeRecord record = DataManager.INSTANCE.getPlaytimeRecord(uuid.toString());
        
        if (record == null) {
            return 0;
        }
        
        // 如果玩家当前在线，加上当前会话时间
        long currentSessionMs = 0;
        Long loginTime = loginTimes.get(uuid);
        if (loginTime != null) {
            currentSessionMs = System.currentTimeMillis() - loginTime;
        }
        
        long baseMs = switch (mode) {
            case 0 -> record.dailyMs;
            case 1 -> record.weeklyMs;
            case 2 -> record.monthlyMs;
            case 3 -> record.totalMs;
            default -> 0;
        };
        
        return (baseMs + currentSessionMs) / 60000;
    }
    
    /**
     * 格式化时长显示
     * 
     * @param minutes 分钟数
     * @return 格式化字符串，例如 "2小时30分钟" 或 "45分钟"
     */
    public static String formatDuration(long minutes) {
        if (minutes < 60) {
            return minutes + " 分钟";
        }
        
        long hours = minutes / 60;
        long mins = minutes % 60;
        
        if (hours < 24) {
            return mins > 0 ? 
                String.format("%d 小时 %d 分钟", hours, mins) : 
                String.format("%d 小时", hours);
        }
        
        long days = hours / 24;
        hours = hours % 24;
        
        if (hours > 0) {
            return String.format("%d 天 %d 小时", days, hours);
        } else {
            return String.format("%d 天", days);
        }
    }
}
