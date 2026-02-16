package com.mapbot.alpha.command;

import com.mapbot.alpha.data.DataManager;
import com.mapbot.alpha.security.ContractRole;

import java.util.Locale;
import java.util.Optional;

/**
 * QQ 命令侧角色解析器。
 * 统一将 legacy 数据模型映射为 contract 角色模型: user/admin/owner。
 */
public final class QqRoleResolver {

    private QqRoleResolver() {
    }

    /**
     * 角色映射规则：
     * 1) 在 admins 集合中 => owner
     * 2) permission >= 2 => admin
     * 3) 其他 => user
     */
    public static ContractRole resolveRole(long qq) {
        if (DataManager.INSTANCE.isAdmin(qq)) {
            return ContractRole.OWNER;
        }
        if (DataManager.INSTANCE.getPermission(qq) >= DataManager.PERMISSION_LEVEL_ADMIN) {
            return ContractRole.ADMIN;
        }
        return ContractRole.USER;
    }

    public static boolean hasAtLeast(long qq, ContractRole requiredRole) {
        return resolveRole(qq).hasAtLeast(requiredRole);
    }

    /**
     * 解析 role 参数。
     * 支持 user/admin/owner 和 legacy 输入 0/1/2。
     */
    public static Optional<ContractRole> parseRoleToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String token = raw.trim().toLowerCase(Locale.ROOT);
        return switch (token) {
            case "0", "user", "viewer", "普通", "用户" -> Optional.of(ContractRole.USER);
            case "1", "2", "admin", "operator", "管理", "管理员" -> Optional.of(ContractRole.ADMIN);
            case "3", "owner", "开发者", "所有者", "超级管理员" -> Optional.of(ContractRole.OWNER);
            default -> Optional.empty();
        };
    }

    /**
     * 将角色回写到 legacy 存储模型（兼容历史数据结构）。
     */
    public static void applyRole(long qq, ContractRole role) {
        switch (role) {
            case USER -> {
                DataManager.INSTANCE.removeAdmin(qq);
                DataManager.INSTANCE.setPermission(qq, DataManager.PERMISSION_LEVEL_USER);
            }
            case ADMIN -> {
                DataManager.INSTANCE.removeAdmin(qq);
                DataManager.INSTANCE.setPermission(qq, DataManager.PERMISSION_LEVEL_ADMIN);
            }
            case OWNER -> {
                DataManager.INSTANCE.addAdmin(qq);
                DataManager.INSTANCE.setPermission(qq, DataManager.PERMISSION_LEVEL_ADMIN);
            }
        }
    }

    public static String roleKey(ContractRole role) {
        if (role == null) {
            return "unknown";
        }
        return role.name().toLowerCase(Locale.ROOT);
    }

    public static String roleLabel(ContractRole role) {
        if (role == null) {
            return "未知角色";
        }
        return switch (role) {
            case USER -> "普通用户 (user)";
            case ADMIN -> "服务器管理员 (admin)";
            case OWNER -> "Bot 所有者 (owner)";
        };
    }
}
