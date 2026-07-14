package org.windy.guildshelter.service.terrain;

import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;

/**
 * <b>世界整备策略</b>：按公会世界的<b>地形类型</b>（{@link org.windy.guildshelter.domain.model.TerrainPrepMode}）
 * 分流"庄园整地 / 路网 / 围墙"的不同做法，让 {@link org.windy.guildshelter.service.GuildService} 只做编排、
 * 不再用 if/else 堆地形分支。<b>新增地形类型 = 新增一个本接口实现 + 在 GuildService 的选择器登记</b>，不动既有逻辑。
 *
 * <ul>
 *   <li>{@link NaturalWorldPrep}：自然/超平坦地形——扫地表整地、沿自然地面铺路网、建围墙。</li>
 *   <li>{@link VoidWorldPrep}：虚空(空岛)——给每个庄园铺一块平台或贴空岛 schematic，<b>不铺路、不建墙</b>。</li>
 * </ul>
 *
 * <p>纯 core：只依赖 domain 端口（{@link org.windy.guildshelter.domain.port.TerrainPreparer} 等），不碰平台 API。
 */
public interface WorldPrep {

    /**
     * 整备某成员庄园的用地（首次分配/升级时）。
     *
     * @param sync         true=同步一次整完（claim，玩家就在现场）；false=分批
     * @param includeRoads 是否同时铺该庄园四周的路（仅首次分配铺；虚空策略忽略——空岛无路）
     */
    void prepareManor(GuildWorld gw, Manor manor, boolean sync, boolean includeRoads);

    /** 把当前世界边界内的整张路网提前铺满（建会/扩边界时）。虚空策略为空操作（空岛无路）。 */
    void prepareRoadsWithinBorder(GuildWorld gw);

    /** 沿最大主城外缘建围墙（若启用）。虚空策略为空操作。 */
    void buildCityWall(GuildWorld gw);
}
