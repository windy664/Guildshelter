package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.adapter.bukkit.gui.Menus;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;

import java.util.HashMap;
import java.util.Map;

/**
 * /gs camp：打开营地管理 UI。
 */
@GsSubCommand(name = "camp", permission = "guildshelter.command.camp")
public class CampCommand extends SubCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.get("error.player_only"));
            return;
        }
        GuildWorld gw = currentCampWorld(player);
        if (gw == null) {
            sender.sendMessage(Messages.get("error.not_in_guild_world"));
            return;
        }
        Map<String, Object> values = campValues(gw);
        ControllerCommand.openUi(player, Menus.campManager(gw, ctx.levels, values));
    }

    private GuildWorld currentCampWorld(Player player) {
        GuildWorld current = ctx.registry.get(player.getWorld().getName());
        if (current != null) return current;
        Manor manor = ctx.currentOwnManor(player).orElse(null);
        return manor == null ? null : ctx.guilds.find(manor.guild()).orElse(null);
    }

    private Map<String, Object> campValues(GuildWorld gw) {
        Map<String, Object> values = new HashMap<>();
        int cityQuota = gw.cityQuotaCap(ctx.levels);
        int cityUnlocked = gw.cityUnlockedChunks().size();
        values.put("city_quota", cityQuota);
        values.put("city_unlocked", cityUnlocked);
        values.put("city_remaining", Math.max(0, cityQuota - cityUnlocked));
        values.put("member_spawn_status", ctx.campSpawn != null && ctx.campSpawn.get(gw.guild(), org.windy.guildshelter.domain.port.CampSpawnStore.Type.MEMBER).isPresent() ? "已设置" : "未设置");
        values.put("visitor_spawn_status", ctx.campSpawn != null && ctx.campSpawn.get(gw.guild(), org.windy.guildshelter.domain.port.CampSpawnStore.Type.VISITOR).isPresent() ? "已设置" : "未设置");
        values.put("cityplot_status", ctx.cityPlotsEnabled && ctx.cityPlotCache != null ? "已启用" : "未启用");
        values.put("cityplot_count", ctx.cityPlotsEnabled && ctx.cityPlotCache != null ? ctx.cityPlotCache.list(gw.guild()).size() : 0);
        values.put("cityplot_limit", ctx.cityPlotsMaxPerGuild);
        values.put("holo_status", ctx.holoEnabled && ctx.holoStore != null && ctx.holoBackend != null && ctx.holoBackend.available() ? "已启用" : "未启用");
        values.put("holo_count", ctx.holoStore != null ? ctx.holoStore.list(gw.guild()).size() : 0);
        values.put("holo_limit", ctx.holoMaxPerGuild);
        values.put("audit_status", ctx.auditLog != null && ctx.auditLog.isEnabled() ? "已启用" : "未启用");
        values.put("greeting_status", ctx.cityFlagCache != null && ctx.cityFlagCache.flags(gw.guild()).containsKey(org.windy.guildshelter.adapter.bukkit.listener.TerritoryGreetingListener.KEY_GREETING) ? "已设置" : "未设置");
        values.put("farewell_status", ctx.cityFlagCache != null && ctx.cityFlagCache.flags(gw.guild()).containsKey(org.windy.guildshelter.adapter.bukkit.listener.TerritoryGreetingListener.KEY_FAREWELL) ? "已设置" : "未设置");
        return values;
    }
}
