package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.layout.LayoutCalculator;
import org.windy.guildshelter.domain.model.ChunkRegion;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;

import java.util.Comparator;
import java.util.List;

/**
 * /gs manors：列出自己在本公会的全部庄园。
 */
@GsSubCommand(name = "manors", permission = "guildshelter.command.manors")
public class ManorsCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        PlayerRef ref = PlayerRef.of(player.getUniqueId());
        Manor any = ctx.manors.findByOwnerAnywhere(ref).orElse(null);
        if (any == null) { sender.sendMessage(Messages.get("error.no_manor")); return; }
        var guild = any.guild();
        List<Manor> mine = ctx.manors.findAllByOwner(guild, ref).stream()
                .sorted(Comparator.comparingInt(Manor::slot)).toList();
        int limit = ctx.multiManor.limitFor(player);
        String cap = limit == Integer.MAX_VALUE ? "∞" : String.valueOf(limit);
        sender.sendMessage(Messages.get("info.manors_header", mine.size(), cap));
        GuildWorld gw = ctx.guilds.find(guild).orElse(null);
        LayoutCalculator layout = gw != null ? new LayoutCalculator(gw.layout()) : null;
        for (Manor m : mine) {
            String coords = "";
            if (gw != null) {
                ChunkRegion active = layout.activeRegion(m.slot(), m.level())
                        .shift(gw.originChunkX(), gw.originChunkZ());
                coords = " §7@ §f" + (active.minBlockX() + 8) + ", " + (active.minBlockZ() + 8);
            }
            boolean isDefault = Boolean.parseBoolean(m.flags().getOrDefault(Manor.HOME_DEFAULT_FLAG, "false"));
            sender.sendMessage(Messages.get("info.manors_entry", m.slot(), m.level(), coords,
                    isDefault ? " §6★默认" : "", m.slot()));
        }
        if (mine.size() < limit) {
            sender.sendMessage(Messages.get("info.manors_claim_hint"));
        }
    }
}
