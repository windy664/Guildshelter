package org.windy.guildshelter.adapter.bukkit.command.manor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;

@GsSubCommand(name = "move", permission = "guildshelter.command.move", requiresConfirm = true)
public class MoveCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        if (!ctx.service.isMoveEnabled()) { sender.sendMessage(Messages.get("error.move_not_enabled")); return; }
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.move")); return; }
        PlayerRef ref = PlayerRef.of(player.getUniqueId());
        Manor currentManor = ctx.manors.findByOwnerAnywhere(ref).orElse(null);
        if (currentManor == null) { sender.sendMessage(Messages.get("error.move_no_manor")); return; }
        GuildId targetGuild = new GuildId(args[1]);
        if (currentManor.guild().equals(targetGuild)) { sender.sendMessage(Messages.get("error.move_same_guild")); return; }
        long lastMove = ctx.manors.getLastMoveTime(ref.uuid());
        if (lastMove > 0 && ctx.service.moveCooldownDays() > 0) {
            long cooldownMs = (long) ctx.service.moveCooldownDays() * 24 * 60 * 60 * 1000;
            long remaining = cooldownMs - (System.currentTimeMillis() - lastMove);
            if (remaining > 0) { long days = remaining / (24 * 60 * 60 * 1000) + 1; sender.sendMessage(Messages.get("error.move_cooldown", days)); return; }
        }
        GuildWorld targetGw = ctx.guilds.find(targetGuild).orElse(null);
        if (targetGw == null) { sender.sendMessage(Messages.get("error.move_target_not_exist")); return; }
        int capacity = ctx.levels.maxMembers(targetGw.guildLevel());
        int targetSlot = ctx.manors.nextFreeSlot(targetGuild);
        if (targetSlot >= capacity) { sender.sendMessage(Messages.get("error.move_target_full")); return; }
        double cost = ctx.service.moveCost();
        String costStr = cost > 0 ? String.format("%.0f", cost) : "免费";
        sender.sendMessage(Messages.get("info.move_header"));
        sender.sendMessage(Messages.get("info.move_current", currentManor.guild().value(), currentManor.slot()));
        sender.sendMessage(Messages.get("info.move_target", targetGuild.value()));
        sender.sendMessage(Messages.get("info.move_cost", costStr));
        sender.sendMessage(Messages.get("info.move_cooldown", ctx.service.moveCooldownDays()));
        sender.sendMessage(Messages.get("info.move_warning"));
        sender.sendMessage(Messages.get("info.move_confirm_hint"));
    }
}
