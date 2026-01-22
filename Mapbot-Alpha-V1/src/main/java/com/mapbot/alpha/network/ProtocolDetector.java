package com.mapbot.alpha.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 端口分流器
 * 自动识别 Minecraft 协议与 HTTP 协议
 */
public class ProtocolDetector extends ByteToMessageDecoder {
    private static final Logger LOGGER = LoggerFactory.getLogger("Mapbot/Network/Detector");

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // 需要至少 3 个字节来判断
        if (in.readableBytes() < 3) {
            return;
        }

        int readerIndex = in.readerIndex();
        byte b1 = in.getByte(readerIndex);
        byte b2 = in.getByte(readerIndex + 1);
        byte b3 = in.getByte(readerIndex + 2);

        // 识别 HTTP (GET, POS, PUT, HEA...)
        if (isHttp(b1, b2, b3)) {
            LOGGER.debug("检测到 HTTP 协议，切换至 Web 控制台 Handler");
            switchToHttp(ctx);
        } 
        // 识别 Minecraft (通常第一个字节是长度 VarInt, 或者老版本的 0xFE)
        else {
            LOGGER.debug("检测到 Minecraft 协议，切换至 TCP 代理 Handler");
            switchToMinecraft(ctx);
        }
    }

    private boolean isHttp(byte b1, byte b2, byte b3) {
        return (b1 == 'G' && b2 == 'E' && b3 == 'T') ||
               (b1 == 'P' && b2 == 'O' && b3 == 'S') ||
               (b1 == 'P' && b2 == 'U' && b3 == 'T') ||
               (b1 == 'H' && b2 == 'E' && b3 == 'A') ||
               (b1 == 'D' && b2 == 'E' && b3 == 'L');
    }

import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

    private void switchToHttp(ChannelHandlerContext ctx) {
        ctx.pipeline().addLast(new HttpServerCodec());
        ctx.pipeline().addLast(new HttpObjectAggregator(65536));
        ctx.pipeline().addLast(new HttpRequestDispatcher());
        ctx.pipeline().remove(this);
    }

    private void switchToMinecraft(ChannelHandlerContext ctx) {
        ctx.pipeline().addLast(new MinecraftProxyHandler());
        ctx.pipeline().remove(this);
    }
}
