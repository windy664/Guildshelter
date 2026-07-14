package org.windy.guildshelter.api.event;

import org.bukkit.event.HandlerList;
import org.windy.guildshelter.api.GuildRef;

/** 公会升级后触发。 */
public final class GuildUpgradedEvent extends GuildShelterEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int newLevel;

    public GuildUpgradedEvent(GuildRef guild, int newLevel) {
        super(guild);
        this.newLevel = newLevel;
    }

    public int newLevel() {
        return newLevel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
