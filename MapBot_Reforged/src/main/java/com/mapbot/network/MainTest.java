package com.mapbot.network;

import com.google.gson.JsonObject;

/**
 * 临时测试类，用于验证 BotClient 逻辑
 * 警告: 请勿在模组中注册此类
 */
public class MainTest {
    public static void main(String[] args) {
        System.out.println("=== MapBot Network Test ===");

        // 获取实例
        BotClient client = BotClient.INSTANCE;

        // 模拟连接
        System.out.println("[Test] 启动连接...");
        client.connect();

        // 模拟发送数据
        JsonObject testPacket = new JsonObject();
        testPacket.addProperty("action", "test_ping");
        testPacket.addProperty("msg", "HelloWorld");

        // 循环保持主线程存活，以便观察重连日志
        // 在实际测试中，如果开启了 NapCat，应该能看到连接成功
        // 如果未开启，应该每5秒看到重连尝试

        try {
            for (int i = 0; i < 15; i++) {
                Thread.sleep(1000);
                System.out.println("[Test] Main Thread Running... " + i);

                if (i == 3) {
                    System.out.println("[Test] 尝试发送数据包...");
                    client.sendPacket(testPacket);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("[Test] 测试结束。");
        System.exit(0);
    }
}
