package com.mapbot.alpha.database;

import com.mapbot.alpha.config.AlphaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.function.Consumer;

/**
 * Redis 管理器
 * 负责连接池管理、基本命令执行以及发布/订阅逻辑
 */
public enum RedisManager {
    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Redis");
    private JedisPool pool;
    private Thread subscriberThread;

    /**
     * 初始化 Redis 连接池
     */
    public void init() {
        AlphaConfig cfg = AlphaConfig.INSTANCE;
        if (!cfg.isRedisEnabled()) {
            LOGGER.info("Redis 尚未启用，跳过初始化");
            return;
        }

        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(20);
            poolConfig.setMaxIdle(10);
            poolConfig.setMinIdle(2);
            poolConfig.setTestOnBorrow(true);

            String password = cfg.getRedisPassword().isEmpty() ? null : cfg.getRedisPassword();
            
            pool = new JedisPool(poolConfig, 
                cfg.getRedisHost(), 
                cfg.getRedisPort(), 
                2000, 
                password,
                cfg.getRedisDatabase());

            // 测试连接
            try (Jedis jedis = pool.getResource()) {
                String response = jedis.ping();
                LOGGER.info("Redis 连接成功: {}", response);
            }
        } catch (Exception e) {
            LOGGER.error("Redis 初始化失败: {}", e.getMessage());
            pool = null;
        }
    }

    /**
     * 执行 Redis 操作（自动处理资源关闭）
     */
    public <T> T execute(RedisCallback<T> callback) {
        if (pool == null) return null;
        try (Jedis jedis = pool.getResource()) {
            return callback.doInRedis(jedis);
        } catch (Exception e) {
            LOGGER.error("Redis 执行失败", e);
            return null;
        }
    }

    /**
     * 发布消息到指定频道
     */
    public void publish(String channel, String message) {
        execute(jedis -> jedis.publish(channel, message));
    }

    /**
     * 订阅指定频道（异步）
     */
    public void subscribe(String channel, Consumer<String> onMessage) {
        if (pool == null) return;
        
        subscriberThread = new Thread(() -> {
            try (Jedis jedis = pool.getResource()) {
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String chan, String msg) {
                        onMessage.accept(msg);
                    }
                }, channel);
            } catch (Exception e) {
                LOGGER.error("Redis 订阅异常: {}", channel, e);
            }
        }, "RedisSubscriber-" + channel);
        
        subscriberThread.setDaemon(true);
        subscriberThread.start();
        LOGGER.info("已开始订阅频道: {}", channel);
    }

    public boolean isEnabled() {
        return pool != null;
    }

    public void shutdown() {
        if (pool != null) {
            pool.close();
        }
        LOGGER.info("Redis 连接池已关闭");
    }

    @FunctionalInterface
    public interface RedisCallback<T> {
        T doInRedis(Jedis jedis);
    }
}
