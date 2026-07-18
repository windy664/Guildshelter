package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.ManorEntityCensus;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.adapter.bukkit.gui.Menus;
import org.windy.guildshelter.domain.flag.Flag;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;

import java.util.HashMap;
import java.util.Map;

/**
 * /gs card [玩家]：查看庄园门牌（GUI）。
 * 玩家执行打开 GUI，控制台走聊天输出。
 */
@GsSubCommand(name = "card", permission = "guildshelter.command.card", requiresPlayer = false)
public class CardCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        PlayerRef targetRef;
        String targetName;
        if (args.length >= 2) {
            targetRef = PlayerRef.of(Bukkit.getOfflinePlayer(args[1]).getUniqueId());
            targetName = args[1];
        } else if (sender instanceof Player player) {
            targetRef = PlayerRef.of(player.getUniqueId());
            targetName = player.getName();
        } else {
            sender.sendMessage(Messages.get("error.console_need_player"));
            return;
        }
        Manor manor = ctx.manors.findByOwnerAnywhere(targetRef).orElse(null);
        if (manor == null) {
            sender.sendMessage(Messages.get("error.target_no_manor", targetName));
            return;
        }
        GuildWorld gw = ctx.guilds.find(manor.guild()).orElse(null);
        if (gw == null) {
            sender.sendMessage(Messages.get("error.world_not_exist"));
            return;
        }

        // 构建占位符值
        Map<String, Object> values = buildValues(manor, gw, targetName);

        if (sender instanceof Player player) {
            // 玩家：打开 GUI
            ControllerCommand.openUi(player, Menus.manorCard(manor, gw, values));
        } else {
            // 控制台：聊天输出（保留原逻辑）
            sendChatCard(sender, manor, gw, targetName, values);
        }
    }

    private Map<String, Object> buildValues(Manor manor, GuildWorld gw, String targetName) {
        Map<String, Object> v = new HashMap<>();
        int side = gw.layout().plotChunks() * 16;
        int capacity = ctx.service.effectiveCapacity(gw);
        int memberCount = ctx.manors.findAll(manor.guild()).size();
        String alias = Flag.ALIAS.resolveString(manor.flags());
        String desc = Flag.DESCRIPTION.resolveString(manor.flags());
        boolean done = Flag.DONE.resolveBool(manor.flags());

        String entityLine = "§8(世界未加载)";
        int score = manor.level() * 100 + memberCount * 10 + manor.flags().size() * 2;
        World world = Bukkit.getWorld(gw.worldName());
        if (world != null && ctx.census != null) {
            ManorEntityCensus.Census c = ctx.census.countAt(world, manor);
            entityLine = "§a" + c.animals() + " §7动物 §c" + c.hostiles() + " §7敌对 §b"
                    + c.otherMobs() + " §7其它 §6" + c.vehicles() + " §7载具";
            score += c.livingTotal() * 5;
        }
        long activeFlags = manor.flags().entrySet().stream()
                .filter(e -> { Flag f = Flag.byId(e.getKey()).orElse(null); return f != null && !e.getValue().equals(f.defaultValue()); })
                .count();

        v.put("owner_name", targetName);
        v.put("side", side);
        v.put("capacity", capacity);
        v.put("member_count", memberCount);
        v.put("alias", alias.isBlank() ? "#" + manor.slot() : alias);
        v.put("description", desc.isBlank() ? "§8(未设置)" : desc);
        v.put("done_status", done ? Messages.get("info.done_status") : Messages.get("info.building_status"));
        v.put("entity_line", entityLine);
        v.put("trusted_count", manor.coBuilders().size());
        v.put("denied_count", manor.denied().size());
        v.put("flag_count", activeFlags);
        v.put("rating_avg", String.format("%.1f", ctx.manors.getAverageRating(manor.guild(), manor.slot())));
        v.put("rating_count", ctx.manors.getRatingCount(manor.guild(), manor.slot()));
        v.put("score", score);
        return v;
    }

    private void sendChatCard(CommandSender sender, Manor manor, GuildWorld gw, String targetName, Map<String, Object> v) {
        int side = gw.layout().plotChunks() * 16;
        String alias = Flag.ALIAS.resolveString(manor.flags());
        String title = alias.isBlank() ? "#" + manor.slot() : alias + " (#" + manor.slot() + ")";
        boolean done = Flag.DONE.resolveBool(manor.flags());

        sender.sendMessage(Messages.get("info.card_header"));
        sender.sendMessage(Messages.get("info.card_plot", title, manor.guild().value(), gw.guildLevel(),
                done ? Messages.get("info.done_status") : Messages.get("info.building_status")));
        sender.sendMessage(Messages.get("info.card_owner", targetName, manor.level(), ctx.levels.manorMaxLevel(), side, side));
        if (!alias.isBlank()) sender.sendMessage(Messages.get("info.card_alias", alias));
        String desc = Flag.DESCRIPTION.resolveString(manor.flags());
        if (!desc.isBlank()) sender.sendMessage(Messages.get("info.card_desc", desc));
        sender.sendMessage(Messages.get("info.card_entities", v.get("entity_line")));
        sender.sendMessage(Messages.get("info.card_members", v.get("member_count"), ctx.service.effectiveCapacity(gw),
                manor.coBuilders().size(), manor.denied().size()));
        sender.sendMessage(Messages.get("info.card_flags", v.get("flag_count")));
        double price = Flag.PRICE.resolveDouble(manor.flags());
        if (price > 0) sender.sendMessage(Messages.get("info.card_price", price));
        sender.sendMessage(Messages.get("info.card_footer"));
        sender.sendMessage(Messages.get("info.card_score_line", v.get("score")));
        sender.sendMessage(Messages.get("info.card_bottom"));
    }
}
