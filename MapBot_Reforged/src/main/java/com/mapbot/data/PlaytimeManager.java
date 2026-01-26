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

import com.mapbot.network.BridgeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        
        // P0: 在线时长统一存储到 Alpha Redis（Mod 端不做本地持久化）
        BridgeClient.INSTANCE.sendPlaytimeAdd(uuid.toString(), sessionMs);
        
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
            BridgeClient.INSTANCE.sendPlaytimeAdd(uuid.toString(), sessionMs);
            LOGGER.info("服务器关闭，保存玩家 {} 在线时长: {} 分钟", uuid, sessionMs / 60000);
        }
        
        loginTimes.clear();
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
        long currentSessionMs = 0;
        Long loginTime = loginTimes.get(uuid);
        if (loginTime != null) {
            currentSessionMs = System.currentTimeMillis() - loginTime;
        }

        // P0: 在线时长已迁移到 Alpha Redis，本地仅返回当前会话时长，避免误用
        return currentSessionMs / 60000;
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
