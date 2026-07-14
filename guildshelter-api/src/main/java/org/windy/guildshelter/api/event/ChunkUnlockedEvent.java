package org.windy.guildshelter.api.event;

import org.bukkit.event.HandlerList;
import org.windy.guildshelter.api.GuildRef;

import java.util.UUID;

/** 玩家解锁了某庄园的一个 chunk 后触发。坐标为世界 chunk 坐标。 */
public final class ChunkUnlockedEvent extends GuildShelterEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final int slot;
    private final int chunkX;
    private final int chunkZ;
    private final UUID player;

    public ChunkUnlockedEvent(GuildRef guild, int slot, int chunkX, int chunkZ, UUID player) {
        super(guild);
        this.slot = slot;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.player = player;
    }

    public int slot() { return slot; }
    public int chunkX() { return chunkX; }
    public int chunkZ() { return chunkZ; }
    public UUID player() { return player; }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
