package org.windy.guildshelter.adapter.bukkit.command.manor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.flag.Flag;
import org.windy.guildshelter.domain.layout.LayoutCalculator;
import org.windy.guildshelter.domain.model.ChunkRegion;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;
import org.windy.guildshelter.domain.port.ManorRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GsSubCommand(name = "sub", permission = "guildshelter.command.sub")
public class SubCommandCmd extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        Manor manor = ctx.currentOwnManor(player).orElse(null);
        if (manor == null || !manor.owner().equals(PlayerRef.of(player.getUniqueId()))) { sender.sendMessage(Messages.get("error.only_owner_sub")); return; }
        GuildId guild = manor.guild();
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.sub")); return; }
        String action = args[1].toLowerCase();
        switch (action) {
            case "create" -> {
                if (args.length < 7) { sender.sendMessage(Messages.get("usage.sub_create")); sender.sendMessage(Messages.get("usage.sub_create_hint")); return; }
                String name = args[2].toLowerCase();
                int bx = player.getLocation().getBlockX(), bz = player.getLocation().getBlockZ();
                int dx1, dz1, dx2, dz2;
                try { dx1 = Integer.parseInt(args[3]); dz1 = Integer.parseInt(args[4]); dx2 = Integer.parseInt(args[5]); dz2 = Integer.parseInt(args[6]); }
                catch (NumberFormatException e) { sender.sendMessage(Messages.get("error.offset_must_be_int")); return; }
                int minX = Math.min(bx + dx1, bx + dx2), minZ = Math.min(bz + dz1, bz + dz2);
                int maxX = Math.max(bx + dx1, bx + dx2), maxZ = Math.max(bz + dz1, bz + dz2);
                GuildWorld subGw = ctx.guilds.find(manor.guild()).orElse(null);
                if (subGw == null) { sender.sendMessage(Messages.get("error.world_not_exist")); return; }
                LayoutCalculator subLayout = new LayoutCalculator(subGw.layout());
                ChunkRegion active = subLayout.activeRegion(manor.slot(), manor.level()).shift(subGw.originChunkX(), subGw.originChunkZ());
                if (minX < active.minBlockX() || maxX > active.maxBlockX() + 15 || minZ < active.minBlockZ() || maxZ > active.maxBlockZ() + 15) { sender.sendMessage(Messages.get("error.sub_out_of_bounds")); return; }
                if (!name.matches("[a-zA-Z0-9_\\-]+")) { sender.sendMessage(Messages.get("error.invalid_name")); return; }
                ctx.manors.saveSub(guild, manor.slot(), name, minX, minZ, maxX, maxZ, Map.of());
                sender.sendMessage(Messages.get("success.sub_created", name, maxX - minX + 1, maxZ - minZ + 1));
            }
            case "delete" -> { if (args.length < 3) { sender.sendMessage(Messages.get("usage.sub_delete")); return; } ctx.manors.deleteSub(guild, manor.slot(), args[2].toLowerCase()); sender.sendMessage(Messages.get("success.sub_deleted", args[2])); }
            case "setflag" -> {
                if (args.length < 5) { sender.sendMessage(Messages.get("usage.sub_setflag")); return; }
                String name = args[2].toLowerCase();
                List<ManorRepository.SubEntry> subs = ctx.manors.getSubs(guild, manor.slot());
                ManorRepository.SubEntry target = subs.stream().filter(s -> s.name().equals(name)).findFirst().orElse(null);
                if (target == null) { sender.sendMessage(Messages.get("error.sub_not_exist", name)); return; }
                Flag f = Flag.byId(args[3]).orElse(null);
                if (f == null) { sender.sendMessage(Messages.get("error.unknown_flag", args[3])); return; }
                String value = f.normalize(args[4]).orElse(null);
                if (value == null) { sender.sendMessage(Messages.get("error.invalid_value")); return; }
                Map<String, String> flags = new HashMap<>(target.flags());
                flags.put(f.id(), value);
                ctx.manors.saveSub(guild, manor.slot(), name, target.minX(), target.minZ(), target.maxX(), target.maxZ(), flags);
                sender.sendMessage(Messages.get("success.sub_flag_set", name, f.id(), value));
            }
            case "list" -> {
                List<ManorRepository.SubEntry> subs = ctx.manors.getSubs(guild, manor.slot());
                if (subs.isEmpty()) { sender.sendMessage(Messages.get("error.no_sub_yet")); return; }
                sender.sendMessage(Messages.get("info.sub_header"));
                for (ManorRepository.SubEntry s : subs) sender.sendMessage(Messages.get("info.sub_entry", s.name(), s.maxX() - s.minX() + 1, s.maxZ() - s.minZ() + 1, s.flags().size()));
            }
            default -> sender.sendMessage(Messages.get("usage.sub"));
        }
    }
}
