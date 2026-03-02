package com.mapbot.alpha.bridge;

import io.netty.channel.ChannelHandlerContext;
import java.util.Map;

/**
 * Bridge 报文处理接口 (Task #05: 策略派发模式)
 * 每种报文类型对应一个实现
 */
@FunctionalInterface
public interface BridgePacketHandler {
    /**
     * 处理一条 Bridge 报文
     * @param ctx     Netty 通道上下文
     * @param serverId 发送方子服 ID（可能为 null，尚未注册时）
     * @param data    已解析的 JSON 键值对
     */
    void handle(ChannelHandlerContext ctx, String serverId, Map<String, Object> data);
}
