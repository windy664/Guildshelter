package org.windy.guildshelter.api.event;

import org.bukkit.event.HandlerList;
import org.windy.guildshelter.api.GuildRef;

import java.util.UUID;

/** 庄园升级后触发。 */
public final class ManorUpgradedEvent extends GuildShelterEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int slot;
    private final int oldLevel;
    private final int newLevel;
    private final UUID owner;

    public ManorUpgradedEvent(GuildRef guild, int slot, int oldLevel, int newLevel, UUID owner) {
        super(guild);
        this.slot = slot;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
        this.owner = owner;
    }

    public int slot() { return slot; }
    public int oldLevel() { return oldLevel; }
    public int newLevel() { return newLevel; }
    public UUID owner() { return owner; }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
