package com.mapbot.event;

import net.minecraft.server.level.ServerPlayer;

/**
 * 玩家签到事件
 * 可被 KubeJS 或其他模组监听
 */
public class MapBotSignInEvent extends MapBotEvent {
    private final ServerPlayer player;
    private final long qq;

    public MapBotSignInEvent(ServerPlayer player, long qq) {
        this.player = player;
        this.qq = qq;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public long getQq() {
        return qq;
    }
}
