package org.windy.guildshelter.farm;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.windy.guildshelter.api.BuildAction;
import org.windy.guildshelter.api.BuildCheckProvider;
import org.windy.guildshelter.api.BuildDecision;
import org.windy.guildshelter.api.GuildRef;
import org.windy.guildshelter.api.GuildShelterAPI;

import java.util.Optional;

/**
 * 农场附属的<b>保护决策参与</b>（dogfood {@link BuildCheckProvider}）：在共享农场区里——
 * <ul>
 *   <li><b>A1 共享农场</b>：会内<b>成员</b>可种/收农事方块（BREAK/PLACE）+ 开容器（CONTAINER，若 allow-containers）。</li>
 *   <li><b>偷菜</b>：<b>非</b>成员可 BREAK <b>成熟</b>农作物（受每日额度限制；实际计数/通知在 {@link FarmStealListener}）。</li>
 * </ul>
 * 其余一律 PASS（交回核心默认）。只读 {@link GuildShelterAPI}，不碰主插件内部。
 */
final class FarmCheckProvider implements BuildCheckProvider {

    private final GuildShelterAPI api;
    private final FarmBlocks farmBlocks;
    private final boolean shareEnabled;
    private final boolean allowContainers;
    private final boolean stealEnabled;
    private final boolean stealOnlyMature;
    private final StealQuota stealQuota;

    FarmCheckProvider(GuildShelterAPI api, FarmBlocks farmBlocks, boolean shareEnabled, boolean allowContainers,
                      boolean stealEnabled, boolean stealOnlyMature, StealQuota stealQuota) {
        this.api = api;
        this.farmBlocks = farmBlocks;
        this.shareEnabled = shareEnabled;
        this.allowContainers = allowContainers;
        this.stealEnabled = stealEnabled;
        this.stealOnlyMature = stealOnlyMature;
        this.stealQuota = stealQuota;
    }

    @Override
    public BuildDecision check(Player player, Location loc, BuildAction action, String blockId) {
        if (!api.isFarmZone(loc)) {
            return BuildDecision.PASS; // 非共享农场区不干预
        }
        Optional<GuildRef> guildOpt = api.guildAt(loc);
        if (guildOpt.isEmpty()) {
            return BuildDecision.PASS;
        }
        boolean member = api.isMember(guildOpt.get(), player.getUniqueId());

        if (member) {
            // A1：会内成员合作种田
            if (!shareEnabled) {
                return BuildDecision.PASS;
            }
            if (action == BuildAction.CONTAINER) {
                return allowContainers ? BuildDecision.ALLOW : BuildDecision.PASS;
            }
            if ((action == BuildAction.BREAK || action == BuildAction.PLACE) && farmBlocks.matches(blockId)) {
                return BuildDecision.ALLOW;
            }
            return BuildDecision.PASS;
        }

        // 偷菜：非成员只能 BREAK 成熟农作物，且今日额度未满
        if (stealEnabled && action == BuildAction.BREAK && farmBlocks.matches(blockId)) {
            if (stealOnlyMature && !FarmBlocks.isMature(loc.getBlock())) {
                return BuildDecision.PASS; // 未成熟不让偷（防铲苗）→ 交回核心(会拒)
            }
            if (stealQuota.canSteal(player.getUniqueId())) {
                return BuildDecision.ALLOW; // 放行收割；计数/通知在 BlockBreakEvent 监听里
            }
        }
        return BuildDecision.PASS;
    }
}
