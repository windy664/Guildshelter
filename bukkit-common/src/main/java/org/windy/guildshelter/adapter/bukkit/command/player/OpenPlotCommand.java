package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.Manor;

@GsSubCommand(name = "open", permission = "guildshelter.command.open")
public class OpenPlotCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        Manor manor = ctx.currentOwnManor(player).orElse(null);
        if (manor == null) { sender.sendMessage(Messages.get("error.no_manor")); return; }
        long durationMs = 3600_000L;
        if (args.length >= 2) {
            String raw = args[1].toLowerCase();
            if (raw.equals("0") || raw.equals("perm") || raw.equals("permanent")) { durationMs = 0; }
            else { try { long minutes = Long.parseLong(raw.replaceAll("[^0-9]", "")); durationMs = minutes * 60_000L; } catch (NumberFormatException e) { sender.sendMessage(Messages.get("usage.open")); return; } }
        }
        String key = manor.guild().value() + ":" + manor.slot();
        long expireAt = durationMs > 0 ? System.currentTimeMillis() + durationMs : 0;
        ctx.openPlots.put(key, expireAt);
        if (durationMs > 0) {
            final String k = key;
            new org.bukkit.scheduler.BukkitRunnable() {
                @Override public void run() {
                    Long exp = ctx.openPlots.get(k);
                    if (exp != null && exp > 0 && System.currentTimeMillis() >= exp) ctx.openPlots.remove(k);
                }
            }.runTaskLater(org.bukkit.Bukkit.getPluginManager().getPlugin("GuildShelter"), durationMs / 50);
        }
        String timeStr = durationMs > 0 ? (durationMs / 60_000) + "分钟" : "永久";
        sender.sendMessage(Messages.get("success.plot_opened", timeStr));
    }
}
