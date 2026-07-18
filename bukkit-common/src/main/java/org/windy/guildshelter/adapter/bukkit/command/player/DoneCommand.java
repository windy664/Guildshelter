package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.flag.Flag;
import org.windy.guildshelter.domain.model.Manor;

import java.util.HashMap;
import java.util.Map;

/**
 * /gs done：切换庄园完工标记。
 */
@GsSubCommand(name = "done", permission = "guildshelter.command.done")
public class DoneCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        Manor manor = ctx.currentOwnManor(player).orElse(null);
        if (manor == null) { sender.sendMessage(Messages.get("error.no_manor")); return; }
        boolean current = Flag.DONE.resolveBool(manor.flags());
        Map<String, String> flags = new HashMap<>(manor.flags());
        flags.put(Flag.DONE.id(), Boolean.toString(!current));
        ctx.manors.save(manor.withFlags(flags));
        sender.sendMessage(current ? Messages.get("success.done_off") : Messages.get("success.done_on"));
    }
}
