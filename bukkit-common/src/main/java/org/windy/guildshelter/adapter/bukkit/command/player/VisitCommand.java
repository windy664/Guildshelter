package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.port.CampSpawnStore;

@GsSubCommand(name = "visit", permission = "guildshelter.command.visit")
public class VisitCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.visit")); return; }
        GuildId guild = new GuildId(args[1]);
        GuildWorld gw = ctx.guilds.find(guild).orElse(null);
        if (gw == null) { sender.sendMessage(Messages.get("error.guild_not_exist", guild.value())); return; }
        if (ctx.proxyChannel.isAvailable() && !gw.serverName().isEmpty() && !gw.serverName().equals(ctx.serverName)) {
            ctx.proxyChannel.sendToServer(player, gw.serverName());
            sender.sendMessage(Messages.get("success.cross_server", gw.serverName())); return;
        }
        gw = ctx.worlds.ensureWorld(gw);
        ctx.guilds.save(gw);
        ctx.registry.register(gw);
        World world = Bukkit.getWorld(gw.worldName());
        if (world == null) { sender.sendMessage(Messages.get("error.world_load_failed", gw.worldName())); return; }
        Location custom = ctx.campSpawnLoc(world, gw, CampSpawnStore.Type.VISITOR);
        boolean teleported = player.teleport(custom != null ? custom : ctx.worlds.safeSpawn(world, gw));
        if (teleported && ctx.mapChannel != null) ctx.mapChannel.refreshPlayer(player, gw);
        sender.sendMessage(Messages.get("success.visit_teleported", guild.value()));
    }
}
