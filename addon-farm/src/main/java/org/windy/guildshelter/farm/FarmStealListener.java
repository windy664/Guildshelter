package org.windy.guildshelter.farm;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.windy.guildshelter.api.GuildRef;
import org.windy.guildshelter.api.GuildShelterAPI;

import java.util.Optional;

/**
 * 偷菜计数 + 通知：在<b>非成员成功收割共享农场成熟作物</b>后记一次额度、按需提示在场成员。
 * 放行由 {@link FarmCheckProvider} 完成；这里只在 MONITOR(未取消)时做副作用，保证 check 幂等无副作用。
 */
public final class FarmStealListener implements Listener {

    private final GuildShelterAPI api;
    private final FarmBlocks farmBlocks;
    private final StealQuota quota;
    private final boolean notifyOwner;

    public FarmStealListener(GuildShelterAPI api, FarmBlocks farmBlocks, StealQuota quota, boolean notifyOwner) {
        this.api = api;
        this.farmBlocks = farmBlocks;
        this.quota = quota;
        this.notifyOwner = notifyOwner;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        var loc = event.getBlock().getLocation();
        if (!api.isFarmZone(loc) || !farmBlocks.matches(FarmBlocks.idOf(event.getBlock().getType()))) {
            return;
        }
        Optional<GuildRef> guildOpt = api.guildAt(loc);
        if (guildOpt.isEmpty()) {
            return;
        }
        GuildRef guild = guildOpt.get();
        if (api.isMember(guild, player.getUniqueId())) {
            return; // 成员正常收割，不算偷
        }
        // 非成员且破坏成功（没被取消）= 偷菜成功
        int count = quota.record(player.getUniqueId());
        String suffix = quota.dailyLimit() < 0 ? "" : ("（今日 " + count + "/" + quota.dailyLimit() + "）");
        player.sendMessage("§e你偷走了 §f" + guild.id() + " §e农场的收成…… " + suffix);
        if (notifyOwner) {
            notifyMembers(guild, player);
        }
    }

    /** 提示该公会在线成员"农场被偷"。纯只读 API + Bukkit 在线玩家遍历，不碰主插件内部。 */
    private void notifyMembers(GuildRef guild, Player thief) {
        for (Player online : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (!online.equals(thief) && api.isMember(guild, online.getUniqueId())) {
                online.sendMessage("§c⚠ §7你公会的共享农场正在被 §f" + thief.getName() + " §7偷菜！");
            }
        }
    }
}
