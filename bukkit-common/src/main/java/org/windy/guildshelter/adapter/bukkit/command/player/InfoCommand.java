package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.flag.Flag;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;

import java.util.Set;

@GsSubCommand(name = "info", permission = "guildshelter.command.info")
public class InfoCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        Manor manor = ctx.currentOwnManor(player).orElse(null);
        if (manor == null) { sender.sendMessage(Messages.get("error.no_manor")); return; }
        GuildWorld gw = ctx.guilds.find(manor.guild()).orElse(null);
        if (gw == null) { sender.sendMessage(Messages.get("error.world_not_exist")); return; }
        int side = gw.layout().plotChunks() * 16;
        int capacity = ctx.service.effectiveCapacity(gw);
        int members = ctx.manors.findAll(manor.guild()).size();
        sender.sendMessage(Messages.get("info.guild_info_header"));
        String alias = Flag.ALIAS.resolveString(manor.flags());
        String title = alias.isBlank() ? "庄园 #" + manor.slot() : alias + " (#" + manor.slot() + ")";
        sender.sendMessage(Messages.get("info.guild_line", manor.guild().value(), gw.guildLevel(), ctx.levels.maxGuildLevel(), members, capacity));
        sender.sendMessage(Messages.get("info.plot_line", title, manor.level(), ctx.levels.manorMaxLevel(), side, side,
                Flag.DONE.resolveBool(manor.flags()) ? Messages.get("info.done_status") : Messages.get("info.building_status")));
        int unlocked = manor.unlockedChunks().size();
        int quota = manor.quotaCap(gw.layout(), ctx.levels);
        sender.sendMessage(Messages.get("info.unlock_line", unlocked, unlocked, quota, quota - unlocked));
        sender.sendMessage(Messages.get("info.trusted_line", sizeOrNone(manor.coBuilders()), sizeOrNone(manor.members()), sizeOrNone(manor.denied())));
        String desc = Flag.DESCRIPTION.resolveString(manor.flags());
        if (!desc.isBlank()) sender.sendMessage(Messages.get("info.card_desc", desc));
        double price = Flag.PRICE.resolveDouble(manor.flags());
        if (price > 0) sender.sendMessage(Messages.get("info.card_price", price));
        String blocked = Flag.BLOCKED_CMDS.resolveString(manor.flags());
        if (!blocked.isBlank()) sender.sendMessage(Messages.get("info.blocked_cmds_line", blocked.replace(",", " /")));
        if (Flag.KEEP.resolveBool(manor.flags())) sender.sendMessage(Messages.get("info.keep_line"));
    }

    private static String sizeOrNone(Set<PlayerRef> set) { return set.isEmpty() ? "§8无" : set.size() + " 人"; }
}
