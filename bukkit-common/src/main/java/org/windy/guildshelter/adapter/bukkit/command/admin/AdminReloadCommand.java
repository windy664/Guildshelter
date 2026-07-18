package org.windy.guildshelter.adapter.bukkit.command.admin;

import org.bukkit.command.CommandSender;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;

@GsSubCommand(name = "admin reload", permission = "guildshelter.admin", requiresPlayer = false)
public class AdminReloadCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        ctx.plugin.reloadConfig();
        sender.sendMessage(Messages.get("success.reload"));
    }
}
