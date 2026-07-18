package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;

/**
 * /gs gift <玩家>：把手持物品送给同世界的玩家。
 */
@GsSubCommand(name = "gift", permission = "guildshelter.command.gift")
public class GiftCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.gift")); return; }
        Player target = org.bukkit.Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) { sender.sendMessage(Messages.get("error.player_offline", args[1])); return; }
        if (target.equals(player)) { sender.sendMessage(Messages.get("error.cannot_self")); return; }
        if (!player.getWorld().equals(target.getWorld())) { sender.sendMessage(Messages.get("error.not_same_world")); return; }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR || item.getAmount() == 0) { sender.sendMessage(Messages.get("error.no_item_in_hand")); return; }
        var leftover = target.getInventory().addItem(item.clone());
        for (var drop : leftover.values()) target.getWorld().dropItemNaturally(target.getLocation(), drop);
        player.getInventory().setItemInMainHand(null);
        sender.sendMessage(Messages.get("success.gift_sent", item.getAmount(), item.getType().name(), target.getName()));
        target.sendMessage(Messages.get("success.gift_received", player.getName(), item.getAmount(), item.getType().name()));
    }
}
