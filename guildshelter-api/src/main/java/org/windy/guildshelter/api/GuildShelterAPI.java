package org.windy.guildshelter.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * GuildShelter 对外<b>只读服务 API</b>（第一期）。第三方附属插件经 Bukkit {@code ServicesManager} 获取：
 * <pre>{@code
 * RegisteredServiceProvider<GuildShelterAPI> rsp =
 *     Bukkit.getServicesManager().getRegistration(GuildShelterAPI.class);
 * if (rsp != null) { GuildShelterAPI api = rsp.getProvider(); ... }
 * }</pre>
 *
 * <p>附属插件 {@code plugin.yml} 应 {@code depend: [GuildShelter]}，并对本 api 模块 <b>compileOnly</b> 依赖
 * （绝不 shade——运行期由主插件 jar 提供这些类）。见 PLAN_API.md。
 *
 * <p>本接口<b>只读</b>：查询领地归属、区域类型、flag、共享农场、公会等级等，供附属做增益玩法（如农场加速）。
 * 写操作 / 保护决策参与 / 事件 / 命令注册为后续期，不在本接口。
 */
public interface GuildShelterAPI {

    /** 该位置所属公会营地；不在任何营地世界返回 empty。 */
    Optional<GuildRef> guildAt(Location loc);

    /** 该位置的区域类型（主城/庄园/路/营地外）。 */
    RegionKind kindAt(Location loc);

    /** 该位置所在的成员庄园；非庄园格返回 empty。 */
    Optional<ManorRef> manorAt(Location loc);

    /**
     * 该位置是否处于<b>共享农场区</b>（挂了 {@code members-farm} flag 的庄园或主城）。
     * <b>位置级判定，不含成员身份</b>——附属可据此对农作物做增益（如生长加速），与"谁能种"正交。
     */
    boolean isFarmZone(Location loc);

    /** 解析该位置生效的<b>布尔 flag</b>（庄园 flag / 主城 flag，未设取默认）。未知 flag 返回 false。 */
    boolean booleanFlag(Location loc, String flagId);

    /** 公会当前等级（跟随宿主公会插件 / 内部等级）；未知公会返回 0。 */
    int guildLevel(GuildRef guild);

    /** 庄园当前等级；未知返回 0。 */
    int manorLevel(ManorRef manor);

    /** 玩家是否该公会成员（在本会拥有庄园）。 */
    boolean isMember(GuildRef guild, UUID player);

    /** 玩家是否该公会会长/副会长。 */
    boolean isGuildAdmin(GuildRef guild, UUID player);

    /**
     * All known GuildShelter camp worlds.
     *
     * <p>Addons should schedule gameplay from GuildShelter camps, not from the
     * backing guild plugin's guild list.
     */
    List<GuildRef> guildCamps();

    /**
     * Territory snapshot for a GuildShelter camp world.
     *
     * <p>This is the same shape as {@link #mapSnapshot(Player)} but without
     * viewer-specific ownership coloring. Addons can use it for world-aware
     * gameplay that needs unlocked chunks or road adjacency.
     */
    Optional<TerritoryMapSnapshot> campSnapshot(GuildRef camp);

    /**
     * 导出该玩家当前庄园的可迁移区域。
     *
     * <p>本方法只返回坐标和元数据，不复制方块、不修改数据。
     */
    Optional<GuildShelterMigrationRegion> exportManor(UUID owner);

    /**
     * 在目标公会中查找一个可迁入的空庄园区域。
     *
     * <p>目标公会不存在、没有空 slot，或目标庄园区域边长小于 {@code requiredSideChunks}
     * 时返回 empty。本方法可能会确保目标公会世界加载，但不会创建庄园记录。
     */
    Optional<GuildShelterMigrationRegion> prepareManorImport(UUID owner, String targetGuild, int requiredSideChunks);

    /**
     * 迁移附属在成功复制方块后，提交目标庄园记录。
     *
     * <p>实现必须重新校验玩家仍无庄园、目标 slot 仍为空、目标公会未满。成功后该庄园区域会归属给玩家。
     */
    boolean completeManorImport(UUID owner, String targetGuild, int slot, int manorLevel);

    Optional<TerritoryMapSnapshot> mapSnapshot(Player viewer);

    MapClaimResult tryClaimMapChunk(Player player, int worldChunkX, int worldChunkZ);

    // ---- 保护决策参与（附属可在破坏/放置判定里额外放行/拒绝）----

    /**
     * 注册一个 {@link BuildCheckProvider}。{@code priority} 小者先询问（聚合规则见 {@link BuildDecision}，
     * 与询问顺序无关——任一 DENY 即拒、任一 ALLOW 即额外放行）。附属<b>禁用/卸载时</b>应调
     * {@link #unregisterBuildChecks(Plugin)} 注销（或由 GuildShelter 在其禁用时自动清理）。
     */
    void registerBuildCheck(org.bukkit.plugin.Plugin plugin, BuildCheckProvider provider, int priority);

    /** 注销某插件注册的全部 {@link BuildCheckProvider}。 */
    void unregisterBuildChecks(org.bukkit.plugin.Plugin plugin);
}
