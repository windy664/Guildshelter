package org.windy.guildshelter.adapter.bukkit.command.manor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.PlayerRef;

@GsSubCommand(name = "cityunlock", permission = "guildshelter.command.cityunlock")
public class CityUnlockCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        GuildWorld gw = ctx.registry.get(player.getWorld().getName());
        if (gw == null) { sender.sendMessage(Messages.get("error.cityunlock_not_in_camp")); return; }
        PlayerRef ref = PlayerRef.of(player.getUniqueId());
        if (!ctx.service.isGuildAdmin(ref, gw.guild()) && !player.isOp()) { sender.sendMessage(Messages.get("error.cityunlock_leader_only")); return; }
        int cx = player.getLocation().getBlockX() >> 4;
        int cz = player.getLocation().getBlockZ() >> 4;
        GuildWorld updated = ctx.service.unlockCityChunk(gw.guild(), cx, cz);
        switch (ctx.service.lastCityUnlockResult()) {
            case SUCCESS -> {
                ctx.registry.register(updated);
                sender.sendMessage(Messages.get("info.cityunlock_success", ctx.service.cityRemainingQuota(updated)));
                player.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0);
                if (ctx.mapChannel != null) ctx.mapChannel.refreshGuild(updated);
            }
            case NOT_YOUR_PLOT -> sender.sendMessage(Messages.get("error.claim_not_claimable"));
            case ALREADY_UNLOCKED -> sender.sendMessage(Messages.get("info.cityunlock_already"));
            case NO_QUOTA -> sender.sendMessage(Messages.get("info.cityunlock_no_quota"));
            case NOT_ADJACENT -> sender.sendMessage(Messages.get("info.cityunlock_not_adjacent"));
            default -> sender.sendMessage(Messages.get("info.cityunlock_failed"));
        }
    }
}
