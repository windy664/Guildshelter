package org.windy.guildshelter.api.event;

import org.bukkit.event.HandlerList;
import org.windy.guildshelter.api.GuildRef;

import java.util.UUID;

/** 玩家加入公会并获分配庄园后触发（系统自动分配 / 自助认领均会触发对应入会）。 */
public final class ManorAssignedEvent extends GuildShelterEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID player;

    public ManorAssignedEvent(GuildRef guild, UUID player) {
        super(guild);
        this.player = player;
    }

    /** 获得庄园的玩家。 */
    public UUID player() {
        return player;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
