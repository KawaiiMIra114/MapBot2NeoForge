package com.mapbot.alpha.metrics;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * 指标数据持久化存储
 * 使用 JSON 文件存储历史数据，每分钟保存一次
 */
public enum MetricsStorage {
    INSTANCE;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsStorage.class);
    private static final Path DATA_FILE = Paths.get("config", "metrics_history.json");
    private static final Gson GSON = new GsonBuilder().create();
    private static final int SAVE_INTERVAL_SEC = 60; // 每分钟保存一次
    
    private ScheduledExecutorService saveScheduler;
    private boolean initialized = false;
    
    /**
     * 初始化并加载历史数据
     */
    public void init() {
        if (initialized) return;
        initialized = true;
        
        // 加载历史数据
        load();
        
        // 启动定期保存
        saveScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MetricsSaver");
            t.setDaemon(true);
            return t;
        });
        saveScheduler.scheduleAtFixedRate(this::save, SAVE_INTERVAL_SEC, SAVE_INTERVAL_SEC, TimeUnit.SECONDS);
        
        LOGGER.info("指标存储已初始化");
    }
    
    /**
     * 保存当前数据到文件
     */
    public void save() {
        try {
            var collector = MetricsCollector.INSTANCE;
            Map<String, Object> data = new HashMap<>();
            
            // 从 MetricsCollector 获取所有数据
            Map<String, List<MetricsCollector.MetricPoint>> tps = new HashMap<>();
            Map<String, List<MetricsCollector.MetricPoint>> memory = new HashMap<>();
            Map<String, List<MetricsCollector.MetricPoint>> players = new HashMap<>();
            
            // 遍历所有服务器
            for (var server : com.mapbot.alpha.bridge.ServerRegistry.INSTANCE.getAllServers()) {
                String id = server.serverId;
                tps.put(id, collector.getTpsHistory(id));
                memory.put(id, collector.getMemoryHistory(id));
                players.put(id, collector.getPlayersHistory(id));
            }
            
            data.put("tps", tps);
            data.put("memory", memory);
            data.put("players", players);
            data.put("savedAt", System.currentTimeMillis());
            
            // 确保目录存在
            Files.createDirectories(DATA_FILE.getParent());
            
            // 写入文件
            String json = GSON.toJson(data);
            Files.writeString(DATA_FILE, json, StandardCharsets.UTF_8);
            
            LOGGER.debug("指标数据已保存 ({} 字节)", json.length());
        } catch (Exception e) {
            LOGGER.error("保存指标数据失败", e);
        }
    }
    
    /**
     * 从文件加载历史数据
     */
    public void load() {
        if (!Files.exists(DATA_FILE)) {
            LOGGER.info("无历史指标数据");
            return;
        }
        
        try {
            String json = Files.readString(DATA_FILE, StandardCharsets.UTF_8);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = GSON.fromJson(json, Map.class);
            
            if (data == null) return;
            
            // 恢复数据到 MetricsCollector
            var collector = MetricsCollector.INSTANCE;
            
            loadHistoryMap(data, "tps", collector::restoreTpsHistory);
            loadHistoryMap(data, "memory", collector::restoreMemoryHistory);
            loadHistoryMap(data, "players", collector::restorePlayersHistory);
            
            Object savedAt = data.get("savedAt");
            if (savedAt instanceof Number) {
                LOGGER.info("已加载历史指标数据 (保存于 {} 前)",
                    formatDuration(System.currentTimeMillis() - ((Number) savedAt).longValue()));
            } else {
                LOGGER.info("已加载历史指标数据 (savedAt 缺失或格式错误)");
            }
            
        } catch (Exception e) {
            LOGGER.error("加载指标数据失败", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void loadHistoryMap(Map<String, Object> data, String key, 
            java.util.function.BiConsumer<String, List<MetricsCollector.MetricPoint>> restorer) {
        Object obj = data.get(key);
        if (!(obj instanceof Map)) return;
        
        Map<String, List<Map<String, Object>>> map = (Map<String, List<Map<String, Object>>>) obj;
        for (var entry : map.entrySet()) {
            String serverId = entry.getKey();
            List<MetricsCollector.MetricPoint> points = new ArrayList<>();
            
            for (var point : entry.getValue()) {
                Object tsObj = point.get("timestamp");
                Object valueObj = point.get("value");
                if (!(tsObj instanceof Number) || !(valueObj instanceof Number)) {
                    continue;
                }
                long ts = ((Number) tsObj).longValue();
                double val = ((Number) valueObj).doubleValue();
                points.add(new MetricsCollector.MetricPoint(ts, val));
            }
            
            restorer.accept(serverId, points);
        }
    }
    
    private String formatDuration(long ms) {
        long sec = ms / 1000;
        if (sec < 60) return sec + " 秒";
        long min = sec / 60;
        if (min < 60) return min + " 分钟";
        long hour = min / 60;
        if (hour < 24) return hour + " 小时";
        return (hour / 24) + " 天";
    }
    
    public void shutdown() {
        if (saveScheduler != null) {
            save(); // 关闭前保存
            saveScheduler.shutdown();
        }
    }
}
