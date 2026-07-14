package org.windy.guildshelter.domain.port;

import org.windy.guildshelter.domain.model.CityPlot;
import org.windy.guildshelter.domain.model.GuildId;

import java.util.List;
import java.util.UUID;

/**
 * 主城子地块（{@link CityPlot}）持久化：每会一组命名子地块，会长圈定/指派给成员。三后端各自实现。
 * 见 PLAN_CITYPLOT.md。
 */
public interface CityPlotStore {

    /** 某公会的全部子地块（含未指派的）。 */
    List<CityPlot> list(GuildId guild);

    /** 圈定/覆盖一个命名子地块（同名则覆盖其范围与指派）。 */
    void save(GuildId guild, CityPlot plot);

    /** 删除某命名子地块。 */
    void remove(GuildId guild, String name);

    /** 把某玩家被指派的所有子地块改为<b>未指派</b>（退会时调用，避免离队成员仍能建店）。 */
    void unassignAllOf(GuildId guild, UUID player);

    /** 清除整会的子地块（解散时）。 */
    void clear(GuildId guild);
}
