package org.windy.guildshelter.adapter.bukkit.command.admin;

import org.bukkit.command.CommandSender;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;

@GsSubCommand(name = "admin setowner", permission = "guildshelter.admin", requiresPlayer = false)
public class AdminSetOwnerCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        sender.sendMessage(Messages.get("success.admin_not_implemented"));
    }
}
