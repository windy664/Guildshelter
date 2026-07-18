package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;

import java.util.ArrayList;
import java.util.List;

/**
 * /gs near：列出附近庄园。
 */
@GsSubCommand(name = "near", permission = "guildshelter.command.near")
public class NearCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        GuildWorld gw = ctx.registry.get(player.getWorld().getName());
        if (gw == null) { sender.sendMessage(Messages.get("error.not_in_guild_world")); return; }
        var all = ctx.manors.findAll(gw.guild());
        List<Manor> nearby = new ArrayList<>();
        for (Manor m : all) {
            var region = new org.windy.guildshelter.domain.layout.LayoutCalculator(gw.layout())
                    .activeRegion(m.slot(), m.level()).shift(gw.originChunkX(), gw.originChunkZ());
            double mx = region.minBlockX() + 8, mz = region.minBlockZ() + 8;
            double dist = Math.hypot(player.getLocation().getX() - mx, player.getLocation().getZ() - mz);
            if (dist < 500) nearby.add(m);
        }
        nearby.sort((a, b) -> {
            var ra = new org.windy.guildshelter.domain.layout.LayoutCalculator(gw.layout())
                    .activeRegion(a.slot(), a.level()).shift(gw.originChunkX(), gw.originChunkZ());
            var rb = new org.windy.guildshelter.domain.layout.LayoutCalculator(gw.layout())
                    .activeRegion(b.slot(), b.level()).shift(gw.originChunkX(), gw.originChunkZ());
            double da = Math.hypot(player.getLocation().getX() - ra.minBlockX() - 8, player.getLocation().getZ() - ra.minBlockZ() - 8);
            double db = Math.hypot(player.getLocation().getX() - rb.minBlockX() - 8, player.getLocation().getZ() - rb.minBlockZ() - 8);
            return Double.compare(da, db);
        });
        if (nearby.isEmpty()) { sender.sendMessage(Messages.get("error.no_nearby")); return; }
        sender.sendMessage(Messages.get("info.near_header"));
        for (Manor m : nearby.subList(0, Math.min(10, nearby.size()))) {
            var region = new org.windy.guildshelter.domain.layout.LayoutCalculator(gw.layout())
                    .activeRegion(m.slot(), m.level()).shift(gw.originChunkX(), gw.originChunkZ());
            double dist = Math.hypot(player.getLocation().getX() - region.minBlockX() - 8, player.getLocation().getZ() - region.minBlockZ() - 8);
            String alias = org.windy.guildshelter.domain.flag.Flag.ALIAS.resolveString(m.flags());
            String name = alias.isBlank() ? "#" + m.slot() : alias + " (#" + m.slot() + ")";
            sender.sendMessage(Messages.get("info.near_entry", name, m.owner().equals(PlayerRef.of(player.getUniqueId())) ? "你的" : "", String.format("%.0f", dist)));
        }
    }
}
