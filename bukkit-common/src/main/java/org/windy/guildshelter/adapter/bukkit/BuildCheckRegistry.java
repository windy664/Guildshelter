package org.windy.guildshelter.adapter.bukkit;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.windy.guildshelter.api.BuildAction;
import org.windy.guildshelter.api.BuildCheckProvider;
import org.windy.guildshelter.api.BuildDecision;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 第三方 {@link BuildCheckProvider} 的注册中心（PLAN_API.md Phase 4）。{@link ClaimGuard} 在破坏/放置判定里
 * 经 {@link #consult} 询问;{@link GuildShelterApiImpl 经 API} 注册/注销。线程安全（CopyOnWrite）。
 *
 * <p>聚合规则：任一 provider {@link BuildDecision#DENY} → DENY（额外拒绝）；否则任一 {@link BuildDecision#ALLOW}
 * → ALLOW（额外放行）；都不表态 → PASS。空列表时 {@link #consult} 走快速路径返回 PASS，热路径零成本。
 */
public final class BuildCheckRegistry {

    private record Entry(Plugin plugin, BuildCheckProvider provider, int priority) {}

    /** 按 priority 升序；读多写极少，CopyOnWrite 合适。 */
    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    public void register(Plugin plugin, BuildCheckProvider provider, int priority) {
        if (plugin == null || provider == null) {
            return;
        }
        entries.add(new Entry(plugin, provider, priority));
        entries.sort(java.util.Comparator.comparingInt(Entry::priority));
    }

    public void unregister(Plugin plugin) {
        entries.removeIf(e -> e.plugin().equals(plugin));
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** 询问所有 provider 并聚合。异常的 provider 视为 PASS（一个坏附属不拖垮保护判定）。 */
    public BuildDecision consult(Player player, Location loc, BuildAction action, String blockId) {
        if (entries.isEmpty()) {
            return BuildDecision.PASS;
        }
        boolean anyAllow = false;
        for (Entry e : entries) {
            BuildDecision d;
            try {
                d = e.provider().check(player, loc, action, blockId);
            } catch (Throwable t) {
                d = BuildDecision.PASS; // 坏 provider 不影响判定
            }
            if (d == BuildDecision.DENY) {
                return BuildDecision.DENY; // DENY 优先且可短路
            }
            if (d == BuildDecision.ALLOW) {
                anyAllow = true;
            }
        }
        return anyAllow ? BuildDecision.ALLOW : BuildDecision.PASS;
    }
}
