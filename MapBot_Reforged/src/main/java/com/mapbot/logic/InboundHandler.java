/*
 * MapBot Reforged - 入站消息处理器
 * 
 * 遵从 .ai_rules.md 中定义的治理规则。
 * 负责解析来自 NapCat 的 WebSocket 消息并调度到游戏主线程执行。
 * 
 * 参考: ./Project_Docs/Architecture/Protocol_Spec.md
 * 关键: 使用 ServerLifecycleHooks 实现跨线程安全调用。
 * 
 * Task #009 更新: Java 21 Switch, #stopserver, #addadmin
 * Task #010 更新: #id/#bind, #unbind
 * Task #012-STEP3 更新: 双群结构, CQ码解析, 权限分离
 * Task #012-STEP4 更新: @提及游戏内 Title 通知
 * Task #013-STEP3 更新: 处理群成员列表响应, 加载昵称缓存
 */

package com.mapbot.logic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mapbot.config.BotConfig;
import com.mapbot.data.DataManager;
import com.mapbot.data.PlaytimeManager;
import com.mapbot.network.BotClient;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mapbot.utils.CQCodeParser;
import com.mapbot.data.GroupMemberCache;
import com.google.gson.JsonArray;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 入站消息处理器
 * 解析来自 QQ 的 JSON 消息，并在游戏主线程中广播
 */
public class InboundHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/Inbound");
    
    /** 玩家名正则验证: 3-16 位字母数字下划线 */
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");
    
    // ========== Task #015 STEP1: 回复上下文缓存 ==========
    /**
     * 回复消息上下文 - 用于存储待处理的回复消息信息
     * Task #015-STEP1 新增
     */
    private record ReplyContext(
            String replierNickname,   // 回复者昵称
            String rawMessage,        // 原始 CQ 码消息
            List<Long> atQQList,      // 被 @ 的 QQ 列表
            long sourceGroupId,       // 来源群号
            long timestamp            // 创建时间戳
    ) {}
    
    /** 待处理的回复消息上下文 Map (UUID -> Context) */
    private static final ConcurrentHashMap<String, ReplyContext> PENDING_REPLY_CONTEXTS = new ConcurrentHashMap<>();
    // ========== End Task #015 STEP1 ==========
    
    /** 仅限管理群使用的命令集合 */
    private static final Set<String> ADMIN_ONLY_COMMANDS = Set.of(
            "inv", "stopserver", "关服", "reload", 
            "addadmin", "removeadmin", "adminunbind",
            "location", "位置"  // Task #016-STEP3
    );

    /**
     * 处理从 WebSocket 接收到的原始文本消息
     * 
     * @param rawJson 原始 JSON 字符串
     */
    public static void handleMessage(String rawJson) {
        try {
            JsonObject json = JsonParser.parseString(rawJson).getAsJsonObject();

            // 检查是否为消息类型
            String postType = getStringOrNull(json, "post_type");
            
            // 空值检查: echo 响应或其他非事件数据包没有 post_type
            if (postType == null) {
                // Task #013-STEP3: 检查是否为群成员列表响应
                String echo = getStringOrNull(json, "echo");
                if (echo != null && echo.startsWith("load_members_")) {
                    handleGroupMemberListResponse(json);
                } else if (echo != null && echo.startsWith("reply_")) {
                    // Task #015-STEP1: 处理 get_msg API 响应 (回复上下文)
                    handleGetMsgResponse(json, echo);
                } else {
                    LOGGER.debug("忽略无 post_type 的数据包 (echo 响应?)");
                }
                return;
            }

            switch (postType) {
                case "message" -> handleGroupMessage(json);
                case "notice" -> handleNoticeEvent(json);  // Task #016-STEP4
                case "meta_event" -> handleMetaEvent(json);
                default -> LOGGER.debug("忽略未知事件类型: {}", postType);
            }

        } catch (Exception e) {
            LOGGER.error("解析 JSON 失败: {}", e.getMessage());
        }
    }

    /**
     * 处理群消息事件
     * Task #013-STEP4: 添加时间戳日志用于延迟排查
     */
    private static void handleGroupMessage(JsonObject json) {
        // Task #013-STEP4: 延迟排查时间戳
        final long t0 = System.currentTimeMillis();
        
        String messageType = getStringOrNull(json, "message_type");

        // 仅处理群消息
        if (!"group".equals(messageType)) {
            return;
        }
        
        // === 双群结构: 检查消息来源 ===
        long playerGroupId = BotConfig.getPlayerGroupId();
        long adminGroupId = BotConfig.getAdminGroupId();
        long sourceGroupId = getLongOrZero(json, "group_id");
        
        // 判断消息来源类型
        boolean isFromPlayerGroup = (sourceGroupId == playerGroupId && playerGroupId != 0L);
        boolean isFromAdminGroup = (sourceGroupId == adminGroupId && adminGroupId != 0L);
        
        // 如果既不是玩家群也不是管理群，忽略
        if (!isFromPlayerGroup && !isFromAdminGroup) {
            LOGGER.debug("忽略来自未配置群的消息: {}", sourceGroupId);
            return;
        }

        // 提取原始消息内容
        String rawMessage = getStringOrNull(json, "raw_message");
        if (rawMessage == null || rawMessage.isEmpty()) {
            return;
        }

        // 提取发送者 QQ
        long senderQQ = getLongOrZero(json, "user_id");

        // 提取发送者信息
        JsonObject sender = json.getAsJsonObject("sender");
        String nickname = "未知用户";
        if (sender != null) {
            String senderNick = getStringOrNull(sender, "nickname");
            if (senderNick != null) {
                nickname = senderNick;
            }
        }
        
        // Task #013-STEP4: 解析完成
        LOGGER.debug("[TIMING] handleGroupMessage 解析完成: {}ms", System.currentTimeMillis() - t0);

        LOGGER.info("收到群消息 [{}]: {} ({}) -> {}", 
                isFromAdminGroup ? "管理群" : "玩家群", nickname, senderQQ, rawMessage);

        // === 命令分发 ===
        if (rawMessage.startsWith("#")) {
            handleCommand(rawMessage, senderQQ, sourceGroupId, isFromAdminGroup);
        } else if (isFromPlayerGroup) {
            // 仅从玩家群转发普通消息到游戏 (管理群的普通消息不转发)
            // 解析 CQ 码
            String parsedMessage = CQCodeParser.parse(rawMessage);
            
            // 如果解析后为空 (纯图片/表情)，跳过
            if (parsedMessage.isEmpty()) {
                LOGGER.debug("消息解析后为空，跳过转发");
                return;
            }
            
            // Task #015-STEP2: 检测回复消息
            String replyMsgId = CQCodeParser.extractReplyId(rawMessage);
            
            // 提取被@的 QQ 号列表
            List<Long> atQQList = CQCodeParser.extractAtTargets(rawMessage);
            
            if (replyMsgId != null) {
                // Task #015-STEP2: 回复消息 - 延迟转发
                // 将上下文存入 Map，等 get_msg 响应后统一处理
                // 同时传递 parsedMessage 用于后续替换
                requestOriginalMessage(replyMsgId, nickname, rawMessage, atQQList, sourceGroupId);
                // 延迟发送：不在这里调用 sendPersonalizedMessage
                LOGGER.debug("[TIMING] 回复消息延迟转发: {}ms (since t0)", System.currentTimeMillis() - t0);
                return; // 等待 handleGetMsgResponse 处理
            }
            
            // 非回复消息：立即转发
            String formattedMessage = String.format("§b[QQ]§r <%s> %s", nickname, parsedMessage);
            
            // Task #013-STEP4: 记录调度前时间
            LOGGER.debug("[TIMING] 准备调度到主线程: {}ms (since t0)", System.currentTimeMillis() - t0);
            
            // Task #014-Fix: 个性化消息发送（被@者看到醒目格式+Title）
            sendPersonalizedMessage(formattedMessage, atQQList, nickname);
        }
        // 管理群的普通消息不做任何处理
    }

    /**
     * 处理命令 (Java 21 Switch 表达式)
     * Task #012-STEP3: 新增群来源参数，支持命令权限分离
     * 
     * @param message 完整命令字符串
     * @param senderQQ 发送者 QQ
     * @param sourceGroupId 消息来源群号
     * @param isFromAdminGroup 是否来自管理群
     */
    private static void handleCommand(String message, long senderQQ, long sourceGroupId, boolean isFromAdminGroup) {
        String rawCmd = message.substring(1).trim();
        String cmd = rawCmd.toLowerCase();
        
        // 提取命令和参数
        String[] parts = cmd.split("\\s+", 2);
        String commandName = parts[0];
        String args = parts.length > 1 ? parts[1] : "";
        // 保留原始大小写的参数 (用于玩家名)
        String[] rawParts = rawCmd.split("\\s+", 2);
        String rawArgs = rawParts.length > 1 ? rawParts[1] : "";
        
        // === 权限检查: 敏感命令仅限管理群 ===
        if (ADMIN_ONLY_COMMANDS.contains(commandName) && !isFromAdminGroup) {
            sendReplyToQQ(sourceGroupId, "❌ 此命令仅限管理群使用");
            LOGGER.info("用户 {} 在玩家群尝试执行管理命令: {}", senderQQ, commandName);
            return;
        }
        
        // Java 21 Switch 表达式
        switch (commandName) {
            case "inv" -> handleInventoryCommand(message, senderQQ, sourceGroupId);
            case "list", "在线" -> handleListCommand(sourceGroupId);
            case "tps", "status", "状态" -> handleStatusCommand(sourceGroupId);
            case "help", "菜单" -> sendReplyToQQ(sourceGroupId, ServerStatusManager.getHelp());
            case "stopserver", "关服" -> handleStopServerCommand(senderQQ, sourceGroupId);
            case "addadmin" -> handleAddAdminCommand(args, senderQQ, sourceGroupId);
            case "removeadmin" -> handleRemoveAdminCommand(args, senderQQ, sourceGroupId);
            case "id", "bind", "绑定" -> handleBindCommand(rawArgs, senderQQ, sourceGroupId);
            case "unbind", "removeid", "解绑" -> handleUnbindCommand(senderQQ, sourceGroupId);
            case "adminunbind" -> handleAdminUnbindCommand(args, senderQQ, sourceGroupId);
            case "reload" -> handleReloadCommand(senderQQ, sourceGroupId);
            case "playtime", "在线时长" -> handlePlaytimeCommand(rawArgs, senderQQ, sourceGroupId);
            case "location", "位置" -> handleLocationCommand(rawArgs, senderQQ, sourceGroupId);
            default -> {
                LOGGER.debug("未知命令: {}", message);
                sendReplyToQQ(sourceGroupId, "❓ 未知命令，输入 #help 查看帮助");
            }
        }
    }

    // ================== 玩家绑定命令 (Task #010) ==================

    /**
     * 处理 #id / #bind <player_name> 命令
     * 绑定 QQ 到 Minecraft 玩家，并添加到白名单
     * 
     * @param playerName 玩家名
     * @param senderQQ 发送者 QQ
     * @param sourceGroupId 消息来源群号
     */
    private static void handleBindCommand(String playerName, long senderQQ, long sourceGroupId) {
        // Step A: 快速验证
        if (playerName.isEmpty()) {
            sendReplyToQQ(sourceGroupId, "[错误] 用法: #id <游戏ID>\n例如: #id Steve");
            return;
        }
        
        // 正则验证玩家名格式
        if (!PLAYER_NAME_PATTERN.matcher(playerName).matches()) {
            sendReplyToQQ(sourceGroupId, "[错误] 无效的游戏ID格式\n只能包含字母、数字、下划线，长度3-16位");
            return;
        }
        
        // 检查 QQ 是否已绑定
        if (DataManager.INSTANCE.isQQBound(senderQQ)) {
            String existingUUID = DataManager.INSTANCE.getBinding(senderQQ);
            sendReplyToQQ(sourceGroupId, "[绑定失败] 该QQ已绑定其他账号，请先解绑。");
            LOGGER.info("用户 {} 尝试重复绑定，已绑定 UUID: {}", senderQQ, existingUUID);
            return;
        }
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            sendReplyToQQ(sourceGroupId, "[错误] 服务器未就绪");
            return;
        }
        
        LOGGER.info("处理绑定请求: QQ {} -> 玩家 {}", senderQQ, playerName);
        
        // Step B: 异步解析玩家档案 (避免阻塞主线程)
        boolean isOnlineMode = server.usesAuthentication();
        
        CompletableFuture.supplyAsync(() -> {
            // 在异步线程中解析 GameProfile
            return resolveGameProfile(server, playerName, isOnlineMode);
        }).thenAcceptAsync(profile -> {
            // Step C: 在服务器主线程执行绑定操作
            if (profile == null) {
                sendReplyToQQ(sourceGroupId, "[绑定失败] 玩家不存在或未加入过服务器。");
                return;
            }
            
            String uuid = profile.getId().toString();
            
            // 检查 UUID 是否已被其他人绑定
            if (DataManager.INSTANCE.isUUIDBound(uuid)) {
                long boundQQ = DataManager.INSTANCE.getQQByUUID(uuid);
                sendReplyToQQ(sourceGroupId, "[绑定失败] 该游戏ID已被其他QQ绑定，请联系管理员。");
                LOGGER.warn("UUID {} 已被 QQ {} 绑定，拒绝 QQ {} 的绑定请求", uuid, boundQQ, senderQQ);
                return;
            }
            
            // 执行绑定
            boolean bindSuccess = DataManager.INSTANCE.bind(senderQQ, uuid);
            if (!bindSuccess) {
                sendReplyToQQ(sourceGroupId, "[错误] 绑定失败，请稍后重试");
                return;
            }
            
            // 添加到白名单
            try {
                UserWhiteList whitelist = server.getPlayerList().getWhiteList();
                if (!whitelist.isWhiteListed(profile)) {
                    whitelist.add(new UserWhiteListEntry(profile));
                    whitelist.save(); // 强制保存到 whitelist.json
                    LOGGER.info("已将玩家 {} ({}) 添加到白名单", profile.getName(), uuid);
                }
                
                sendReplyToQQ(sourceGroupId, String.format("[绑定成功]\nQQ <-> %s\n白名单已同步，可加入服务器。", profile.getName()));
                LOGGER.info("绑定成功: QQ {} -> {} ({})", senderQQ, profile.getName(), uuid);
                
            } catch (Exception e) {
                LOGGER.error("添加白名单失败: {}", e.getMessage());
                sendReplyToQQ(sourceGroupId, "[警告] 绑定成功，但添加白名单时出错，请联系管理员。");
            }
            
        }, server); // 使用 server 作为 Executor，确保在主线程执行
    }

    /**
     * 解析玩家档案 (GameProfile)
     * 
     * @param server 服务器实例
     * @param playerName 玩家名
     * @param onlineMode 是否为正版模式
     * @return GameProfile，找不到返回 null
     */
    private static GameProfile resolveGameProfile(MinecraftServer server, String playerName, boolean onlineMode) {
        try {
            if (onlineMode) {
                // 正版模式: 先查缓存，再查 Mojang API
                Optional<GameProfile> cached = server.getProfileCache().get(playerName);
                if (cached.isPresent()) {
                    LOGGER.debug("从缓存获取到玩家档案: {}", playerName);
                    return cached.get();
                }
                
                // 缓存未命中，尝试在线玩家
                ServerPlayer onlinePlayer = server.getPlayerList().getPlayerByName(playerName);
                if (onlinePlayer != null) {
                    return onlinePlayer.getGameProfile();
                }
                
                // 可能需要从 Mojang API 获取，但这里简化处理
                // 建议玩家先进服一次让系统缓存其档案
                LOGGER.debug("未找到玩家档案: {}", playerName);
                return null;
                
            } else {
                // 离线模式: 生成离线 UUID
                UUID offlineUUID = UUIDUtil.createOfflinePlayerUUID(playerName);
                return new GameProfile(offlineUUID, playerName);
            }
        } catch (Exception e) {
            LOGGER.error("解析玩家档案失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 处理 #unbind / #removeid 命令
     * 解绑 QQ 与 Minecraft 玩家的关联，并从白名单移除
     * 
     * @param senderQQ 发送者 QQ
     * @param sourceGroupId 消息来源群号
     */
    private static void handleUnbindCommand(long senderQQ, long sourceGroupId) {
        // 检查是否已绑定
        if (!DataManager.INSTANCE.isQQBound(senderQQ)) {
            sendReplyToQQ(sourceGroupId, "[操作失败] 未找到绑定记录。");
            return;
        }
        
        String uuid = DataManager.INSTANCE.getBinding(senderQQ);
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            sendReplyToQQ("[错误] 服务器未就绪");
            return;
        }
        
        // 在主线程执行解绑操作
        server.execute(() -> {
            // 执行解绑
            boolean unbindSuccess = DataManager.INSTANCE.unbind(senderQQ);
            if (!unbindSuccess) {
                sendReplyToQQ(sourceGroupId, "[错误] 解绑失败，请稍后重试");
                return;
            }
            
            // 从白名单移除 (安全方式: 通过 UUID 遍历查找)
            try {
                removeFromWhitelistByUUID(server, uuid);
                
                sendReplyToQQ(sourceGroupId, "[解绑成功] 已移除白名单。");
                LOGGER.info("解绑成功: QQ {} (原UUID: {})", senderQQ, uuid);
                
            } catch (Exception e) {
                LOGGER.error("移除白名单失败: {}", e.getMessage());
                sendReplyToQQ(sourceGroupId, "[警告] 解绑成功，但移除白名单时出错。");
            }
        });
    }

    /**
     * 处理 #adminunbind <qq> 命令
     * 管理员强制解绑指定 QQ 的游戏账号
     * 
     * @param args 目标 QQ
     * @param senderQQ 发送者 QQ
     * @param sourceGroupId 消息来源群号
     */
    private static void handleAdminUnbindCommand(String args, long senderQQ, long sourceGroupId) {
        // 权限检查
        if (!DataManager.INSTANCE.isAdmin(senderQQ)) {
            sendReplyToQQ(sourceGroupId, "[错误] 权限不足");
            return;
        }
        
        if (args.isEmpty()) {
            sendReplyToQQ(sourceGroupId, "[错误] 用法: #adminunbind <QQ号>");
            return;
        }
        
        long targetQQ;
        try {
            targetQQ = Long.parseLong(args.trim());
        } catch (NumberFormatException e) {
            sendReplyToQQ(sourceGroupId, "[错误] 无效的 QQ 号格式");
            return;
        }
        
        // 检查目标是否已绑定
        if (!DataManager.INSTANCE.isQQBound(targetQQ)) {
            sendReplyToQQ(sourceGroupId, String.format("[提示] QQ %d 未绑定任何游戏账号", targetQQ));
            return;
        }
        
        // 获取绑定的 UUID (在解绑前)
        String uuid = DataManager.INSTANCE.getBinding(targetQQ);
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            sendReplyToQQ(sourceGroupId, "[错误] 服务器未就绪");
            return;
        }
        
        // 在主线程执行
        server.execute(() -> {
            // 执行解绑
            boolean unbindSuccess = DataManager.INSTANCE.unbind(targetQQ);
            if (!unbindSuccess) {
                sendReplyToQQ(sourceGroupId, "[错误] 解绑失败，请稍后重试");
                return;
            }
            
            // 从白名单移除 (安全方式: 通过 UUID 遍历查找)
            try {
                removeFromWhitelistByUUID(server, uuid);
                
                sendReplyToQQ(sourceGroupId, String.format("[成功] 已强制解绑 QQ %d，已移除白名单。", targetQQ));
                LOGGER.info("管理员 {} 强制解绑: QQ {} (UUID: {})", senderQQ, targetQQ, uuid);
                
            } catch (Exception e) {
                LOGGER.error("移除白名单失败: {}", e.getMessage());
                sendReplyToQQ(sourceGroupId, String.format("[警告] 已解绑 QQ %d，但移除白名单时出错", targetQQ));
            }
        });
    }

    /**
     * 处理 #reload 命令
     * 重载配置文件和数据
     * 
     * @param senderQQ 发送者 QQ
     * @param sourceGroupId 消息来源群号
     */
    private static void handleReloadCommand(long senderQQ, long sourceGroupId) {
        // 权限检查
        if (!DataManager.INSTANCE.isAdmin(senderQQ)) {
            sendReplyToQQ(sourceGroupId, "[错误] 权限不足");
            return;
        }
        
        try {
            // 重载数据管理器 (重新读取 mapbot_data.json)
            DataManager.INSTANCE.init();
            
            // 注: BotConfig 使用 ModConfigSpec，会自动同步配置文件变更
            // 无需手动重载
            
            sendReplyToQQ(sourceGroupId, "[系统] 配置已重载。");
            LOGGER.info("管理员 {} 执行了配置重载", senderQQ);
            
        } catch (Exception e) {
            LOGGER.error("重载配置失败: {}", e.getMessage());
            sendReplyToQQ(sourceGroupId, "[错误] 重载失败: " + e.getMessage());
        }
    }

    /**
     * 安全地通过 UUID 从白名单移除玩家
     * 使用 ProfileCache 获取完整档案，或构造安全的 dummy profile
     * 
     * @param server 服务器实例
     * @param uuidStr UUID 字符串
     */
    private static void removeFromWhitelistByUUID(MinecraftServer server, String uuidStr) {
        try {
            UUID uuid = UUID.fromString(uuidStr);
            UserWhiteList whitelist = server.getPlayerList().getWhiteList();
            
            // 尝试从缓存获取真实档案，否则使用带安全名称的 dummy profile
            // 注意: 必须使用非 null 的名称，否则会抛出异常
            GameProfile profile = server.getProfileCache().get(uuid)
                    .orElse(new GameProfile(uuid, "Unknown"));
            
            if (whitelist.isWhiteListed(profile)) {
                whitelist.remove(profile);
                whitelist.save(); // 持久化到 whitelist.json
                LOGGER.info("已从白名单移除 UUID: {}", uuidStr);
            } else {
                LOGGER.debug("UUID {} 不在白名单中，跳过移除", uuidStr);
            }
            
        } catch (IllegalArgumentException e) {
            LOGGER.error("无效的 UUID 格式，无法解绑: {}", uuidStr);
        } catch (Exception e) {
            LOGGER.error("移除白名单时发生未知错误: {}", e.getMessage());
        }
    }

    // ================== 其他命令 ==================

    /**
     * 处理 #list 命令
     * @param sourceGroupId 消息来源群号
     */
    private static void handleListCommand(long sourceGroupId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        
        if (server == null) {
            sendReplyToQQ(sourceGroupId, "[错误] 服务器未就绪");
            return;
        }
        
        // 线程安全: 调度到主线程
        server.execute(() -> {
            String result = ServerStatusManager.getList();
            sendReplyToQQ(sourceGroupId, result);
        });
    }

    /**
     * 处理 #tps / #status 命令
     * @param sourceGroupId 消息来源群号
     */
    private static void handleStatusCommand(long sourceGroupId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        
        if (server == null) {
            sendReplyToQQ(sourceGroupId, "[错误] 服务器未就绪");
            return;
        }
        
        // 线程安全: 调度到主线程
        server.execute(() -> {
            String result = ServerStatusManager.getServerInfo();
            sendReplyToQQ(sourceGroupId, result);
        });
    }

    /**
     * 处理 #stopserver 命令
     * 需要管理员权限
     * 
     * @param senderQQ 发送者 QQ
     * @param sourceGroupId 消息来源群号
     */
    private static void handleStopServerCommand(long senderQQ, long sourceGroupId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        
        if (server == null) {
            sendReplyToQQ(sourceGroupId, "[错误] 服务器未就绪");
            return;
        }
        
        // 权限检查
        if (!DataManager.INSTANCE.isAdmin(senderQQ)) {
            sendReplyToQQ(sourceGroupId, "[错误] 权限不足");
            LOGGER.warn("用户 {} 尝试执行 #stopserver 但权限不足", senderQQ);
            return;
        }
        
        sendReplyToQQ(sourceGroupId, "[系统] 正在执行关服序列...");
        
        // 线程安全: 调度到主线程
        server.execute(() -> {
            LOGGER.warn("收到远程停服命令 (操作者: {})，服务器即将关闭...", senderQQ);
            server.halt(false);
        });
    }

    /**
     * 处理 #addadmin <qq> 命令
     * 只有现有管理员可以添加新管理员
     * 
     * @param args 命令参数
     * @param senderQQ 发送者 QQ
     * @param sourceGroupId 消息来源群号
     */
    private static void handleAddAdminCommand(String args, long senderQQ, long sourceGroupId) {
        // 权限检查: 只有管理员可以添加管理员
        // 特殊情况: 如果没有任何管理员，允许第一个添加
        if (DataManager.INSTANCE.getAdmins().isEmpty()) {
            LOGGER.info("当前无管理员，允许首次添加");
        } else if (!DataManager.INSTANCE.isAdmin(senderQQ)) {
            sendReplyToQQ(sourceGroupId, "[错误] 权限不足");
            return;
        }
        
        // 解析目标 QQ
        if (args.isEmpty()) {
            sendReplyToQQ(sourceGroupId, "[错误] 用法: #addadmin <QQ号>");
            return;
        }
        
        try {
            long targetQQ = Long.parseLong(args.trim());
            
            if (DataManager.INSTANCE.addAdmin(targetQQ)) {
                sendReplyToQQ(sourceGroupId, String.format("[成功] 已添加管理员: %d", targetQQ));
                LOGGER.info("用户 {} 添加了新管理员: {}", senderQQ, targetQQ);
            } else {
                sendReplyToQQ(sourceGroupId, String.format("[提示] %d 已经是管理员", targetQQ));
            }
        } catch (NumberFormatException e) {
            sendReplyToQQ(sourceGroupId, "[错误] 无效的 QQ 号格式");
        }
    }

    /**
     * 处理 #removeadmin <qq> 命令
     * 
     * @param args 命令参数
     * @param senderQQ 发送者 QQ
     * @param sourceGroupId 消息来源群号
     */
    private static void handleRemoveAdminCommand(String args, long senderQQ, long sourceGroupId) {
        // 权限检查
        if (!DataManager.INSTANCE.isAdmin(senderQQ)) {
            sendReplyToQQ(sourceGroupId, "[错误] 权限不足");
            return;
        }
        
        // 解析目标 QQ
        if (args.isEmpty()) {
            sendReplyToQQ(sourceGroupId, "[错误] 用法: #removeadmin <QQ号>");
            return;
        }
        
        try {
            long targetQQ = Long.parseLong(args.trim());
            
            // 防止自我移除
            if (targetQQ == senderQQ) {
                sendReplyToQQ(sourceGroupId, "[错误] 无法移除自己的管理员权限");
                return;
            }
            
            if (DataManager.INSTANCE.removeAdmin(targetQQ)) {
                sendReplyToQQ(sourceGroupId, String.format("[成功] 已移除管理员: %d", targetQQ));
                LOGGER.info("用户 {} 移除了管理员: {}", senderQQ, targetQQ);
            } else {
                sendReplyToQQ(sourceGroupId, String.format("[提示] %d 不是管理员", targetQQ));
            }
        } catch (NumberFormatException e) {
            sendReplyToQQ(sourceGroupId, "[错误] 无效的 QQ 号格式");
        }
    }

    /**
     * 处理 #inv <玩家名> [-e] 命令
     * Task #007 新增, Task #012-STEP5 更新
     * 
     * @param message 完整命令字符串
     * @param senderQQ 发送者 QQ
     * @param sourceGroupId 消息来源群号
     */
    private static void handleInventoryCommand(String message, long senderQQ, long sourceGroupId) {
        // 权限检查: 仅管理员可用
        if (!DataManager.INSTANCE.isAdmin(senderQQ)) {
            sendReplyToQQ(sourceGroupId, "[错误] 权限不足");
            return;
        }
        
        // 解析参数: #inv <玩家名> [-e]
        String argsStr = message.substring(4).trim(); // 去掉 "#inv"
        
        if (argsStr.isEmpty()) {
            sendReplyToQQ(sourceGroupId, "[错误] 用法: #inv <玩家名> [-e]\n  -e  查看末影箱");
            return;
        }
        
        // 检查是否包含 -e 参数
        boolean queryEnderChest = argsStr.contains("-e") || argsStr.contains("-E");
        
        // 提取玩家名 (移除 -e 参数)
        String targetPlayerName = argsStr.replace("-e", "").replace("-E", "").trim();
        
        if (targetPlayerName.isEmpty()) {
            sendReplyToQQ(sourceGroupId, "[错误] 请指定玩家名");
            return;
        }
        
        LOGGER.info("收到{}查询请求: {}", queryEnderChest ? "末影箱" : "库存", targetPlayerName);
        
        // 关键: 线程调度
        // 必须在服务器主线程执行 getPlayerList() 操作
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        
        if (server == null) {
            sendReplyToQQ(sourceGroupId, "[错误] 服务器未就绪");
            return;
        }
        
        // 使用 final 变量传递到 lambda
        final boolean isEnderChest = queryEnderChest;
        final String playerName = targetPlayerName;
        
        // 调度到服务器主线程
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
            
            // 根据参数调用不同方法
            String result;
            if (isEnderChest) {
                result = InventoryManager.getPlayerEnderChest(player);
            } else {
                result = InventoryManager.getPlayerInventory(player);
            }
            
            // 发送结果回 QQ
            sendReplyToQQ(sourceGroupId, result);
        });
    }

    // ================== Task #016-STEP2: 在线时长查询 ==================
    
    /**
     * 处理 #playtime / #在线时长 命令
     * Task #016-STEP2 新增
     * 
     * @param args 命令参数 (玩家名 [时段])
     * @param senderQQ 发送者 QQ
     * @param sourceGroupId 消息来源群号
     */
    private static void handlePlaytimeCommand(String args, long senderQQ, long sourceGroupId) {
        // 解析参数
        String[] parts = args.trim().split("\\s+");
        
        if (parts.length == 0 || parts[0].isEmpty()) {
            sendReplyToQQ(sourceGroupId, "[错误] 用法: #playtime <玩家名> [时段]\n时段: 0=今天, 1=本周, 2=本月, 3=总计");
            return;
        }
        
        String targetPlayerName = parts[0];
        int mode = 0; // 默认: 今天
        
        // 解析时段参数
        if (parts.length > 1) {
            try {
                mode = Integer.parseInt(parts[1]);
                if (mode < 0 || mode > 3) {
                    sendReplyToQQ(sourceGroupId, "[错误] 时段参数无效\n0=今天, 1=本周, 2=本月, 3=总计");
                    return;
                }
            } catch (NumberFormatException e) {
                sendReplyToQQ(sourceGroupId, "[错误] 时段必须为数字 (0-3)");
                return;
            }
        }
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            sendReplyToQQ(sourceGroupId, "[错误] 服务器未就绪");
            return;
        }
        
        // 时段名称映射
        final String[] periodNames = {"今天", "本周", "本月", "总计"};
        final int finalMode = mode;
        final String periodName = periodNames[mode];
        
        // 在主线程执行
        server.execute(() -> {
            // 尝试查找在线玩家
            ServerPlayer player = server.getPlayerList().getPlayerByName(targetPlayerName);
            
            UUID targetUUID = null;
            String displayName = targetPlayerName;
            
            if (player != null) {
                // 玩家在线
                targetUUID = player.getUUID();
                displayName = player.getName().getString();
            } else {
                // 玩家不在线，尝试从缓存查找
                Optional<GameProfile> cached = server.getProfileCache().get(targetPlayerName);
                if (cached.isPresent()) {
                    targetUUID = cached.get().getId();
                    displayName = cached.get().getName();
                }
            }
            
            if (targetUUID == null) {
                sendReplyToQQ(sourceGroupId, "[错误] 找不到玩家: " + targetPlayerName);
                return;
            }
            
            // 查询在线时长
            long minutes = PlaytimeManager.INSTANCE.getPlaytimeMinutes(targetUUID, finalMode);
            String formattedTime = PlaytimeManager.formatDuration(minutes);
            
            // 构建返回消息
            String response = String.format("[在线时长] %s\n统计范围: %s\n时长: %s", 
                    displayName, periodName, formattedTime);
            
            sendReplyToQQ(sourceGroupId, response);
            LOGGER.debug("查询玩家 {} ({}) 的{}在线时长: {} 分钟", displayName, targetUUID, periodName, minutes);
        });
    }

    // ================== Task #016-STEP3: 位置查询 ==================
    
    /**
     * 处理 #location / #位置 命令
     * Task #016-STEP3 新增
     * 仅限管理群使用
     * 
     * @param args 命令参数 (玩家名)
     * @param senderQQ 发送者 QQ
     * @param sourceGroupId 消息来源群号
     */
    private static void handleLocationCommand(String args, long senderQQ, long sourceGroupId) {
        // 解析玩家名
        String targetPlayerName = args.trim();
        
        if (targetPlayerName.isEmpty()) {
            sendReplyToQQ(sourceGroupId, "[错误] 用法: #location <玩家名>");
            return;
        }
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            sendReplyToQQ(sourceGroupId, "[错误] 服务器未就绪");
            return;
        }
        
        // 在主线程执行
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayerByName(targetPlayerName);
            
            if (player == null) {
                sendReplyToQQ(sourceGroupId, "[错误] 玩家 " + targetPlayerName + " 不在线");
                return;
            }
            
            // 获取玩家位置信息
            String playerName = player.getName().getString();
            
            // 获取维度信息
            String dimension = getDimensionDisplayName(player.level().dimension().location().toString());
            
            // 获取坐标 (取整)
            int x = (int) Math.floor(player.getX());
            int y = (int) Math.floor(player.getY());
            int z = (int) Math.floor(player.getZ());
            
            // 获取朝向
            float yaw = player.getYRot();
            String facing = getYawDirection(yaw);
            
            // 构建返回消息
            StringBuilder sb = new StringBuilder();
            sb.append("[位置信息] ").append(playerName).append("\n");
            sb.append("维度: ").append(dimension).append("\n");
            sb.append("坐标: ").append(x).append(", ").append(y).append(", ").append(z).append("\n");
            sb.append("朝向: ").append(facing);
            
            sendReplyToQQ(sourceGroupId, sb.toString());
            LOGGER.info("查询玩家 {} 位置: {} [{}, {}, {}]", playerName, dimension, x, y, z);
        });
    }
    
    /**
     * 获取维度的显示名称
     * 
     * @param dimensionId 维度 ID (如 minecraft:overworld)
     * @return 友好显示名称
     */
    private static String getDimensionDisplayName(String dimensionId) {
        return switch (dimensionId) {
            case "minecraft:overworld" -> "主世界";
            case "minecraft:the_nether" -> "下界";
            case "minecraft:the_end" -> "末地";
            default -> {
                // 处理模组维度: 移除命名空间前缀，使其更可读
                if (dimensionId.contains(":")) {
                    yield dimensionId.substring(dimensionId.indexOf(":") + 1);
                }
                yield dimensionId;
            }
        };
    }
    
    /**
     * 将 Yaw 角度转换为方向文字
     * 
     * @param yaw Yaw 角度 (-180 到 180)
     * @return 方向文字 (北/东/南/西等)
     */
    private static String getYawDirection(float yaw) {
        // 标准化到 0-360
        float normalizedYaw = (yaw % 360 + 360) % 360;
        
        // 8 方向判断
        if (normalizedYaw >= 337.5 || normalizedYaw < 22.5) {
            return "南 (S)";
        } else if (normalizedYaw >= 22.5 && normalizedYaw < 67.5) {
            return "西南 (SW)";
        } else if (normalizedYaw >= 67.5 && normalizedYaw < 112.5) {
            return "西 (W)";
        } else if (normalizedYaw >= 112.5 && normalizedYaw < 157.5) {
            return "西北 (NW)";
        } else if (normalizedYaw >= 157.5 && normalizedYaw < 202.5) {
            return "北 (N)";
        } else if (normalizedYaw >= 202.5 && normalizedYaw < 247.5) {
            return "东北 (NE)";
        } else if (normalizedYaw >= 247.5 && normalizedYaw < 292.5) {
            return "东 (E)";
        } else {
            return "东南 (SE)";
        }
    }

    // ================== Task #016-STEP4: 通知事件处理 ==================
    
    /**
     * 处理通知事件 (群成员增减等)
     * Task #016-STEP4 新增
     * 
     * OneBot v11 notice 事件类型:
     * - group_increase: 群成员增加
     * - group_decrease: 群成员减少
     * - group_admin: 群管理员变动
     * - 等等
     */
    private static void handleNoticeEvent(JsonObject json) {
        String noticeType = getStringOrNull(json, "notice_type");
        
        if (noticeType == null) {
            return;
        }
        
        switch (noticeType) {
            case "group_increase" -> handleMemberJoin(json);
            case "group_decrease" -> {
                LOGGER.debug("群成员减少事件");
                // 暂不处理退群事件
            }
            default -> LOGGER.debug("忽略未知通知类型: {}", noticeType);
        }
    }
    
    /**
     * 处理群成员增加事件 (新人入群)
     * Task #016-STEP4 核心逻辑
     * 
     * 事件格式:
     * {
     *   "post_type": "notice",
     *   "notice_type": "group_increase",
     *   "sub_type": "approve" / "invite",
     *   "group_id": 123456,
     *   "user_id": 654321,
     *   "operator_id": 0
     * }
     */
    private static void handleMemberJoin(JsonObject json) {
        long groupId = getLongOrZero(json, "group_id");
        long userId = getLongOrZero(json, "user_id");
        String subType = getStringOrNull(json, "sub_type"); // approve=管理员同意, invite=被邀请
        
        // 获取玩家群 ID
        long playerGroupId = BotConfig.getPlayerGroupId();
        
        // 只处理玩家群的入群事件
        if (groupId != playerGroupId || playerGroupId == 0L) {
            LOGGER.debug("忽略非玩家群的入群事件: 群 {}", groupId);
            return;
        }
        
        LOGGER.info("检测到新成员加入玩家群: QQ {}, 类型: {}", userId, subType);
        
        // 发送欢迎消息
        sendWelcomeMessage(groupId, userId);
    }
    
    /**
     * 发送新人欢迎消息
     * 
     * @param groupId 群号
     * @param userId 新成员 QQ
     */
    private static void sendWelcomeMessage(long groupId, long userId) {
        // 构建欢迎消息 (使用 CQ 码 @新成员)
        String welcomeMessage = String.format(
            "[CQ:at,qq=%d] [Bot] 欢迎入群\n" +
            "请发送 #id <游戏ID> 绑定白名单。\n" +
            "发送 #help 查看完整命令。",
            userId
        );
        
        // 发送到玩家群
        BotClient.INSTANCE.sendGroupMessage(groupId, welcomeMessage);
        
        LOGGER.info("已发送欢迎消息给新成员: QQ {}", userId);
    }

    /**
     * 处理元事件 (心跳等)
     */
    private static void handleMetaEvent(JsonObject json) {
        String metaEventType = getStringOrNull(json, "meta_event_type");
        
        // null 检查
        if (metaEventType == null) {
            return;
        }

        switch (metaEventType) {
            case "heartbeat" -> LOGGER.debug("收到心跳事件");
            case "lifecycle" -> {
                String subType = getStringOrNull(json, "sub_type");
                LOGGER.info("生命周期事件: {}", subType);
            }
            default -> LOGGER.debug("忽略未知元事件: {}", metaEventType);
        }
    }

    /**
     * 处理群成员列表响应
     * Task #013-STEP3 新增
     * 
     * 解析 get_group_member_list 的返回数据，将成员昵称加载到缓存
     * 
     * @param json OneBot 响应 JSON
     */
    private static void handleGroupMemberListResponse(JsonObject json) {
        JsonElement dataElement = json.get("data");
        if (dataElement == null || !dataElement.isJsonArray()) {
            LOGGER.warn("群成员列表响应格式异常");
            return;
        }
        
        JsonArray data = dataElement.getAsJsonArray();
        java.util.Map<Long, String> members = new java.util.HashMap<>();
        
        for (JsonElement elem : data) {
            if (!elem.isJsonObject()) continue;
            
            JsonObject member = elem.getAsJsonObject();
            JsonElement userIdElem = member.get("user_id");
            
            if (userIdElem == null) continue;
            
            long userId = userIdElem.getAsLong();
            
            // 优先使用群名片 (card), 其次使用 QQ 昵称 (nickname)
            String card = getStringOrNull(member, "card");
            String nickname = getStringOrNull(member, "nickname");
            
            String displayName = (card != null && !card.isEmpty()) ? card : nickname;
            if (displayName != null && !displayName.isEmpty()) {
                members.put(userId, displayName);
            }
        }
        
        GroupMemberCache.INSTANCE.loadMembers(members);
        LOGGER.info("成功加载 {} 个群成员昵称到缓存", members.size());
    }

    /**
     * 请求获取原消息内容
     * Task #015-STEP1 重构: 使用 UUID + Map 替代字符串编码
     * 
     * 用于回复消息通知：当群成员回复机器人转发的消息时，
     * 需要获取原消息内容以解析原发送者玩家名
     * 
     * @param messageId 被回复消息的 ID
     * @param replierNickname 回复者昵称
     * @param rawMessage 原始消息内容 (含 CQ 码)
     * @param atQQList 被 @ 的 QQ 列表
     * @param sourceGroupId 来源群号
     */
    private static void requestOriginalMessage(String messageId, String replierNickname, 
                                               String rawMessage, List<Long> atQQList, long sourceGroupId) {
        // 生成唯一 UUID 作为 echo
        String echoUUID = "reply_" + UUID.randomUUID().toString();
        
        // 存储上下文到 Map
        ReplyContext context = new ReplyContext(
                replierNickname, 
                rawMessage, 
                atQQList, 
                sourceGroupId,
                System.currentTimeMillis()
        );
        PENDING_REPLY_CONTEXTS.put(echoUUID, context);
        
        // 发送 get_msg 请求
        JsonObject params = new JsonObject();
        params.addProperty("message_id", messageId);
        
        JsonObject packet = new JsonObject();
        packet.addProperty("action", "get_msg");
        packet.add("params", params);
        packet.addProperty("echo", echoUUID);
        
        BotClient.INSTANCE.sendPacket(packet);
        LOGGER.debug("请求获取被回复消息: {} (echo={})", messageId, echoUUID);
    }
    
    /**
     * 处理 get_msg API 响应
     * Task #015-STEP2 重构: 延迟转发 + @替换
     * 
     * 1. 从 Map 获取上下文
     * 2. 解析原消息玩家名
     * 3. 替换 @BotQQ 为 @玩家名
     * 4. 发送通知并转发消息
     * 
     * @param json OneBot 响应 JSON
     * @param echo 请求时的 echo 字符串 (UUID)
     */
    private static void handleGetMsgResponse(JsonObject json, String echo) {
        // Task #015-STEP1: 从 Map 获取上下文
        ReplyContext context = PENDING_REPLY_CONTEXTS.remove(echo);
        if (context == null) {
            LOGGER.warn("未找到回复上下文 (可能已过期): echo={}", echo);
            return;
        }
        
        // 从上下文获取信息 (修复 "msg" bug)
        String replierNickname = context.replierNickname();
        String rawMessage = context.rawMessage();
        List<Long> atQQList = context.atQQList();
        LOGGER.debug("收到 get_msg 响应, 回复者: {}", replierNickname);
        
        // 获取响应数据
        JsonObject data = json.getAsJsonObject("data");
        if (data == null) {
            LOGGER.debug("get_msg 响应无 data 字段");
            // 即使失败也要尝试转发原始消息
            fallbackForwardMessage(context);
            return;
        }
        
        // 获取原消息发送者 ID
        long senderId = 0L;
        JsonElement senderElem = data.get("sender");
        if (senderElem != null && senderElem.isJsonObject()) {
            JsonObject sender = senderElem.getAsJsonObject();
            JsonElement userIdElem = sender.get("user_id");
            if (userIdElem != null) {
                senderId = userIdElem.getAsLong();
            }
        }
        
        // 获取原消息内容
        String originalRawMessage = getStringOrNull(data, "raw_message");
        if (originalRawMessage == null) {
            originalRawMessage = getStringOrNull(data, "message");
        }
        
        LOGGER.debug("get_msg 响应: sender={}, message={}", senderId, originalRawMessage);
        
        // 判断是否是机器人自己发送的消息
        long botQQ = BotConfig.getBotQQ();
        String originalPlayerName = null;
        
        if (senderId == botQQ) {
            // 解析机器人转发的消息格式，提取原玩家名
            originalPlayerName = extractPlayerNameFromBotMessage(originalRawMessage);
            if (originalPlayerName != null) {
                LOGGER.info("[DEBUG] 成功提取被回复的玩家名: {}", originalPlayerName);
            }
        }
        
        // ========== Task #015-STEP2: @替换 + 消息转发 ==========
        // 解析 CQ 码得到可读消息
        String parsedMessage = CQCodeParser.parse(rawMessage);
        
        // 如果成功解析出原玩家名，替换 @机器人 为 @玩家名
        if (originalPlayerName != null) {
            // 替换消息中的 @机器人 引用
            // parsedMessage 中 @机器人 已被 CQCodeParser 解析为 "@Bot昵称" 或 "@QQ号"
            // 直接在 parsedMessage 中替换
            String botAtText = "@" + botQQ;
            String botNickAtText = "@CIR-Bot"; // 机器人常用昵称
            String replacement = "@" + originalPlayerName;
            
            parsedMessage = parsedMessage.replace(botAtText, replacement);
            parsedMessage = parsedMessage.replace(botNickAtText, replacement);
            
            LOGGER.debug("[STEP2] @替换: {} -> {}", botAtText, replacement);
        }
        
        // 构建最终消息
        String formattedMessage = String.format("§b[QQ]§r <%s> %s", replierNickname, parsedMessage);
        
        // 发送个性化消息到游戏
        sendPersonalizedMessage(formattedMessage, atQQList, replierNickname);
        
        // 向原玩家发送回复通知 (Title)
        if (originalPlayerName != null) {
            notifyPlayerOfReply(originalPlayerName, replierNickname);
        }
    }
    
    /**
     * 备用消息转发（当 get_msg 失败时）
     * Task #015-STEP2 新增
     */
    private static void fallbackForwardMessage(ReplyContext context) {
        String parsedMessage = CQCodeParser.parse(context.rawMessage());
        String formattedMessage = String.format("§b[QQ]§r <%s> %s", 
                context.replierNickname(), parsedMessage);
        sendPersonalizedMessage(formattedMessage, context.atQQList(), context.replierNickname());
        LOGGER.debug("使用备用路径转发消息");
    }
    
    /**
     * 从机器人转发的消息中提取原玩家名
     * Task #014-Fix v2: 修复 HTML 转义和格式解析
     * 
     * 机器人转发格式: "[玩家名] 内容"
     * OneBot 返回时会 HTML 转义: "&#91;玩家名&#93; 内容"
     * 
     * @param botMessage 机器人发送的消息 (可能是 HTML 转义的)
     * @return 玩家名，解析失败返回 null
     */
    private static String extractPlayerNameFromBotMessage(String botMessage) {
        if (botMessage == null || botMessage.isEmpty()) {
            return null;
        }
        
        // 1. 反转义 HTML 实体
        String decoded = botMessage
                .replace("&#91;", "[")
                .replace("&#93;", "]")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
        
        LOGGER.debug("[DEBUG] extractPlayerNameFromBotMessage: decoded={}", decoded);
        
        // 2. 解析 [玩家名] 格式
        if (decoded.startsWith("[") && decoded.contains("]")) {
            int endBracket = decoded.indexOf("]");
            String playerName = decoded.substring(1, endBracket).trim();
            LOGGER.info("[DEBUG] 提取玩家名成功: {}", playerName);
            return playerName;
        }
        
        // 3. 兼容旧格式 "玩家名: 内容"
        int colonIndex = decoded.indexOf(": ");
        if (colonIndex > 0) {
            return decoded.substring(0, colonIndex).trim();
        }
        
        LOGGER.debug("[DEBUG] 无法从消息中提取玩家名: {}", decoded);
        return null;
    }
    
    /**
     * 向指定玩家发送回复通知
     * Task #014-STEP2 新增
     * 
     * @param playerName 目标玩家名
     * @param replierNickname 回复者昵称
     */
    private static void notifyPlayerOfReply(String playerName, String replierNickname) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        
        server.execute(() -> {
            // 通过玩家名查找在线玩家
            ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
            if (player == null) {
                LOGGER.debug("玩家 {} 不在线，无法发送回复通知", playerName);
                return;
            }
            
            // 发送 Title 通知
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal("§b[QQ] §f" + replierNickname + " §6回复了你!")
            ));
            
            LOGGER.info("已向玩家 {} 发送回复通知 (来自 {})", playerName, replierNickname);
        });
    }

    /**
     * 发送个性化消息到所有在线玩家
     * Task #014-Fix 新增
     * 
     * 被@的玩家看到醒目格式 + 收到 Title 通知
     * 其他玩家看到普通格式
     * 
     * @param baseMessage 基础消息（普通@格式）
     * @param atQQList 被@的 QQ 号列表
     * @param senderNickname 发送者昵称
     */
    private static void sendPersonalizedMessage(String baseMessage, List<Long> atQQList, String senderNickname) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            LOGGER.warn("服务器实例不可用，无法发送消息");
            return;
        }
        
        // Task #014-Debug: 调试日志
        LOGGER.info("[DEBUG] sendPersonalizedMessage: atQQList={}, sender={}", atQQList, senderNickname);
        
        server.execute(() -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                String playerUUID = player.getUUID().toString();
                String playerName = player.getName().getString();
                boolean isAtTarget = false;
                
                // 检查此玩家是否被 @
                for (Long qq : atQQList) {
                    String boundUUID = DataManager.INSTANCE.getBinding(qq);
                    LOGGER.debug("[DEBUG] 检查: player={}, playerUUID={}, qq={}, boundUUID={}", 
                            playerName, playerUUID, qq, boundUUID);
                    if (playerUUID.equals(boundUUID)) {
                        isAtTarget = true;
                        LOGGER.info("[DEBUG] 匹配成功! 玩家 {} 被 @", playerName);
                        break;
                    }
                }
                
                // 构建个性化消息
                String personalMessage;
                if (isAtTarget) {
                    // 被@者: 将消息中的 @xxx 替换为醒目格式
                    personalMessage = highlightAtMentions(baseMessage, playerName);
                    LOGGER.info("[DEBUG] 向玩家 {} 发送 Title 通知", playerName);
                    
                    // 发送 Title 通知
                    player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                    player.connection.send(new ClientboundSetTitleTextPacket(
                            Component.literal("§b[QQ] §f" + senderNickname + " §6@了你!")
                    ));
                } else {
                    // 其他人: 普通格式
                    personalMessage = baseMessage;
                }
                
                player.sendSystemMessage(Component.literal(personalMessage));
            }
            LOGGER.debug("个性化消息已发送给 {} 个玩家", server.getPlayerList().getPlayerCount());
        });
    }
    
    /**
     * 高亮消息中与玩家相关的 @提及
     * Task #014-Fix 新增
     * 
     * @param message 原始消息
     * @param playerName 目标玩家名
     * @return 高亮后的消息
     */
    private static String highlightAtMentions(String message, String playerName) {
        // 将 @玩家名 替换为粗体金色格式
        return message.replace("@" + playerName, "§l§6@" + playerName + "§r");
    }

    /**
     * 发送回复消息到指定 QQ 群
     * Task #012-STEP3: 新增指定群号版本
     * Task #016 STEP1: 改为 public 以供 ServerStatusManager 调用
     * 
     * @param groupId 目标群号
     * @param message 消息内容
     */
    public static void sendReplyToQQ(long groupId, String message) {
        if (groupId == 0L) {
            LOGGER.warn("无法发送回复: 目标群号未配置");
            return;
        }
        
        JsonObject params = new JsonObject();
        params.addProperty("group_id", groupId);
        params.addProperty("message", message);
        
        JsonObject packet = new JsonObject();
        packet.addProperty("action", "send_group_msg");
        packet.add("params", params);
        packet.addProperty("echo", "reply_" + System.currentTimeMillis());
        
        BotClient.INSTANCE.sendPacket(packet);
    }
    
    /**
     * 发送回复消息到默认玩家群 (兼容旧代码)
     * @deprecated 请使用 sendReplyToQQ(long groupId, String message)
     */
    @Deprecated
    private static void sendReplyToQQ(String message) {
        sendReplyToQQ(BotConfig.getPlayerGroupId(), message);
    }

    /**
     * 安全地在服务器主线程广播消息
     * Task #013-STEP4: 添加时间戳日志
     * 
     * @param message 要广播的消息
     */
    private static void broadcastToServer(String message) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        if (server == null) {
            LOGGER.warn("服务器实例不可用，无法广播消息");
            return;
        }

        // 使用 execute() 将任务调度到服务器主线程
        long scheduleTime = System.currentTimeMillis();
        server.execute(() -> {
            long execStart = System.currentTimeMillis();
            LOGGER.debug("[TIMING] 主线程执行开始 (排队: {}ms)", execStart - scheduleTime);
            
            Component chatComponent = Component.literal(message);
            server.getPlayerList().broadcastSystemMessage(chatComponent, false);
            
            LOGGER.debug("[TIMING] 主线程执行完成: {}ms", System.currentTimeMillis() - execStart);
        });
    }

    /**
     * 安全地从 JsonObject 获取字符串值
     */
    private static String getStringOrNull(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element != null && !element.isJsonNull()) {
            return element.getAsString();
        }
        return null;
    }
    
    /**
     * 安全地从 JsonObject 获取 long 值
     */
    private static long getLongOrZero(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element != null && !element.isJsonNull()) {
            try {
                return element.getAsLong();
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }

    // ================== @提及通知 (Task #012-STEP4) ==================

    /**
     * 处理 @提及 通知
     * 解析消息中的 @目标，向对应的在线玩家发送 Title 通知
     * 
     * @param rawMessage 原始消息 (包含 CQ 码)
     * @param senderNickname 发送者昵称 (用于通知内容)
     */
    private static void notifyAtMentions(String rawMessage, String senderNickname) {
        // 提取被 @ 的 QQ 号列表
        java.util.List<Long> atTargets = CQCodeParser.extractAtTargets(rawMessage);
        
        if (atTargets.isEmpty()) {
            return;
        }
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        
        // 在主线程执行玩家查找和通知
        server.execute(() -> {
            for (Long targetQQ : atTargets) {
                // 获取 QQ 绑定的 UUID
                String uuidStr = DataManager.INSTANCE.getBinding(targetQQ);
                if (uuidStr == null) {
                    LOGGER.debug("QQ {} 未绑定，跳过通知", targetQQ);
                    continue;
                }
                
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                    
                    if (player != null) {
                        // 发送 Title 通知
                        sendAtNotification(player, senderNickname);
                        LOGGER.debug("已向玩家 {} 发送 @通知", player.getName().getString());
                    }
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("无效的 UUID 格式: {}", uuidStr);
                }
            }
        });
    }

    /**
     * 向玩家发送 @提及 Title 通知
     * 
     * @param player 目标玩家
     * @param senderNickname 发送者昵称
     */
    private static void sendAtNotification(ServerPlayer player, String senderNickname) {
        // 设置 Title 动画时间: fadeIn=10, stay=70, fadeOut=20 (单位: tick)
        player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        
        // 发送 Title 主标题
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal("§b[QQ] §f" + senderNickname + " §6@了你!")
        ));
    }
}
