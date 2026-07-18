package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.flag.Flag;
import org.windy.guildshelter.domain.layout.LayoutCalculator;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;

@GsSubCommand(name = "flower", permission = "guildshelter.command.flower")
public class FlowerCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        PlayerRef ref = PlayerRef.of(player.getUniqueId());
        GuildId targetGuild = null; int targetSlot = -1;
        if (args.length >= 3) {
            targetGuild = new GuildId(args[1]);
            try { targetSlot = Integer.parseInt(args[2]); } catch (NumberFormatException e) { sender.sendMessage(Messages.get("usage.flower")); return; }
        } else {
            GuildWorld gw = ctx.registry.get(player.getWorld().getName());
            if (gw == null) { sender.sendMessage(Messages.get("error.not_in_guild_world")); return; }
            LayoutCalculator layout = new LayoutCalculator(gw.layout());
            int lx = (player.getLocation().getBlockX() >> 4) - gw.originChunkX();
            int lz = (player.getLocation().getBlockZ() >> 4) - gw.originChunkZ();
            var slotOpt = layout.slotAt(lx, lz);
            if (slotOpt.isEmpty()) { sender.sendMessage(Messages.get("error.not_on_plot")); return; }
            targetGuild = gw.guild(); targetSlot = slotOpt.getAsInt();
        }
        Manor target = ctx.manors.findBySlot(targetGuild, targetSlot).orElse(null);
        if (target == null) { sender.sendMessage(Messages.get("error.slot_empty", targetSlot)); return; }
        if (target.owner().equals(ref)) { sender.sendMessage(Messages.get("error.cannot_flower_self")); return; }
        if (ctx.manors.hasSentFlowerToday(targetGuild, targetSlot, ref)) { sender.sendMessage(Messages.get("error.already_flowered_today")); return; }
        ctx.manors.sendFlower(targetGuild, targetSlot, ref);
        int todayCount = ctx.manors.getTodayFlowerCount(targetGuild, targetSlot);
        String alias = Flag.ALIAS.resolveString(target.flags());
        String name = alias.isBlank() ? targetGuild.value() + " #" + targetSlot : alias;
        sender.sendMessage(Messages.get("success.flower_sent", name, todayCount));
        Player owner = Bukkit.getPlayer(target.owner().uuid());
        if (owner != null && owner.isOnline()) owner.sendMessage(Messages.get("success.flower_received", player.getName(), name));
    }
}
