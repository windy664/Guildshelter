package org.windy.guildshelter.adapter.bukkit.command.admin;

import org.bukkit.command.CommandSender;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildId;

@GsSubCommand(name = "admin purge", permission = "guildshelter.admin", requiresPlayer = false)
public class AdminPurgeCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.admin_purge")); return; }
        GuildId guild = new GuildId(args[1]);
        if (!ctx.guilds.exists(guild)) { sender.sendMessage(Messages.get("error.guild_not_exist", guild.value())); return; }
        sender.sendMessage(Messages.get("success.admin_not_implemented"));
    }
}
