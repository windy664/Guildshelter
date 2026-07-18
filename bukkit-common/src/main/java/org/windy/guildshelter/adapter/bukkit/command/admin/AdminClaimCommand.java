package org.windy.guildshelter.adapter.bukkit.command.admin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.layout.LayoutCalculator;
import org.windy.guildshelter.domain.model.ChunkRegion;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;
import org.windy.guildshelter.service.GuildFullException;

@GsSubCommand(name = "admin claim", permission = "guildshelter.admin", requiresPlayer = true)
public class AdminClaimCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.only_player_claim")); return; }
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.admin_claim")); return; }
        GuildId guild = new GuildId(args[1]);
        GuildWorld gw = ctx.guilds.find(guild).orElse(null);
        if (gw == null) { sender.sendMessage(Messages.get("error.guild_not_exist", guild.value())); return; }
        gw = ctx.worlds.ensureWorld(gw);
        ctx.registry.register(gw);
        Manor manor;
        try { manor = ctx.service.assignManor(guild, PlayerRef.of(player.getUniqueId())); }
        catch (GuildFullException e) { sender.sendMessage(Messages.get("error.guild_full_with_level", e.capacity(), e.guildLevel())); return; }
        gw = ctx.guilds.find(guild).orElse(gw);
        ctx.registry.register(gw);
        World world = Bukkit.getWorld(gw.worldName());
        if (world == null) { sender.sendMessage(Messages.get("error.world_load_failed", gw.worldName())); return; }
        ChunkRegion active = new LayoutCalculator(gw.layout()).activeRegion(manor.slot(), manor.level()).shift(gw.originChunkX(), gw.originChunkZ());
        int cx = (active.minBlockX() + active.maxBlockX()) / 2;
        int cz = (active.minBlockZ() + active.maxBlockZ()) / 2;
        world.loadChunk(cx >> 4, cz >> 4, true);
        int cy = world.getHighestBlockYAt(cx, cz) + 1;
        player.teleport(new Location(world, cx + 0.5, cy, cz + 0.5));
        sender.sendMessage(Messages.get("success.claim", manor.slot(), manor.level()));
        sender.sendMessage(Messages.get("success.claim_hint", guild.value()));
        ctx.logMap(guild);
    }
}
