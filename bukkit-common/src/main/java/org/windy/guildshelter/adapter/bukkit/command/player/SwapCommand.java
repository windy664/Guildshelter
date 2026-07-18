package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;

@GsSubCommand(name = "swap", permission = "guildshelter.command.swap", requiresConfirm = true)
public class SwapCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.swap")); return; }
        PlayerRef ref = PlayerRef.of(player.getUniqueId());
        Manor myManor = ctx.currentOwnManor(player).orElse(null);
        if (myManor == null) { sender.sendMessage(Messages.get("error.no_manor")); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) { sender.sendMessage(Messages.get("error.player_offline", args[1])); return; }
        if (target.getUniqueId().equals(player.getUniqueId())) { sender.sendMessage(Messages.get("error.cannot_self_swap")); return; }
        PlayerRef targetRef = PlayerRef.of(target.getUniqueId());
        Manor targetManor = ctx.manors.findByOwnerAnywhere(targetRef).orElse(null);
        if (targetManor == null) { sender.sendMessage(Messages.get("error.target_no_manor", target.getName())); return; }
        if (!myManor.guild().equals(targetManor.guild())) { sender.sendMessage(Messages.get("error.not_same_guild")); return; }
        int mySlot = myManor.slot(); int theirSlot = targetManor.slot();
        Manor newMine = new Manor(theirSlot, myManor.guild(), ref, myManor.level(), myManor.coBuilders(), myManor.members(), myManor.denied(), myManor.flags(), myManor.unlockedChunks());
        Manor newTheirs = new Manor(mySlot, targetManor.guild(), targetRef, targetManor.level(), targetManor.coBuilders(), targetManor.members(), targetManor.denied(), targetManor.flags(), targetManor.unlockedChunks());
        try { ctx.manors.save(newMine); ctx.manors.save(newTheirs); } catch (Exception e) { sender.sendMessage(Messages.get("error.export_failed", e.getMessage())); return; }
        sender.sendMessage(Messages.get("success.swap", target.getName(), mySlot, theirSlot));
        target.sendMessage(Messages.get("success.swap_notify", player.getName(), theirSlot, mySlot));
    }
}
