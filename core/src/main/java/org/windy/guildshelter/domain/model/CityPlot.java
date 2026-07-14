package org.windy.guildshelter.domain.model;

import java.util.UUID;

/**
 * <b>主城子地块</b>（借鉴 HuskTowns plot，见 PLAN_CITYPLOT.md）：会长在主城那一格里圈出的<b>命名小块</b>，
 * 指派给某会内成员当店面/摊位。坐标是<b>相对 cell0 原点的局部 chunk 坐标</b>（与 {@code classify} 的 lx/lz、
 * {@code GuildWorld.isCityUnlocked} 同口径），范围必须 ⊆ 主城中心格且已被主城解锁。
 *
 * <p>与<b>解锁制正交</b>：解锁决定"地属不属于主城/能不能建"，子地块只是把已解锁主城地的<b>建造权委托</b>给某成员，
 * 不改变领地归属。{@code assignee} 为 {@code null} = 已圈定但未指派（无人能凭它建造）。
 */
public record CityPlot(String name, int minCx, int minCz, int maxCx, int maxCz, UUID assignee) {

    /** 局部 chunk 坐标 (lx,lz) 是否落在本子地块内。 */
    public boolean contains(int lx, int lz) {
        return lx >= minCx && lx <= maxCx && lz >= minCz && lz <= maxCz;
    }

    /** 返回换了指派人的副本（{@code null} = 取消指派）。 */
    public CityPlot withAssignee(UUID who) {
        return new CityPlot(name, minCx, minCz, maxCx, maxCz, who);
    }
}
