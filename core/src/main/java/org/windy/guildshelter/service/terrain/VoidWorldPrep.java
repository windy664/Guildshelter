package org.windy.guildshelter.service.terrain;

import org.windy.guildshelter.domain.layout.LayoutCalculator;
import org.windy.guildshelter.domain.model.ChunkRegion;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.port.SchematicStore;
import org.windy.guildshelter.domain.port.TerrainPreparer;

/**
 * {@link WorldPrep} 的<b>虚空(空岛)</b>实现：虚空没有自然地表，按"空岛"处理——给每个庄园在锚定角铺一块平台，
 * <b>或</b>（服主配了 schematic 且 schematic 存储可用时）贴一份空岛模板。<b>不铺路、不建围墙</b>（空岛无路）。
 *
 * <p>平台/模板都落在庄园锚定角 chunk 中心(+8,+8)、高度 {@code baseY}，与 {@code /gs home} 默认落点一致，
 * 玩家回家正好站岛上（见 GsCommand home 锚点）。
 */
public final class VoidWorldPrep implements WorldPrep {

    private final TerrainPreparer terrain;
    /** 空岛模板存储（可选；null = 不用模板，铺默认平台）。 */
    private final SchematicStore schematic;
    /** 空岛 schematic 名（config {@code void-island.schematic}；空/null = 不用模板）。 */
    private final String schematicName;
    /** 默认平台方块（config {@code void-island.platform-block}）。 */
    private final String platformBlock;
    /** 默认平台半径（格；config {@code void-island.platform-radius}）。边长 = 2*r+1。 */
    private final int platformRadius;

    public VoidWorldPrep(TerrainPreparer terrain, SchematicStore schematic,
                         String schematicName, String platformBlock, int platformRadius) {
        this.terrain = terrain;
        this.schematic = schematic;
        this.schematicName = (schematicName == null || schematicName.isBlank()) ? null : schematicName.trim();
        this.platformBlock = platformBlock;
        this.platformRadius = Math.max(0, platformRadius);
    }

    @Override
    public void prepareManor(GuildWorld gw, Manor manor, boolean sync, boolean includeRoads) {
        LayoutCalculator layout = new LayoutCalculator(gw.layout());
        ChunkRegion plot = layout.plotRegion(manor.slot()).shift(gw.originChunkX(), gw.originChunkZ());
        int ax = plot.minBlockX() + 8; // 锚定角 chunk 中心，与 /gs home 默认落点一致
        int az = plot.minBlockZ() + 8;
        int baseY = gw.layout().baseY();
        if (schematicName != null && schematic != null) {
            schematic.paste(gw.worldName(), schematicName, ax, baseY, az, !sync); // 贴空岛模板
        } else {
            terrain.platform(gw.worldName(), ax - platformRadius, az - platformRadius,
                    ax + platformRadius, az + platformRadius, baseY, platformBlock); // 默认平台
        }
    }

    @Override
    public void prepareRoadsWithinBorder(GuildWorld gw) {
        // 空岛无路：不铺。
    }

    @Override
    public void buildCityWall(GuildWorld gw) {
        // 空岛无墙：不建。
    }
}
