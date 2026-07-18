package org.windy.guildshelter.adapter.bukkit.command.manor;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.CampSpawn;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.PlayerRef;
import org.windy.guildshelter.domain.port.CampSpawnStore;

@GsSubCommand(name = "setspawn", permission = "guildshelter.command.setspawn")
public class SetSpawnCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        if (ctx.campSpawn == null) { sender.sendMessage(Messages.get("error.feature_unavailable")); return; }
        GuildWorld gw = ctx.registry.get(player.getWorld().getName());
        if (gw == null) { sender.sendMessage(Messages.get("error.setspawn_not_in_camp")); return; }
        PlayerRef ref = PlayerRef.of(player.getUniqueId());
        if (!ctx.service.isGuildAdmin(ref, gw.guild()) && !player.isOp()) { sender.sendMessage(Messages.get("error.setspawn_leader_only")); return; }
        String which = args.length >= 2 ? args[1].toLowerCase() : "";
        CampSpawnStore.Type type;
        if (which.equals("member") || which.equals("成员")) type = CampSpawnStore.Type.MEMBER;
        else if (which.equals("visitor") || which.equals("访客")) type = CampSpawnStore.Type.VISITOR;
        else { sender.sendMessage(Messages.get("usage.setspawn")); return; }
        Location loc = player.getLocation();
        ctx.campSpawn.set(gw.guild(), type, new CampSpawn(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()));
        sender.sendMessage(Messages.get(type == CampSpawnStore.Type.MEMBER ? "success.setspawn_member" : "success.setspawn_visitor"));
    }
}
