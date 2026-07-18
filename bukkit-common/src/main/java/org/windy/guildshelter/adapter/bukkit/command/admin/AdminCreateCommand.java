package org.windy.guildshelter.adapter.bukkit.command.admin;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.TerrainPrepMode;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@GsSubCommand(name = "admin create", permission = "guildshelter.admin", requiresPlayer = false)
public class AdminCreateCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.admin_create")); return; }
        GuildId guild = new GuildId(args[1]);
        if (ctx.guilds.exists(guild)) { sender.sendMessage(Messages.get("error.guild_already_exist", guild.value())); return; }
        TerrainPrepMode terrainMode = null;
        if (args.length >= 3) {
            try { terrainMode = TerrainPrepMode.valueOf(args[2].toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException e) { sender.sendMessage(Messages.get("error.unknown_terrain", args[2])); return; }
        }
        long seed = ThreadLocalRandom.current().nextLong();
        TerrainPrepMode mode = terrainMode != null ? terrainMode : TerrainPrepMode.CLEAR_VEGETATION;
        UUID audience = sender instanceof Player p ? p.getUniqueId() : null;
        Bukkit.getScheduler().runTask(ctx.plugin, () -> {
            try {
                ctx.service.createGuildAsync(guild, seed, mode, ctx.serverName, audience, gw -> {
                    ctx.registry.register(gw);
                    sender.sendMessage(Messages.get("success.create_world", gw.worldName(), gw.seed(), gw.originChunkX(), gw.originChunkZ()));
                    ctx.logMap(guild);
                });
            } catch (RuntimeException e) { sender.sendMessage(Messages.get("error.world_load_failed", ctx.worlds.worldName(guild))); ctx.logger.warning("[GuildShelter] 建会失败 " + guild.value() + ": " + e); }
        });
    }
}
