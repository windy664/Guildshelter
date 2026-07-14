package org.windy.guildshelter.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * <b>保护决策参与点</b>：附属插件实现并经 {@link GuildShelterAPI#registerBuildCheck} 注册后，会在
 * GuildShelter 的破坏/放置判定里被询问，可<b>额外放行或额外拒绝</b>（见 {@link BuildDecision}）。
 *
 * <p>这是"核心留 API、玩法做附属"的关键：共享农场、主城子地块等"在特定条件下额外放行建造"的玩法，
 * 都可由附属经本接口实现，与第三方走同一机制（dogfooding）。
 *
 * <p>调用在<b>主线程·热路径</b>，实现需快速、无阻塞 IO。
 */
@FunctionalInterface
public interface BuildCheckProvider {

    /**
     * @param player  操作者
     * @param loc     目标方块位置
     * @param action  破坏 / 放置
     * @param blockId 目标方块命名空间 id（如 {@code minecraft:wheat}）
     * @return 本 provider 的表态（{@link BuildDecision#PASS} = 不干预）
     */
    BuildDecision check(Player player, Location loc, BuildAction action, String blockId);
}
