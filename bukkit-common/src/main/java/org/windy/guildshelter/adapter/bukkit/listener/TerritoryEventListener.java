package org.windy.guildshelter.adapter.bukkit.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.windy.guildshelter.adapter.bukkit.FakePlayerFilter;
import org.windy.guildshelter.adapter.bukkit.GuildWorldRegistry;
import org.windy.guildshelter.api.GuildRef;
import org.windy.guildshelter.api.event.PlayerEnterTerritoryEvent;
import org.windy.guildshelter.api.event.PlayerLeaveTerritoryEvent;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.bukkit.Bukkit;

/**
 * 对外领域事件：玩家<b>进/出公会营地领地（世界级）</b>时 fire {@link PlayerEnterTerritoryEvent}/
 * {@link PlayerLeaveTerritoryEvent}，供附属插件 {@code @EventHandler} 监听。
 *
 * <p>与 {@code TerritoryGreetingListener}（弹迎送词）正交：本类只 fire 事件、<b>不</b>受 greeting 开关门控，
 * 总是注册——保证第三方拿到的领地进出信号稳定。两个公会世界互跳=对旧会 leave + 新会 enter。
 */
public final class TerritoryEventListener implements Listener {

    private final GuildWorldRegistry registry;

    public TerritoryEventListener(GuildWorldRegistry registry) {
        this.registry = registry;
    }

    private static GuildRef ref(GuildWorld gw) {
        return new GuildRef(gw.guild().value(), gw.worldName());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (!FakePlayerFilter.isRealPlayer(player)) {
            return;
        }
        GuildWorld now = registry.get(player.getWorld().getName());
        GuildWorld from = registry.get(event.getFrom().getName());
        if (from != null && (now == null || !now.guild().equals(from.guild()))) {
            Bukkit.getPluginManager().callEvent(new PlayerLeaveTerritoryEvent(ref(from), player));
        }
        if (now != null && (from == null || !from.guild().equals(now.guild()))) {
            Bukkit.getPluginManager().callEvent(new PlayerEnterTerritoryEvent(ref(now), player));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!FakePlayerFilter.isRealPlayer(player)) {
            return;
        }
        GuildWorld gw = registry.get(player.getWorld().getName());
        if (gw != null) {
            Bukkit.getPluginManager().callEvent(new PlayerEnterTerritoryEvent(ref(gw), player));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!FakePlayerFilter.isRealPlayer(player)) {
            return;
        }
        GuildWorld gw = registry.get(player.getWorld().getName());
        if (gw != null) {
            Bukkit.getPluginManager().callEvent(new PlayerLeaveTerritoryEvent(ref(gw), player));
        }
    }
}
