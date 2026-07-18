package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.flag.Flag;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;

import java.util.List;

@GsSubCommand(name = "top", permission = "guildshelter.command.top", requiresPlayer = false)
public class TopCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        GuildId guild = null; String sortBy = "rating";
        for (int i = 1; i < args.length; i++) {
            String a = args[i].toLowerCase();
            if (a.equals("rating") || a.equals("level") || a.equals("members") || a.equals("entities") || a.equals("visits")) sortBy = a;
            else if (guild == null) guild = new GuildId(args[i]);
        }
        if (guild == null && sender instanceof Player player) { GuildWorld gw = ctx.registry.get(player.getWorld().getName()); if (gw != null) guild = gw.guild(); }
        if (guild == null && sender instanceof Player player) { Manor manor = ctx.manors.findByOwnerAnywhere(PlayerRef.of(player.getUniqueId())).orElse(null); if (manor != null) guild = manor.guild(); }
        if (guild == null) { sender.sendMessage(Messages.get("usage.top")); return; }
        final GuildId finalGuild = guild;
        List<Manor> all = ctx.manors.findAll(finalGuild);
        if (all.isEmpty()) { sender.sendMessage(Messages.get("error.no_plots_in_guild")); return; }
        switch (sortBy) {
            case "level" -> all.sort((a, b) -> Integer.compare(b.level(), a.level()));
            case "members" -> all.sort((a, b) -> Integer.compare(b.coBuilders().size() + b.members().size(), a.coBuilders().size() + a.members().size()));
            case "visits" -> all.sort((a, b) -> Integer.compare(ctx.manors.getVisitCount(finalGuild, b.slot()), ctx.manors.getVisitCount(finalGuild, a.slot())));
            case "entities" -> {
                World world = Bukkit.getWorld(ctx.guilds.find(finalGuild).map(GuildWorld::worldName).orElse(""));
                if (world != null && ctx.census != null) all.sort((a, b) -> Integer.compare(ctx.census.countAt(world, b).livingTotal(), ctx.census.countAt(world, a).livingTotal()));
            }
            default -> all.sort((a, b) -> Double.compare(ctx.manors.getAverageRating(finalGuild, b.slot()), ctx.manors.getAverageRating(finalGuild, a.slot())));
        }
        String title = switch (sortBy) { case "level" -> "等级"; case "members" -> "成员数"; case "entities" -> "实体数"; case "visits" -> "访问量"; default -> "评分"; };
        sender.sendMessage(Messages.get("info.top_header", finalGuild.value(), title));
        int show = Math.min(all.size(), 10);
        for (int i = 0; i < show; i++) {
            Manor m = all.get(i);
            String alias = Flag.ALIAS.resolveString(m.flags());
            String name = alias.isBlank() ? "#" + m.slot() : alias + " (#" + m.slot() + ")";
            String ownerName = Bukkit.getOfflinePlayer(m.owner().uuid()).getName();
            String value = switch (sortBy) {
                case "level" -> "Lv" + m.level();
                case "members" -> (m.coBuilders().size() + m.members().size()) + "人";
                case "visits" -> ctx.manors.getVisitCount(finalGuild, m.slot()) + "次";
                case "entities" -> { World w = Bukkit.getWorld(ctx.guilds.find(finalGuild).map(GuildWorld::worldName).orElse("")); yield w != null && ctx.census != null ? ctx.census.countAt(w, m).livingTotal() + "只" : "?"; }
                default -> { double avg = ctx.manors.getAverageRating(finalGuild, m.slot()); int count = ctx.manors.getRatingCount(finalGuild, m.slot()); yield String.format("%.1f", avg) + "分(" + count + "人)"; }
            };
            sender.sendMessage(Messages.get("info.top_entry", i + 1, name, ownerName != null ? ownerName : "?", title, value));
        }
    }
}
