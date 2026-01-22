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
    
    private final Path dataDir = Paths.get("data");
    
    // QQ -> UUID 绑定
    private final ConcurrentHashMap<Long, String> bindings = new ConcurrentHashMap<>();
    // UUID -> 禁言到期时间 (-1=永久, 0=未禁言)
    private final ConcurrentHashMap<String, Long> mutes = new ConcurrentHashMap<>();
    // QQ -> 权限等级 (0=普通, 1=VIP, 2=OP)
    private final ConcurrentHashMap<Long, Integer> permissions = new ConcurrentHashMap<>();
    // 管理员 QQ 列表
    private final Set<Long> admins = ConcurrentHashMap.newKeySet();
    
    private DataManager() {}
    
    public void init() {
        try {
            Files.createDirectories(dataDir);
            loadBindings();
            loadMutes();
            loadPermissions();
            loadAdmins();
            LOGGER.info("数据管理器初始化完成: {} 绑定, {} 禁言, {} 管理员", 
                bindings.size(), mutes.size(), admins.size());
        } catch (Exception e) {
            LOGGER.error("数据管理器初始化失败", e);
        }
    }
    
    // ==================== 绑定 ====================
    
    public boolean bind(long qq, String uuid) {
        if (bindings.containsKey(qq)) return false;
        bindings.put(qq, uuid);
        saveBindings();
        return true;
    }
    
    public boolean unbind(long qq) {
        if (bindings.remove(qq) != null) {
            saveBindings();
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
    
    // ==================== 禁言 ====================
    
    public void mute(String uuid, long expiryMs) {
        mutes.put(uuid, expiryMs);
        saveMutes();
    }
    
    public void unmute(String uuid) {
        mutes.remove(uuid);
        saveMutes();
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
    }
    
    public int getPermission(long qq) {
        return permissions.getOrDefault(qq, 0);
    }
    
    // ==================== 管理员 ====================
    
    public void addAdmin(long qq) {
        admins.add(qq);
        saveAdmins();
    }
    
    public void removeAdmin(long qq) {
        admins.remove(qq);
        saveAdmins();
    }
    
    public boolean isAdmin(long qq) {
        return admins.contains(qq);
    }
    
    public Set<Long> getAdmins() {
        return Collections.unmodifiableSet(admins);
    }
    
    // ==================== 持久化 ====================
    
    private void loadBindings() throws IOException {
        Path file = dataDir.resolve("bindings.txt");
        if (!Files.exists(file)) return;
        for (String line : Files.readAllLines(file)) {
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                bindings.put(Long.parseLong(parts[0]), parts[1]);
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
                mutes.put(parts[0], Long.parseLong(parts[1]));
            }
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
                permissions.put(Long.parseLong(parts[0]), Integer.parseInt(parts[1]));
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
                admins.add(Long.parseLong(line.trim()));
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
}
