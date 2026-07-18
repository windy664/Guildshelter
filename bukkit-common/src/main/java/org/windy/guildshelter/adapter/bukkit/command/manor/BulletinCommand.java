package org.windy.guildshelter.adapter.bukkit.command.manor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.Permissions;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;

import java.util.Arrays;

@GsSubCommand(name = "bulletin", permission = "guildshelter.command.bulletin")
public class BulletinCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        Manor manor = ctx.manors.findByOwnerAnywhere(PlayerRef.of(player.getUniqueId())).orElse(null);
        if (manor == null) { sender.sendMessage(Messages.get("error.no_manor")); return; }
        GuildWorld gw = ctx.guilds.find(manor.guild()).orElse(null);
        if (gw == null) { sender.sendMessage(Messages.get("error.world_not_exist")); return; }
        if (!manor.owner().equals(PlayerRef.of(player.getUniqueId())) && !Permissions.hasAdminPerm(player, Permissions.ADMIN_FLAG_OTHER)) {
            sender.sendMessage(Messages.get("error.only_owner")); return;
        }
        String action = args.length >= 2 ? args[1].toLowerCase() : "show";
        switch (action) {
            case "set" -> {
                if (args.length < 3) { sender.sendMessage(Messages.get("usage.bulletin")); return; }
                String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                if (text.length() > 200) { sender.sendMessage(Messages.get("error.too_long", 200)); return; }
                ctx.guilds.save(gw.withBulletin(text.replace(';', ',')));
                sender.sendMessage(Messages.get("success.bulletin_set", text));
            }
            case "clear" -> { ctx.guilds.save(gw.withBulletin("")); sender.sendMessage(Messages.get("success.bulletin_cleared")); }
            case "show" -> {
                String bulletin = gw.bulletin();
                if (bulletin == null || bulletin.isBlank()) sender.sendMessage(Messages.get("info.bulletin_empty"));
                else sender.sendMessage(Messages.get("info.bulletin_show", manor.guild().value(), bulletin));
            }
            default -> sender.sendMessage(Messages.get("usage.bulletin"));
        }
    }
}
