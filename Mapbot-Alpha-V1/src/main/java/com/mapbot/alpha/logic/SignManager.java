package com.mapbot.alpha.logic;

import com.mapbot.alpha.database.RedisManager;
import com.mapbot.alpha.data.DataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Random;

/**
 * Alpha 签到管理器 (Redis 版)
 * 所有签到状态存储在 Redis，支持跨服同步
 */
public class SignManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Sign");
    public static final SignManager INSTANCE = new SignManager();
    
    // Redis Key 前缀
    private static final String KEY_LAST_SIGN = "mapbot:sign:last:";      // QQ -> yyyy-MM-dd
    private static final String KEY_SIGN_DAYS = "mapbot:sign:days:";      // QQ -> 累计天数
    private static final String KEY_PENDING = "mapbot:sign:pending:";     // QQ -> Item JSON
    private static final String KEY_CDK = "mapbot:cdk:";                  // CODE -> CDK JSON
    
    private SignManager() {}
    
    /**
     * 检查今日是否已签到
     */
    public boolean hasSignedInToday(long qq) {
        var redis = RedisManager.INSTANCE;
        if (!redis.isEnabled()) return false;
        
        String lastDate = redis.execute(jedis -> jedis.get(KEY_LAST_SIGN + qq));
        if (lastDate == null) return false;
        
        String today = LocalDate.now().toString();
        return today.equals(lastDate);
    }
    
    /**
     * 记录签到并增加累计天数
     */
    public void recordSignIn(long qq) {
        var redis = RedisManager.INSTANCE;
        if (!redis.isEnabled()) return;
        
        String today = LocalDate.now().toString();
        redis.execute(jedis -> {
            jedis.set(KEY_LAST_SIGN + qq, today);
            jedis.incr(KEY_SIGN_DAYS + qq);
            return null;
        });
    }
    
    /**
     * 获取累计签到天数
     */
    public int getSignInDays(long qq) {
        var redis = RedisManager.INSTANCE;
        if (!redis.isEnabled()) return 0;
        
        String days = redis.execute(jedis -> jedis.get(KEY_SIGN_DAYS + qq));
        return days != null ? Integer.parseInt(days) : 0;
    }
    
    /**
     * 保存待领取奖励
     */
    public void setPendingReward(long qq, String itemJson) {
        var redis = RedisManager.INSTANCE;
        if (!redis.isEnabled()) return;
        
        redis.execute(jedis -> {
            jedis.set(KEY_PENDING + qq, itemJson);
            // 设置 24 小时过期
            jedis.expire(KEY_PENDING + qq, 86400);
            return null;
        });
    }
    
    /**
     * 获取待领取奖励
     */
    public String getPendingReward(long qq) {
        var redis = RedisManager.INSTANCE;
        if (!redis.isEnabled()) return null;
        
        return redis.execute(jedis -> jedis.get(KEY_PENDING + qq));
    }
    
    /**
     * 检查是否有待领取奖励
     */
    public boolean hasPendingReward(long qq) {
        return getPendingReward(qq) != null;
    }
    
    /**
     * 删除待领取奖励
     */
    public void removePendingReward(long qq) {
        var redis = RedisManager.INSTANCE;
        if (!redis.isEnabled()) return;
        
        redis.execute(jedis -> jedis.del(KEY_PENDING + qq));
    }
    
    /**
     * 生成 CDK
     * @return 兑换码字符串
     */
    public String createCdk(long qq, String itemJson) {
        var redis = RedisManager.INSTANCE;
        if (!redis.isEnabled()) return null;
        
        String code = "MP-" + randomString(4);
        long expiry = System.currentTimeMillis() + 86400000L; // 24h
        
        // CDK JSON: {"qq":123,"item":{...},"expiry":...}
        String cdkJson = String.format("{\"qq\":%d,\"item\":%s,\"expiry\":%d}", qq, itemJson, expiry);
        
        redis.execute(jedis -> {
            jedis.set(KEY_CDK + code, cdkJson);
            jedis.expire(KEY_CDK + code, 86400);
            return null;
        });
        
        // 从 pending 移除
        removePendingReward(qq);
        
        return code;
    }
    
    /**
     * 获取 CDK 信息
     * @return CDK JSON，无效返回 null
     */
    public String getCdkInfo(String code) {
        var redis = RedisManager.INSTANCE;
        if (!redis.isEnabled()) return null;
        
        return redis.execute(jedis -> jedis.get(KEY_CDK + code));
    }
    
    /**
     * 删除 CDK (兑换成功后)
     */
    public void removeCdk(String code) {
        var redis = RedisManager.INSTANCE;
        if (!redis.isEnabled()) return;
        
        redis.execute(jedis -> jedis.del(KEY_CDK + code));
    }
    
    private String randomString(int length) {
        String chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
