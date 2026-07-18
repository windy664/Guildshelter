package org.windy.guildshelter.adapter.bukkit.command.manor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.PlayerRef;

import java.util.Arrays;

@GsSubCommand(name = "greeting", permission = "guildshelter.command.greeting", aliases = {"farewell"})
public class GreetingCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        if (ctx.cityFlagCache == null) { sender.sendMessage(Messages.get("error.feature_unavailable")); return; }
        boolean isGreeting = args[0].equalsIgnoreCase("greeting");
        String key = isGreeting
                ? org.windy.guildshelter.adapter.bukkit.listener.TerritoryGreetingListener.KEY_GREETING
                : org.windy.guildshelter.adapter.bukkit.listener.TerritoryGreetingListener.KEY_FAREWELL;
        GuildWorld gw = ctx.registry.get(player.getWorld().getName());
        if (gw == null) { sender.sendMessage(Messages.get("error.greeting_not_in_camp")); return; }
        PlayerRef ref = PlayerRef.of(player.getUniqueId());
        if (!ctx.service.isGuildAdmin(ref, gw.guild()) && !player.isOp()) { sender.sendMessage(Messages.get("error.greeting_leader_only")); return; }
        String action = args.length >= 2 ? args[1].toLowerCase() : "";
        if (action.equals("clear")) { ctx.cityFlagCache.remove(gw.guild(), key); sender.sendMessage(Messages.get(isGreeting ? "success.greeting_cleared" : "success.farewell_cleared")); return; }
        if (action.equals("set") && args.length >= 3) {
            String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            ctx.cityFlagCache.put(gw.guild(), key, text);
            sender.sendMessage(Messages.get(isGreeting ? "success.greeting_set" : "success.farewell_set", text)); return;
        }
        sender.sendMessage(Messages.get(isGreeting ? "usage.greeting" : "usage.farewell"));
    }
}
