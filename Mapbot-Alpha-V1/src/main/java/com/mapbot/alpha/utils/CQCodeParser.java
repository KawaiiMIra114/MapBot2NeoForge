package com.mapbot.alpha.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CQ 码解析器
 * 从 Reforged 移植
 */
public class CQCodeParser {
    
    // CQ码正则
    private static final Pattern CQ_PATTERN = Pattern.compile("\\[CQ:(\\w+)(,[^\\]]*)?\\]");
    // 兼容多参数格式: [CQ:at,qq=123] 或 [CQ:at,qq=123,name=xxx]
    private static final Pattern AT_PATTERN = Pattern.compile("\\[CQ:at,qq=(\\d+)[^\\]]*\\]");
    private static final Pattern REPLY_PATTERN = Pattern.compile("\\[CQ:reply,id=(-?\\d+)[^\\]]*\\]");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("\\[CQ:image[^\\]]*\\]");
    private static final Pattern FACE_PATTERN = Pattern.compile("\\[CQ:face,id=(\\d+)[^\\]]*\\]");
    
    /**
     * 解析 CQ 码，转换为可读文本
     */
    public static String parse(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        
        String result = raw;
        
        // 移除回复引用
        result = REPLY_PATTERN.matcher(result).replaceAll("");
        
        // 转换 @ (兼容多参数格式)
        Matcher atMatcher = AT_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (atMatcher.find()) {
            String qq = atMatcher.group(1);
            // D. @QQ号→@玩家名解析
            String atDisplay = qq;
            try {
                long qqNum = Long.parseLong(qq);
                String uuid = com.mapbot.alpha.data.DataManager.INSTANCE.getBinding(qqNum);
                if (uuid != null && !uuid.isBlank()) {
                    String pName = com.mapbot.alpha.data.DataManager.INSTANCE.getPlayerName(uuid);
                    if (pName != null && !pName.isBlank()) {
                        atDisplay = pName;
                    }
                }
            } catch (NumberFormatException ignored) {}
            atMatcher.appendReplacement(sb, "@" + atDisplay);
        }
        atMatcher.appendTail(sb);
        result = sb.toString();
        
        // 移除图片
        result = IMAGE_PATTERN.matcher(result).replaceAll("[图片]");
        
        // 转换表情
        result = FACE_PATTERN.matcher(result).replaceAll("[表情]");
        
        // 清理其他 CQ 码
        result = CQ_PATTERN.matcher(result).replaceAll("");
        
        return result.trim();
    }
    
    /**
     * 提取所有 @ 目标的 QQ 号
     */
    public static List<Long> extractAtTargets(String raw) {
        List<Long> targets = new ArrayList<>();
        if (raw == null) return targets;
        
        Matcher m = AT_PATTERN.matcher(raw);
        while (m.find()) {
            try {
                targets.add(Long.parseLong(m.group(1)));
            } catch (NumberFormatException ignored) {}
        }
        return targets;
    }
    
    /**
     * 提取回复消息 ID
     */
    public static String extractReplyId(String raw) {
        if (raw == null) return null;
        Matcher m = REPLY_PATTERN.matcher(raw);
        return m.find() ? m.group(1) : null;
    }
}
