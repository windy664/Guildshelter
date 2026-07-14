package org.windy.guildshelter.adapter.bukkit.world;

import org.windy.guildshelter.adapter.bukkit.GuildWorldRegistry;
import org.windy.guildshelter.domain.layout.LayoutCalculator;
import org.windy.guildshelter.domain.layout.RoadMask;
import org.windy.guildshelter.domain.model.ChunkRegion;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.TerrainPrepMode;
import org.windy.guildshelter.domain.port.TerrainPreparer;

import java.util.function.Predicate;

/**
 * <b>按需路面铺设</b>：不在建会/认领庄园时提前强制生成或预铺整张路网。
 * 改成"<b>路区块被加载/生成时顺手铺它那一格路</b>"——玩家走到哪，路补到哪。
 * 这样既避开 Iris lazy-gen 的大范围生成，也避免非惰性世界认领新庄园时一次铺完整圈路。
 *
 * <p>两端共用的决策核心：Bukkit 端 {@link LazyRoadPaveListener}（{@code ChunkLoadEvent#isNewChunk}）、
 * NeoForge 端原生 {@code ChunkEvent.Load} 各自把"新生成的区块坐标"喂给 {@link #onChunkGenerated}。
 *
 * <p>历史上只对<b>惰性世界</b>生效；现在非惰性世界也走按需补铺，避免大型路网一次性写方块。
 *
 * <p><b>用原始整地器</b>（非 Iris 预生成装饰器）：区块已生成且在内存里，{@code surfaceRoad} 的
 * {@code getHighestBlockYAt/setBlock} 命中已加载区块、<b>不触发任何生成</b>；若再套预生成装饰器反而会无谓地
 * 重新 pregen 整片 region。故装配时须把<b>未包装</b>的 terrain 传进来。
 */
public final class LazyRoadPaver {

    private final GuildWorldRegistry registry;
    private final TerrainPreparer rawTerrain;
    /** 兼容旧构造参数；道路已统一按需补铺，不再用它排除非惰性世界。 */
    private final Predicate<String> lazyCheck;
    /** 整地总开关：config terrain-prep==NONE 时不铺任何路（与 NaturalWorldPrep 同语义）。 */
    private final boolean roadsEnabled;

    public LazyRoadPaver(GuildWorldRegistry registry, TerrainPreparer rawTerrain,
                         Predicate<String> lazyCheck, boolean roadsEnabled) {
        this.registry = registry;
        this.rawTerrain = rawTerrain;
        this.lazyCheck = lazyCheck;
        this.roadsEnabled = roadsEnabled;
    }

    /**
     * 某区块<b>刚生成</b>时调用（须在主线程）：若它属于某惰性公会世界、且该区块按布局是路 → 把这一格路铺好。
     * 非公会世界 / 非惰性世界 / 非路区块 / 整地关闭 → 直接返回，零开销。
     *
     * @param worldName registry key（混合端 = 维度 path {@code guild_xxx}）
     * @param chunkX/chunkZ 该区块的世界 chunk 坐标
     */
    public void onChunkGenerated(String worldName, int chunkX, int chunkZ) {
        if (!roadsEnabled) {
            return;
        }
        GuildWorld gw = registry.get(worldName);
        if (gw == null || gw.terrainMode() == TerrainPrepMode.VOID) {
            return; // 非公会世界 / 虚空(空岛无路)
        }
        LayoutCalculator layout = new LayoutCalculator(gw.layout());
        int lx = chunkX - gw.originChunkX();
        int lz = chunkZ - gw.originChunkZ();
        if (!layout.classify(lx, lz).isRoad()) {
            return; // 不是路区块
        }
        // 已生成的单个区块入<b>全局铺路队列</b>（而非每区块各起 timer + EditSession）：常驻 worker 会把同一 tick
        // 内多区块的重发/重光照合成一次 flush，消除探索时的 tick 尖峰。roadMask 让十字路口/桥边护栏判定正确。
        RoadMask roadMask = layout.roadMask(gw.originChunkX(), gw.originChunkZ());
        rawTerrain.enqueueLazyRoad(worldName, new ChunkRegion(chunkX, chunkZ, chunkX, chunkZ), roadMask);
    }
}
