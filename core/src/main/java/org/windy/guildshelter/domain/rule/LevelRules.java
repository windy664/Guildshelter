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
 *   <li><b>公会等级</b>：容量的事——公会升级 → 按 {@code guild.levels} 时代表放开更多成员名额，
 *       边界随之扩大，从而能容纳/加入更多新成员。</li>
 * </ul>
 *
 * @param maxGuildLevel        公会最高等级
 * @param manorMaxLevelCap     庄园等级绝对上限（= 庄园从初始涨到物理满级所需的级数）
 */
public record LevelRules(int maxGuildLevel, int manorMaxLevelCap,
                         Map<Integer, Integer> manorUnlockQuotas,
                         Map<Integer, Integer> guildMemberCaps,
                         Map<Integer, Integer> guildCityUnlockQuotas,
                         Map<Integer, String> guildLevelNames) {

    public static final int DEFAULT_GUILD_MAX_LEVEL = 5;
    public static final int DEFAULT_MANOR_MAX_LEVEL = 20;

    public static final Map<Integer, Integer> DEFAULT_MANOR_UNLOCK_QUOTAS = Map.ofEntries(
            Map.entry(1, 36),
            Map.entry(2, 45),
            Map.entry(3, 54),
            Map.entry(4, 64),
            Map.entry(5, 74),
            Map.entry(6, 84),
            Map.entry(7, 95),
            Map.entry(8, 106),
            Map.entry(9, 118),
            Map.entry(10, 130),
            Map.entry(11, 142),
            Map.entry(12, 155),
            Map.entry(13, 168),
            Map.entry(14, 181),
            Map.entry(15, 194),
            Map.entry(16, 205),
            Map.entry(17, 213),
            Map.entry(18, 219),
            Map.entry(19, 223),
            Map.entry(20, 225));

    public static final Map<Integer, Integer> DEFAULT_GUILD_MEMBER_CAPS = Map.of(
            1, 20,
            2, 40,
            3, 60,
            4, 90,
            5, 100);

    public static final Map<Integer, Integer> DEFAULT_GUILD_CITY_UNLOCK_QUOTAS = Map.of(
            1, 9,
            2, 16,
            3, 25,
            4, 49,
            5, 100);

    public static final Map<Integer, String> DEFAULT_GUILD_LEVEL_NAMES = Map.of(
            1, "蛮荒时代",
            2, "农耕时代",
            3, "蒸汽时代",
            4, "电力时代",
            5, "后工业时代");

    public LevelRules(int maxGuildLevel, int manorMaxLevelCap) {
        this(maxGuildLevel, manorMaxLevelCap,
                DEFAULT_MANOR_UNLOCK_QUOTAS,
                DEFAULT_GUILD_MEMBER_CAPS,
                DEFAULT_GUILD_CITY_UNLOCK_QUOTAS,
                DEFAULT_GUILD_LEVEL_NAMES);
    }

    /** 兼容旧构造签名；第二个参数已废弃，现在不再参与计算。 */
    public LevelRules(int maxGuildLevel, int ignoredMembersPerGuildLevel, int manorMaxLevelCap) {
        this(maxGuildLevel, manorMaxLevelCap);
    }

    public LevelRules {
        if (maxGuildLevel < 1 || manorMaxLevelCap < 1) {
            throw new IllegalArgumentException("等级规则参数必须 ≥1");
        }
        manorUnlockQuotas = Map.copyOf(manorUnlockQuotas == null ? Map.of() : manorUnlockQuotas);
        guildMemberCaps = Map.copyOf(guildMemberCaps == null ? Map.of() : guildMemberCaps);
        guildCityUnlockQuotas = Map.copyOf(guildCityUnlockQuotas == null ? Map.of() : guildCityUnlockQuotas);
        guildLevelNames = Map.copyOf(guildLevelNames == null ? Map.of() : guildLevelNames);
    }

    /** 给定公会等级时允许的成员名额（容量）。优先使用 levels.yml 显式表，缺省使用内置默认表。 */
    public int maxMembers(int guildLevel) {
        if (guildLevel < 1) {
            throw new IllegalArgumentException("guildLevel 必须 ≥1");
        }
        return valueAt(guildMemberCaps.isEmpty() ? DEFAULT_GUILD_MEMBER_CAPS : guildMemberCaps, guildLevel);
    }

    /** 给定庄园等级时允许的已解锁 chunk 总数。只走等级表，不按边长公式推导。 */
    public int manorQuotaCap(LayoutConfig layout, int manorLevel) {
        if (manorLevel < 1) {
            throw new IllegalArgumentException("manorLevel 必须 ≥1");
        }
        int cap = layout.plotChunks() * layout.plotChunks();
        if (manorLevel >= manorMaxLevelCap) {
            return cap;
        }
        int explicit = valueAt(manorUnlockQuotas.isEmpty() ? DEFAULT_MANOR_UNLOCK_QUOTAS : manorUnlockQuotas, manorLevel);
        return Math.min(Math.max(1, explicit), cap);
    }

    /** 给定公会等级时允许的主城已解锁 chunk 总数。只走等级表，不按等级数公式推导。 */
    public int cityQuotaCap(LayoutConfig layout, int guildLevel) {
        if (guildLevel < 1) {
            throw new IllegalArgumentException("guildLevel 必须 ≥1");
        }
        int cap = layout.mainCityMaxChunks() * layout.mainCityMaxChunks();
        int explicit = valueAt(guildCityUnlockQuotas.isEmpty() ? DEFAULT_GUILD_CITY_UNLOCK_QUOTAS : guildCityUnlockQuotas, guildLevel);
        return Math.min(Math.max(1, explicit), cap);
    }

    /** 公会等级显示名；未配置时返回 LvN。 */
    public String guildLevelName(int guildLevel) {
        Map<Integer, String> names = guildLevelNames.isEmpty() ? DEFAULT_GUILD_LEVEL_NAMES : guildLevelNames;
        return names.getOrDefault(guildLevel, "Lv" + guildLevel);
    }

    private static int valueAt(Map<Integer, Integer> source, int level) {
        TreeMap<Integer, Integer> sorted = new TreeMap<>(source);
        Map.Entry<Integer, Integer> floor = sorted.floorEntry(level);
        if (floor != null) {
            return floor.getValue();
        }
        return sorted.firstEntry().getValue();
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

    /** 默认：公会 5 个时代、庄园 20 级；容量与额度均来自等级表。 */
    public static LevelRules defaults() {
        return new LevelRules(DEFAULT_GUILD_MAX_LEVEL, DEFAULT_MANOR_MAX_LEVEL);
    }
}
