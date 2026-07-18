package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.flag.Flag;
import org.windy.guildshelter.domain.model.Manor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@GsSubCommand(name = "alias", permission = "guildshelter.command.alias")
public class AliasCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        Manor manor = ctx.currentOwnManor(player).orElse(null);
        if (manor == null) { sender.sendMessage(Messages.get("error.no_manor")); return; }
        String name = args.length >= 2 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "";
        if (name.length() > 50) { sender.sendMessage(Messages.get("error.too_long", 50)); return; }
        Map<String, String> flags = new HashMap<>(manor.flags());
        if (name.isBlank()) { flags.remove(Flag.ALIAS.id()); ctx.manors.save(manor.withFlags(flags)); sender.sendMessage(Messages.get("success.alias_cleared")); }
        else { flags.put(Flag.ALIAS.id(), name.replace(';', ',')); ctx.manors.save(manor.withFlags(flags)); sender.sendMessage(Messages.get("success.alias_set", name)); }
    }
}
