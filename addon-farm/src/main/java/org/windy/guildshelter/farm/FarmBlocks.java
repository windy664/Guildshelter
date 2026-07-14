package org.windy.guildshelter.farm;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 农事方块名单匹配（字面 + 正则混用），并提供"是否成熟"判定。附属自带，不依赖主插件内部类。
 * 与核心 BlockMatcher 同款语义：含正则元字符的条目按正则整串匹配（大小写不敏感），否则当精确 id。
 */
final class FarmBlocks {

    private static final Pattern META = Pattern.compile("[.*+?\\[\\]()|^$\\\\{}]");

    private final Set<String> exact = new HashSet<>();
    private final List<Pattern> patterns = new ArrayList<>();

    FarmBlocks(List<String> entries) {
        for (String raw : entries) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String s = raw.trim();
            if (META.matcher(s).find()) {
                try {
                    patterns.add(Pattern.compile(s, Pattern.CASE_INSENSITIVE));
                } catch (PatternSyntaxException e) {
                    exact.add(s.toLowerCase(Locale.ROOT));
                }
            } else {
                exact.add(s.toLowerCase(Locale.ROOT));
            }
        }
    }

    /** blockId（如 minecraft:wheat）是否在名单内。 */
    boolean matches(String blockId) {
        if (blockId == null) {
            return false;
        }
        String id = blockId.toLowerCase(Locale.ROOT);
        if (exact.contains(id)) {
            return true;
        }
        for (Pattern p : patterns) {
            if (p.matcher(id).matches()) {
                return true;
            }
        }
        return false;
    }

    /** 方块命名空间 id（小写，如 minecraft:wheat）。 */
    static String idOf(Material m) {
        return m.getKey().toString();
    }

    /**
     * 作物是否"成熟"：Ageable 且 age 达到 max（小麦/胡萝卜等）。非 Ageable（如甘蔗/西瓜本体/耕地）一律视为可收（true），
     * 由名单决定能不能动；only-mature 时仅对 Ageable 生效——非 Ageable 农作物没有"未成熟"概念。
     */
    static boolean isMature(Block block) {
        if (block.getBlockData() instanceof Ageable age) {
            return age.getAge() >= age.getMaximumAge();
        }
        return true;
    }
}
