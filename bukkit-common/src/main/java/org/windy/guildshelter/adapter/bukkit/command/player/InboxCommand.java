package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;
import org.windy.guildshelter.domain.port.ManorRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * /gs inbox：查看自己庄园收到的留言。
 */
@GsSubCommand(name = "inbox", permission = "guildshelter.command.inbox")
public class InboxCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        PlayerRef ref = PlayerRef.of(player.getUniqueId());
        List<ManorRepository.CommentEntry> entries = ctx.manors.getInbox(ref, 20);
        if (entries.isEmpty()) { sender.sendMessage(Messages.get("error.no_comments")); return; }
        sender.sendMessage(Messages.get("info.inbox_header"));
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");
        for (ManorRepository.CommentEntry e : entries) {
            String authorName = Bukkit.getOfflinePlayer(e.author().uuid()).getName();
            String time = sdf.format(new Date(e.timestamp()));
            sender.sendMessage(Messages.get("info.inbox_entry", time, authorName != null ? authorName : "?", e.slot(), e.message()));
        }
    }
}
