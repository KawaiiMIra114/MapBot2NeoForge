/*
 * MapBot Reforged - 配置系统
 * 
 * 遵从 .ai_rules.md 中定义的治理规则。
 * 使用 NeoForge ModConfigSpec 原生配置系统。
 * 
 * 配置文件将自动生成于: config/mapbot-common.toml
 * 
 * Task #012-STEP2: 新增双群结构 (playerGroupId + adminGroupId)
 * Task #014-STEP2: 新增 botQQ 配置项
 */

package com.mapbot.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * MapBot 配置类
 * 使用 NeoForge 原生配置 API，兼容服务器控制面板
 */
public class BotConfig {

    // 配置规范实例
    public static final ModConfigSpec SPEC;
    public static final BotConfig INSTANCE;

    // 配置项
    public final ModConfigSpec.ConfigValue<String> wsUrl;
    public final ModConfigSpec.LongValue playerGroupId;
    public final ModConfigSpec.LongValue adminGroupId;
    public final ModConfigSpec.LongValue botQQ;
    public final ModConfigSpec.IntValue reconnectInterval;
    public final ModConfigSpec.BooleanValue debugMode;
    
    // Alpha Core 配置 (STEP 11)
    public final ModConfigSpec.ConfigValue<String> serverId;
    public final ModConfigSpec.ConfigValue<String> alphaHost;
    public final ModConfigSpec.IntValue alphaPort;
    public final ModConfigSpec.ConfigValue<String> alphaToken;
    public final ModConfigSpec.ConfigValue<String> transferHost;
    public final ModConfigSpec.IntValue transferPort;

    static {
        Pair<BotConfig, ModConfigSpec> pair = new ModConfigSpec.Builder()
                .configure(BotConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    private BotConfig(ModConfigSpec.Builder builder) {
        builder.comment("MapBot Reforged 配置文件")
                .comment("此文件由 NeoForge 自动管理，修改后无需重启服务器")
                .push("connection");

        wsUrl = builder
                .comment("NapCat WebSocket 服务器地址")
                .comment("默认为本地地址，如 NapCat 在远程服务器请修改")
                .define("wsUrl", "ws://127.0.0.1:3000");

        reconnectInterval = builder
                .comment("断线重连间隔 (秒)")
                .comment("连接失败后等待多少秒再次尝试")
                .defineInRange("reconnectInterval", 5, 1, 60);

        builder.pop();

        builder.push("messaging");

        playerGroupId = builder
                .comment("玩家群号")
                .comment("普通消息将从此群转发到游戏")
                .comment("设置为 0 将禁用消息转发功能")
                .defineInRange("playerGroupId", 0L, 0L, Long.MAX_VALUE);
        
        adminGroupId = builder
                .comment("管理群号")
                .comment("敏感命令 (#inv, #stopserver 等) 仅限此群使用")
                .comment("设置为 0 将禁用管理群功能 (不推荐)")
                .defineInRange("adminGroupId", 0L, 0L, Long.MAX_VALUE);
        
        botQQ = builder
                .comment("机器人 QQ 号")
                .comment("用于识别机器人转发的消息，实现回复通知功能")
                .defineInRange("botQQ", 0L, 0L, Long.MAX_VALUE);

        builder.pop();
        
        // Alpha Core 连接配置 (STEP 11)
        builder.push("alpha");
        
        serverId = builder
                .comment("服务器 ID")
                .comment("在 Alpha Core 中标识此服务器的唯一名称")
                .define("serverId", "default");
        
        alphaHost = builder
                .comment("Alpha Core 主机地址")
                .comment("设置为空字符串将禁用 Bridge 功能")
                .define("alphaHost", "");
        
        alphaPort = builder
                .comment("Alpha Core Bridge 端口")
                .comment("除智能分流口 25560 外，请避开 25560-25566 保留端口段")
                .comment("默认使用 25661")
                .defineInRange("alphaPort", 25661, 1, 65535);

        alphaToken = builder
                .comment("Alpha Core Bridge 鉴权令牌")
                .comment("需与 Alpha 端 auth.bridge.token 配置一致")
                .define("alphaToken", "");

        transferHost = builder
                .comment("本服对外可转移地址（用于跨服 /transfer）")
                .comment("填写玩家客户端可访问到的主机名/IP，例如 mc.example.com")
                .comment("留空表示不参与跨服转移地址上报")
                .define("transferHost", "");

        transferPort = builder
                .comment("本服对外可转移端口（用于跨服 /transfer）")
                .defineInRange("transferPort", 25565, 1, 65535);
        
        builder.pop();

        builder.push("debug");

        debugMode = builder
                .comment("调试模式")
                .comment("启用后将记录所有原始 WebSocket 数据包")
                .define("debugMode", false);

        builder.pop();
    }

    /**
     * 获取 WebSocket URL
     */
    public static String getWsUrl() {
        return INSTANCE.wsUrl.get();
    }

    /**
     * 获取玩家群号 (消息转发源)
     */
    public static long getPlayerGroupId() {
        return INSTANCE.playerGroupId.get();
    }
    
    /**
     * 获取管理群号 (敏感命令专用)
     */
    public static long getAdminGroupId() {
        return INSTANCE.adminGroupId.get();
    }
    
    /**
     * 获取 OP 群号 (getAdminGroupId 的别名)
     * Task #016 STEP1 新增
     */
    public static long getOpGroupId() {
        return getAdminGroupId();
    }
    
    /**
     * 获取目标群号 (兼容旧代码，即将废弃)
     * @deprecated 请使用 getPlayerGroupId() 或 getAdminGroupId()
     */
    @Deprecated
    public static long getTargetGroupId() {
        return getPlayerGroupId();
    }

    /**
     * 获取重连间隔
     */
    public static int getReconnectInterval() {
        return INSTANCE.reconnectInterval.get();
    }

    /**
     * 是否启用调试模式
     */
    public static boolean isDebugMode() {
        return INSTANCE.debugMode.get();
    }
    
    /**
     * 获取机器人 QQ 号
     * Task #014-STEP2 新增
     */
    public static long getBotQQ() {
        return INSTANCE.botQQ.get();
    }
    
    /**
     * 获取服务器 ID (Bridge 注册名)
     * STEP 11 新增
     */
    public static String getServerId() {
        return INSTANCE.serverId.get();
    }
    
    /**
     * 获取 Alpha Core 主机地址
     * STEP 11 新增
     */
    public static String getAlphaHost() {
        return INSTANCE.alphaHost.get();
    }
    
    /**
     * 获取 Alpha Core Bridge 端口
     * STEP 11 新增
     */
    public static int getAlphaPort() {
        return INSTANCE.alphaPort.get();
    }

    /**
     * 获取 Alpha Core Bridge 鉴权令牌
     */
    public static String getAlphaToken() {
        return INSTANCE.alphaToken.get();
    }

    /**
     * 获取本服对外可转移主机
     */
    public static String getTransferHost() {
        return INSTANCE.transferHost.get();
    }

    /**
     * 获取本服对外可转移端口
     */
    public static int getTransferPort() {
        return INSTANCE.transferPort.get();
    }
}
