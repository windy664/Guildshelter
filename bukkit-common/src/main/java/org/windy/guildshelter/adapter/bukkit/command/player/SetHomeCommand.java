package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.flag.Flag;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;

import java.util.HashMap;
import java.util.Map;

/**
 * /gs sethome：把当前位置设为 home 传送点。
 */
@GsSubCommand(name = "sethome", permission = "guildshelter.command.sethome")
public class SetHomeCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        Manor manor = ctx.currentOwnManor(player).orElse(null);
        if (manor == null) { sender.sendMessage(Messages.get("error.no_manor")); return; }
        GuildWorld gw = ctx.registry.get(player.getWorld().getName());
        if (gw == null || !gw.guild().equals(manor.guild())) { sender.sendMessage(Messages.get("error.not_in_own_world")); return; }
        Location loc = player.getLocation();
        Map<String, String> flags = new HashMap<>(manor.flags());
        flags.put(Flag.HOME_X.id(), Integer.toString(loc.getBlockX()));
        flags.put(Flag.HOME_Y.id(), Integer.toString(loc.getBlockY()));
        flags.put(Flag.HOME_Z.id(), Integer.toString(loc.getBlockZ()));
        ctx.manors.save(manor.withFlags(flags));
        sender.sendMessage(Messages.get("success.home_set", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
    }
}
