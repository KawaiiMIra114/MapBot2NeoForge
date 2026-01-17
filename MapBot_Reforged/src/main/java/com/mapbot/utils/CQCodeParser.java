/*
 * MapBot Reforged - CQ码解析器
 * 
 * 遵从 .ai_rules.md 中定义的治理规则。
 * 负责解析 OneBot v11 的 CQ 码，将其转换为 Minecraft 可显示的纯文本。
 * 
 * CQ码格式: [CQ:type,param1=value1,param2=value2,...]
 * 
 * Task #012-STEP1 创建
 * Task #013-STEP2 更新: @提及昵称解析优化
 * Task #014-STEP1 更新: 添加 reply CQ 码解析
 */

package com.mapbot.utils;

import com.mapbot.data.DataManager;
import com.mapbot.data.GroupMemberCache;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CQ码解析器
 * 将 OneBot v11 的 CQ 码转换为可读文本
 */
public class CQCodeParser {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/CQParser");
    
    /**
     * CQ 码正则表达式
     * 匹配格式: [CQ:type,key=value,...]
     * Group 1: type (如 image, at, reply)
     * Group 2: 参数部分 (如 ,file=xxx,url=xxx)
     */
    private static final Pattern CQ_PATTERN = Pattern.compile("\\[CQ:(\\w+)([^\\]]*)]");
    
    /**
     * 解析 CQ 码，返回可读文本
     * 
     * @param raw 原始消息 (可能包含 CQ 码)
     * @return 解析后的纯文本
     */
    public static String parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        
        Matcher matcher = CQ_PATTERN.matcher(raw);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String type = matcher.group(1);
            String params = matcher.group(2);
            
            String replacement = switch (type) {
                case "image" -> parseImage(params);
                case "at" -> parseAt(params);
                case "reply" -> ""; // 回复标签直接移除
                case "face" -> "[表情]";
                case "record" -> "[语音]";
                case "video" -> "[视频]";
                case "forward" -> "[转发消息]";
                case "json" -> "[卡片消息]";
                default -> {
                    LOGGER.debug("未知 CQ 类型: {}", type);
                    yield "";
                }
            };
            
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        // 清理多余空格
        return result.toString().trim();
    }
    
    /**
     * 解析图片 CQ 码
     * - 如果包含 summary=动画表情 → [动画表情]
     * - 否则 → [图片]
     * 
     * @param params CQ 码参数部分
     * @return 可读文本
     */
    private static String parseImage(String params) {
        // 检查是否为动画表情 (summary 参数包含 "动画表情")
        if (params.contains("summary=")) {
            // 尝试提取 summary 值
            Pattern summaryPattern = Pattern.compile("summary=([^,\\]]+)");
            Matcher summaryMatcher = summaryPattern.matcher(params);
            if (summaryMatcher.find()) {
                String summary = summaryMatcher.group(1);
                // 如果 summary 包含 "动画表情" 或类似标识
                if (summary.contains("动画表情") || summary.contains("&#") || summary.contains("表情")) {
                    return "[动画表情]";
                }
            }
        }
        return "[图片]";
    }
    
    /**
     * 解析 @ 提及 CQ 码
     * 优先级: 绑定玩家名 > 群昵称缓存 > QQ号
     * 
     * Task #013-STEP2 优化
     * Task #014-Fix: 还原普通格式（醒目格式化在发送阶段处理）
     * 
     * @param params CQ 码参数部分
     * @return 可读文本
     */
    private static String parseAt(String params) {
        Pattern qqPattern = Pattern.compile("qq=([^,\\]]+)");
        Matcher qqMatcher = qqPattern.matcher(params);
        
        if (qqMatcher.find()) {
            String qq = qqMatcher.group(1);
            
            // @全体成员
            if ("all".equalsIgnoreCase(qq)) {
                return "@全体成员";
            }
            
            try {
                long qqNum = Long.parseLong(qq);
                
                // 1. 优先查绑定的玩家名
                String uuid = DataManager.INSTANCE.getBinding(qqNum);
                if (uuid != null) {
                    MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                    if (server != null && server.getProfileCache() != null) {
                        try {
                            Optional<GameProfile> profile = server.getProfileCache().get(UUID.fromString(uuid));
                            if (profile.isPresent()) {
                                return "@" + profile.get().getName();
                            }
                        } catch (IllegalArgumentException e) {
                            LOGGER.debug("无效的 UUID 格式: {}", uuid);
                        }
                    }
                }
                
                // 2. 其次查群昵称缓存
                String nickname = GroupMemberCache.INSTANCE.getNickname(qqNum);
                if (nickname != null && !nickname.isEmpty()) {
                    return "@" + nickname;
                }
                
            } catch (NumberFormatException e) {
                LOGGER.debug("无法解析 QQ 号: {}", qq);
            }
            
            // 3. 兜底: 显示 QQ 号
            return "@" + qq;
        }
        return "@未知";
    }
    
    /**
     * 提取消息中所有被 @ 的 QQ 号
     * Task #014-Debug: 修复正则支持多参数格式
     * 
     * @param raw 原始消息
     * @return 被 @ 的 QQ 号列表 (不包含 "all")
     */
    public static List<Long> extractAtTargets(String raw) {
        List<Long> targets = new ArrayList<>();
        
        if (raw == null || raw.isEmpty()) {
            return targets;
        }
        
        // 修复: 匹配 [CQ:at,qq=xxx] 或 [CQ:at,qq=xxx,...] 格式
        Pattern atPattern = Pattern.compile("\\[CQ:at,qq=(\\d+)[^\\]]*\\]");
        Matcher matcher = atPattern.matcher(raw);
        
        while (matcher.find()) {
            try {
                long qq = Long.parseLong(matcher.group(1));
                targets.add(qq);
                LOGGER.debug("[DEBUG] extractAtTargets: 找到 @QQ={}", qq);
            } catch (NumberFormatException e) {
                LOGGER.debug("无法解析 QQ 号: {}", matcher.group(1));
            }
        }
        
        LOGGER.debug("[DEBUG] extractAtTargets: 总共找到 {} 个 @目标", targets.size());
        return targets;
    }
    
    /**
     * 检查消息是否 @ 了指定 QQ 号
     * 
     * @param raw 原始消息
     * @param targetQQ 目标 QQ 号
     * @return 是否 @ 了该 QQ
     */
    public static boolean isAtTarget(String raw, long targetQQ) {
        return extractAtTargets(raw).contains(targetQQ);
    }
    
    /**
     * 提取回复消息的 message_id
     * Task #014-STEP1 新增
     * 
     * 解析 [CQ:reply,id=xxx] 格式
     * 
     * @param raw 原始消息
     * @return 被回复消息的 ID，未找到返回 null
     */
    public static String extractReplyId(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        
        // 匹配 [CQ:reply,id=xxx] 格式
        Pattern replyPattern = Pattern.compile("\\[CQ:reply,id=([^,\\]]+)");
        Matcher matcher = replyPattern.matcher(raw);
        
        if (matcher.find()) {
            String replyId = matcher.group(1);
            LOGGER.debug("检测到回复消息 ID: {}", replyId);
            return replyId;
        }
        
        return null;
    }
    
    /**
     * 检查消息是否包含回复
     * 
     * @param raw 原始消息
     * @return 是否包含回复
     */
    public static boolean hasReply(String raw) {
        return extractReplyId(raw) != null;
    }
}
