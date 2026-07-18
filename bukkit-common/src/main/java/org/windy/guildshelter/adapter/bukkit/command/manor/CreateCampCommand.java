package org.windy.guildshelter.adapter.bukkit.command.manor;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.Permissions;
import org.windy.guildshelter.adapter.bukkit.command.CommandContext;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.PlayerRef;
import org.windy.guildshelter.domain.model.TerrainPrepMode;
import org.windy.guildshelter.service.GuildFullException;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@GsSubCommand(name = "createcamp", permission = "guildshelter.command.createcamp")
public class CreateCampCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        PlayerRef ref = PlayerRef.of(player.getUniqueId());
        GuildId guild = ctx.guildProvider.guildOf(ref).orElse(null);
        if (guild == null) { sender.sendMessage(Messages.get("error.createcamp_not_in_guild")); return; }
        if (!ctx.service.isGuildAdmin(ref, guild) && !Permissions.hasAdminPerm(sender, Permissions.ADMIN) && !sender.isOp()) {
            sender.sendMessage(Messages.get("error.createcamp_leader_only")); return;
        }
        if (ctx.guilds.exists(guild)) { sender.sendMessage(Messages.get("error.guild_already_exist", guild.value())); return; }
        TerrainPrepMode terrainMode = TerrainPrepMode.CLEAR_VEGETATION;
        if (args.length >= 2) {
            try { terrainMode = TerrainPrepMode.valueOf(args[1].toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException e) { sender.sendMessage(Messages.get("error.unknown_terrain", args[1])); return; }
        }
        long seed = ThreadLocalRandom.current().nextLong();
        UUID audience = player.getUniqueId();
        final TerrainPrepMode selectedTerrainMode = terrainMode;
        sender.sendMessage(Messages.get("info.createcamp_starting"));
        Bukkit.getScheduler().runTask(ctx.plugin, () -> {
            try {
                ctx.service.createGuildAsync(guild, seed, selectedTerrainMode, ctx.serverName, audience, gw -> {
                    ctx.registry.register(gw);
                    try { var manor = ctx.service.assignManor(guild, ref); sender.sendMessage(Messages.get("success.createcamp_manor_assigned", manor.slot())); }
                    catch (GuildFullException e) { sender.sendMessage(Messages.get("error.guild_full", e.capacity())); }
                    sender.sendMessage(Messages.get("success.create_world", gw.worldName(), gw.seed(), gw.originChunkX(), gw.originChunkZ()));
                    sender.sendMessage(Messages.get("success.createcamp_done"));
                    ctx.logMap(guild);
                });
            } catch (RuntimeException e) { sender.sendMessage(Messages.get("error.world_load_failed", ctx.worlds.worldName(guild))); ctx.logger.warning("[GuildShelter] 建会失败 " + guild.value() + ": " + e); }
        });
    }
}
