package org.windy.guildshelter.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.windy.guildshelter.api.GuildRef;

/**
 * 玩家<b>进入</b>某公会营地领地（世界级）后触发。纯通知，不可取消（v1）。
 * 进出主城/庄园等更细粒度后续期再加。
 */
public final class PlayerEnterTerritoryEvent extends GuildShelterEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;

    public PlayerEnterTerritoryEvent(GuildRef guild, Player player) {
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
