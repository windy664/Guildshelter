package org.windy.guildshelter.adapter.bukkit.command.admin;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildWorld;

@GsSubCommand(name = "admin whereami", permission = "guildshelter.admin", requiresPlayer = true)
public class AdminWhereamiCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        GuildWorld gw = ctx.registry.get(player.getWorld().getName());
        if (gw == null) { sender.sendMessage(Messages.get("error.not_in_guild_world")); return; }
        int cx = player.getLocation().getBlockX() >> 4;
        int cz = player.getLocation().getBlockZ() >> 4;
        int lx = cx - gw.originChunkX();
        int lz = cz - gw.originChunkZ();
        sender.sendMessage(Messages.get("info.whereami", gw.guild().value(), gw.worldName(), cx, cz, lx, lz));
    }
}
