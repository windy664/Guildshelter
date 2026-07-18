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
 * /gs untrust <玩家>：移除共建人。
 */
@GsSubCommand(name = "untrust", permission = "guildshelter.command.untrust")
public class UntrustCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.untrust")); return; }
        Manor manor = ctx.currentOwnManor(player).orElse(null);
        if (manor == null) { sender.sendMessage(Messages.get("error.no_manor")); return; }
        org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(args[1]);
        Set<PlayerRef> co = new HashSet<>(manor.coBuilders());
        if (!co.remove(PlayerRef.of(target.getUniqueId()))) { sender.sendMessage(Messages.get("success.trust_removed_not", args[1])); return; }
        ctx.manors.save(manor.withCoBuilders(co));
        sender.sendMessage(Messages.get("success.trust_removed", args[1]));
    }
}
