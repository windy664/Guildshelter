package org.windy.guildshelter.adapter.bukkit.command.admin;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildId;

@GsSubCommand(name = "admin roadpermit", permission = "guildshelter.admin", requiresPlayer = false)
public class AdminRoadPermitCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 4) { sender.sendMessage(Messages.get("usage.admin_roadpermit")); return; }
        GuildId guild = new GuildId(args[2]);
        if (ctx.guilds.find(guild).isEmpty()) { sender.sendMessage(Messages.get("error.guild_not_exist", guild.value())); return; }
        var cache = org.windy.guildshelter.GuildShelterPlugin.roadPermitCache();
        if (cache == null) { sender.sendMessage(Messages.get("error.roadpermit_not_ready")); return; }
        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        java.util.UUID targetId = target.getUniqueId();
        String dur = args[3].toLowerCase();
        if (dur.equals("0") || dur.equals("revoke") || dur.equals("remove")) {
            cache.revoke(guild, targetId);
            sender.sendMessage(Messages.get("success.admin_roadpermit_revoke", args[1], guild.value())); return;
        }
        long millis = parseDurationMillis(dur);
        if (millis <= 0) { sender.sendMessage(Messages.get("error.roadpermit_invalid_duration", args[3])); return; }
        long expireAt = System.currentTimeMillis() + millis;
        cache.grant(guild, targetId, expireAt);
        sender.sendMessage(Messages.get("success.admin_roadpermit_grant", args[1], guild.value()));
    }

    private static long parseDurationMillis(String s) {
        try {
            if (s.endsWith("d")) return Long.parseLong(s.substring(0, s.length() - 1)) * 24 * 60 * 60 * 1000;
            if (s.endsWith("h")) return Long.parseLong(s.substring(0, s.length() - 1)) * 60 * 60 * 1000;
            if (s.endsWith("m")) return Long.parseLong(s.substring(0, s.length() - 1)) * 60 * 1000;
            return Long.parseLong(s) * 60 * 1000;
        } catch (NumberFormatException e) { return 0; }
    }
}
