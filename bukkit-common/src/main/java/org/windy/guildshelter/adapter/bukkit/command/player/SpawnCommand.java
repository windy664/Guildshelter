package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;
import org.windy.guildshelter.domain.port.CampSpawnStore;

/**
 * /gs spawn：传送到自己公会的主城。
 */
@GsSubCommand(name = "spawn", permission = "guildshelter.command.spawn")
public class SpawnCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        Manor manor = ctx.manors.findByOwnerAnywhere(PlayerRef.of(player.getUniqueId())).orElse(null);
        if (manor == null) { sender.sendMessage(Messages.get("error.no_guild_joined")); return; }
        GuildWorld gw = ctx.ensureLoadedWorld(sender, manor.guild());
        if (gw == null) return;
        World world = Bukkit.getWorld(gw.worldName());
        Location custom = ctx.campSpawnLoc(world, gw, CampSpawnStore.Type.MEMBER);
        player.teleport(custom != null ? custom : ctx.worlds.safeSpawn(world, gw));
        sender.sendMessage(Messages.get("success.spawn_teleported"));
    }
}
