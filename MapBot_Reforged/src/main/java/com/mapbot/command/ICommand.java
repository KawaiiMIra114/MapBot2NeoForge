package com.mapbot.command;

import com.mapbot.security.CommandCategory;

/**
 * 命令执行器接口
 * 
 * 重构自 Task04: 废弃旧版 getRequiredLevel() 魔法数字，
 * 改为声明 CommandCategory 以由 AuthorizationEngine 统一鉴权。
 */
public interface ICommand {
    /**
     * 执行命令
     * 
     * @param args 命令参数
     * @param senderQQ 发送者 QQ
     * @param sourceGroupId 来源群号
     */
    void execute(String args, long senderQQ, long sourceGroupId);

    /**
     * 获取命令分类 (决定最小权限角色)
     * 默认为 PUBLIC_READ (任何人可用)。
     * 
     * @return 命令分类枚举
     */
    default CommandCategory getCategory() {
        return CommandCategory.PUBLIC_READ;
    }

    /**
     * 获取命令描述
     */
    String getDescription();

    // ================== 旧版兼容层 (已废弃) ==================

    /**
     * @deprecated 使用 {@link #getCategory()} 替代。
     * 保留此方法仅为过渡期防止编译断裂，将在后续清理中删除。
     */
    @Deprecated
    default int getRequiredLevel() {
        // 由 getCategory() 的 minRole 反向推算旧值
        return getCategory().getMinRole().toLegacyLevel();
    }
}
