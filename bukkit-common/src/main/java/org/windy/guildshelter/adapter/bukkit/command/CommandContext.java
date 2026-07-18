package org.windy.guildshelter.adapter.bukkit.command;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.windy.guildshelter.adapter.bukkit.BlockMatcher;
import org.windy.guildshelter.adapter.bukkit.CityFlagCache;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.CityPlotCache;
import org.windy.guildshelter.adapter.bukkit.GridAsciiMap;
import org.windy.guildshelter.adapter.bukkit.GuildWorldRegistry;
import org.windy.guildshelter.adapter.bukkit.ManorEntityCensus;
import org.windy.guildshelter.adapter.bukkit.ManorRoles;
import org.windy.guildshelter.adapter.bukkit.MergeAwareClassifier;
import org.windy.guildshelter.adapter.bukkit.MergeRegistry;
import org.windy.guildshelter.adapter.bukkit.MultiManorSettings;
import org.windy.guildshelter.adapter.bukkit.holo.HologramBackend;
import org.windy.guildshelter.adapter.bukkit.listener.ManorAccessListener;
import org.windy.guildshelter.adapter.bukkit.map.MapClaimChannel;
import org.windy.guildshelter.adapter.bukkit.world.WorldManager;
import org.windy.guildshelter.adapter.bungee.ProxyChannel;
import org.windy.guildshelter.domain.flag.Flag;
import org.windy.guildshelter.domain.layout.Classification;
import org.windy.guildshelter.domain.layout.LayoutCalculator;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;
import org.windy.guildshelter.domain.port.CampSpawnStore;
import org.windy.guildshelter.domain.port.CityHologramStore;
import org.windy.guildshelter.domain.port.GuildProvider;
import org.windy.guildshelter.domain.port.GuildRepository;
import org.windy.guildshelter.domain.port.ManorRepository;
import org.windy.guildshelter.domain.port.SchematicStore;
import org.windy.guildshelter.domain.port.ui.UiActionRouter;
import org.windy.guildshelter.domain.rule.LevelRules;
import org.windy.guildshelter.service.AuditLog;
import org.windy.guildshelter.service.GuildService;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 命令上下文：所有子命令共享的依赖容器。
 *
 * <p>构造时注入核心服务；可选服务通过 setter 延迟注入（在 Plugin 启动流程中按阶段调用）。
 * 替代原 GsCommand 的 20+ 个 setter 字段。
 */
public final class CommandContext {

    // ── 核心服务（构造时必须） ──
    public final WorldManager worlds;
    public final GuildRepository guilds;
    public final ManorRepository manors;
    public final GuildService service;
    public final GuildWorldRegistry registry;
    public final LevelRules levels;
    public final ManorEntityCensus census;
    public final MergeRegistry merges;
    public final ProxyChannel proxyChannel;
    public final String serverName;
    public final Logger logger;
    public final Plugin plugin;
    public final MultiManorSettings multiManor;

    // ── 可选服务（setter 延迟注入） ──
    public GuildProvider guildProvider = GuildProvider.NONE;
    public ManorAccessListener accessListener;
    public SchematicStore schematicStore;
    public CampSpawnStore campSpawn;
    public CityFlagCache cityFlagCache;
    public HologramBackend holoBackend;
    public CityHologramStore holoStore;
    public boolean holoEnabled;
    public int holoMaxPerGuild;
    public BlockMatcher holoPapiWhitelist;
    public MapClaimChannel mapChannel;
    public AuditLog auditLog = AuditLog.disabled();
    public CityPlotCache cityPlotCache;
    public boolean cityPlotsEnabled;
    public int cityPlotsMaxPerGuild;
    public UiActionRouter uiRouter;

    // ── 跨命令共享状态 ──
    /** 玩家待确认操作队列。 */
    public final Map<UUID, PendingAction> pendingConfirm = new ConcurrentHashMap<>();
    /** 庄园临时开放状态："guildId:slot" → 过期时间戳。0 = 永久开放。 */
    public final Map<String, Long> openPlots = new ConcurrentHashMap<>();

    public CommandContext(WorldManager worlds, GuildRepository guilds, ManorRepository manors,
                          GuildService service, GuildWorldRegistry registry,
                          LevelRules levels, ManorEntityCensus census, MergeRegistry merges,
                          ProxyChannel proxyChannel, String serverName, Logger logger,
                          Plugin plugin) {
        this.worlds = worlds;
        this.guilds = guilds;
        this.manors = manors;
        this.service = service;
        this.registry = registry;
        this.levels = levels;
        this.census = census;
        this.merges = merges;
        this.proxyChannel = proxyChannel;
        this.serverName = serverName;
        this.logger = logger;
        this.plugin = plugin;
        this.multiManor = MultiManorSettings.fromConfig(plugin);
    }

    // ── 便捷方法（跨子命令复用） ──

    /** 玩家脚下所在庄园格对应的庄园（不论归属）。 */
    public Optional<Manor> manorAt(Player player) {
        GuildWorld gw = registry.get(player.getWorld().getName());
        if (gw == null) return Optional.empty();
        int lx = (player.getLocation().getBlockX() >> 4) - gw.originChunkX();
        int lz = (player.getLocation().getBlockZ() >> 4) - gw.originChunkZ();
        var layout = new LayoutCalculator(gw.layout());
        var slotOpt = layout.slotAt(lx, lz);
        if (slotOpt.isEmpty()) return Optional.empty();
        return manors.findBySlot(gw.guild(), slotOpt.getAsInt());
    }

    /** 玩家当前拥有的庄园（脚下所在格，仅自己的）。 */
    public Optional<Manor> currentOwnManor(Player player) {
        return manorAt(player).filter(m -> m.owner().equals(PlayerRef.of(player.getUniqueId())));
    }

    // ── 共享工具方法（从 GsCommand 迁移） ──

    /** 确保公会世界已加载并注册。失败返回 null 并给 sender 发消息。 */
    public GuildWorld ensureLoadedWorld(org.bukkit.command.CommandSender sender, GuildId guild) {
        GuildWorld gw = guilds.find(guild).orElse(null);
        if (gw == null) {
            sender.sendMessage(Messages.get("error.world_not_exist"));
            return null;
        }
        gw = worlds.ensureWorld(gw);
        guilds.save(gw);
        registry.register(gw);
        if (org.bukkit.Bukkit.getWorld(gw.worldName()) == null) {
            sender.sendMessage(Messages.get("error.world_load_failed", gw.worldName()));
            return null;
        }
        return gw;
    }

    /** 营地传送点位置（成员/访客）。未设置返回 null。 */
    public org.bukkit.Location campSpawnLoc(org.bukkit.World world, GuildWorld gw,
                                             CampSpawnStore.Type type) {
        if (campSpawn == null || world == null) return null;
        return campSpawn.get(gw.guild(), type).map(s -> {
            world.loadChunk((int) Math.floor(s.x()) >> 4, (int) Math.floor(s.z()) >> 4, true);
            return new org.bukkit.Location(world, s.x(), s.y(), s.z(), s.yaw(), s.pitch());
        }).orElse(null);
    }

    /** 在日志中输出公会占用地图。 */
    public void logMap(GuildId guild) {
        GuildWorld gw = guilds.find(guild).orElse(null);
        if (gw == null) return;
        java.util.Set<Integer> occupied = new java.util.HashSet<>();
        for (Manor m : manors.findAll(guild)) occupied.add(m.slot());
        int capacity = service.effectiveCapacity(gw);
        var layout = new LayoutCalculator(gw.layout());
        int cityQuota = gw.cityQuotaCap(levels);
        for (String line : GridAsciiMap.render(layout, gw, occupied, capacity, cityQuota)) {
            logger.info(line);
        }
    }

    /** 合并感知的分类（脚下 chunk 属于哪个庄园/主城/路）。 */
    public Classification classify(GuildWorld gw, int chunkX, int chunkZ) {
        var layout = new LayoutCalculator(gw.layout());
        return new MergeAwareClassifier(layout, merges, gw.guild())
                .classify(chunkX, chunkZ);
    }

    /** Tab 补全用：某玩家在某公会拥有的全部庄园 slot。 */
    public java.util.List<String> targetSlots(String guildArg, String playerArg) {
        java.util.List<String> out = new java.util.ArrayList<>();
        if (guildArg == null || guildArg.isBlank() || playerArg == null || playerArg.isBlank()) return out;
        PlayerRef ref = PlayerRef.of(org.bukkit.Bukkit.getOfflinePlayer(playerArg).getUniqueId());
        for (Manor m : manors.findAllByOwner(new GuildId(guildArg), ref)) {
            out.add(String.valueOf(m.slot()));
        }
        return out;
    }

    /** 检查 flag 是否在信任者可设集合内。 */
    public static boolean isTrustedFlag(Flag flag) {
        return TRUSTED_FLAG_IDS.contains(flag.id());
    }

    /** 检查 flag 是否在主城可设集合内。 */
    public static boolean isCityFlag(Flag flag) {
        return CITY_FLAG_IDS.contains(flag.id());
    }

    // ── 常量集（从 GsCommand 迁移） ──

    static final java.util.Set<String> TRUSTED_FLAG_IDS = java.util.Set.of(
            "pvp", "pve", "pve-monster", "pve-player", "use", "container", "item-frame", "vehicle-use",
            "greeting", "farewell", "titles", "notify-enter", "notify-leave",
            "fly", "feed", "heal", "invincible",
            "deny-entry", "deny-exit", "description");

    static final java.util.Set<String> CITY_FLAG_IDS = java.util.Set.of(
            "redstone", "liquid-flow", "crop-grow", "grass-grow", "vine-grow", "mycelium-grow",
            "ice-form", "ice-melt", "snow-form", "snow-melt", "leaf-decay",
            "pvp", "pve", "pve-monster", "pve-player",
            "use", "container", "item-frame", "vehicle-use", "vehicle-place", "vehicle-destroy",
            "armor-stand-interact", "hanging-break", "hanging-place",
            "pressure-plate", "button", "lever", "door", "trapdoor", "fence-gate",
            "note-block", "jukebox", "beacon",
            "mob-spawn", "mob-damage", "creeper-damage", "enderman-grief", "wither-damage",
            "crop-trample", "leaf-trample", "soil-change",
            "fly", "feed", "heal", "invincible",
            "deny-entry", "deny-exit",
            "greeting", "farewell", "titles",
            "notify-enter", "notify-leave",
            "description");
}
