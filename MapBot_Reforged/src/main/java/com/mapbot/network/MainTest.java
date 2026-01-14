package com.mapbot.network;

/**
 * 临时测试类，用于验证 BotClient 逻辑
 * 
 * ⚠️ 警告: 请勿在模组中注册此类
 * 
 * ⚠️ 重要说明 (Task #006 更新):
 * 由于 BotClient 现在依赖 BotConfig (NeoForge ModConfigSpec)，
 * 此测试类在独立运行时将无法工作。
 * 
 * BotConfig 需要 NeoForge 运行时环境来初始化配置系统。
 * 如需测试，请使用以下方法之一:
 * 1. 在 Minecraft 客户端/服务端环境中运行 (推荐)
 * 2. 编写 JUnit 测试并 Mock BotConfig 类
 * 
 * 此文件保留用于代码结构参考。
 */
public class MainTest {

    /**
     * 此 main 方法已被标记为过时
     * 不再能够独立运行
     */
    @Deprecated
    public static void main(String[] args) {
        System.out.println("=== MapBot Network Test ===");
        System.out.println("错误: 此测试类已过时。");
        System.out.println("BotClient 现在依赖 NeoForge 配置系统 (BotConfig)。");
        System.out.println("请在 Minecraft 环境中测试，或编写 Mock 测试。");
        System.out.println("");
        System.out.println("配置文件位置: config/mapbot-common.toml");
        System.out.println("配置项:");
        System.out.println("  - wsUrl: WebSocket 服务器地址");
        System.out.println("  - targetGroupId: 目标 QQ 群号");
        System.out.println("  - reconnectInterval: 重连间隔 (秒)");
        System.out.println("  - debugMode: 调试模式开关");

        System.exit(1);
    }
}
