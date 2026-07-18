package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.Permissions;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;

import java.util.HashSet;
import java.util.Set;

/**
 * /gs deny <玩家|*>：拉黑（需确认）。
 */
@GsSubCommand(name = "deny", permission = "guildshelter.command.deny", requiresConfirm = true)
public class DenyCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.deny")); return; }
        Manor manor = ctx.currentOwnManor(player).orElse(null);
        if (manor == null) { sender.sendMessage(Messages.get("error.no_manor")); return; }
        if (args[1].equals("*")) {
            if (!player.hasPermission(Permissions.DENY_EVERYONE) && !Permissions.hasAdminPerm(player, Permissions.ADMIN_TRUST_OTHER)) {
                sender.sendMessage(Messages.get("error.batch_deny_perm", Permissions.DENY_EVERYONE)); return;
            }
            GuildId guild = manor.guild();
            int count = 0;
            for (Manor m : ctx.manors.findAll(guild)) {
                PlayerRef tref = m.owner();
                if (tref.equals(PlayerRef.of(player.getUniqueId()))) continue;
                if (manor.denied().contains(tref)) continue;
                Set<PlayerRef> co = new HashSet<>(manor.coBuilders());
                Set<PlayerRef> members = new HashSet<>(manor.members());
                Set<PlayerRef> denied = new HashSet<>(manor.denied());
                denied.add(tref); co.remove(tref); members.remove(tref);
                manor = manor.withCoBuilders(co).withMembers(members).withDenied(denied);
                count++;
            }
            if (count == 0) { sender.sendMessage(Messages.get("error.no_need_deny")); return; }
            ctx.manors.save(manor);
            sender.sendMessage(Messages.get("success.denied_batch", count, manor.slot()));
            return;
        }
        org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(args[1]);
        if (target.getUniqueId().equals(player.getUniqueId())) { sender.sendMessage(Messages.get("error.cannot_self_deny")); return; }
        PlayerRef tref = PlayerRef.of(target.getUniqueId());
        Set<PlayerRef> denied = new HashSet<>(manor.denied());
        if (!denied.add(tref)) { sender.sendMessage(Messages.get("success.denied_already", args[1])); return; }
        Set<PlayerRef> co = new HashSet<>(manor.coBuilders());
        Set<PlayerRef> members = new HashSet<>(manor.members());
        co.remove(tref); members.remove(tref);
        ctx.manors.save(manor.withCoBuilders(co).withMembers(members).withDenied(denied));
        sender.sendMessage(Messages.get("success.denied_added", args[1], manor.slot()));
    }
}
