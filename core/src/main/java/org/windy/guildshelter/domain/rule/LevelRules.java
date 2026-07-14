package org.windy.guildshelter.domain.rule;

import org.windy.guildshelter.domain.layout.LayoutConfig;

import java.util.Map;
import java.util.TreeMap;

/**
 * 两套<b>互相独立</b>的等级规则（纯逻辑，配置驱动）：
 *
 * <ul>
 *   <li><b>庄园等级</b>：成员个人的事——成员自行把自己那块庄园从初始升到满级，
 *       只受物理满级 {@code manorMaxLevelCap} 限制，<b>不</b>受公会等级门控。</li>
 *   <li><b>公会等级</b>：容量的事——公会升级 → 放开更多成员名额 {@code membersPerGuildLevel}，
 *       边界随之扩大，从而能容纳/加入更多新成员。</li>
 * </ul>
 *
 * @param maxGuildLevel        公会最高等级
 * @param membersPerGuildLevel 公会每升 1 级放开多少成员名额（容量 = 等级 × 本值），未配置显式时代表时使用
 * @param manorMaxLevelCap     庄园等级绝对上限（= 庄园从初始涨到物理满级所需的级数）
 */
public record LevelRules(int maxGuildLevel, int membersPerGuildLevel, int manorMaxLevelCap,
                         Map<Integer, Integer> manorUnlockQuotas,
                         Map<Integer, Integer> guildMemberCaps,
                         Map<Integer, Integer> guildCityUnlockQuotas,
                         Map<Integer, String> guildLevelNames) {

    public LevelRules(int maxGuildLevel, int membersPerGuildLevel, int manorMaxLevelCap) {
        this(maxGuildLevel, membersPerGuildLevel, manorMaxLevelCap, Map.of(), Map.of(), Map.of(), Map.of());
    }

    public LevelRules {
        if (maxGuildLevel < 1 || membersPerGuildLevel < 1 || manorMaxLevelCap < 1) {
            throw new IllegalArgumentException("等级规则参数必须 ≥1");
        }
        manorUnlockQuotas = Map.copyOf(manorUnlockQuotas == null ? Map.of() : manorUnlockQuotas);
        guildMemberCaps = Map.copyOf(guildMemberCaps == null ? Map.of() : guildMemberCaps);
        guildCityUnlockQuotas = Map.copyOf(guildCityUnlockQuotas == null ? Map.of() : guildCityUnlockQuotas);
        guildLevelNames = Map.copyOf(guildLevelNames == null ? Map.of() : guildLevelNames);
    }

    /** 给定公会等级时允许的成员名额（容量）。公会升级让这个数变大 → 能进更多人。 */
    public int maxMembers(int guildLevel) {
        if (guildLevel < 1) {
            throw new IllegalArgumentException("guildLevel 必须 ≥1");
        }
        Integer explicit = floorValue(guildMemberCaps, guildLevel);
        return explicit != null ? explicit : guildLevel * membersPerGuildLevel;
    }

    /** 给定庄园等级时允许的已解锁 chunk 总数。优先 levels.yml 显式表，缺省回退旧线性公式。 */
    public int manorQuotaCap(LayoutConfig layout, int manorLevel) {
        int cap = layout.plotChunks() * layout.plotChunks();
        Integer explicit = floorValue(manorUnlockQuotas, manorLevel);
        if (explicit != null) {
            return Math.min(Math.max(1, explicit), cap);
        }
        return layout.quotaAtLevel(manorLevel, manorMaxLevelCap);
    }

    /** 给定公会等级时允许的主城已解锁 chunk 总数。优先 levels.yml 显式时代表，缺省回退旧线性公式。 */
    public int cityQuotaCap(LayoutConfig layout, int guildLevel) {
        int cap = layout.mainCityMaxChunks() * layout.mainCityMaxChunks();
        Integer explicit = floorValue(guildCityUnlockQuotas, guildLevel);
        if (explicit != null) {
            return Math.min(Math.max(1, explicit), cap);
        }
        return layout.cityQuotaAtLevel(guildLevel, maxGuildLevel);
    }

    /** 公会等级显示名；未配置时返回 LvN。 */
    public String guildLevelName(int guildLevel) {
        return guildLevelNames.getOrDefault(guildLevel, "Lv" + guildLevel);
    }

    private static Integer floorValue(Map<Integer, Integer> source, int level) {
        if (source.isEmpty()) {
            return null;
        }
        Map.Entry<Integer, Integer> floor = new TreeMap<>(source).floorEntry(level);
        return floor != null ? floor.getValue() : null;
    }

    /** 庄园物理满级（与公会等级无关）。 */
    public int manorMaxLevel() {
        return manorMaxLevelCap;
    }

    /** 庄园能否再升一级——只看是否已达物理满级，成员自己的事。 */
    public boolean canUpgradeManor(int currentManorLevel) {
        return currentManorLevel < manorMaxLevelCap;
    }

    public boolean canUpgradeGuild(int currentGuildLevel) {
        return currentGuildLevel < maxGuildLevel;
    }

    /** 默认：公会 5 级、每级放开 5 个成员名额（满级 25 人）、庄园物理满级 20。 */
    public static LevelRules defaults() {
        return new LevelRules(5, 5, 20);
    }
}
