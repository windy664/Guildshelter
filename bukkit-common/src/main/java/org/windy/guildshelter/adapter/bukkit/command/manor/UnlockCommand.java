package org.windy.guildshelter.adapter.bukkit.command.manor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;

@GsSubCommand(name = "unlock", permission = "guildshelter.command.unlock")
public class UnlockCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        PlayerRef ref = PlayerRef.of(player.getUniqueId());
        GuildWorld gw = ctx.registry.get(player.getWorld().getName());
        if (gw == null) { sender.sendMessage(Messages.get("error.no_manor")); return; }
        int cx = player.getLocation().getBlockX() >> 4;
        int cz = player.getLocation().getBlockZ() >> 4;
        switch (ctx.service.unlockChunk(gw.guild(), ref, cx, cz)) {
            case SUCCESS -> {
                Manor now = ctx.manorAt(player).orElse(null);
                int remain = now != null ? ctx.service.remainingQuota(gw, now) : 0;
                sender.sendMessage(Messages.get("info.unlock_success", remain));
                player.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0);
                if (ctx.mapChannel != null) ctx.mapChannel.refreshGuild(gw);
            }
            case NOT_YOUR_PLOT -> sender.sendMessage(Messages.get("info.unlock_not_your_plot"));
            case ALREADY_UNLOCKED -> sender.sendMessage(Messages.get("info.unlock_already"));
            case NO_QUOTA -> sender.sendMessage(Messages.get("info.unlock_no_quota"));
            case NOT_ADJACENT -> sender.sendMessage(Messages.get("info.unlock_not_adjacent"));
            case NO_MANOR -> sender.sendMessage(Messages.get("error.no_manor"));
        }
    }
}
