package org.windy.guildshelter.adapter.bukkit.command.manor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.Permissions;
import org.windy.guildshelter.adapter.bukkit.command.CommandContext;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.flag.Flag;
import org.windy.guildshelter.domain.flag.FlagType;
import org.windy.guildshelter.domain.layout.LayoutCalculator;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@GsSubCommand(name = "flag", permission = "guildshelter.command.flag")
public class FlagCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        GuildWorld gwHere = ctx.registry.get(player.getWorld().getName());
        if (gwHere != null && ctx.cityFlagCache != null && standingInMainCity(player, gwHere)) {
            cityFlag(player, gwHere, args); return;
        }
        PlayerRef ref = PlayerRef.of(player.getUniqueId());
        Manor manor = ctx.currentOwnManor(player).orElse(null);
        if (manor == null) { sender.sendMessage(Messages.get("error.no_manor_for_command")); return; }
        boolean isOwner = manor.owner().equals(ref);
        String action = args.length >= 2 ? args[1].toLowerCase() : "";
        switch (action) {
            case "set" -> {
                if (args.length < 4) { sender.sendMessage(Messages.get("usage.flag_set")); return; }
                Flag f = Flag.byId(args[2]).orElse(null);
                if (f == null) { sender.sendMessage(Messages.get("error.unknown_flag", args[2])); return; }
                boolean isTrusted = manor.coBuilders().contains(ref);
                if (!isOwner && !(isTrusted && CommandContext.isTrustedFlag(f))
                        && !player.hasPermission(Permissions.flagSet(f.id()))
                        && !Permissions.hasAdminPerm(player, Permissions.ADMIN_FLAG_OTHER)) {
                    sender.sendMessage(Messages.get("error.flag_set_perm", Permissions.flagSet(f.id()))); return;
                }
                String raw = f.type() == FlagType.STRING ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : args[3];
                String value = f.normalize(raw).orElse(null);
                if (value == null) { sender.sendMessage(Messages.get("error.invalid_value")); return; }
                Map<String, String> flags = new HashMap<>(manor.flags());
                flags.put(f.id(), value);
                ctx.manors.save(manor.withFlags(flags));
                sender.sendMessage(Messages.get("success.flag_set", f.id(), value, manor.slot()));
            }
            case "unset" -> {
                if (args.length < 3) { sender.sendMessage(Messages.get("usage.flag_unset")); return; }
                Flag f = Flag.byId(args[2]).orElse(null);
                if (f == null) { sender.sendMessage(Messages.get("error.unknown_flag", args[2])); return; }
                boolean isTrustedUnset = manor.coBuilders().contains(ref);
                if (!isOwner && !(isTrustedUnset && CommandContext.isTrustedFlag(f))
                        && !player.hasPermission(Permissions.flagSet(f.id()))
                        && !Permissions.hasAdminPerm(player, Permissions.ADMIN_FLAG_OTHER)) {
                    sender.sendMessage(Messages.get("error.flag_set_perm", Permissions.flagSet(f.id()))); return;
                }
                Map<String, String> flags = new HashMap<>(manor.flags());
                if (flags.remove(f.id()) == null) { sender.sendMessage(Messages.get("error.flag_already_default", f.defaultValue())); return; }
                ctx.manors.save(manor.withFlags(flags));
                sender.sendMessage(Messages.get("success.flag_unset", f.id(), f.defaultValue()));
            }
            default -> {
                sender.sendMessage(Messages.get("info.flag_header", manor.slot()));
                sender.sendMessage(Messages.get("info.flag_usage"));
                for (Flag f : Flag.values()) {
                    String cur = manor.flags().get(f.id());
                    String shown = cur != null ? "§f" + cur : "§8" + f.defaultValue() + "(默认)";
                    sender.sendMessage(Messages.get("info.flag_entry", f.id(), shown, Messages.get(f.description())));
                }
            }
        }
    }

    private boolean standingInMainCity(Player player, GuildWorld gw) {
        LayoutCalculator layout = new LayoutCalculator(gw.layout());
        int lx = (player.getLocation().getBlockX() >> 4) - gw.originChunkX();
        int lz = (player.getLocation().getBlockZ() >> 4) - gw.originChunkZ();
        return layout.classify(lx, lz).isMainCity();
    }

    private void cityFlag(Player player, GuildWorld gw, String[] args) {
        PlayerRef ref = PlayerRef.of(player.getUniqueId());
        boolean leader = ctx.service.isGuildAdmin(ref, gw.guild()) || player.isOp();
        String action = args.length >= 2 ? args[1].toLowerCase() : "";
        switch (action) {
            case "set" -> {
                if (!leader) { player.sendMessage(Messages.get("error.cityflag_leader_only")); return; }
                if (args.length < 4) { player.sendMessage(Messages.get("usage.cityflag_set")); return; }
                Flag f = Flag.byId(args[2]).orElse(null);
                if (f == null) { player.sendMessage(Messages.get("error.unknown_flag", args[2])); return; }
                if (!CommandContext.isCityFlag(f)) { player.sendMessage(Messages.get("error.cityflag_not_allowed", f.id())); return; }
                String raw = f.type() == FlagType.STRING ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : args[3];
                String value = f.normalize(raw).orElse(null);
                if (value == null) { player.sendMessage(Messages.get("error.invalid_value")); return; }
                ctx.cityFlagCache.put(gw.guild(), f.id(), value);
                player.sendMessage(Messages.get("success.cityflag_set", f.id(), value));
            }
            case "unset" -> {
                if (!leader) { player.sendMessage(Messages.get("error.cityflag_leader_only")); return; }
                if (args.length < 3) { player.sendMessage(Messages.get("usage.cityflag_unset")); return; }
                Flag f = Flag.byId(args[2]).orElse(null);
                if (f == null) { player.sendMessage(Messages.get("error.unknown_flag", args[2])); return; }
                ctx.cityFlagCache.remove(gw.guild(), f.id());
                player.sendMessage(Messages.get("success.cityflag_unset", f.id(), f.defaultValue()));
            }
            default -> {
                Map<String, String> cf = ctx.cityFlagCache.flags(gw.guild());
                player.sendMessage(Messages.get("info.cityflag_header", gw.guild().value()));
                player.sendMessage(Messages.get("info.cityflag_usage"));
                for (Flag f : Flag.values()) {
                    if (!CommandContext.isCityFlag(f)) continue;
                    String cur = cf.get(f.id());
                    String shown = cur != null ? "§f" + cur : "§8" + f.defaultValue() + "(默认)";
                    player.sendMessage(Messages.get("info.flag_entry", f.id(), shown, Messages.get(f.description())));
                }
            }
        }
    }
}
