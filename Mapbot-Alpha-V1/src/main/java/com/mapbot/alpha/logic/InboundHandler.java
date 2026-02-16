package com.mapbot.alpha.logic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mapbot.alpha.bridge.BridgeProxy;
import com.mapbot.alpha.bridge.ServerRegistry;
import com.mapbot.alpha.command.CommandRegistry;
import com.mapbot.alpha.command.QqRoleResolver;
import com.mapbot.alpha.command.impl.*;
import com.mapbot.alpha.config.AlphaConfig;
import com.mapbot.alpha.network.OneBotClient;
import com.mapbot.alpha.security.ContractRole;
import com.mapbot.alpha.utils.CQCodeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 入站消息处理器
 * 从 Reforged InboundHandler 移植
 */
public class InboundHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Inbound");
    
    /** 命令冷却缓存 */
    private static final ConcurrentHashMap<Long, Long> COMMAND_COOLDOWNS = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 5000;
    
    static {
        // 注册所有命令
        CommandRegistry.register("help", new HelpCommand());
        CommandRegistry.register("list", new ListCommand());
        CommandRegistry.register("status", new StatusCommand());
        CommandRegistry.register("id", new BindCommand());
        CommandRegistry.register("unbind", new UnbindCommand());
        CommandRegistry.register("sign", new SignCommand());
        CommandRegistry.register("accept", new AcceptCommand());
        CommandRegistry.register("inv", new InventoryCommand());
        CommandRegistry.register("location", new LocationCommand());
        CommandRegistry.register("mute", new MuteCommand());
        CommandRegistry.register("unmute", new UnmuteCommand());
        CommandRegistry.register("setperm", new SetPermCommand());
        CommandRegistry.register("myperm", new MyPermCommand());
        CommandRegistry.register("addadmin", new AddAdminCommand());
        CommandRegistry.register("removeadmin", new RemoveAdminCommand());
        CommandRegistry.register("reload", new ReloadCommand());
        CommandRegistry.register("adminunbind", new ForceUnbindCommand());
        CommandRegistry.register("agreeunbind", new AgreeUnbindCommand());
        CommandRegistry.register("playtime", new PlaytimeCommand());
        CommandRegistry.register("time", new TimeCommand());
        CommandRegistry.register("cdk", new CdkCommand());
        CommandRegistry.register("stopserver", new StopServerCommand());
        CommandRegistry.register("cancelstop", new CancelStopCommand());
        
        // 别名
        CommandRegistry.registerAlias("菜单", "help");
        CommandRegistry.registerAlias("在线", "list");
        CommandRegistry.registerAlias("状态", "status");
        CommandRegistry.registerAlias("tps", "status");
        CommandRegistry.registerAlias("绑定", "id");
        CommandRegistry.registerAlias("bind", "id");
        CommandRegistry.registerAlias("解绑", "unbind");
        CommandRegistry.registerAlias("签到", "sign");
        CommandRegistry.registerAlias("领取", "accept");
        CommandRegistry.registerAlias("位置", "location");
        CommandRegistry.registerAlias("禁言", "mute");
        CommandRegistry.registerAlias("解禁", "unmute");
        CommandRegistry.registerAlias("强制解绑", "adminunbind");
        CommandRegistry.registerAlias("在线时长", "playtime");
        CommandRegistry.registerAlias("时间", "time");
        CommandRegistry.registerAlias("setrole", "setperm");
        CommandRegistry.registerAlias("role", "myperm");
        CommandRegistry.registerAlias("兑换码", "cdk");
        CommandRegistry.registerAlias("关服", "stopserver");
        CommandRegistry.registerAlias("取消关服", "cancelstop");
        
        LOGGER.info("命令系统初始化完成: {} 个命令", CommandRegistry.getCommands().size());
    }
    
    /**
     * 处理 OneBot 原始消息
     */
    public static void handleMessage(String rawJson) {
        try {
            JsonObject json = JsonParser.parseString(rawJson).getAsJsonObject();
            String postType = getStringOrNull(json, "post_type");
            
            if (postType == null) {
                return;
            }
            
        switch (postType) {
            case "message" -> handleChatMessage(json);
            case "notice" -> handleNoticeEvent(json);
            case "meta_event" -> handleMetaEvent(json);
            default -> LOGGER.debug("忽略事件: {}", postType);
        }
    } catch (Exception e) {
        LOGGER.error("协议解析异常", e);
    }
}

    private static void handleChatMessage(JsonObject json) {
        String messageType = getStringOrNull(json, "message_type");
        if (messageType == null) {
            return;
        }

        switch (messageType) {
            case "group" -> handleGroupMessage(json);
            case "private" -> handlePrivateMessage(json);
            default -> LOGGER.debug("忽略消息类型: {}", messageType);
        }
    }

    private static void handlePrivateMessage(JsonObject json) {
        if (!"private".equals(getStringOrNull(json, "message_type"))) return;

        String rawMessage = getStringOrNull(json, "raw_message");
        if (rawMessage == null || rawMessage.isEmpty()) return;

        long senderQQ = getLongOrZero(json, "user_id");
        if (rawMessage.startsWith("#")) {
            handleCommandDispatch(rawMessage.substring(1).trim(), senderQQ, 0, true);
        }
    }

    private static void handleGroupMessage(JsonObject json) {
        if (!"group".equals(getStringOrNull(json, "message_type"))) return;
        
        long sourceGroupId = getLongOrZero(json, "group_id");
        boolean isFromPlayerGroup = (sourceGroupId == AlphaConfig.getPlayerGroupId());
        boolean isFromAdminGroup = (sourceGroupId == AlphaConfig.getAdminGroupId());
        
        if (!isFromPlayerGroup && !isFromAdminGroup) return;
        
        String rawMessage = getStringOrNull(json, "raw_message");
        if (rawMessage == null || rawMessage.isEmpty()) return;
        
        long senderQQ = getLongOrZero(json, "user_id");
        String nickname = "未知用户";
        JsonObject sender = json.getAsJsonObject("sender");
        if (sender != null) {
            String nick = getStringOrNull(sender, "nickname");
            if (nick != null) nickname = nick;
        }
        
        // 命令处理
        if (rawMessage.startsWith("#")) {
            handleCommandDispatch(rawMessage.substring(1).trim(), senderQQ, sourceGroupId, false);
        } 
        // 普通消息转发到 MC
        else if (isFromPlayerGroup) {
            forwardToMinecraft(rawMessage, nickname, senderQQ, sourceGroupId);
        }
    }
    
    private static void handleCommandDispatch(String fullCmd, long senderQQ, long sourceGroupId, boolean privateChat) {
        String[] parts = fullCmd.split("\\s+", 2);
        String cmdName = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";
        
        // 冷却检查
        if (!QqRoleResolver.hasAtLeast(senderQQ, ContractRole.ADMIN)) {
            long now = System.currentTimeMillis();
            if (COMMAND_COOLDOWNS.containsKey(senderQQ) && now - COMMAND_COOLDOWNS.get(senderQQ) < COOLDOWN_MS) {
                sendCommandFeedback(senderQQ, sourceGroupId, privateChat, "[提示] 慢点，请等待几秒再试");
                return;
            }
            COMMAND_COOLDOWNS.put(senderQQ, now);
        }
        
        // 分发
        if (!CommandRegistry.dispatch(cmdName, args, senderQQ, sourceGroupId, privateChat)) {
            sendCommandFeedback(senderQQ, sourceGroupId, privateChat, "[提示] 未知命令，输入 #help 查看帮助");
        }
    }

    private static void sendCommandFeedback(long senderQQ, long sourceGroupId, boolean privateChat, String message) {
        if (privateChat) {
            OneBotClient.INSTANCE.sendPrivateMessage(senderQQ, message);
        } else {
            OneBotClient.INSTANCE.sendGroupMessage(sourceGroupId, message);
        }
    }
    
    private static void forwardToMinecraft(String rawMessage, String nickname, long senderQQ, long sourceGroupId) {
        String parsed = CQCodeParser.parse(rawMessage);
        if (parsed.isEmpty()) return;
        
        List<Long> atQQList = CQCodeParser.extractAtTargets(rawMessage);

        // 解析被 @ 的玩家名
        java.util.List<String> atPlayerNames = new java.util.ArrayList<>();
        for (Long atQq : atQQList) {
            String uuid = com.mapbot.alpha.data.DataManager.INSTANCE.getBinding(atQq);
            if (uuid != null && !uuid.isBlank()) {
                String playerName = com.mapbot.alpha.data.DataManager.INSTANCE.getPlayerName(uuid);
                if (playerName != null && !playerName.isBlank()) {
                    atPlayerNames.add(playerName);
                }
            }
        }

        // 检测回复消息
        String replyId = CQCodeParser.extractReplyId(rawMessage);
        
        // 构建消息并广播到所有服务器
        String formattedMsg = String.format("[QQ] %s: %s", nickname, parsed);
        
        // 构建分发消息
        StringBuilder json = new StringBuilder();
        json.append("{\"type\":\"qq_message\"");
        json.append(",\"sender\":\"").append(escapeJson(nickname)).append("\"");
        json.append(",\"content\":\"").append(escapeJson(parsed)).append("\"");
        json.append(",\"rawContent\":\"").append(escapeJson(rawMessage)).append("\"");
        json.append(",\"senderQQ\":").append(senderQQ);
        json.append(",\"groupId\":").append(sourceGroupId);
        if (!atQQList.isEmpty()) {
            json.append(",\"atList\":[");
            for (int i = 0; i < atQQList.size(); i++) {
                if (i > 0) json.append(",");
                json.append(atQQList.get(i));
            }
            json.append("]");
        }
        if (!atPlayerNames.isEmpty()) {
            json.append(",\"atPlayerNames\":[");
            for (int i = 0; i < atPlayerNames.size(); i++) {
                if (i > 0) json.append(",");
                json.append("\"").append(escapeJson(atPlayerNames.get(i))).append("\"");
            }
            json.append("]");
        }
        if (replyId != null) {
            json.append(",\"replyId\":\"").append(escapeJson(replyId)).append("\"");
        }
        json.append("}");
        
        ServerRegistry.INSTANCE.broadcast(json.toString());
        LOGGER.debug("[QQ->MC] {}", formattedMsg);
    }
    
    private static void handleNoticeEvent(JsonObject json) {
        String type = getStringOrNull(json, "notice_type");
        if ("group_increase".equals(type)) {
            long gid = getLongOrZero(json, "group_id");
            long uid = getLongOrZero(json, "user_id");
            if (gid == AlphaConfig.getPlayerGroupId()) {
                OneBotClient.INSTANCE.sendGroupMessage(gid, 
                    String.format("[CQ:at,qq=%d] [Bot] 欢迎入群。\n请发送 #id <游戏ID> 绑定白名单。", uid));
            }
        }
    }
    
    private static void handleMetaEvent(JsonObject json) {
        if ("lifecycle".equals(getStringOrNull(json, "meta_event_type"))) {
            LOGGER.info("OneBot 生命周期: {}", getStringOrNull(json, "sub_type"));
        }
    }
    
    private static String getStringOrNull(JsonObject j, String k) {
        JsonElement e = j.get(k);
        return (e != null && !e.isJsonNull()) ? e.getAsString() : null;
    }
    
    private static long getLongOrZero(JsonObject j, String k) {
        if (j == null) return 0;
        JsonElement e = j.get(k);
        return (e != null && !e.isJsonNull()) ? e.getAsLong() : 0;
    }
    
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}
