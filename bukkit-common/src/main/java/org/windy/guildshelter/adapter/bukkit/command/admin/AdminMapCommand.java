package org.windy.guildshelter.adapter.bukkit.command.admin;

import org.bukkit.command.CommandSender;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildId;

@GsSubCommand(name = "admin map", permission = "guildshelter.admin", requiresPlayer = false)
public class AdminMapCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.admin_map")); return; }
        ctx.logMap(new GuildId(args[1]));
        sender.sendMessage(Messages.get("success.admin_map"));
    }
}
