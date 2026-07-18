package org.windy.guildshelter.adapter.bukkit.command.admin;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.GuildWorld;

@GsSubCommand(name = "admin tp", permission = "guildshelter.admin", requiresPlayer = true)
public class AdminTpCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.only_player_tp")); return; }
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.admin_tp")); return; }
        GuildWorld gw = ctx.guilds.find(new GuildId(args[1])).orElse(null);
        if (gw == null) { sender.sendMessage(Messages.get("error.guild_not_exist", args[1])); return; }
        gw = ctx.worlds.ensureWorld(gw);
        ctx.guilds.save(gw);
        ctx.registry.register(gw);
        World world = Bukkit.getWorld(gw.worldName());
        if (world == null) { sender.sendMessage(Messages.get("error.world_load_failed", gw.worldName())); return; }
        player.teleport(ctx.worlds.safeSpawn(world, gw));
        sender.sendMessage(Messages.get("success.tp_teleported", gw.worldName()));
    }
}
