package org.windy.guildshelter.horde;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.windy.guildshelter.api.GuildRef;
import org.windy.guildshelter.api.GuildShelterAPI;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;

final class HordeReminderListener implements Listener {

    private final JavaPlugin plugin;
    private final GuildShelterAPI api;
    private final HordeManager manager;
    private final WeeklyHordeScheduler scheduler;
    private final HordeMessages messages;
    private final boolean enabled;
    private final long joinDelayTicks;

    HordeReminderListener(JavaPlugin plugin, GuildShelterAPI api, HordeManager manager, WeeklyHordeScheduler scheduler,
                          HordeMessages messages) {
        this.plugin = plugin;
        this.api = api;
        this.manager = manager;
        this.scheduler = scheduler;
        this.messages = messages;
        this.enabled = plugin.getConfig().getBoolean("reminder.enabled", true);
        this.joinDelayTicks = Math.max(1L, plugin.getConfig().getLong("reminder.join-delay-ticks", 20L));
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        remindLater(event.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        remindLater(event.getPlayer());
    }

    private void remindLater(Player player) {
        if (!enabled) {
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> remind(player), joinDelayTicks);
    }

    private void remind(Player player) {
        if (!player.isOnline()) {
            return;
        }
        Optional<GuildRef> camp = api.guildAt(player.getLocation());
        if (camp.isEmpty()) {
            return;
        }
        GuildRef guild = camp.get();
        if (manager.hasActiveHorde(guild.worldName())) {
            player.sendMessage(messages.get("reminder.running"));
            return;
        }
        Optional<WeeklyHordeScheduler.ScheduleEta> eta = scheduler.etaFor(guild);
        if (eta.isEmpty()) {
            return;
        }
        WeeklyHordeScheduler.ScheduleEta value = eta.get();
        if (value.queued()) {
            player.sendMessage(messages.get("reminder.queued"));
            return;
        }
        Duration remaining = Duration.between(ZonedDateTime.now(value.dueAt().getZone()), value.dueAt());
        player.sendMessage(messages.get("reminder.scheduled", "remaining", formatRemaining(remaining)));
    }

    private String formatRemaining(Duration duration) {
        long minutes = Math.max(0, duration.toMinutes());
        if (minutes < 60 * 24) {
            return messages.get("reminder.less-than-day");
        }
        long days = (minutes + 60 * 24 - 1) / (60 * 24);
        return messages.get("reminder.days", "days", days);
    }
}
