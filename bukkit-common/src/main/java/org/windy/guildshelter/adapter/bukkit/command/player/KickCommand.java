package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.ManorRoles;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;

@GsSubCommand(name = "kick", permission = "guildshelter.command.kick")
public class KickCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.kick")); return; }
        Manor manor = ctx.currentOwnManor(player).orElse(null);
        if (manor == null) { sender.sendMessage(Messages.get("error.no_manor")); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) { sender.sendMessage(Messages.get("error.player_offline", args[1])); return; }
        if (target.getUniqueId().equals(player.getUniqueId())) { sender.sendMessage(Messages.get("error.cannot_self_kick")); return; }
        GuildWorld gw = ctx.registry.get(target.getWorld().getName());
        if (gw == null || !gw.guild().equals(manor.guild())) { sender.sendMessage(Messages.get("error.not_guild_world_target", target.getName())); return; }
        PlayerRef targetRef = PlayerRef.of(target.getUniqueId());
        if (ManorRoles.isMemberOrAbove(manor, targetRef)) { sender.sendMessage(Messages.get("error.is_member_cannot_kick", target.getName())); return; }
        World world = Bukkit.getWorld(gw.worldName());
        if (world != null) {
            target.teleport(ctx.worlds.safeSpawn(world, gw));
            target.sendMessage(Messages.get("success.kicked_notify", player.getName(), manor.slot()));
            sender.sendMessage(Messages.get("success.kicked", target.getName()));
        }
    }
}
