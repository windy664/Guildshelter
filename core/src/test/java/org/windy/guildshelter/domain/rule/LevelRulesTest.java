package org.windy.guildshelter.domain.rule;

import org.junit.jupiter.api.Test;
import org.windy.guildshelter.domain.layout.LayoutConfig;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelRulesTest {

    private final LevelRules rules = LevelRules.defaults(); // 公会5个时代, 庄园上限20

    @Test
    void maxMembersComesFromLevelTable() {
        assertEquals(20, rules.maxMembers(1));
        assertEquals(40, rules.maxMembers(2));
        assertEquals(100, rules.maxMembers(5));
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
    void explicitGuildTableOverridesBuiltInDefaults() {
        LevelRules r = new LevelRules(10, 6,
                Map.of(),
                Map.of(1, 12, 3, 36),
                Map.of(),
                Map.of());
        assertEquals(12, r.maxMembers(1));
        assertEquals(12, r.maxMembers(2));
        assertEquals(36, r.maxMembers(10));
    }

    @Test
    void explicitManorQuotaTableDrivesQuota() {
        LayoutConfig layout = LayoutConfig.defaults();
        LevelRules r = new LevelRules(5, 20,
                Map.of(1, 36, 2, 45, 5, 74, 20, 300),
                Map.of(), Map.of(), Map.of());

        assertEquals(36, r.manorQuotaCap(layout, 1));
        assertEquals(45, r.manorQuotaCap(layout, 2));
        assertEquals(74, r.manorQuotaCap(layout, 6)); // 未配置等级向下继承最近配置
        assertEquals(225, r.manorQuotaCap(layout, 20)); // 配大了夹到 15x15 物理上限
    }

    @Test
    void maxManorLevelAlwaysUnlocksWholePhysicalPlot() {
        LayoutConfig layout = LayoutConfig.defaults();
        LevelRules r = new LevelRules(5, 20,
                Map.of(1, 36, 19, 120, 20, 120),
                Map.of(), Map.of(), Map.of());

        assertEquals(120, r.manorQuotaCap(layout, 19));
        assertEquals(225, r.manorQuotaCap(layout, 20));
    }
}
