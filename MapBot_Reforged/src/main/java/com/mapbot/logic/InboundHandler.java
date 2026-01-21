/*
 * MapBot Reforged - 入站消息处理器 (架构 v2)
 * 
 * 遵从 .ai_rules.md 治理规则。
 * 负责解析 OneBot 协议消息并分发至命令系统或核心逻辑。
 */

package com.mapbot.logic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mapbot.command.CommandRegistry;
import com.mapbot.command.*;
import com.mapbot.config.BotConfig;
import com.mapbot.data.DataManager;
import com.mapbot.data.GroupMemberCache;
import com.mapbot.network.BotClient;
import com.mapbot.utils.CQCodeParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 入站消息处理器
 * 核心职责: 协议解析、消息分发、回复上下文管理
 */
public class InboundHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/Inbound");
    
    /** 命令冷却缓存 (QQ -> 上次执行时间) */
    private static final ConcurrentHashMap<Long, Long> COMMAND_COOLDOWNS = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 5000;
    
    /** 回复上下文缓存 */
    private record ReplyContext(String replierNickname, String rawMessage, List<Long> atQQList, long sourceGroupId, long timestamp) {}
    private static final ConcurrentHashMap<String, ReplyContext> PENDING_REPLY_CONTEXTS = new ConcurrentHashMap<>();

    static {
        // --- 注册所有命令 ---
        // 基础命令
        CommandRegistry.register("help", new HelpCommand());
        CommandRegistry.register("list", new ListCommand());
        CommandRegistry.register("status", new StatusCommand());
        CommandRegistry.register("id", new BindCommand());
        CommandRegistry.register("unbind", new UnbindCommand());
        CommandRegistry.register("playtime", new PlaytimeCommand());
        CommandRegistry.register("report", new ReportCommand());
        CommandRegistry.register("myperm", new MyPermCommand());

        // 管理命令
        CommandRegistry.register("stopserver", new StopServerCommand());
        CommandRegistry.register("cancelstop", new CancelStopCommand());
        CommandRegistry.register("inv", new InventoryCommand());
        CommandRegistry.register("location", new LocationCommand());
        CommandRegistry.register("mute", new MuteCommand());
        CommandRegistry.register("unmute", new UnmuteCommand());
        CommandRegistry.register("setperm", new SetPermCommand());
        CommandRegistry.register("adminunbind", new ForceUnbindCommand());
        CommandRegistry.register("addadmin", new AddAdminCommand());
        CommandRegistry.register("removeadmin", new RemoveAdminCommand());
        CommandRegistry.register("reload", new ReloadCommand());
        CommandRegistry.register("sign", new SignCommand());
        CommandRegistry.register("accept", new AcceptCommand());
        CommandRegistry.register("cdk", new CdkCommand());

        // --- 注册别名 ---
        CommandRegistry.registerAlias("菜单", "help");
        CommandRegistry.registerAlias("在线", "list");
        CommandRegistry.registerAlias("状态", "status");
        CommandRegistry.registerAlias("tps", "status");
        CommandRegistry.registerAlias("绑定", "id");
        CommandRegistry.registerAlias("bind", "id");
        CommandRegistry.registerAlias("解绑", "unbind");
        CommandRegistry.registerAlias("在线时长", "playtime");
        CommandRegistry.registerAlias("报告", "report");
        CommandRegistry.registerAlias("位置", "location");
        CommandRegistry.registerAlias("关服", "stopserver");
        CommandRegistry.registerAlias("取消关服", "cancelstop");
        CommandRegistry.registerAlias("禁言", "mute");
        CommandRegistry.registerAlias("解禁", "unmute");
        CommandRegistry.registerAlias("签到", "sign");
        CommandRegistry.registerAlias("领取", "accept");
        CommandRegistry.registerAlias("兑换码", "cdk");
    }

    /**
     * 处理 WebSocket 原始消息
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
                case "message" -> handleGroupMessage(json);
                case "notice" -> handleNoticeEvent(json);
                case "meta_event" -> handleMetaEvent(json);
                default -> LOGGER.debug("忽略未知事件: {}", postType);
            }
        } catch (Exception e) {
            LOGGER.error("协议解析异常", e);
        }
    }

    private static void handleApiEcho(JsonObject json) {
        String echo = getStringOrNull(json, "echo");
        if (echo != null) {
            if (echo.startsWith("load_members_")) handleGroupMemberListResponse(json);
            else if (echo.startsWith("reply_")) handleGetMsgResponse(json, echo);
        }
    }

    private static void handleGroupMessage(JsonObject json) {
        if (!"group".equals(getStringOrNull(json, "message_type"))) return; 
        
        long sourceGroupId = getLongOrZero(json, "group_id");
        boolean isFromPlayerGroup = (sourceGroupId == BotConfig.getPlayerGroupId());
        boolean isFromAdminGroup = (sourceGroupId == BotConfig.getAdminGroupId());
        
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

        if (rawMessage.startsWith("#")) {
            handleCommandDispatch(rawMessage.substring(1).trim(), senderQQ, sourceGroupId);
        } else if (isFromPlayerGroup) {
            processGroupChatForward(rawMessage, nickname, sourceGroupId);
        }
    }

    private static void handleCommandDispatch(String fullCmd, long senderQQ, long sourceGroupId) {
        String[] parts = fullCmd.split("\s+", 2);
        String cmdName = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        // 冷却检查
        if (!DataManager.INSTANCE.isAdmin(senderQQ)) {
            long now = System.currentTimeMillis();
            if (COMMAND_COOLDOWNS.containsKey(senderQQ) && now - COMMAND_COOLDOWNS.get(senderQQ) < COOLDOWN_MS) {
                sendReplyToQQ(sourceGroupId, "[提示] 慢点，请等待几秒再试");
                return;
            }
            COMMAND_COOLDOWNS.put(senderQQ, now);
        }

        // 分发至注册表
        if (!CommandRegistry.dispatch(cmdName, args, senderQQ, sourceGroupId)) {
            sendReplyToQQ(sourceGroupId, "[提示] 未知命令，输入 #help 查看帮助");
        }
    }

    private static void processGroupChatForward(String rawMessage, String nickname, long sourceGroupId) {
        String parsed = CQCodeParser.parse(rawMessage);
        if (parsed.isEmpty()) return;

        String replyId = CQCodeParser.extractReplyId(rawMessage);
        List<Long> atQQList = CQCodeParser.extractAtTargets(rawMessage);

        if (replyId != null) {
            requestOriginalMessage(replyId, nickname, rawMessage, atQQList, sourceGroupId);
            return;
        }

        sendPersonalizedMessage(String.format("§b[QQ]§r <%s> %s", nickname, parsed), atQQList, nickname);
    }

    // --- 核心事件逻辑 ---

    private static void handleNoticeEvent(JsonObject json) {
        String type = getStringOrNull(json, "notice_type");
        if ("group_increase".equals(type)) {
            long gid = getLongOrZero(json, "group_id");
            long uid = getLongOrZero(json, "user_id");
            if (gid == BotConfig.getPlayerGroupId()) {
                BotClient.INSTANCE.sendGroupMessage(gid, String.format("[CQ:at,qq=%d] [Bot] 欢迎入群！\n请发送 #id <游戏ID> 绑定白名单。", uid));
            }
        }
    }

    private static void handleMetaEvent(JsonObject json) {
        if ("lifecycle".equals(getStringOrNull(json, "meta_event_type"))) {
            LOGGER.info("OneBot 生命周期变更: {}", getStringOrNull(json, "sub_type"));
        }
    }

    // --- 个性化通知逻辑 ---

    public static void sendPersonalizedMessage(String baseMessage, List<Long> atQQList, String senderNick) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        server.execute(() -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                boolean isAt = false;
                for (Long qq : atQQList) {
                    if (player.getUUID().toString().equals(DataManager.INSTANCE.getBinding(qq))) {
                        isAt = true;
                        break;
                    }
                }

                if (isAt) {
                    player.sendSystemMessage(Component.literal(baseMessage.replace("@" + player.getName().getString(), "§l§6@" + player.getName().getString() + "§r")));
                    player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                    player.connection.send(new ClientboundSetTitleTextPacket(Component.literal("§b[QQ] §f" + senderNick + " §6@了你!")));
                    player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 2.0f);
                } else {
                    player.sendSystemMessage(Component.literal(baseMessage));
                }
            }
        });
    }

    // --- 通讯辅助方法 ---

    public static void sendReplyToQQ(long groupId, String message) {
        if (groupId == 0) return;
        JsonObject params = new JsonObject();
        params.addProperty("group_id", groupId);
        params.addProperty("message", message);
        
        JsonObject packet = new JsonObject();
        packet.addProperty("action", "send_group_msg");
        packet.add("params", params);
        packet.addProperty("echo", "reply_" + System.currentTimeMillis());
        BotClient.INSTANCE.sendPacket(packet);
    }

    private static void requestOriginalMessage(String mid, String nick, String raw, List<Long> at, long gid) {
        String echo = "reply_" + UUID.randomUUID();
        PENDING_REPLY_CONTEXTS.put(echo, new ReplyContext(nick, raw, at, gid, System.currentTimeMillis()));
        
        JsonObject packet = new JsonObject();
        packet.addProperty("action", "get_msg");
        JsonObject p = new JsonObject();
        p.addProperty("message_id", mid);
        packet.add("params", p);
        packet.addProperty("echo", echo);
        BotClient.INSTANCE.sendPacket(packet);
    }

    private static void handleGetMsgResponse(JsonObject json, String echo) {
        ReplyContext ctx = PENDING_REPLY_CONTEXTS.remove(echo);
        if (ctx == null) return;

        JsonObject data = json.getAsJsonObject("data");
        String originalPlayer = null;
        if (data != null && getLongOrZero(data.getAsJsonObject("sender"), "user_id") == BotConfig.getBotQQ()) {
            String msg = getStringOrNull(data, "raw_message");
            if (msg == null) msg = getStringOrNull(data, "message");
            if (msg != null && msg.contains("[") && msg.contains("]")) {
                originalPlayer = msg.substring(msg.indexOf("[") + 1, msg.indexOf("]")).trim();
            }
        }

        String parsed = CQCodeParser.parse(ctx.rawMessage);
        if (originalPlayer != null) {
            parsed = parsed.replace("@" + BotConfig.getBotQQ(), "@" + originalPlayer);
        }
        
        sendPersonalizedMessage(String.format("§b[QQ]§r <%s> %s", ctx.replierNickname, parsed), ctx.atQQList, ctx.replierNickname);
    }

    private static void handleGroupMemberListResponse(JsonObject json) {
        JsonElement data = json.get("data");
        if (data != null && data.isJsonArray()) {
            java.util.Map<Long, String> members = new java.util.HashMap<>();
            for (JsonElement e : data.getAsJsonArray()) {
                JsonObject m = e.getAsJsonObject();
                long uid = getLongOrZero(m, "user_id");
                String name = getStringOrNull(m, "card");
                if (name == null || name.isEmpty()) name = getStringOrNull(m, "nickname");
                if (name != null) members.put(uid, name);
            }
            GroupMemberCache.INSTANCE.loadMembers(members);
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
}