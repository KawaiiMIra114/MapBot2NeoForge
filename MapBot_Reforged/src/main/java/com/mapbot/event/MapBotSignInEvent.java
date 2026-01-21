package com.mapbot.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 玩家签到事件
 * 可被 KubeJS 或其他模组监听
 * 如果取消此事件 (setCanceled(true))，MapBot 将不会发放默认保底奖励
 */
public class MapBotSignInEvent extends MapBotEvent implements ICancellableEvent {
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
