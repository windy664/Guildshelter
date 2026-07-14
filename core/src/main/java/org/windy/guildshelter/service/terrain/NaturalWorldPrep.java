package org.windy.guildshelter.service.terrain;

import org.windy.guildshelter.domain.layout.LayoutCalculator;
import org.windy.guildshelter.domain.layout.RoadMask;
import org.windy.guildshelter.domain.model.ChunkRegion;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.TerrainPrepMode;
import org.windy.guildshelter.domain.port.TerrainPreparer;

/**
 * {@link WorldPrep} 的<b>自然/超平坦</b>实现：庄园按列整地（清植被/铺平）、沿自然地表铺路网、建围墙。
 * 即把原先散在 {@code GuildService} 里的"整地+铺路+围墙"逻辑收拢于此（见 {@link WorldPrep}）。
 */
public final class NaturalWorldPrep implements WorldPrep {

    /** 路网"提前铺满"的缓冲环数：与世界边界缓冲一致，保证下一个加入者落点已有路。 */
    public static final int ROAD_FILL_BUFFER = 1;

    private final TerrainPreparer terrain;
    private final TerrainPrepMode prepMode;

    public NaturalWorldPrep(TerrainPreparer terrain, TerrainPrepMode prepMode) {
        this.terrain = terrain;
        this.prepMode = prepMode;
    }

    @Override
    public void prepareManor(GuildWorld gw, Manor manor, boolean sync, boolean includeRoads) {
        if (prepMode == TerrainPrepMode.NONE) {
            return;
        }
        LayoutCalculator layout = new LayoutCalculator(gw.layout());
        // 首次分配只整【初始解锁的角落正方形】(initialUnlockSide²)，其余靠玩家自己解锁开荒。
        ChunkRegion plot = layout.plotRegion(manor.slot());
        int side = gw.layout().initialUnlockSide();
        ChunkRegion initial = new ChunkRegion(plot.minChunkX(), plot.minChunkZ(),
                plot.minChunkX() + side - 1, plot.minChunkZ() + side - 1);
        terrain.prepare(gw.worldName(), initial.shift(gw.originChunkX(), gw.originChunkZ()), prepMode, sync);
        if (includeRoads) {
            RoadMask roadMask = layout.roadMask(gw.originChunkX(), gw.originChunkZ());
            for (ChunkRegion road : layout.roadStripsFor(manor.slot())) {
                terrain.surfaceRoad(gw.worldName(), road.shift(gw.originChunkX(), gw.originChunkZ()), roadMask);
            }
        }
    }

    @Override
    public void prepareRoadsWithinBorder(GuildWorld gw) {
        if (prepMode == TerrainPrepMode.NONE) {
            return;
        }
        LayoutCalculator layout = new LayoutCalculator(gw.layout());
        int ring = layout.adaptiveBorderRingCells(gw.allocatedSlots(), ROAD_FILL_BUFFER);
        RoadMask roadMask = layout.roadMask(gw.originChunkX(), gw.originChunkZ());
        for (ChunkRegion strip : layout.allRoadStrips(ring, gw.originChunkX(), gw.originChunkZ())) {
            terrain.surfaceRoad(gw.worldName(), strip, roadMask); // allRoadStrips 已含 origin 偏移
        }
    }

    @Override
    public void buildCityWall(GuildWorld gw) {
        LayoutCalculator layout = new LayoutCalculator(gw.layout());
        ChunkRegion maxCity = layout.mainCityRegion().shift(gw.originChunkX(), gw.originChunkZ());
        RoadMask roadMask = layout.roadMask(gw.originChunkX(), gw.originChunkZ());
        terrain.encloseMainCity(gw.worldName(), maxCity, roadMask);
    }
}
