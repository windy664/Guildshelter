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
 * /gs trust <玩家|*>：加共建人。
 */
@GsSubCommand(name = "trust", permission = "guildshelter.command.trust")
public class TrustCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.trust")); return; }
        Manor manor = ctx.currentOwnManor(player).orElse(null);
        if (manor == null) { sender.sendMessage(Messages.get("error.no_manor")); return; }
        if (args[1].equals("*")) {
            if (!player.hasPermission(Permissions.TRUST_EVERYONE) && !Permissions.hasAdminPerm(player, Permissions.ADMIN_TRUST_OTHER)) {
                sender.sendMessage(Messages.get("error.batch_trust_perm", Permissions.TRUST_EVERYONE)); return;
            }
            GuildId guild = manor.guild();
            int count = 0;
            for (Manor m : ctx.manors.findAll(guild)) {
                PlayerRef tref = m.owner();
                if (tref.equals(PlayerRef.of(player.getUniqueId()))) continue;
                if (manor.coBuilders().contains(tref)) continue;
                Set<PlayerRef> co = new HashSet<>(manor.coBuilders());
                Set<PlayerRef> members = new HashSet<>(manor.members());
                Set<PlayerRef> denied = new HashSet<>(manor.denied());
                co.add(tref); members.remove(tref); denied.remove(tref);
                manor = manor.withCoBuilders(co).withMembers(members).withDenied(denied);
                count++;
            }
            if (count == 0) { sender.sendMessage(Messages.get("error.no_need_trust")); return; }
            ctx.manors.save(manor);
            sender.sendMessage(Messages.get("success.trust_batch", count, manor.slot()));
            return;
        }
        org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(args[1]);
        if (target.getUniqueId().equals(player.getUniqueId())) { sender.sendMessage(Messages.get("error.cannot_self")); return; }
        PlayerRef tref = PlayerRef.of(target.getUniqueId());
        Set<PlayerRef> co = new HashSet<>(manor.coBuilders());
        if (!co.add(tref)) { sender.sendMessage(Messages.get("success.trust_added_already", args[1])); return; }
        Set<PlayerRef> members = new HashSet<>(manor.members());
        Set<PlayerRef> denied = new HashSet<>(manor.denied());
        members.remove(tref); denied.remove(tref);
        ctx.manors.save(manor.withCoBuilders(co).withMembers(members).withDenied(denied));
        sender.sendMessage(Messages.get("success.trust_added", args[1], manor.slot()));
    }
}
