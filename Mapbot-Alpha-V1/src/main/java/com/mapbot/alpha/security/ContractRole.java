package com.mapbot.alpha.security;

import java.util.Locale;
import java.util.Optional;

/**
 * 合同角色模型 (Step-04 B2)
 * 唯一鉴权决策输入，替代 legacy Role 枚举。
 *
 * 合同映射：
 *   VIEWER   → USER
 *   OPERATOR → ADMIN
 *   ADMIN    → OWNER
 *
 * 未知角色必须被拒绝并审计。
 */
public enum ContractRole {
    /** 普通用户 — 只读 */
    USER,
    /** 管理员 — 可执行操作命令 */
    ADMIN,
    /** 所有者 — 完全控制（用户管理、配置变更等） */
    OWNER;

    /**
     * 从 legacy Role 映射到 ContractRole。
     * 未知/null 返回 empty。
     */
    public static Optional<ContractRole> fromLegacy(AuthManager.Role legacyRole) {
        if (legacyRole == null) return Optional.empty();
        return switch (legacyRole) {
            case VIEWER   -> Optional.of(USER);
            case OPERATOR -> Optional.of(ADMIN);
            case ADMIN    -> Optional.of(OWNER);
        };
    }

    /**
     * 从字符串解析为 ContractRole。
     * 同时支持 contract 名称 (user/admin/owner) 和 legacy 名称 (VIEWER/OPERATOR/ADMIN)。
     * 未知值返回 empty。
     */
    public static Optional<ContractRole> fromString(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        // 先尝试 contract 名称
        return switch (normalized) {
            case "USER"     -> Optional.of(USER);
            case "ADMIN"    -> Optional.of(ADMIN);
            case "OWNER"    -> Optional.of(OWNER);
            // legacy 名称兼容
            case "VIEWER"   -> Optional.of(USER);
            case "OPERATOR" -> Optional.of(ADMIN);
            default         -> Optional.empty();
        };
    }

    /**
     * 权限级别检查：当前角色是否满足所需的最低角色。
     * USER < ADMIN < OWNER
     */
    public boolean hasAtLeast(ContractRole required) {
        if (required == null) return false;
        return this.ordinal() >= required.ordinal();
    }

    /**
     * 转换回 legacy Role（用于向后兼容存储/序列化）。
     */
    public AuthManager.Role toLegacy() {
        return switch (this) {
            case USER  -> AuthManager.Role.VIEWER;
            case ADMIN -> AuthManager.Role.OPERATOR;
            case OWNER -> AuthManager.Role.ADMIN;
        };
    }
}
