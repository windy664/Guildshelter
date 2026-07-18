package org.windy.guildshelter.adapter.bukkit.command.admin;

import org.bukkit.command.CommandSender;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildWorld;

@GsSubCommand(name = "admin worlds", permission = "guildshelter.admin", requiresPlayer = false)
public class AdminWorldsCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        var all = ctx.guilds.findAll();
        if (all.isEmpty()) { sender.sendMessage(Messages.get("info.no_guilds")); return; }
        sender.sendMessage(Messages.get("info.worlds_header", all.size()));
        for (GuildWorld gw : all) {
            sender.sendMessage(Messages.get("info.worlds_entry", gw.guild().value(), gw.worldName(), gw.guildLevel()));
        }
    }
}
