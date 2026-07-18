package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.adapter.bukkit.gui.Menus;
import org.windy.guildshelter.domain.flag.Flag;
import org.windy.guildshelter.domain.layout.Classification;
import org.windy.guildshelter.domain.layout.LayoutCalculator;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.port.ManorRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * /gs board：查看脚下庄园的留言墙（GUI）。
 * 玩家执行打开 GUI，控制台走聊天输出。
 */
@GsSubCommand(name = "board", permission = "guildshelter.command.board")
public class BoardCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.get("error.player_only"));
            return;
        }
        GuildWorld gw = ctx.registry.get(player.getWorld().getName());
        if (gw == null) {
            sender.sendMessage(Messages.get("error.not_in_guild_world"));
            return;
        }
        LayoutCalculator layout = new LayoutCalculator(gw.layout());
        int lx = (player.getLocation().getBlockX() >> 4) - gw.originChunkX();
        int lz = (player.getLocation().getBlockZ() >> 4) - gw.originChunkZ();
        Classification c = ctx.classify(gw, lx, lz);
        if (!c.isPlot()) {
            sender.sendMessage(Messages.get("error.not_on_plot"));
            return;
        }

        Manor m = ctx.manors.findBySlot(gw.guild(), c.slot()).orElse(null);
        String alias = m != null ? Flag.ALIAS.resolveString(m.flags()) : "";
        List<ManorRepository.CommentEntry> comments = ctx.manors.getComments(gw.guild(), c.slot(), 10);

        // 构建占位符
        Map<String, Object> values = new HashMap<>();
        values.put("slot", c.slot());
        values.put("alias", alias.isBlank() ? "#" + c.slot() : alias);
        values.put("comment_count", comments.size());

        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");
        for (int i = 0; i < 10; i++) {
            if (i < comments.size()) {
                var entry = comments.get(i);
                String authorName = Bukkit.getOfflinePlayer(entry.author().uuid()).getName();
                if (authorName == null) authorName = "???";
                String time = sdf.format(new Date(entry.timestamp()));
                values.put("comment_preview_" + (i + 1), time + " " + authorName + ": " + entry.message());
            } else {
                values.put("comment_preview_" + (i + 1), "§8(空)");
            }
        }

        // 打开 GUI
        ControllerCommand.openUi(player, Menus.boardView(gw, values));
    }
}
