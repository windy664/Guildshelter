package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.flag.Flag;
import org.windy.guildshelter.domain.layout.LayoutCalculator;
import org.windy.guildshelter.domain.model.ChunkRegion;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GsSubCommand(name = "home", permission = "guildshelter.command.home")
public class HomeCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        PlayerRef ref = PlayerRef.of(player.getUniqueId());
        Manor any = ctx.manors.findByOwnerAnywhere(ref).orElse(null);
        if (any == null) { sender.sendMessage(Messages.get("error.no_manor_hint")); return; }
        List<Manor> mine = ctx.manors.findAllByOwner(any.guild(), ref);
        Manor manor;
        if (args.length >= 2 && args[1].equalsIgnoreCase("set")) {
            Manor here = ctx.currentOwnManor(player).orElse(null);
            if (here == null) { sender.sendMessage(Messages.get("error.home_set_not_on_plot")); return; }
            for (Manor m : mine) {
                Map<String, String> nf = new HashMap<>(m.flags());
                if (m.slot() == here.slot()) nf.put(Manor.HOME_DEFAULT_FLAG, "true");
                else nf.remove(Manor.HOME_DEFAULT_FLAG);
                if (!nf.equals(m.flags())) ctx.manors.save(m.withFlags(nf));
            }
            sender.sendMessage(Messages.get("success.home_default_set", here.slot()));
            return;
        }
        if (args.length >= 2) {
            int slot;
            try { slot = Integer.parseInt(args[1]); } catch (NumberFormatException e) {
                sender.sendMessage(Messages.get("error.home_invalid_slot")); return;
            }
            manor = mine.stream().filter(m -> m.slot() == slot).findFirst().orElse(null);
            if (manor == null) { sender.sendMessage(Messages.get("error.home_slot_not_found", slot)); return; }
        } else {
            manor = mine.stream()
                    .filter(m -> Boolean.parseBoolean(m.flags().getOrDefault(Manor.HOME_DEFAULT_FLAG, "false")))
                    .findFirst()
                    .orElseGet(() -> mine.stream().min(Comparator.comparingInt(Manor::slot)).orElse(any));
            if (mine.size() > 1) {
                sender.sendMessage(Messages.get("success.home_multi_hint", mine.size(), manor.slot()));
            }
        }
        GuildWorld gw = ctx.ensureLoadedWorld(sender, manor.guild());
        if (gw == null) return;
        World world = Bukkit.getWorld(gw.worldName());
        int hx = Flag.HOME_X.resolveInt(manor.flags());
        int hz = Flag.HOME_Z.resolveInt(manor.flags());
        Location dest;
        if (hx != 0 || hz != 0) {
            int hy = Flag.HOME_Y.resolveInt(manor.flags());
            if (hy != 0) { world.loadChunk(hx >> 4, hz >> 4, true); dest = new Location(world, hx + 0.5, hy, hz + 0.5); }
            else dest = ctx.worlds.safeLanding(world, hx, hz);
        } else {
            ChunkRegion active = new LayoutCalculator(gw.layout())
                    .activeRegion(manor.slot(), manor.level()).shift(gw.originChunkX(), gw.originChunkZ());
            dest = ctx.worlds.safeLanding(world, active.minBlockX() + 8, active.minBlockZ() + 8);
        }
        player.teleport(dest);
        sender.sendMessage(Messages.get("success.home_teleported", manor.slot()));
    }
}
