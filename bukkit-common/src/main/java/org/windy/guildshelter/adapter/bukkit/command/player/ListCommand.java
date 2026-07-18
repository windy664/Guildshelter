package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.PlayerRef;

import java.util.List;

@GsSubCommand(name = "list", permission = "guildshelter.command.list", requiresPlayer = false)
public class ListCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        List<GuildWorld> all = ctx.guilds.findAll();
        boolean mineOnly = args.length >= 2 && args[1].equalsIgnoreCase("mine");
        if (mineOnly) {
            if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.console_cannot_mine")); return; }
            PlayerRef myRef = PlayerRef.of(player.getUniqueId());
            all = all.stream().filter(gw -> ctx.manors.findByOwner(gw.guild(), myRef).isPresent()).toList();
        }
        if (all.isEmpty()) { sender.sendMessage(Messages.get(mineOnly ? "error.no_guild_joined" : "info.no_guilds")); return; }
        sender.sendMessage(Messages.get("info.guild_list_header", mineOnly ? "我的" : "", all.size()));
        for (GuildWorld gw : all) {
            int members = ctx.manors.findAll(gw.guild()).size();
            int cap = ctx.levels.maxMembers(gw.guildLevel());
            sender.sendMessage(Messages.get("info.list_entry", gw.guild().value(), gw.guildLevel(), members, cap, gw.guild().value()));
        }
    }
}
