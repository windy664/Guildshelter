package org.windy.guildshelter.adapter.bukkit.command.manor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.Manor;

import java.util.Set;

@GsSubCommand(name = "unmerge", permission = "guildshelter.command.unmerge", requiresConfirm = true)
public class UnmergeCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        Manor myManor = ctx.currentOwnManor(player).orElse(null);
        if (myManor == null) { sender.sendMessage(Messages.get("error.no_manor")); return; }
        GuildId guild = myManor.guild();
        int primarySlot = myManor.slot();
        Set<Integer> absorbed = ctx.merges.getMergedSlots(guild, primarySlot);
        if (absorbed.isEmpty()) { sender.sendMessage(Messages.get("error.no_merges")); return; }
        if (args.length >= 2) {
            int targetSlot;
            try { targetSlot = Integer.parseInt(args[1]); } catch (NumberFormatException e) { sender.sendMessage(Messages.get("error.number_must_be_int")); return; }
            if (!absorbed.contains(targetSlot)) { sender.sendMessage(Messages.get("error.not_merged", targetSlot)); return; }
            ctx.manors.unmergeOne(guild, primarySlot, targetSlot);
            ctx.merges.removeOne(guild, primarySlot, targetSlot);
            sender.sendMessage(Messages.get("success.unmerged_one", targetSlot));
        } else {
            ctx.merges.unmerge(guild, primarySlot);
            sender.sendMessage(Messages.get("success.unmerged_all", primarySlot, absorbed.size()));
        }
    }
}
