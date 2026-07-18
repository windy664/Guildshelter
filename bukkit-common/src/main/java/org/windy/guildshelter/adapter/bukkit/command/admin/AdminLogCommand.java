package org.windy.guildshelter.adapter.bukkit.command.admin;

import org.bukkit.command.CommandSender;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildId;

@GsSubCommand(name = "admin log", permission = "guildshelter.admin", requiresPlayer = false)
public class AdminLogCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (ctx.auditLog == null || !ctx.auditLog.isEnabled()) { sender.sendMessage(Messages.get("error.feature_unavailable")); return; }
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.admin_log")); return; }
        GuildId guild = new GuildId(args[1]);
        if (ctx.guilds.find(guild).isEmpty()) { sender.sendMessage(Messages.get("error.guild_not_exist", guild.value())); return; }
        int page = args.length >= 3 ? parsePage(args, 2) : 1;
        renderLog(sender, guild, page);
    }

    private void renderLog(CommandSender sender, GuildId guild, int page) {
        int pageSize = 8;
        int offset = (page - 1) * pageSize;
        var rows = ctx.auditLog.recent(guild, pageSize + 1, offset);
        boolean hasNext = rows.size() > pageSize;
        if (rows.isEmpty()) { sender.sendMessage(Messages.get("info.log_empty", guild.value())); return; }
        sender.sendMessage(Messages.get("info.log_header", guild.value(), page));
        int shown = Math.min(rows.size(), pageSize);
        for (int i = 0; i < shown; i++) {
            var e = rows.get(i);
            String time = java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.ofEpochMilli(e.ts()));
            String actionKey = "audit.action." + e.action();
            String action = Messages.get(actionKey);
            if (action.equals(actionKey)) action = e.action();
            String actor = e.actorUuid() == null ? Messages.get("audit.actor_system") : e.actorUuid().substring(0, 8);
            String target = e.target() == null ? "" : " §7" + e.target();
            String detail = e.detail() == null ? "" : " §8" + e.detail();
            sender.sendMessage(Messages.get("info.log_entry", time, action, actor) + target + detail);
        }
        if (hasNext) sender.sendMessage(Messages.get("info.log_more", page + 1));
    }

    private static int parsePage(String[] args, int idx) {
        if (args.length > idx) { try { return Math.max(1, Integer.parseInt(args[idx])); } catch (NumberFormatException ignored) {} }
        return 1;
    }
}
