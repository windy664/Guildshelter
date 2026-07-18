package org.windy.guildshelter.adapter.bukkit.command.admin;

import org.bukkit.command.CommandSender;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.GuildWorld;

@GsSubCommand(name = "admin roads", permission = "guildshelter.admin", requiresPlayer = false)
public class AdminRoadsCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.admin_roads")); return; }
        GuildId guild = new GuildId(args[1]);
        GuildWorld gw = ctx.guilds.find(guild).orElse(null);
        if (gw == null) { sender.sendMessage(Messages.get("error.guild_not_exist", guild.value())); return; }
        ctx.service.prepareRoadsWithinBorder(gw);
        sender.sendMessage(Messages.get("success.admin_roads", guild.value()));
    }
}
