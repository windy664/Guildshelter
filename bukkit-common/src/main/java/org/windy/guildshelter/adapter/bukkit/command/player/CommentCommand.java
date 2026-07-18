package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.layout.Classification;
import org.windy.guildshelter.domain.layout.LayoutCalculator;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.PlayerRef;

import java.util.Arrays;

@GsSubCommand(name = "comment", permission = "guildshelter.command.comment")
public class CommentCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.comment")); return; }
        GuildWorld gw = ctx.registry.get(player.getWorld().getName());
        if (gw == null) { sender.sendMessage(Messages.get("error.not_in_guild_world")); return; }
        LayoutCalculator layout = new LayoutCalculator(gw.layout());
        int lx = (player.getLocation().getBlockX() >> 4) - gw.originChunkX();
        int lz = (player.getLocation().getBlockZ() >> 4) - gw.originChunkZ();
        Classification c = ctx.classify(gw, lx, lz);
        if (!c.isPlot()) { sender.sendMessage(Messages.get("error.not_on_plot")); return; }
        String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        ctx.manors.addComment(gw.guild(), c.slot(), PlayerRef.of(player.getUniqueId()), msg);
        sender.sendMessage(Messages.get("success.comment_added", c.slot()));
    }
}
