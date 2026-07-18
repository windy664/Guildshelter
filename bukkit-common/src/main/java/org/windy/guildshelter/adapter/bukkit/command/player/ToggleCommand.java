package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;

@GsSubCommand(name = "toggle", permission = "guildshelter.command.toggle")
public class ToggleCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.toggle")); return; }
        String what = args[1].toLowerCase();
        if ("titles".equals(what)) {
            if (ctx.accessListener == null) { sender.sendMessage(Messages.get("error.titles_not_enabled")); return; }
            boolean now = ctx.accessListener.toggleTitles(player.getUniqueId());
            sender.sendMessage(now ? "§a已开启进出标题消息。" : "§e已关闭进出标题消息（改为聊天框显示）。");
        } else { sender.sendMessage(Messages.get("error.toggle_unknown")); }
    }
}
