package org.windy.guildshelter.adapter.bukkit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 方块 id 名单匹配器：<b>精确 id 与正则混用</b>。精确 id 走 O(1) {@link Set}（热路径快），含正则元字符的条目
 * 编译成 {@link Pattern}（整串匹配、大小写不敏感）。供"主城禁放 / 路权禁放"等名单用——<b>两处各持一个独立实例</b>，
 * 互不影响（见 {@link ClaimGuard}）。
 *
 * <p>条目判定为正则：含 {@code . * + ? [ ] ( ) | ^ $ \ { }} 任一元字符；否则当精确 id（转小写）。
 * 例：{@code create:.*} 命中所有 Create 方块、{@code .*_shulker_box} 命中所有潜影盒、{@code minecraft:hopper} 精确命中漏斗。
 * 坏正则不抛错——退化成精确 id 兜底，避免一条写错让整张名单失效。
 */
public final class BlockMatcher {

    /** 检测条目是否含正则元字符（含则按正则编译，否则按精确 id）。 */
    private static final Pattern META = Pattern.compile("[.*+?\\[\\]()|^$\\\\{}]");

    private final Set<String> exact;
    private final List<Pattern> patterns;
    private final boolean empty;

    private BlockMatcher(Set<String> exact, List<Pattern> patterns) {
        this.exact = exact;
        this.patterns = patterns;
        this.empty = exact.isEmpty() && patterns.isEmpty();
    }

    /** 从配置条目集合构建（精确条目转小写入 Set，正则条目编译为大小写不敏感 Pattern）。 */
    public static BlockMatcher of(Collection<String> entries) {
        Set<String> exact = new HashSet<>();
        List<Pattern> patterns = new ArrayList<>();
        if (entries != null) {
            for (String raw : entries) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String s = raw.trim();
                if (META.matcher(s).find()) {
                    try {
                        patterns.add(Pattern.compile(s, Pattern.CASE_INSENSITIVE));
                    } catch (PatternSyntaxException e) {
                        exact.add(s.toLowerCase(Locale.ROOT)); // 坏正则当精确 id 兜底
                    }
                } else {
                    exact.add(s.toLowerCase(Locale.ROOT));
                }
            }
        }
        return new BlockMatcher(Set.copyOf(exact), List.copyOf(patterns));
    }

    public boolean isEmpty() {
        return empty;
    }

    /** {@code blockId}（形如 {@code minecraft:hopper}）是否命中名单：精确集合命中 或 任一正则整串匹配。 */
    public boolean matches(String blockId) {
        if (empty || blockId == null) {
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
}
