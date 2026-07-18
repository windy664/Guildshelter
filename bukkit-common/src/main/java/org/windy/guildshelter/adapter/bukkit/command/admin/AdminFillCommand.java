package org.windy.guildshelter.adapter.bukkit.command.admin;

import org.bukkit.command.CommandSender;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.PlayerRef;
import org.windy.guildshelter.service.GuildFullException;

@GsSubCommand(name = "admin fill", permission = "guildshelter.admin", requiresPlayer = false)
public class AdminFillCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage(Messages.get("usage.admin_fill")); return; }
        GuildId guild = new GuildId(args[1]);
        GuildWorld gw = ctx.guilds.find(guild).orElse(null);
        if (gw == null) { sender.sendMessage(Messages.get("error.guild_not_exist", guild.value())); return; }
        int count;
        try { count = Integer.parseInt(args[2]); } catch (NumberFormatException e) { sender.sendMessage(Messages.get("error.number_must_be_int")); return; }
        int filled = 0;
        for (int i = 0; i < count; i++) {
            try { ctx.service.assignManor(guild, PlayerRef.of(java.util.UUID.randomUUID())); filled++; }
            catch (GuildFullException e) { break; }
        }
        sender.sendMessage(Messages.get("success.admin_fill", filled, guild.value()));
        ctx.logMap(guild);
    }
}
