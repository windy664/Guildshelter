package org.windy.guildshelter.adapter.bukkit.command.manor;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;

@GsSubCommand(name = "citytrust", permission = "guildshelter.command.citytrust", aliases = {"cityuntrust"})
public class CityTrustCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        boolean add = args[0].equalsIgnoreCase("citytrust");
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.citytrust")); return; }
        PlayerRef self = PlayerRef.of(player.getUniqueId());
        Manor myManor = ctx.manors.findByOwnerAnywhere(self).orElse(null);
        if (myManor == null) { sender.sendMessage(Messages.get("error.no_manor")); return; }
        GuildId guild = myManor.guild();
        if (!ctx.service.isGuildAdmin(self, guild) && !player.isOp()) { sender.sendMessage(Messages.get("error.citytrust_leader_only")); return; }
        var cache = org.windy.guildshelter.GuildShelterPlugin.cityTrustCache();
        if (cache == null) { sender.sendMessage(Messages.get("error.citytrust_not_ready")); return; }
        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        java.util.UUID targetId = target.getUniqueId();
        if (add) {
            if (ctx.manors.findByOwner(guild, PlayerRef.of(targetId)).isEmpty()) { sender.sendMessage(Messages.get("error.citytrust_not_member")); return; }
            cache.add(guild, targetId);
            sender.sendMessage(Messages.get("success.citytrust_added", args[1]));
        } else {
            cache.remove(guild, targetId);
            sender.sendMessage(Messages.get("success.citytrust_removed", args[1]));
        }
    }
}
