package org.windy.guildshelter.domain.rule;

import org.junit.jupiter.api.Test;
import org.windy.guildshelter.domain.layout.LayoutConfig;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelRulesTest {

    private final LevelRules rules = LevelRules.defaults(); // 公会5, 每级5名额, 庄园上限20

    @Test
    void maxMembersScalesWithGuildLevel() {
        assertEquals(5, rules.maxMembers(1));
        assertEquals(10, rules.maxMembers(2));
        assertEquals(25, rules.maxMembers(5));
    }

    @Test
    void manorUpgradeOnlyGatedByPhysicalCap() {
        // 庄园升级与公会等级无关，只看是否到物理满级（默认 20）。
        assertEquals(20, rules.manorMaxLevel());
        assertTrue(rules.canUpgradeManor(1));
        assertTrue(rules.canUpgradeManor(19));
        assertFalse(rules.canUpgradeManor(20)); // 已满级
    }

    @Test
    void guildUpgradeBoundary() {
        assertTrue(rules.canUpgradeGuild(4));
        assertFalse(rules.canUpgradeGuild(5));
    }

    @Test
    void customCapacityCurve() {
        LevelRules r = new LevelRules(10, 8, 6); // 公会10级, 每级8名额, 庄园上限6
        assertEquals(8, r.maxMembers(1));
        assertEquals(80, r.maxMembers(10));
        assertEquals(6, r.manorMaxLevel());
        assertFalse(r.canUpgradeManor(6));
    }

    @Test
    void explicitManorQuotaTableOverridesLinearCurve() {
        LayoutConfig layout = LayoutConfig.defaults();
        LevelRules r = new LevelRules(5, 5, 20,
                Map.of(1, 36, 2, 45, 5, 74, 20, 300),
                Map.of(), Map.of(), Map.of());

        assertEquals(36, r.manorQuotaCap(layout, 1));
        assertEquals(45, r.manorQuotaCap(layout, 2));
        assertEquals(74, r.manorQuotaCap(layout, 6)); // 未配置等级向下继承最近配置
        assertEquals(225, r.manorQuotaCap(layout, 20)); // 配大了夹到 15x15 物理上限
    }
}
