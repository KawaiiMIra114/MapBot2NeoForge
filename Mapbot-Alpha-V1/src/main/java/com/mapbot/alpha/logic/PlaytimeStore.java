package com.mapbot.alpha.logic;

import com.mapbot.alpha.database.RedisManager;
import com.mapbot.alpha.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线时长存储 (P0 数据统一管理)
 *
 * 数据源优先使用 Redis（跨服统一），Redis 未启用时回退到 Alpha 本地内存。
 */
public class PlaytimeStore {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Playtime");
    public static final PlaytimeStore INSTANCE = new PlaytimeStore();

    private static final String KEY_PREFIX = "mapbot:playtime:"; // uuid -> JSON

    private final ConcurrentHashMap<String, PlaytimeRecord> local = new ConcurrentHashMap<>();

    private PlaytimeStore() {}

    public void addPlaytime(String uuid, long deltaMs) {
        if (uuid == null || uuid.isEmpty() || deltaMs <= 0) return;

        try {
            PlaytimeRecord record = load(uuid);
            boolean changed = applyRolloverIfNeeded(record);

            record.dailyMs += deltaMs;
            record.weeklyMs += deltaMs;
            record.monthlyMs += deltaMs;
            record.totalMs += deltaMs;
            record.lastReset = todayStr();

            save(uuid, record);

            if (changed) {
                LOGGER.debug("在线时长周期已重置: {}", uuid);
            }
        } catch (Exception e) {
            LOGGER.error("在线时长写入失败: {}", uuid, e);
        }
    }

    public PlaytimeRecord getPlaytime(String uuid) {
        if (uuid == null || uuid.isEmpty()) return new PlaytimeRecord();

        PlaytimeRecord record = load(uuid);
        boolean changed = applyRolloverIfNeeded(record);
        if (changed) {
            save(uuid, record);
        }
        return record;
    }

    private PlaytimeRecord load(String uuid) {
        var redis = RedisManager.INSTANCE;
        if (redis.isEnabled()) {
            String json = redis.execute(jedis -> jedis.get(KEY_PREFIX + uuid));
            if (json != null && !json.isEmpty()) {
                try {
                    PlaytimeRecord r = JsonUtils.fromJson(json, PlaytimeRecord.class);
                    if (r != null) return r;
                } catch (Exception ignored) {}
            }
            return new PlaytimeRecord();
        }

        return local.computeIfAbsent(uuid, k -> new PlaytimeRecord());
    }

    private void save(String uuid, PlaytimeRecord record) {
        var redis = RedisManager.INSTANCE;
        if (redis.isEnabled()) {
            String json = JsonUtils.toJson(record);
            redis.execute(jedis -> jedis.set(KEY_PREFIX + uuid, json));
            return;
        }
        local.put(uuid, record);
    }

    private boolean applyRolloverIfNeeded(PlaytimeRecord record) {
        String last = record.lastReset;
        if (last == null || last.isEmpty()) {
            record.lastReset = todayStr();
            return false;
        }

        LocalDate lastDate;
        try {
            lastDate = LocalDate.parse(last);
        } catch (Exception e) {
            record.lastReset = todayStr();
            return false;
        }

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        boolean changed = false;

        // 每日重置
        if (!lastDate.equals(today)) {
            record.dailyMs = 0;
            changed = true;
        }

        // 每周重置（周一为周起点）
        LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (lastDate.isBefore(thisMonday)) {
            record.weeklyMs = 0;
            changed = true;
        }

        // 每月重置
        if (lastDate.getYear() != today.getYear() || lastDate.getMonth() != today.getMonth()) {
            record.monthlyMs = 0;
            changed = true;
        }

        if (changed) {
            record.lastReset = today.toString();
        }

        return changed;
    }

    private String todayStr() {
        return LocalDate.now(ZoneId.systemDefault()).toString();
    }

    public static String formatDurationMs(long ms) {
        long minutes = ms / 60000;
        if (minutes < 60) return minutes + " 分钟";

        long hours = minutes / 60;
        long mins = minutes % 60;

        if (hours < 24) {
            return mins > 0 ? (hours + " 小时 " + mins + " 分钟") : (hours + " 小时");
        }

        long days = hours / 24;
        hours = hours % 24;
        return hours > 0 ? (days + " 天 " + hours + " 小时") : (days + " 天");
    }

    /**
     * 存储结构（与 Report_01_Continue.md 的 mapbot_data.json 结构对齐）
     */
    public static class PlaytimeRecord {
        public long dailyMs = 0;
        public long weeklyMs = 0;
        public long monthlyMs = 0;
        public long totalMs = 0;
        public String lastReset = null;

        public Map<String, Object> toMap() {
            return Map.of(
                "dailyMs", dailyMs,
                "weeklyMs", weeklyMs,
                "monthlyMs", monthlyMs,
                "totalMs", totalMs,
                "lastReset", lastReset
            );
        }
    }
}

