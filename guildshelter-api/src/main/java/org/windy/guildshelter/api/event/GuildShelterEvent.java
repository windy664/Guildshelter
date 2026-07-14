package org.windy.guildshelter.api.event;

import org.bukkit.event.Event;
import org.windy.guildshelter.api.GuildRef;

/**
 * GuildShelter 对外领域事件基类。第三方附属用标准 Bukkit {@code @EventHandler} 监听具体子类即可
 * （门槛最低，任何 Bukkit 开发者都会）。所有子类都关联一个 {@link GuildRef}。
 *
 * <p>这些是<b>同步</b>事件,在主线程触发。子类各自持有 {@code HandlerList}（Bukkit 约定）。
 */
public abstract class GuildShelterEvent extends Event {

    private final GuildRef guild;

    protected GuildShelterEvent(GuildRef guild) {
        this.guild = guild;
    }

    /** 关联的公会营地。 */
    public GuildRef guild() {
        return guild;
    }
}
