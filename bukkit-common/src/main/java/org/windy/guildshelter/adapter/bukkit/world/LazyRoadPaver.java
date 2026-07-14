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
 * <b>惰性路面铺设</b>：对 Iris 等惰性世界，<b>绝不</b>提前强制生成区块来铺路（那会触犯 Iris 的 lazy-gen 原则、
 * 白白生成大片没人去的世界）。改成"<b>区块自然生成时顺手铺它那一格路</b>"——玩家探索到哪、Iris 生成到哪，
 * 就在刚生成的那个区块上把路铺好。零强制生成，路网跟着玩家脚步自然成形。
 *
 * <p>两端共用的决策核心：Bukkit 端 {@link LazyRoadPaveListener}（{@code ChunkLoadEvent#isNewChunk}）、
 * NeoForge 端原生 {@code ChunkEvent.Load} 各自把"新生成的区块坐标"喂给 {@link #onChunkGenerated}。
 *
 * <p>只对<b>惰性世界</b>生效（{@code lazyCheck}）：非惰性世界由 {@code prepareRoadsWithinBorder} 的
 * 提前铺满负责（那种世界区块加载廉价，预铺无妨）。
 *
 * <p><b>用原始整地器</b>（非 Iris 预生成装饰器）：区块已生成且在内存里，{@code surfaceRoad} 的
 * {@code getHighestBlockYAt/setBlock} 命中已加载区块、<b>不触发任何生成</b>；若再套预生成装饰器反而会无谓地
 * 重新 pregen 整片 region。故装配时须把<b>未包装</b>的 terrain 传进来。
 */
public final class LazyRoadPaver {

    private final GuildWorldRegistry registry;
    private final TerrainPreparer rawTerrain;
    /** 世界是否惰性生成（=Iris 世界）。只对惰性世界铺，非惰性走提前铺满。 */
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
        if (!lazyCheck.test(worldName)) {
            return; // 非惰性世界：由提前铺满负责，这里不重复
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
