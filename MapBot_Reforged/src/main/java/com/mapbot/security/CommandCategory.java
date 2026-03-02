package com.mapbot.security;

/**
 * 命令分类枚举
 * 遵循《COMMAND_AUTHORIZATION_CONTRACT》5.2 命令分类表。
 * 每个命令必须绑定一个分类，分类决定最小权限角色闪值。
 */
public enum CommandCategory {
    /** 无副作用查询 (如 help, status) — 最小角色: USER */
    PUBLIC_READ(PermissionRole.USER),
    /** 与自身上下文相关查询 (如 my_profile, playtime) — 最小角色: USER */
    SCOPED_READ(PermissionRole.USER),
    /** 常规运维写操作 (如 mute, unmute, reload) — 最小角色: ADMIN */
    OPS_WRITE(PermissionRole.ADMIN),
    /** 高风险写操作 (如 set_admin, stop_server) — 最小角色: OWNER */
    SENSITIVE_WRITE(PermissionRole.OWNER),
    /** 权限/合同/策略治理 (如 set_owner, force_unbind) — 最小角色: OWNER */
    GOVERNANCE(PermissionRole.OWNER);

    private final PermissionRole minRole;

    CommandCategory(PermissionRole minRole) {
        this.minRole = minRole;
    }

    /** 获取该分类下的最小允许角色 */
    public PermissionRole getMinRole() {
        return minRole;
    }
}
