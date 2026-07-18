package org.windy.guildshelter.adapter.bukkit.command.admin;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;

import java.util.List;
import java.util.stream.Collectors;

@GsSubCommand(name = "admin upgrade-manor", permission = "guildshelter.admin", requiresPlayer = false)
public class AdminUpgradeManorCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage(Messages.get("usage.admin_upgrade_manor")); return; }
        GuildId guild = new GuildId(args[1]);
        if (ctx.guilds.find(guild).isEmpty()) { sender.sendMessage(Messages.get("error.guild_not_exist", guild.value())); return; }
        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        PlayerRef ref = PlayerRef.of(target.getUniqueId());
        Integer slot = resolveTargetSlot(sender, guild, ref, args, 3, args[2]);
        if (slot == null) return;
        int cap = ctx.levels.manorMaxLevel();
        if (ctx.service.upgradeManor(guild, slot)) {
            Manor m = ctx.manors.findBySlot(guild, slot).orElseThrow();
            sender.sendMessage(Messages.get("success.admin_upgrade_manor", args[2], slot, m.level(), cap));
            org.bukkit.entity.Player online = target.getPlayer();
            if (online != null) online.sendMessage(Messages.get("success.upgraded", m.level(), cap));
        } else { sender.sendMessage(Messages.get("error.already_max_level", cap)); }
    }

    private Integer resolveTargetSlot(CommandSender sender, GuildId guild, PlayerRef owner, String[] args, int slotArgIdx, String playerName) {
        if (args.length > slotArgIdx) {
            int slot;
            try { slot = Integer.parseInt(args[slotArgIdx]); } catch (NumberFormatException e) { sender.sendMessage(Messages.get("error.admin_slot_must_be_int")); return null; }
            Manor m = ctx.manors.findBySlot(guild, slot).orElse(null);
            if (m == null || !m.owner().equals(owner)) { sender.sendMessage(Messages.get("error.admin_slot_not_found", slot, playerName)); return null; }
            return slot;
        }
        List<Manor> owned = ctx.manors.findAllByOwner(guild, owner);
        if (owned.isEmpty()) { sender.sendMessage(Messages.get("error.admin_no_manor_in_guild", playerName, guild.value())); return null; }
        if (owned.size() > 1) { String slots = owned.stream().map(m -> "#" + m.slot()).collect(Collectors.joining(" ")); sender.sendMessage(Messages.get("error.admin_multi_manor_specify_slot", playerName, slots)); return null; }
        return owned.get(0).slot();
    }
}
