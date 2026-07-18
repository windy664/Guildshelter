package org.windy.guildshelter.adapter.bukkit.command.manor;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.layout.LayoutCalculator;
import org.windy.guildshelter.domain.model.CityPlot;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.PlayerRef;

@GsSubCommand(name = "cityplot", permission = "guildshelter.command.cityplot")
public class CityPlotCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        if (!ctx.cityPlotsEnabled || ctx.cityPlotCache == null) { player.sendMessage(Messages.get("error.feature_unavailable")); return; }
        GuildWorld gw = ctx.registry.get(player.getWorld().getName());
        if (gw == null) { player.sendMessage(Messages.get("error.cityplot_not_in_camp")); return; }
        String action = args.length >= 2 ? args[1].toLowerCase() : "list";
        if (action.equals("list")) {
            var plots = ctx.cityPlotCache.list(gw.guild());
            if (plots.isEmpty()) { player.sendMessage(Messages.get("info.cityplot_empty")); return; }
            player.sendMessage(Messages.get("info.cityplot_header", gw.guild().value(), plots.size(), ctx.cityPlotsMaxPerGuild));
            for (var p : plots) {
                String who = p.assignee() == null ? Messages.get("info.cityplot_unassigned") : Bukkit.getOfflinePlayer(p.assignee()).getName();
                player.sendMessage(Messages.get("info.cityplot_entry", p.name(), "(" + p.minCx() + "," + p.minCz() + ")", who != null ? who : "?"));
            }
            return;
        }
        PlayerRef ref = PlayerRef.of(player.getUniqueId());
        if (!ctx.service.isGuildAdmin(ref, gw.guild()) && !player.isOp()) { player.sendMessage(Messages.get("error.cityplot_leader_only")); return; }
        switch (action) {
            case "define" -> {
                if (args.length < 3) { player.sendMessage(Messages.get("usage.cityplot")); return; }
                String name = args[2];
                if (ctx.cityPlotCache.get(gw.guild(), name) != null) { player.sendMessage(Messages.get("error.cityplot_name_taken", name)); return; }
                if (ctx.cityPlotCache.list(gw.guild()).size() >= ctx.cityPlotsMaxPerGuild) { player.sendMessage(Messages.get("error.cityplot_limit", ctx.cityPlotsMaxPerGuild)); return; }
                int lx = (player.getLocation().getBlockX() >> 4) - gw.originChunkX();
                int lz = (player.getLocation().getBlockZ() >> 4) - gw.originChunkZ();
                if (!new LayoutCalculator(gw.layout()).classify(lx, lz).isMainCity()) { player.sendMessage(Messages.get("error.cityplot_not_in_city")); return; }
                if (!gw.isCityUnlocked(lx, lz)) { player.sendMessage(Messages.get("error.cityplot_not_unlocked")); return; }
                ctx.cityPlotCache.save(gw.guild(), new CityPlot(name, lx, lz, lx, lz, null));
                player.sendMessage(Messages.get("success.cityplot_defined", name, "(" + lx + "," + lz + ")"));
            }
            case "assign" -> {
                if (args.length < 4) { player.sendMessage(Messages.get("usage.cityplot")); return; }
                var plot = ctx.cityPlotCache.get(gw.guild(), args[2]);
                if (plot == null) { player.sendMessage(Messages.get("error.cityplot_not_found", args[2])); return; }
                org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[3]);
                if (ctx.manors.findByOwner(gw.guild(), PlayerRef.of(target.getUniqueId())).isEmpty()) { player.sendMessage(Messages.get("error.cityplot_not_member", args[3])); return; }
                ctx.cityPlotCache.save(gw.guild(), plot.withAssignee(target.getUniqueId()));
                player.sendMessage(Messages.get("success.cityplot_assigned", plot.name(), args[3]));
            }
            case "unassign" -> {
                if (args.length < 3) { player.sendMessage(Messages.get("usage.cityplot")); return; }
                var plot = ctx.cityPlotCache.get(gw.guild(), args[2]);
                if (plot == null) { player.sendMessage(Messages.get("error.cityplot_not_found", args[2])); return; }
                ctx.cityPlotCache.save(gw.guild(), plot.withAssignee(null));
                player.sendMessage(Messages.get("success.cityplot_unassigned", plot.name()));
            }
            case "remove", "delete" -> {
                if (args.length < 3) { player.sendMessage(Messages.get("usage.cityplot")); return; }
                if (ctx.cityPlotCache.remove(gw.guild(), args[2])) player.sendMessage(Messages.get("success.cityplot_removed", args[2]));
                else player.sendMessage(Messages.get("error.cityplot_not_found", args[2]));
            }
            default -> player.sendMessage(Messages.get("usage.cityplot"));
        }
    }
}
