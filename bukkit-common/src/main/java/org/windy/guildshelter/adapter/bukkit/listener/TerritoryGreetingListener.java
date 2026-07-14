package org.windy.guildshelter.adapter.bukkit.listener;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.windy.guildshelter.adapter.bukkit.CityFlagCache;
import org.windy.guildshelter.adapter.bukkit.FakePlayerFilter;
import org.windy.guildshelter.adapter.bukkit.GuildWorldRegistry;
import org.windy.guildshelter.domain.model.GuildWorld;

/**
 * 公会领地<b>迎送词</b>：玩家进入某公会世界弹欢迎语、离开弹告别语（title 或聊天）。借鉴 HuskTowns 的
 * greeting/farewell（见 PLAN_GREETING.md）。
 *
 * <p>文案优先级：<b>该公会自定义</b>（会长 {@code /gs greeting|farewell} 写入 {@link CityFlagCache} 的
 * 保留键 {@code greeting}/{@code farewell}）→ 回退 config 默认模板。占位符 {@code %guild%}/{@code %player%}。
 * 当前会话先落"config 默认"这条链路即可生效；自定义命令为后续增量（读取路径此处已就绪）。
 *
 * <p>与 {@link GuildMotdListener}（公告，走聊天）正交：本类管进/出领地的 title 迎送，互不影响。
 */
public final class TerritoryGreetingListener implements Listener {

    /** 展示方式：标题 或 聊天行（不用 actionbar/Adventure，避开混合端 adventure 序列化器缺失问题）。 */
    public enum Mode { TITLE, CHAT }

    /**
     * {@link CityFlagCache} 里存自定义迎送词的保留键（值=显示文本）。<b>刻意带 {@code territory-} 前缀</b>避开
     * 既有<b>庄园级</b> {@code Flag.GREETING}/{@code FAREWELL}（那俩存于 {@code manor.flags}、按本键名解析）——
     * 二者本就存储隔离（城 flag vs 庄园 flag），加前缀再杜绝任何理论上的键名混淆。这里是<b>公会世界级</b>迎送。
     */
    public static final String KEY_GREETING = "territory-greeting";
    public static final String KEY_FAREWELL = "territory-farewell";

    private final GuildWorldRegistry registry;
    private final CityFlagCache cityFlags; // 自定义覆盖来源；可为 null（无则只用默认模板）
    private final boolean enabled;
    private final Mode mode;
    private final int fadeIn;
    private final int stay;
    private final int fadeOut;
    private final String defaultGreeting;
    private final String defaultFarewell;

    public TerritoryGreetingListener(GuildWorldRegistry registry, CityFlagCache cityFlags, boolean enabled,
                                     Mode mode, int fadeIn, int stay, int fadeOut,
                                     String defaultGreeting, String defaultFarewell) {
        this.registry = registry;
        this.cityFlags = cityFlags;
        this.enabled = enabled;
        this.mode = mode == null ? Mode.TITLE : mode;
        this.fadeIn = fadeIn;
        this.stay = stay;
        this.fadeOut = fadeOut;
        this.defaultGreeting = defaultGreeting == null ? "" : defaultGreeting;
        this.defaultFarewell = defaultFarewell == null ? "" : defaultFarewell;
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (!enabled) {
            return;
        }
        Player player = event.getPlayer();
        if (!FakePlayerFilter.isRealPlayer(player)) {
            return;
        }
        GuildWorld now = registry.get(player.getWorld().getName());
        GuildWorld from = registry.get(event.getFrom().getName());
        if (now != null && (from == null || !from.guild().equals(now.guild()))) {
            show(player, now, true);  // 进入了一个（新的）公会世界 → 欢迎
        } else if (now == null && from != null) {
            show(player, from, false); // 从公会世界离开到非公会世界 → 告别
        }
        // 两个公会世界间互跳：只对新世界弹欢迎（上面第一支），不再补旧世界告别，免得一次跳出两条 title。
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) {
            return;
        }
        Player player = event.getPlayer();
        if (!FakePlayerFilter.isRealPlayer(player)) {
            return;
        }
        GuildWorld gw = registry.get(player.getWorld().getName());
        if (gw != null) {
            show(player, gw, true); // 直接登录在公会世界里 → 欢迎
        }
    }

    private void show(Player player, GuildWorld gw, boolean greeting) {
        String template = override(gw, greeting);
        if (template == null || template.isBlank()) {
            template = greeting ? defaultGreeting : defaultFarewell;
        }
        if (template.isBlank()) {
            return; // 该方向未配置 → 不弹
        }
        String text = ChatColor.translateAlternateColorCodes('&',
                template.replace("%guild%", gw.guild().value()).replace("%player%", player.getName()));
        if (mode == Mode.TITLE) {
            player.sendTitle(text, "", fadeIn, stay, fadeOut); // 5 参 legacy 重载，混合端通用
        } else {
            player.sendMessage(text);
        }
    }

    /** 取该公会自定义迎送词（会长写入 CityFlagCache 保留键）；无缓存或未设 → null（回退默认模板）。 */
    private String override(GuildWorld gw, boolean greeting) {
        if (cityFlags == null) {
            return null;
        }
        return cityFlags.flags(gw.guild()).get(greeting ? KEY_GREETING : KEY_FAREWELL);
    }
}
