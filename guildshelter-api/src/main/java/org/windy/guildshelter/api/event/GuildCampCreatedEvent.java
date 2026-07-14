package org.windy.guildshelter.api.event;

import org.bukkit.event.HandlerList;
import org.windy.guildshelter.api.GuildRef;

/** 公会营地建成后触发。 */
public final class GuildCampCreatedEvent extends GuildShelterEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    public GuildCampCreatedEvent(GuildRef guild) {
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
