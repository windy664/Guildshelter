package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.Permissions;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;

@GsSubCommand(name = "grant", permission = "guildshelter.command.grant", requiresConfirm = true)
public class GrantCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        if (!Permissions.hasAdminPerm(player, Permissions.ADMIN_TRUST_OTHER)) { sender.sendMessage(Messages.get("error.need_admin_perm")); return; }
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.grant")); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) { sender.sendMessage(Messages.get("error.player_offline", args[1])); return; }
        PlayerRef targetRef = PlayerRef.of(target.getUniqueId());
        GuildWorld gw = ctx.registry.get(player.getWorld().getName());
        GuildId guild;
        if (gw != null) { guild = gw.guild(); }
        else {
            Manor myManor = ctx.manors.findByOwnerAnywhere(PlayerRef.of(player.getUniqueId())).orElse(null);
            if (myManor == null) { sender.sendMessage(Messages.get("error.not_in_any_world")); return; }
            guild = myManor.guild();
        }
        int slot = ctx.manors.nextFreeSlot(guild);
        switch (ctx.service.claimManorAt(guild, targetRef, slot)) {
            case SUCCESS -> {
                sender.sendMessage(Messages.get("success.grant", target.getName()));
                org.windy.guildshelter.GuildShelterPlugin.sendWelcome(target, guild.value(), slot);
            }
            case GUILD_FULL -> {
                GuildWorld g2 = ctx.guilds.find(guild).orElse(null);
                sender.sendMessage(Messages.get("error.guild_full", g2 != null ? ctx.service.effectiveCapacity(g2) : 0));
            }
            default -> sender.sendMessage(Messages.get("success.grant_failed"));
        }
    }
}
