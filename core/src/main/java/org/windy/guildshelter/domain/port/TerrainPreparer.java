package org.windy.guildshelter.domain.port;

import org.windy.guildshelter.domain.layout.RoadMask;
import org.windy.guildshelter.domain.model.ChunkRegion;
import org.windy.guildshelter.domain.model.TerrainPrepMode;

/**
 * 整地端口：对给定世界的一块<b>世界坐标</b>区域按模式整地（清植被/铺平）。
 * 实现侧负责异步/分批，避免卡主线程（Phase: Bukkit 实现）。
 */
public interface TerrainPreparer {

    void prepare(String worldName, ChunkRegion worldRegion, TerrainPrepMode mode);

    /** 同步/异步整地。sync=true 一次处理完（claim 用），sync=false 分批（升级/重置用）。 */
    default void prepare(String worldName, ChunkRegion region, TerrainPrepMode mode, boolean sync) {
        prepare(worldName, region, mode);
    }

    /**
     * 把给定区域的<b>道路顶层</b>铺成土径：穿过并清掉植被/树木/积雪定位真正的自然地面，
     * 再把地面顶层换成土径；水面/虚空跳过。实现侧负责异步/分批。
     *
     * @param roadMask 路网判定（世界 chunk 坐标）。水上架桥时据此抑制十字路口护栏：外侧也是路则不架栏。
     *                 无需路口感知时传 {@link RoadMask#NONE}。
     */
    void surfaceRoad(String worldName, ChunkRegion worldRegion, RoadMask roadMask);

    /**
     * <b>惰性铺路入队</b>：把一个<b>刚生成的单区块</b>路区块加入实现侧的<b>全局铺路队列</b>，由一个<b>常驻 worker</b>
     * 按节流（每 tick 列数/墙钟上限、运行间隔）批量铺——关键是把同一 tick 内多个区块的<b>重发 + 重光照塌缩成一次</b>
     * （旧实现每区块各起一个 timer + 各自一个 EditSession，玩家探索时一 tick 十几次 flush → tick 尖峰）。
     *
     * <p>须在<b>服务器主线程</b>调用（Bukkit {@code ChunkLoadEvent} / NeoForge 经 {@code server.execute} 均已在主线程）。
     * 默认实现回退到逐区块 {@link #surfaceRoad}（未覆盖的实现仍可用，只是没有合批收益）。
     */
    default void enqueueLazyRoad(String worldName, ChunkRegion chunkRegion, RoadMask roadMask) {
        surfaceRoad(worldName, chunkRegion, roadMask);
    }

    /**
     * 配置惰性铺路节流（可选；不调用则用实现内置默认值）。全部由 config 驱动，避免写死魔数。
     *
     * @param columnsPerTick 每次 worker 运行最多铺多少<b>列</b>（跨区块累计；一个区块=256 列）
     * @param budgetMs       每次运行在铺路上花的<b>墙钟上限(毫秒)</b>，到点立刻停、下次续
     * @param intervalTicks  每隔几 tick 跑一次铺路 worker（越大越省、路出现越慢）
     */
    default void configureLazyRoad(int columnsPerTick, long budgetMs, int intervalTicks) {
    }

    /**
     * 沿<b>最大主城</b>外缘建一圈围墙（栅栏/墙），只立在<b>外侧是成员庄园（非路）</b>的主城边块上：
     * 这样不踩成员庄园、贴道路的那几段自动留口。{@code maxCityRegion} 为世界坐标（已含 origin 偏移），
     * {@code roadMask} 判外侧那格是不是路。实现侧负责异步/分批；未启用或不支持则空操作。
     */
    default void encloseMainCity(String worldName, ChunkRegion maxCityRegion, RoadMask roadMask) {
    }

    /**
     * 在 {@code [minX..maxX]×[minZ..maxZ]}（世界<b>方块</b>坐标）于高度 {@code y} 铺一层 {@code blockId} 实心平台。
     * 给<b>虚空(VOID)世界</b>的庄园/空岛当地基用（虚空没有自然地表，只能定高铺）。默认空实现（不支持的实现忽略）。
     */
    default void platform(String worldName, int minX, int minZ, int maxX, int maxZ, int y, String blockId) {
    }
}
