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
import java.util.UUID;

/**
 * 入站消息处理器
 * 从 Reforged InboundHandler 移植
 */
public class InboundHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Inbound");
    
    /** 命令冷却缓存 */
    private static final ConcurrentHashMap<Long, Long> COMMAND_COOLDOWNS = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 5000;
    
    // 回复消息回调上下文
    private static final ConcurrentHashMap<String, ReplyForwardContext> PENDING_REPLY_CONTEXTS = new ConcurrentHashMap<>();
    
    private record ReplyForwardContext(String nickname, String rawMessage, long senderQQ, long sourceGroupId,
                                        java.util.List<Long> atQQList, java.util.List<String> atPlayerNames,
                                        long timestamp) {}
    
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
                handleApiEcho(json);
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

    /**
     * 处理 OneBot API 回包 (echo 响应)
     */
    private static void handleApiEcho(JsonObject json) {
        com.google.gson.JsonElement echoEl = json.get("echo");
        if (echoEl == null || echoEl.isJsonNull()) return;
        String echo = echoEl.getAsString();
        
        if (echo.startsWith("reply_fwd_")) {
            handleReplyForwardResponse(json, echo);
        }
    }
    
    /**
     * 处理回复消息的 get_msg 响应
     * 从原始消息内容中提取被回复的 MC 玩家名, 然后转发到 Bridge
     */
    private static void handleReplyForwardResponse(JsonObject json, String echo) {
        ReplyForwardContext ctx = PENDING_REPLY_CONTEXTS.remove(echo);
        if (ctx == null) return;
        
        // 提取原始消息中的玩家名
        String replyToPlayer = null;
        JsonObject data = json.getAsJsonObject("data");
        if (data != null) {
            // 诊断日志: 查看 get_msg 返回了什么
            String rawMsg = getStringOrNull(data, "raw_message");
            String msgContent = getStringOrNull(data, "message");
            JsonObject sender = data.getAsJsonObject("sender");
            long senderQQ = 0;
            if (sender != null) {
                com.google.gson.JsonElement uidEl = sender.get("user_id");
                if (uidEl != null && !uidEl.isJsonNull()) senderQQ = uidEl.getAsLong();
            }
            LOGGER.info("[Reply] get_msg 响应: senderQQ={}, raw_message={}, message={}", senderQQ, rawMsg, msgContent);
            
            // 确认原始消息是 bot 自己发的
            if (senderQQ == AlphaConfig.getBotQQ()) {
                String msg = rawMsg != null ? rawMsg : msgContent;
                if (msg != null) {
                    // 格式1: [serverId] 玩家名: 内容  (MC→QQ 聊天消息)
                    java.util.regex.Matcher m1 = java.util.regex.Pattern.compile("\\[.*?\\]\\s+(\\S+?):\\s").matcher(msg);
                    if (m1.find()) {
                        replyToPlayer = m1.group(1);
                    }
                    // 格式2: <玩家名> 内容  (旧格式兼容)
                    if (replyToPlayer == null) {
                        int lt = msg.indexOf("<");
                        int gt = msg.indexOf(">");
                        if (lt >= 0 && gt > lt) {
                            replyToPlayer = msg.substring(lt + 1, gt).trim();
                        }
                    }
                    // 格式3: [+] 玩家名 (加入事件)
                    if (replyToPlayer == null) {
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[\\+\\]\\s+(\\S+)").matcher(msg);
                        if (m.find()) replyToPlayer = m.group(1);
                    }
                    // 格式4: [-] 玩家名 (离开事件)
                    if (replyToPlayer == null) {
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[-\\]\\s+(\\S+)").matcher(msg);
                        if (m.find()) replyToPlayer = m.group(1);
                    }
                }
            } else {
                LOGGER.info("[Reply] 原始消息不是 bot 发的 (senderQQ={}, botQQ={}), 跳过", senderQQ, AlphaConfig.getBotQQ());
            }
        } else {
            LOGGER.warn("[Reply] get_msg 响应中没有 data 字段, 完整响应: {}", json);
        }
        
        LOGGER.info("[Reply] 原始消息玩家: {}", replyToPlayer);
        
        // 如果提取到了玩家名, 加入 atPlayerNames
        java.util.List<String> atPlayerNames = ctx.atPlayerNames;
        if (replyToPlayer != null && !replyToPlayer.isBlank()) {
            if (!atPlayerNames.contains(replyToPlayer)) {
                atPlayerNames.add(replyToPlayer);
            }
        }
        
        // 现在构建 JSON 并转发到 Bridge
        String parsed = CQCodeParser.parse(ctx.rawMessage);
        if (parsed.isEmpty()) return;
        
        // 如果回复了 bot, 替换 @botQQ 为 @玩家名
        if (replyToPlayer != null) {
            parsed = parsed.replace("@" + AlphaConfig.getBotQQ(), "@" + replyToPlayer);
        }
        
        StringBuilder jsonOut = new StringBuilder();
        jsonOut.append("{\"type\":\"qq_message\"");
        jsonOut.append(",\"sender\":\"").append(escapeJson(ctx.nickname)).append("\"");
        jsonOut.append(",\"content\":\"").append(escapeJson(parsed)).append("\"");
        jsonOut.append(",\"rawContent\":\"").append(escapeJson(ctx.rawMessage)).append("\"");
        jsonOut.append(",\"senderQQ\":").append(ctx.senderQQ);
        jsonOut.append(",\"groupId\":").append(ctx.sourceGroupId);
        if (!ctx.atQQList.isEmpty()) {
            jsonOut.append(",\"atList\":[");
            for (int i = 0; i < ctx.atQQList.size(); i++) {
                if (i > 0) jsonOut.append(",");
                jsonOut.append(ctx.atQQList.get(i));
            }
            jsonOut.append("]");
        }
        if (!atPlayerNames.isEmpty()) {
            jsonOut.append(",\"atPlayerNames\":[");
            for (int i = 0; i < atPlayerNames.size(); i++) {
                if (i > 0) jsonOut.append(",");
                jsonOut.append("\"").append(escapeJson(atPlayerNames.get(i))).append("\"");
            }
            jsonOut.append("]");
        }
        jsonOut.append("}");
        
        ServerRegistry.INSTANCE.broadcast(jsonOut.toString());
        LOGGER.debug("[QQ->MC] (reply) [QQ] {}: {}", ctx.nickname, parsed);
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
        
        // B. 发送者名字解析: 已绑定的用户显示游戏ID
        String displayName = nickname;
        String senderUuid = com.mapbot.alpha.data.DataManager.INSTANCE.getBinding(senderQQ);
        if (senderUuid != null && !senderUuid.isBlank()) {
            String gameName = com.mapbot.alpha.data.DataManager.INSTANCE.getPlayerName(senderUuid);
            if (gameName != null && !gameName.isBlank()) {
                displayName = gameName;
            }
        }
        
        // 构建消息并广播到所有服务器
        String formattedMsg = String.format("[QQ] %s: %s", displayName, parsed);
        
        // 构建分发消息
        StringBuilder json = new StringBuilder();
        json.append("{\"type\":\"qq_message\"");
        json.append(",\"sender\":\"").append(escapeJson(displayName)).append("\"");
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
        json.append("}");
        
        // 如果是回复消息且 @ 了 bot, 走异步回复识别流程
        if (replyId != null) {
            String echo = "reply_fwd_" + UUID.randomUUID();
            // 保存 60 秒超时的上下文 (清理过期上下文)
            long now = System.currentTimeMillis();
            PENDING_REPLY_CONTEXTS.entrySet().removeIf(e -> (now - e.getValue().timestamp()) > 60_000);
            PENDING_REPLY_CONTEXTS.put(echo, new ReplyForwardContext(
                displayName, rawMessage, senderQQ, sourceGroupId, atQQList, atPlayerNames, now));
            
            // 发送 get_msg API 请求查询被回复的原始消息
            String getMsgJson = String.format(
                "{\"action\":\"get_msg\",\"params\":{\"message_id\":%s},\"echo\":\"%s\"}",
                replyId, echo);
            OneBotClient.INSTANCE.sendPacket(getMsgJson);
            LOGGER.info("[Reply] 检测到回复消息, 查询原始消息 replyId={}, echo={}", replyId, echo);
            return; // 异步处理, 等 handleReplyForwardResponse 回调
        }
        
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
        if (e == null || e.isJsonNull()) return null;
        if (e.isJsonPrimitive()) return e.getAsString();
        return null; // JsonArray 或 JsonObject 不转为 String
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
