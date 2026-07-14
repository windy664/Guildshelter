package org.windy.guildshelter.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.windy.guildshelter.api.GuildRef;

/** 玩家<b>离开</b>某公会营地领地（世界级）后触发。纯通知，不可取消（v1）。 */
public final class PlayerLeaveTerritoryEvent extends GuildShelterEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;

    public PlayerLeaveTerritoryEvent(GuildRef guild, Player player) {
        super(guild);
        this.player = player;
    }

    public Player getPlayer() {
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
