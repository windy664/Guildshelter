package org.windy.guildshelter.adapter.bukkit;

import org.windy.guildshelter.domain.model.CityPlot;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.port.CityPlotStore;
import org.windy.guildshelter.domain.port.MembershipChangeListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 主城子地块内存缓存：{@code guild → [CityPlot]}。启动全量加载，增删改同步内存 + 持久层。
 * 供 {@link ClaimGuard} 在<b>热路径</b>判定"某局部 chunk 是否归某成员的子地块"——O(子地块数) 遍历，零 DB。
 *
 * <p>实现 {@link MembershipChangeListener}：成员退会 → 把其名下子地块自动改未指派（不删块，会长可再指派他人）；
 * 公会解散 → 清空。
 */
public final class CityPlotCache implements MembershipChangeListener {

    private final CityPlotStore store;
    private final Map<String, List<CityPlot>> byGuild = new ConcurrentHashMap<>();

    public CityPlotCache(CityPlotStore store, List<GuildWorld> allGuilds) {
        this.store = store;
        for (GuildWorld gw : allGuilds) {
            byGuild.put(gw.guild().value(), new ArrayList<>(store.list(gw.guild())));
        }
    }

    /** 该公会的子地块列表（只读副本）。 */
    public List<CityPlot> list(GuildId guild) {
        List<CityPlot> l = byGuild.get(guild.value());
        return l == null ? List.of() : new ArrayList<>(l);
    }

    /**
     * 局部 chunk 坐标 (lx,lz) 是否落在<b>指派给 {@code who}</b> 的某个子地块内（ClaimGuard 主城分支用）。
     * 未指派的子地块不放行任何人。
     */
    public boolean assignedTo(GuildId guild, int lx, int lz, UUID who) {
        List<CityPlot> l = byGuild.get(guild.value());
        if (l == null || who == null) {
            return false;
        }
        for (CityPlot p : l) {
            if (who.equals(p.assignee()) && p.contains(lx, lz)) {
                return true;
            }
        }
        return false;
    }

    /** 取某命名子地块（不存在返回 null）。 */
    public CityPlot get(GuildId guild, String name) {
        List<CityPlot> l = byGuild.get(guild.value());
        if (l != null) {
            for (CityPlot p : l) {
                if (p.name().equalsIgnoreCase(name)) {
                    return p;
                }
            }
        }
        return null;
    }

    /** 圈定/覆盖（内存 + 持久层）。 */
    public void save(GuildId guild, CityPlot plot) {
        List<CityPlot> l = byGuild.computeIfAbsent(guild.value(), k -> new ArrayList<>());
        l.removeIf(p -> p.name().equalsIgnoreCase(plot.name()));
        l.add(plot);
        store.save(guild, plot);
    }

    /** 删除某命名子地块（内存 + 持久层）；返回是否删到。 */
    public boolean remove(GuildId guild, String name) {
        List<CityPlot> l = byGuild.get(guild.value());
        boolean removed = l != null && l.removeIf(p -> p.name().equalsIgnoreCase(name));
        if (removed) {
            store.remove(guild, name);
        }
        return removed;
    }

    // ===== MembershipChangeListener =====

    @Override
    public void onMemberAssigned(GuildId guild, UUID playerId) {
        // no-op：入会不影响已有子地块
    }

    @Override
    public void onMemberReleased(GuildId guild, UUID playerId) {
        // 退会：其名下子地块改未指派（保留块，会长可再分配他人），内存 + 持久层同步。
        List<CityPlot> l = byGuild.get(guild.value());
        if (l == null) {
            return;
        }
        boolean changed = false;
        for (int i = 0; i < l.size(); i++) {
            if (playerId.equals(l.get(i).assignee())) {
                l.set(i, l.get(i).withAssignee(null));
                changed = true;
            }
        }
        if (changed) {
            store.unassignAllOf(guild, playerId);
        }
    }

    @Override
    public void onGuildDissolved(GuildId guild) {
        byGuild.remove(guild.value());
        store.clear(guild);
    }
}
