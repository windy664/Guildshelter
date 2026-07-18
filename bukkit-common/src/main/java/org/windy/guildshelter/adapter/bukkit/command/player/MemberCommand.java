package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;

import java.util.HashSet;
import java.util.Set;

/**
 * /gs member <add|remove> <玩家>：管理受限成员。
 */
@GsSubCommand(name = "member", permission = "guildshelter.command.member")
public class MemberCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        if (args.length < 3 || (!args[1].equalsIgnoreCase("add") && !args[1].equalsIgnoreCase("remove"))) {
            sender.sendMessage(Messages.get("usage.member")); return;
        }
        Manor manor = ctx.currentOwnManor(player).orElse(null);
        if (manor == null) { sender.sendMessage(Messages.get("error.no_manor")); return; }
        org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(args[2]);
        if (target.getUniqueId().equals(player.getUniqueId())) { sender.sendMessage(Messages.get("error.cannot_self")); return; }
        PlayerRef tref = PlayerRef.of(target.getUniqueId());
        Set<PlayerRef> members = new HashSet<>(manor.members());
        if (args[1].equalsIgnoreCase("add")) {
            if (!members.add(tref)) { sender.sendMessage(Messages.get("success.member_added_already", args[2])); return; }
            Set<PlayerRef> co = new HashSet<>(manor.coBuilders());
            Set<PlayerRef> denied = new HashSet<>(manor.denied());
            co.remove(tref); denied.remove(tref);
            ctx.manors.save(manor.withCoBuilders(co).withMembers(members).withDenied(denied));
            sender.sendMessage(Messages.get("success.member_added", args[2]));
        } else {
            if (!members.remove(tref)) { sender.sendMessage(Messages.get("success.member_removed_not", args[2])); return; }
            ctx.manors.save(manor.withMembers(members));
            sender.sendMessage(Messages.get("success.member_removed", args[2]));
        }
    }
}
