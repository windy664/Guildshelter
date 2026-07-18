package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.layout.Classification;
import org.windy.guildshelter.domain.layout.LayoutCalculator;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;

@GsSubCommand(name = "rate", permission = "guildshelter.command.rate")
public class RateCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.rate")); return; }
        int score;
        try { score = Integer.parseInt(args[1]); } catch (NumberFormatException e) { sender.sendMessage(Messages.get("error.score_must_be_int")); return; }
        if (score < 1 || score > 10) { sender.sendMessage(Messages.get("error.score_range")); return; }
        Manor targetManor = null;
        if (args.length >= 4) {
            GuildId guild = new GuildId(args[2]);
            try { int slot = Integer.parseInt(args[3]); targetManor = ctx.manors.findBySlot(guild, slot).orElse(null); } catch (NumberFormatException ignored) {}
            if (targetManor == null) { sender.sendMessage(Messages.get("error.plot_not_found", args[2], args[3])); return; }
        } else {
            GuildWorld gw = ctx.registry.get(player.getWorld().getName());
            if (gw == null) { sender.sendMessage(Messages.get("error.rate_need_world")); return; }
            LayoutCalculator layout = new LayoutCalculator(gw.layout());
            int lx = (player.getLocation().getBlockX() >> 4) - gw.originChunkX();
            int lz = (player.getLocation().getBlockZ() >> 4) - gw.originChunkZ();
            Classification c = ctx.classify(gw, lx, lz);
            if (!c.isPlot()) { sender.sendMessage(Messages.get("error.rate_need_plot")); return; }
            targetManor = ctx.manors.findBySlot(gw.guild(), c.slot()).orElse(null);
            if (targetManor == null) { sender.sendMessage(Messages.get("error.slot_empty", c.slot())); return; }
        }
        PlayerRef ref = PlayerRef.of(player.getUniqueId());
        if (targetManor.owner().equals(ref)) { sender.sendMessage(Messages.get("error.cannot_rate_self")); return; }
        ctx.manors.rate(targetManor.guild(), targetManor.slot(), ref, score);
        double avg = ctx.manors.getAverageRating(targetManor.guild(), targetManor.slot());
        int count = ctx.manors.getRatingCount(targetManor.guild(), targetManor.slot());
        sender.sendMessage(Messages.get("success.rated", targetManor.slot(), score, String.format("%.1f", avg), count));
    }
}
