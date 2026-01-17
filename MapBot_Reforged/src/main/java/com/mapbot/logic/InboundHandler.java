/*
 * MapBot Reforged - 入站消息处理器
 * 
 * 遵从 .ai_rules.md 中定义的治理规则。
 * 负责解析来自 NapCat 的 WebSocket 消息并调度到游戏主线程执行。
 * 
 * 参考: ./Project_Docs/Architecture/Protocol_Spec.md
 * 关键: 使用 ServerLifecycleHooks 实现跨线程安全调用。
 * 
 * Task #009 更新:
 * - Java 21 Switch 表达式语法
 * - #stopserver 权限检查
 * - 新增 #addadmin 命令
 * 
 * Task #010 更新:
 * - 新增 #id / #bind 命令 (玩家绑定 + 白名单)
 * - 新增 #unbind / #removeid 命令 (解绑 + 移除白名单)
 */

package com.mapbot.logic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mapbot.config.BotConfig;
import com.mapbot.data.DataManager;
import com.mapbot.network.BotClient;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * 入站消息处理器
 * 解析来自 QQ 的 JSON 消息，并在游戏主线程中广播
 */
public class InboundHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("MapBot/Inbound");
    
    /** 玩家名正则验证: 3-16 位字母数字下划线 */
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");

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

            switch (postType) {
                case "message" -> handleGroupMessage(json);
                case "meta_event" -> handleMetaEvent(json);
                default -> LOGGER.debug("忽略未知事件类型: {}", postType);
            }

        } catch (Exception e) {
            LOGGER.error("解析 JSON 失败: {}", e.getMessage());
        }
    }

    /**
     * 处理群消息事件
     */
    private static void handleGroupMessage(JsonObject json) {
        String messageType = getStringOrNull(json, "message_type");

        // 仅处理群消息
        if (!"group".equals(messageType)) {
            return;
        }
        
        // 安全检查: 验证消息来源群号
        long targetGroupId = BotConfig.getTargetGroupId();
        if (targetGroupId == 0L) {
            return; // 未配置群号，跳过
        }
        
        long sourceGroupId = getLongOrZero(json, "group_id");
        if (sourceGroupId != targetGroupId) {
            LOGGER.debug("忽略来自其他群的消息: {}", sourceGroupId);
            return;
        }

        // 提取消息内容
        String message = getStringOrNull(json, "raw_message");
        if (message == null || message.isEmpty()) {
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

        LOGGER.info("收到群消息: {} ({}) -> {}", nickname, senderQQ, message);

        // === 命令分发 ===
        if (message.startsWith("#")) {
            handleCommand(message, senderQQ);
        } else {
            // 普通消息，转发到游戏
            String formattedMessage = String.format("§b[QQ]§r <%s> %s", nickname, message);
            broadcastToServer(formattedMessage);
        }
    }

    /**
     * 处理命令 (Java 21 Switch 表达式)
     * 
     * @param message 完整命令字符串
     * @param senderQQ 发送者 QQ
     */
    private static void handleCommand(String message, long senderQQ) {
        String rawCmd = message.substring(1).trim();
        String cmd = rawCmd.toLowerCase();
        
        // 提取命令和参数
        String[] parts = cmd.split("\\s+", 2);
        String commandName = parts[0];
        String args = parts.length > 1 ? parts[1] : "";
        // 保留原始大小写的参数 (用于玩家名)
        String[] rawParts = rawCmd.split("\\s+", 2);
        String rawArgs = rawParts.length > 1 ? rawParts[1] : "";
        
        // Java 21 Switch 表达式
        switch (commandName) {
            case "inv" -> handleInventoryCommand(message);
            case "list", "在线" -> handleListCommand();
            case "tps", "status", "状态" -> handleStatusCommand();
            case "help", "菜单" -> sendReplyToQQ(ServerStatusManager.getHelp());
            case "stopserver", "关服" -> handleStopServerCommand(senderQQ);
            case "addadmin" -> handleAddAdminCommand(args, senderQQ);
            case "removeadmin" -> handleRemoveAdminCommand(args, senderQQ);
            case "id", "bind", "绑定" -> handleBindCommand(rawArgs, senderQQ);
            case "unbind", "removeid", "解绑" -> handleUnbindCommand(senderQQ);
            case "adminunbind" -> handleAdminUnbindCommand(args, senderQQ);
            case "reload" -> handleReloadCommand(senderQQ);
            default -> {
                LOGGER.debug("未知命令: {}", message);
                sendReplyToQQ("❓ 未知命令，输入 #help 查看帮助");
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
     */
    private static void handleBindCommand(String playerName, long senderQQ) {
        // Step A: 快速验证
        if (playerName.isEmpty()) {
            sendReplyToQQ("❌ 用法: #id <游戏ID>\n例如: #id Steve");
            return;
        }
        
        // 正则验证玩家名格式
        if (!PLAYER_NAME_PATTERN.matcher(playerName).matches()) {
            sendReplyToQQ("❌ 无效的游戏ID格式\n只能包含字母、数字、下划线，长度3-16位");
            return;
        }
        
        // 检查 QQ 是否已绑定
        if (DataManager.INSTANCE.isQQBound(senderQQ)) {
            String existingUUID = DataManager.INSTANCE.getBinding(senderQQ);
            sendReplyToQQ("❌ 你已经绑定了游戏账号\n如需更换，请先使用 #unbind 解绑");
            LOGGER.info("用户 {} 尝试重复绑定，已绑定 UUID: {}", senderQQ, existingUUID);
            return;
        }
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            sendReplyToQQ("❌ 服务器未就绪");
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
                sendReplyToQQ("❌ 找不到玩家 " + playerName + "\n请确认ID正确，或先尝试进服一次");
                return;
            }
            
            String uuid = profile.getId().toString();
            
            // 检查 UUID 是否已被其他人绑定
            if (DataManager.INSTANCE.isUUIDBound(uuid)) {
                long boundQQ = DataManager.INSTANCE.getQQByUUID(uuid);
                sendReplyToQQ("❌ 该游戏ID已被其他QQ绑定\n如有疑问请联系管理员");
                LOGGER.warn("UUID {} 已被 QQ {} 绑定，拒绝 QQ {} 的绑定请求", uuid, boundQQ, senderQQ);
                return;
            }
            
            // 执行绑定
            boolean bindSuccess = DataManager.INSTANCE.bind(senderQQ, uuid);
            if (!bindSuccess) {
                sendReplyToQQ("❌ 绑定失败，请稍后重试");
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
                
                sendReplyToQQ(String.format("✅ 绑定成功！\n已将 %s 加入白名单\n现在可以进入服务器了", profile.getName()));
                LOGGER.info("绑定成功: QQ {} -> {} ({})", senderQQ, profile.getName(), uuid);
                
            } catch (Exception e) {
                LOGGER.error("添加白名单失败: {}", e.getMessage());
                sendReplyToQQ("⚠️ 绑定成功，但添加白名单时出错\n请联系管理员");
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
     */
    private static void handleUnbindCommand(long senderQQ) {
        // 检查是否已绑定
        if (!DataManager.INSTANCE.isQQBound(senderQQ)) {
            sendReplyToQQ("❌ 你还没有绑定游戏账号\n使用 #id <游戏ID> 进行绑定");
            return;
        }
        
        String uuid = DataManager.INSTANCE.getBinding(senderQQ);
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            sendReplyToQQ("❌ 服务器未就绪");
            return;
        }
        
        // 在主线程执行解绑操作
        server.execute(() -> {
            // 执行解绑
            boolean unbindSuccess = DataManager.INSTANCE.unbind(senderQQ);
            if (!unbindSuccess) {
                sendReplyToQQ("❌ 解绑失败，请稍后重试");
                return;
            }
            
            // 从白名单移除
            try {
                UUID playerUUID = UUID.fromString(uuid);
                GameProfile profile = new GameProfile(playerUUID, null);
                
                // 尝试从缓存获取完整档案
                Optional<GameProfile> cached = server.getProfileCache().get(playerUUID);
                if (cached.isPresent()) {
                    profile = cached.get();
                }
                
                UserWhiteList whitelist = server.getPlayerList().getWhiteList();
                if (whitelist.isWhiteListed(profile)) {
                    whitelist.remove(profile);
                    whitelist.save();
                    LOGGER.info("已将玩家 {} 从白名单移除", uuid);
                }
                
                sendReplyToQQ("✅ 解绑成功！\n已从白名单中移除");
                LOGGER.info("解绑成功: QQ {} (原UUID: {})", senderQQ, uuid);
                
            } catch (Exception e) {
                LOGGER.error("移除白名单失败: {}", e.getMessage());
                sendReplyToQQ("⚠️ 解绑成功，但移除白名单时出错\n请联系管理员");
            }
        });
    }

    /**
     * 处理 #adminunbind <qq> 命令
     * 管理员强制解绑指定 QQ 的游戏账号
     * 
     * @param args 目标 QQ
     * @param senderQQ 发送者 QQ
     */
    private static void handleAdminUnbindCommand(String args, long senderQQ) {
        // 权限检查
        if (!DataManager.INSTANCE.isAdmin(senderQQ)) {
            sendReplyToQQ("❌ 权限不足: 只有管理员可以执行此命令");
            return;
        }
        
        if (args.isEmpty()) {
            sendReplyToQQ("❌ 用法: #adminunbind <QQ号>");
            return;
        }
        
        long targetQQ;
        try {
            targetQQ = Long.parseLong(args.trim());
        } catch (NumberFormatException e) {
            sendReplyToQQ("❌ 无效的 QQ 号格式");
            return;
        }
        
        // 检查目标是否已绑定
        if (!DataManager.INSTANCE.isQQBound(targetQQ)) {
            sendReplyToQQ(String.format("⚠️ QQ %d 未绑定任何游戏账号", targetQQ));
            return;
        }
        
        // 获取绑定的 UUID (在解绑前)
        String uuid = DataManager.INSTANCE.getBinding(targetQQ);
        
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            sendReplyToQQ("❌ 服务器未就绪");
            return;
        }
        
        // 在主线程执行
        server.execute(() -> {
            // 执行解绑
            boolean unbindSuccess = DataManager.INSTANCE.unbind(targetQQ);
            if (!unbindSuccess) {
                sendReplyToQQ("❌ 解绑失败，请稍后重试");
                return;
            }
            
            // 从白名单移除
            try {
                UUID playerUUID = UUID.fromString(uuid);
                GameProfile profile = new GameProfile(playerUUID, null);
                
                Optional<GameProfile> cached = server.getProfileCache().get(playerUUID);
                if (cached.isPresent()) {
                    profile = cached.get();
                }
                
                UserWhiteList whitelist = server.getPlayerList().getWhiteList();
                if (whitelist.isWhiteListed(profile)) {
                    whitelist.remove(profile);
                    whitelist.save();
                    LOGGER.info("管理员已将玩家 {} 从白名单移除", uuid);
                }
                
                sendReplyToQQ(String.format("✅ 已强制解绑 QQ %d\n已从白名单移除", targetQQ));
                LOGGER.info("管理员 {} 强制解绑: QQ {} (UUID: {})", senderQQ, targetQQ, uuid);
                
            } catch (Exception e) {
                LOGGER.error("移除白名单失败: {}", e.getMessage());
                sendReplyToQQ(String.format("⚠️ 已解绑 QQ %d，但移除白名单时出错", targetQQ));
            }
        });
    }

    /**
     * 处理 #reload 命令
     * 重载配置文件和数据
     * 
     * @param senderQQ 发送者 QQ
     */
    private static void handleReloadCommand(long senderQQ) {
        // 权限检查
        if (!DataManager.INSTANCE.isAdmin(senderQQ)) {
            sendReplyToQQ("❌ 权限不足: 只有管理员可以执行此命令");
            return;
        }
        
        try {
            // 重载数据管理器 (重新读取 mapbot_data.json)
            DataManager.INSTANCE.init();
            
            // 注: BotConfig 使用 ModConfigSpec，会自动同步配置文件变更
            // 无需手动重载
            
            sendReplyToQQ("🔄 配置和数据已重载\n• mapbot_data.json ✓\n• mapbot-common.toml (自动同步)");
            LOGGER.info("管理员 {} 执行了配置重载", senderQQ);
            
        } catch (Exception e) {
            LOGGER.error("重载配置失败: {}", e.getMessage());
            sendReplyToQQ("❌ 重载失败: " + e.getMessage());
        }
    }

    // ================== 其他命令 ==================

    /**
     * 处理 #list 命令
     */
    private static void handleListCommand() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        
        if (server == null) {
            sendReplyToQQ("❌ 服务器未就绪");
            return;
        }
        
        // 线程安全: 调度到主线程
        server.execute(() -> {
            String result = ServerStatusManager.getList();
            sendReplyToQQ(result);
        });
    }

    /**
     * 处理 #tps / #status 命令
     */
    private static void handleStatusCommand() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        
        if (server == null) {
            sendReplyToQQ("❌ 服务器未就绪");
            return;
        }
        
        // 线程安全: 调度到主线程
        server.execute(() -> {
            String result = ServerStatusManager.getServerInfo();
            sendReplyToQQ(result);
        });
    }

    /**
     * 处理 #stopserver 命令
     * 需要管理员权限
     * 
     * @param senderQQ 发送者 QQ
     */
    private static void handleStopServerCommand(long senderQQ) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        
        if (server == null) {
            sendReplyToQQ("❌ 服务器未就绪");
            return;
        }
        
        // 权限检查
        if (!DataManager.INSTANCE.isAdmin(senderQQ)) {
            sendReplyToQQ("❌ 权限不足: 只有管理员可以执行此命令");
            LOGGER.warn("用户 {} 尝试执行 #stopserver 但权限不足", senderQQ);
            return;
        }
        
        sendReplyToQQ("⏹️ 服务器正在关闭...");
        
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
     */
    private static void handleAddAdminCommand(String args, long senderQQ) {
        // 权限检查: 只有管理员可以添加管理员
        // 特殊情况: 如果没有任何管理员，允许第一个添加
        if (DataManager.INSTANCE.getAdmins().isEmpty()) {
            LOGGER.info("当前无管理员，允许首次添加");
        } else if (!DataManager.INSTANCE.isAdmin(senderQQ)) {
            sendReplyToQQ("❌ 权限不足: 只有管理员可以添加新管理员");
            return;
        }
        
        // 解析目标 QQ
        if (args.isEmpty()) {
            sendReplyToQQ("❌ 用法: #addadmin <QQ号>");
            return;
        }
        
        try {
            long targetQQ = Long.parseLong(args.trim());
            
            if (DataManager.INSTANCE.addAdmin(targetQQ)) {
                sendReplyToQQ(String.format("✅ 已添加管理员: %d", targetQQ));
                LOGGER.info("用户 {} 添加了新管理员: {}", senderQQ, targetQQ);
            } else {
                sendReplyToQQ(String.format("⚠️ %d 已经是管理员", targetQQ));
            }
        } catch (NumberFormatException e) {
            sendReplyToQQ("❌ 无效的 QQ 号格式");
        }
    }

    /**
     * 处理 #removeadmin <qq> 命令
     * 
     * @param args 命令参数
     * @param senderQQ 发送者 QQ
     */
    private static void handleRemoveAdminCommand(String args, long senderQQ) {
        // 权限检查
        if (!DataManager.INSTANCE.isAdmin(senderQQ)) {
            sendReplyToQQ("❌ 权限不足: 只有管理员可以移除管理员");
            return;
        }
        
        // 解析目标 QQ
        if (args.isEmpty()) {
            sendReplyToQQ("❌ 用法: #removeadmin <QQ号>");
            return;
        }
        
        try {
            long targetQQ = Long.parseLong(args.trim());
            
            // 防止自我移除
            if (targetQQ == senderQQ) {
                sendReplyToQQ("⚠️ 无法移除自己的管理员权限");
                return;
            }
            
            if (DataManager.INSTANCE.removeAdmin(targetQQ)) {
                sendReplyToQQ(String.format("✅ 已移除管理员: %d", targetQQ));
                LOGGER.info("用户 {} 移除了管理员: {}", senderQQ, targetQQ);
            } else {
                sendReplyToQQ(String.format("⚠️ %d 不是管理员", targetQQ));
            }
        } catch (NumberFormatException e) {
            sendReplyToQQ("❌ 无效的 QQ 号格式");
        }
    }

    /**
     * 处理 #inv <玩家名> 命令
     * Task #007 新增
     */
    private static void handleInventoryCommand(String message) {
        // 解析玩家名
        String targetPlayerName = message.substring(5).trim();
        
        if (targetPlayerName.isEmpty()) {
            sendReplyToQQ("❌ 用法: #inv <玩家名>");
            return;
        }
        
        LOGGER.info("收到库存查询请求: {}", targetPlayerName);
        
        // 关键: 线程调度
        // 必须在服务器主线程执行 getPlayerList() 操作
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        
        if (server == null) {
            sendReplyToQQ("❌ 服务器未就绪");
            return;
        }
        
        // 调度到服务器主线程
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayerByName(targetPlayerName);
            
            // 调用 InventoryManager 获取库存信息
            String result = InventoryManager.getPlayerInventory(player);
            
            // 发送结果回 QQ
            sendReplyToQQ(result);
        });
    }

    /**
     * 处理元事件 (心跳等)
     */
    private static void handleMetaEvent(JsonObject json) {
        String metaEventType = getStringOrNull(json, "meta_event_type");

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
     * 发送回复消息到 QQ 群
     */
    private static void sendReplyToQQ(String message) {
        long targetGroupId = BotConfig.getTargetGroupId();
        if (targetGroupId == 0L) {
            LOGGER.warn("无法发送回复: 目标群号未配置");
            return;
        }
        
        JsonObject params = new JsonObject();
        params.addProperty("group_id", targetGroupId);
        params.addProperty("message", message);
        
        JsonObject packet = new JsonObject();
        packet.addProperty("action", "send_group_msg");
        packet.add("params", params);
        packet.addProperty("echo", "reply_" + System.currentTimeMillis());
        
        BotClient.INSTANCE.sendPacket(packet);
    }

    /**
     * 安全地在服务器主线程广播消息
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
        server.execute(() -> {
            Component chatComponent = Component.literal(message);
            server.getPlayerList().broadcastSystemMessage(chatComponent, false);
            LOGGER.debug("消息已广播到服务器");
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
}