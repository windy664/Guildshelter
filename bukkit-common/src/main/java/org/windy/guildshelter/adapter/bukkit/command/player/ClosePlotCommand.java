package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.Manor;

@GsSubCommand(name = "close", permission = "guildshelter.command.close")
public class ClosePlotCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        Manor manor = ctx.currentOwnManor(player).orElse(null);
        if (manor == null) { sender.sendMessage(Messages.get("error.no_manor")); return; }
        String key = manor.guild().value() + ":" + manor.slot();
        if (ctx.openPlots.remove(key) != null) sender.sendMessage(Messages.get("success.plot_closed"));
        else sender.sendMessage(Messages.get("error.plot_not_open"));
    }
}
