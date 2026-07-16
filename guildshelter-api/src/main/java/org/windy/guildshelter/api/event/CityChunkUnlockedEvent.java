package org.windy.guildshelter.api.event;

import org.bukkit.event.HandlerList;
import org.windy.guildshelter.api.GuildRef;

/** A guild main-city chunk was unlocked. Coordinates are world chunk coordinates. */
public final class CityChunkUnlockedEvent extends GuildShelterEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int chunkX;
    private final int chunkZ;

    public CityChunkUnlockedEvent(GuildRef guild, int chunkX, int chunkZ) {
        super(guild);
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public int chunkX() {
        return chunkX;
    }

    public int chunkZ() {
        return chunkZ;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
