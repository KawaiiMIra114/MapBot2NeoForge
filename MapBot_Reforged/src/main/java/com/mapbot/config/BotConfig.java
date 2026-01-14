/*
 * MapBot Reforged - 配置系统
 * 
 * 遵从 .ai_rules.md 中定义的治理规则。
 * 使用 NeoForge ModConfigSpec 原生配置系统。
 * 
 * 配置文件将自动生成于: config/mapbot-common.toml
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
    public final ModConfigSpec.LongValue targetGroupId;
    public final ModConfigSpec.IntValue reconnectInterval;
    public final ModConfigSpec.BooleanValue debugMode;

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

        targetGroupId = builder
                .comment("目标 QQ 群号")
                .comment("设置为 0 将禁用消息同步功能")
                .comment("只有来自此群的消息会被转发到游戏")
                .defineInRange("targetGroupId", 0L, 0L, Long.MAX_VALUE);

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
     * 获取目标群号
     */
    public static long getTargetGroupId() {
        return INSTANCE.targetGroupId.get();
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
}
