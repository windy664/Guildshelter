package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.PlayerRef;

/**
 * /gs log [页]：会长查看本会领地操作审计日志。
 */
@GsSubCommand(name = "log", permission = "guildshelter.command.log")
public class LogCommand extends SubCommand {

    private static final int LOG_PAGE_SIZE = 8;
    private static final java.time.format.DateTimeFormatter LOG_TIME_FMT =
            java.time.format.DateTimeFormatter.ofPattern("MM-dd HH:mm").withZone(java.time.ZoneId.systemDefault());

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.get("error.player_only"));
            return;
        }
        if (ctx.auditLog == null || !ctx.auditLog.isEnabled()) {
            sender.sendMessage(Messages.get("error.feature_unavailable"));
            return;
        }
        GuildWorld gw = ctx.registry.get(player.getWorld().getName());
        if (gw == null) {
            sender.sendMessage(Messages.get("error.log_not_in_camp"));
            return;
        }
        if (!ctx.service.isGuildAdmin(PlayerRef.of(player.getUniqueId()), gw.guild()) && !player.isOp()) {
            sender.sendMessage(Messages.get("error.log_leader_only"));
            return;
        }
        int page = parsePage(args, 1);
        renderLog(sender, gw.guild(), page);
    }

    private void renderLog(CommandSender sender, org.windy.guildshelter.domain.model.GuildId guild, int page) {
        int offset = (page - 1) * LOG_PAGE_SIZE;
        var rows = ctx.auditLog.recent(guild, LOG_PAGE_SIZE + 1, offset);
        boolean hasNext = rows.size() > LOG_PAGE_SIZE;
        if (rows.isEmpty()) {
            sender.sendMessage(Messages.get("info.log_empty", guild.value()));
            return;
        }
        sender.sendMessage(Messages.get("info.log_header", guild.value(), page));
        int shown = Math.min(rows.size(), LOG_PAGE_SIZE);
        for (int i = 0; i < shown; i++) {
            var e = rows.get(i);
            String time = LOG_TIME_FMT.format(java.time.Instant.ofEpochMilli(e.ts()));
            String actionKey = "audit.action." + e.action();
            String action = Messages.get(actionKey);
            if (action.equals(actionKey)) action = e.action();
            String actor = e.actorUuid() == null ? Messages.get("audit.actor_system") : nameOf(e.actorUuid());
            String target = e.target() == null ? "" : " §7" + e.target();
            String detail = e.detail() == null ? "" : " §8" + e.detail();
            sender.sendMessage(Messages.get("info.log_entry", time, action, actor) + target + detail);
        }
        if (hasNext) sender.sendMessage(Messages.get("info.log_more", page + 1));
    }

    static int parsePage(String[] args, int idx) {
        if (args.length > idx) {
            try { return Math.max(1, Integer.parseInt(args[idx])); } catch (NumberFormatException ignored) {}
        }
        return 1;
    }

    static String nameOf(String uuid) {
        try {
            String n = Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuid)).getName();
            return n != null ? n : uuid.substring(0, 8);
        } catch (IllegalArgumentException e) {
            return uuid;
        }
    }
}
