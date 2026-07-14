package org.windy.guildshelter.api.event;

import org.bukkit.event.HandlerList;
import org.windy.guildshelter.api.GuildRef;

/** 公会解散、营地被卸载/清理后触发。 */
public final class GuildDissolvedEvent extends GuildShelterEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public GuildDissolvedEvent(GuildRef guild) {
        super(guild);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
