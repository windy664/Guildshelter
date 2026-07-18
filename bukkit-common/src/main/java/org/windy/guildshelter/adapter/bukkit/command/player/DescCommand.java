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

@GsSubCommand(name = "desc", permission = "guildshelter.command.desc")
public class DescCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        Manor manor = ctx.currentOwnManor(player).orElse(null);
        if (manor == null) { sender.sendMessage(Messages.get("error.no_manor")); return; }
        String text = args.length >= 2 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "";
        Map<String, String> flags = new HashMap<>(manor.flags());
        if (text.isBlank()) { flags.remove(Flag.DESCRIPTION.id()); ctx.manors.save(manor.withFlags(flags)); sender.sendMessage(Messages.get("success.desc_cleared")); }
        else { flags.put(Flag.DESCRIPTION.id(), text.replace(';', ',')); ctx.manors.save(manor.withFlags(flags)); sender.sendMessage(Messages.get("success.desc_set", text)); }
    }
}
