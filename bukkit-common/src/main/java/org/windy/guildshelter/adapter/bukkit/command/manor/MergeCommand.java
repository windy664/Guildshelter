package org.windy.guildshelter.adapter.bukkit.command.manor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.Permissions;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.layout.LayoutCalculator;
import org.windy.guildshelter.domain.model.ChunkRegion;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;

@GsSubCommand(name = "merge", permission = "guildshelter.command.merge", requiresConfirm = true)
public class MergeCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.merge")); return; }
        PlayerRef ref = PlayerRef.of(player.getUniqueId());
        Manor myManor = ctx.currentOwnManor(player).orElse(null);
        if (myManor == null) { sender.sendMessage(Messages.get("error.no_manor")); return; }
        int absorbedSlot;
        try { absorbedSlot = Integer.parseInt(args[1]); } catch (NumberFormatException e) { sender.sendMessage(Messages.get("error.number_must_be_int")); return; }
        if (absorbedSlot == myManor.slot()) { sender.sendMessage(Messages.get("error.cannot_self_merge")); return; }
        int existingTarget = ctx.merges.getMergedTarget(myManor.guild(), absorbedSlot);
        if (existingTarget != absorbedSlot && existingTarget != myManor.slot()) { sender.sendMessage(Messages.get("error.already_merged_to_other", absorbedSlot, existingTarget)); return; }
        if (existingTarget == myManor.slot()) { sender.sendMessage(Messages.get("error.already_merged", absorbedSlot)); return; }
        Manor absorbed = ctx.manors.findBySlot(myManor.guild(), absorbedSlot).orElse(null);
        if (absorbed == null) { sender.sendMessage(Messages.get("error.slot_empty", absorbedSlot)); return; }
        if (!absorbed.owner().equals(ref) && !Permissions.hasAdminPerm(player, Permissions.ADMIN_TRUST_OTHER)) { sender.sendMessage(Messages.get("error.only_owner")); return; }
        GuildWorld gw = ctx.guilds.find(myManor.guild()).orElse(null);
        if (gw == null) { sender.sendMessage(Messages.get("error.world_not_exist")); return; }
        LayoutCalculator layout = new LayoutCalculator(gw.layout());
        ChunkRegion myRegion = layout.activeRegion(myManor.slot(), myManor.level());
        ChunkRegion theirRegion = layout.activeRegion(absorbedSlot, absorbed.level());
        int dx = Math.min(Math.abs(myRegion.maxChunkX() - theirRegion.minChunkX()), Math.abs(theirRegion.maxChunkX() - myRegion.minChunkX()));
        int dz = Math.min(Math.abs(myRegion.maxChunkZ() - theirRegion.minChunkZ()), Math.abs(theirRegion.maxChunkZ() - myRegion.minChunkZ()));
        boolean adjacent = (dx <= 1 && dz == 0) || (dz <= 1 && dx == 0);
        if (!adjacent) { sender.sendMessage(Messages.get("error.not_adjacent")); return; }
        ctx.merges.merge(myManor.guild(), myManor.slot(), absorbedSlot);
        sender.sendMessage(Messages.get("success.merged", absorbedSlot, myManor.slot()));
    }
}
