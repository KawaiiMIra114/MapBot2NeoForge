package com.mapbot.alpha.metrics;

import java.util.*;
import java.util.concurrent.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mapbot.alpha.bridge.ServerRegistry;

/**
 * 服务器性能指标收集器
 * 每 5 秒采集一次，保留最近 60 分钟数据
 */
public enum MetricsCollector {
    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(MetricsCollector.class);
    private static final int COLLECT_INTERVAL_SEC = 5;
    private static final int MAX_DATA_POINTS = 720; // 60分钟 / 5秒 = 720 个点
    
    // serverId -> 时间序列数据
    private final Map<String, Deque<MetricPoint>> tpsHistory = new ConcurrentHashMap<>();
    private final Map<String, Deque<MetricPoint>> memoryHistory = new ConcurrentHashMap<>();
    private final Map<String, Deque<MetricPoint>> playersHistory = new ConcurrentHashMap<>();
    
    private ScheduledExecutorService scheduler;
    
    public void start() {
        if (scheduler != null) return;
        
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "MetricsCollector");
            t.setDaemon(true);
            return t;
        });
        
        scheduler.scheduleAtFixedRate(this::collect, 0, COLLECT_INTERVAL_SEC, TimeUnit.SECONDS);
        LOGGER.info("性能指标收集器已启动");
    }
    
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }
    
    private void collect() {
        try {
            long now = System.currentTimeMillis();
            
            for (var server : ServerRegistry.INSTANCE.getAllServers()) {
                String id = server.serverId;
                
                // 获取或创建历史队列
                tpsHistory.computeIfAbsent(id, k -> new ConcurrentLinkedDeque<>());
                memoryHistory.computeIfAbsent(id, k -> new ConcurrentLinkedDeque<>());
                playersHistory.computeIfAbsent(id, k -> new ConcurrentLinkedDeque<>());
                
                // 添加新数据点 (tps 和 players 是 String，需要转换)
                double tps = 0;
                try { tps = Double.parseDouble(server.tps); } catch (Exception ignored) {}
                int players = 0;
                try { players = Integer.parseInt(server.players); } catch (Exception ignored) {}
                
                addPoint(tpsHistory.get(id), now, tps);
                addPoint(memoryHistory.get(id), now, parseMemory(server.memory));
                addPoint(playersHistory.get(id), now, players);
            }
        } catch (Exception e) {
            LOGGER.error("指标收集失败", e);
        }
    }
    
    private void addPoint(Deque<MetricPoint> queue, long timestamp, double value) {
        queue.addLast(new MetricPoint(timestamp, value));
        while (queue.size() > MAX_DATA_POINTS) {
            queue.removeFirst();
        }
    }
    
    /**
     * 解析内存字符串 (如 "1.2G" -> 1228.8)
     */
    private double parseMemory(String mem) {
        if (mem == null || mem.isEmpty()) return 0;
        try {
            mem = mem.toUpperCase().trim();
            if (mem.endsWith("G")) {
                return Double.parseDouble(mem.replace("G", "")) * 1024;
            } else if (mem.endsWith("M")) {
                return Double.parseDouble(mem.replace("M", ""));
            } else if (mem.endsWith("K")) {
                return Double.parseDouble(mem.replace("K", "")) / 1024;
            }
            return Double.parseDouble(mem);
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 获取 TPS 历史数据
     */
    public List<MetricPoint> getTpsHistory(String serverId) {
        Deque<MetricPoint> q = tpsHistory.get(serverId);
        return q != null ? new ArrayList<>(q) : Collections.emptyList();
    }
    
    /**
     * 获取内存历史数据
     */
    public List<MetricPoint> getMemoryHistory(String serverId) {
        Deque<MetricPoint> q = memoryHistory.get(serverId);
        return q != null ? new ArrayList<>(q) : Collections.emptyList();
    }
    
    /**
     * 获取玩家数历史数据
     */
    public List<MetricPoint> getPlayersHistory(String serverId) {
        Deque<MetricPoint> q = playersHistory.get(serverId);
        return q != null ? new ArrayList<>(q) : Collections.emptyList();
    }
    
    public static class MetricPoint {
        public final long timestamp;
        public final double value;
        
        public MetricPoint(long timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }
    }
    
    // === 数据恢复方法 ===
    
    public void restoreTpsHistory(String serverId, List<MetricPoint> points) {
        var q = tpsHistory.computeIfAbsent(serverId, k -> new ConcurrentLinkedDeque<>());
        q.addAll(points);
        trimQueue(q);
    }
    
    public void restoreMemoryHistory(String serverId, List<MetricPoint> points) {
        var q = memoryHistory.computeIfAbsent(serverId, k -> new ConcurrentLinkedDeque<>());
        q.addAll(points);
        trimQueue(q);
    }
    
    public void restorePlayersHistory(String serverId, List<MetricPoint> points) {
        var q = playersHistory.computeIfAbsent(serverId, k -> new ConcurrentLinkedDeque<>());
        q.addAll(points);
        trimQueue(q);
    }
    
    private void trimQueue(Deque<MetricPoint> q) {
        while (q.size() > MAX_DATA_POINTS) {
            q.removeFirst();
        }
    }
}
