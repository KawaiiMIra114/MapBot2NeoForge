package com.mapbot.security;

/**
 * 权限角色枚举
 * 遵循《COMMAND_AUTHORIZATION_CONTRACT》定义的三角色模型。
 * 取代旧版 DataManager 中 0/1/2 的魔法数字。
 *
 * 迁移映射:
 *   旧 0 (USER)  -> USER
 *   旧 1 (MOD)   -> ADMIN
 *   旧 2 (ADMIN) -> OWNER
 */
public enum PermissionRole {
    /** 普通使用者，可执行公开且低风险命令 */
    USER(0),
    /** 运维管理员，可执行管理类与敏感运维命令 */
    ADMIN(1),
    /** 系统所有者，可执行全量命令并管理权限模型本身 */
    OWNER(2);

    private final int level;

    PermissionRole(int level) {
        this.level = level;
    }

    /** 获取枚举对应的数值等级（用于比较） */
    public int getLevel() {
        return level;
    }

    /**
     * 判断当前角色是否满足最低要求角色
     *
     * @param required 最低要求角色
     * @return 当前角色 >= 要求角色 时返回 true
     */
    public boolean isAtLeast(PermissionRole required) {
        return this.level >= required.level;
    }

    /**
     * 从旧版 DataManager 存储的整数值安全映射到新枚举。
     * 遵循《COMMAND_AUTHORIZATION_CONTRACT》7.迁移与兼容 条款:
     * 无法映射的旧角色回退为 USER。
     *
     * @param legacyLevel 旧有的数字权限等级 (0/1/2)
     * @return 对应的 PermissionRole 枚举
     */
    public static PermissionRole fromLegacyLevel(int legacyLevel) {
        return switch (legacyLevel) {
            case 0 -> USER;
            case 1 -> ADMIN;   // 旧 MOD -> 新 ADMIN
            case 2 -> OWNER;   // 旧 ADMIN -> 新 OWNER
            default -> USER;   // 未知角色回退为 USER
        };
    }

    /**
     * 将新枚举角色转换为旧版兼容的整数值（用于存储兼容层）。
     * 保证旧版 JSON 反序列化时不会因为数值变化而出错。
     *
     * @return 旧版数字等级
     */
    public int toLegacyLevel() {
        return switch (this) {
            case USER -> 0;
            case ADMIN -> 1;
            case OWNER -> 2;
        };
    }
}
