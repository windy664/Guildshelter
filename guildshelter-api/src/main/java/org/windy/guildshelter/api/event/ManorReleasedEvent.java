package org.windy.guildshelter.api.event;

import org.bukkit.event.HandlerList;
import org.windy.guildshelter.api.GuildRef;

import java.util.UUID;

/** 玩家退出公会、其庄园被释放后触发。 */
public final class ManorReleasedEvent extends GuildShelterEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID player;

    public ManorReleasedEvent(GuildRef guild, UUID player) {
        super(guild);
        this.player = player;
    }

    /** 被释放庄园的原庄主。 */
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
