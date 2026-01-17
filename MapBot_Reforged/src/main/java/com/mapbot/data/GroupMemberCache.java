/*
 * MapBot Reforged - 群成员缓存管理器
 * Task #013-STEP1 新增
 * 
 * 功能:
 * - 缓存 QQ 群成员的昵称信息
 * - 避免频繁调用 OneBot API 导致延迟
 * - 支持批量加载、单个更新、查询操作
 */
package com.mapbot.data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 群成员昵称缓存管理器 (单例)
 * 
 * 使用 ConcurrentHashMap 保证线程安全，
 * 在 Bot 连接时预加载群成员列表，
 * 后续查询直接从缓存读取，无网络延迟。
 */
public class GroupMemberCache {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/MemberCache");
    
    /** 单例实例 */
    public static final GroupMemberCache INSTANCE = new GroupMemberCache();
    
    /** 缓存结构: QQ号 -> 昵称 (群名片优先，其次QQ昵称) */
    private final ConcurrentHashMap<Long, String> nicknameCache = new ConcurrentHashMap<>();
    
    private GroupMemberCache() {}
    
    /**
     * 批量加载群成员列表
     * 通常在 Bot 连接成功后调用
     * 
     * @param members Map<QQ号, 昵称>
     */
    public void loadMembers(Map<Long, String> members) {
        nicknameCache.putAll(members);
        LOGGER.info("已加载 {} 个群成员昵称到缓存", members.size());
    }
    
    /**
     * 更新单个成员昵称
     * 成员入群或修改群名片时调用
     * 
     * @param qq QQ号
     * @param nickname 昵称
     */
    public void updateMember(long qq, String nickname) {
        nicknameCache.put(qq, nickname);
        LOGGER.debug("更新成员昵称: {} -> {}", qq, nickname);
    }
    
    /**
     * 移除成员
     * 成员退群时调用
     * 
     * @param qq QQ号
     */
    public void removeMember(long qq) {
        nicknameCache.remove(qq);
        LOGGER.debug("移除成员: {}", qq);
    }
    
    /**
     * 获取成员昵称
     * 
     * @param qq QQ号
     * @return 昵称，未找到返回 null
     */
    public String getNickname(long qq) {
        return nicknameCache.get(qq);
    }
    
    /**
     * 检查成员是否在缓存中
     * 
     * @param qq QQ号
     * @return 是否存在
     */
    public boolean hasMember(long qq) {
        return nicknameCache.containsKey(qq);
    }
    
    /**
     * 获取缓存大小
     * 
     * @return 缓存的成员数量
     */
    public int size() {
        return nicknameCache.size();
    }
    
    /**
     * 清空缓存
     * 通常在 Bot 断开连接时调用
     */
    public void clear() {
        nicknameCache.clear();
        LOGGER.info("群成员缓存已清空");
    }
}
