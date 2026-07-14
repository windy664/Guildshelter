package org.windy.guildshelter;

import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.windy.guildshelter.adapter.bukkit.ClaimGuard;
import org.windy.guildshelter.adapter.bukkit.GuildShelterConfig;
import org.windy.guildshelter.adapter.bukkit.GuildShelterPapi;
import org.windy.guildshelter.adapter.bukkit.GuildUpkeepTask;
import org.windy.guildshelter.adapter.bukkit.GuildWorldRegistry;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.SupervisorCache;
import org.windy.guildshelter.adapter.bukkit.WorldCache;
import org.windy.guildshelter.adapter.bukkit.gui.BukkitInventoryUi;
import org.windy.guildshelter.adapter.bukkit.gui.ModChannelUi;
import org.windy.guildshelter.adapter.bukkit.gui.VanillaGuiListener;
import org.windy.guildshelter.adapter.bukkit.gui.YamlGuiLoader;
import org.windy.guildshelter.domain.port.ui.UiActionRouter;
import org.windy.guildshelter.domain.port.ui.UiBackend;
import org.windy.guildshelter.adapter.bukkit.XaeroIntegration;
import org.windy.guildshelter.adapter.bukkit.ManorLimitTask;
import org.windy.guildshelter.adapter.bukkit.ManorUpgradeCommandHook;
import org.windy.guildshelter.adapter.bukkit.WorldOptimizer;
import org.windy.guildshelter.adapter.bukkit.PerformanceBroadcastTask;
import org.windy.guildshelter.adapter.bukkit.ManorChunkManager;
import org.windy.guildshelter.adapter.bukkit.InteractionPolicy;
import org.windy.guildshelter.adapter.bukkit.GuildMemberCache;
import org.windy.guildshelter.adapter.bukkit.ManorBuffTask;
import org.windy.guildshelter.adapter.bukkit.ManorEntityCensus;
import org.windy.guildshelter.adapter.bukkit.ManorLookup;
import org.windy.guildshelter.adapter.bukkit.VisitCounter;
import org.windy.guildshelter.adapter.bukkit.MergeRegistry;
import org.windy.guildshelter.adapter.bukkit.VaultEconomy;
import org.windy.guildshelter.adapter.bukkit.command.GsCommand;
import org.windy.guildshelter.adapter.bukkit.listener.ManorAccessListener;
import org.windy.guildshelter.adapter.bukkit.listener.GuildMotdListener;
import org.windy.guildshelter.adapter.bukkit.listener.ManorCapListener;
import org.windy.guildshelter.adapter.bukkit.listener.ManorCommandListener;
import org.windy.guildshelter.adapter.bukkit.listener.ManorEntityListener;
import org.windy.guildshelter.adapter.bukkit.listener.ManorEnvListener;
import org.windy.guildshelter.adapter.bukkit.listener.ManorFireListener;
import org.windy.guildshelter.adapter.bukkit.listener.ManorFlagListener;
import org.windy.guildshelter.adapter.bukkit.listener.ManorParticleTask;
import org.windy.guildshelter.adapter.bukkit.listener.ManorPlayerListener;
import org.windy.guildshelter.adapter.bukkit.listener.ManorProtectionListener;
import org.windy.guildshelter.adapter.bukkit.listener.RegionTitleListener;
import org.windy.guildshelter.adapter.bukkit.world.BukkitTerrainPreparer;
import org.windy.guildshelter.adapter.bukkit.world.WorldManager;
import org.windy.guildshelter.adapter.provider.LegendaryGuildListener;
import org.windy.guildshelter.adapter.provider.PlayerGuildListener;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.port.EconomyPort;
import org.windy.guildshelter.domain.port.GuildRepository;
import org.windy.guildshelter.domain.port.ManorRepository;
import org.windy.guildshelter.domain.port.TerrainPreparer;
import org.windy.guildshelter.domain.rule.PermissionRules;
import org.windy.guildshelter.platform.PlatformBindings;
import org.windy.guildshelter.adapter.bukkit.SchematicStores;
import org.windy.guildshelter.domain.port.SchematicStore;
import org.windy.guildshelter.domain.port.ManorMover;
import org.windy.guildshelter.domain.port.ModDataMoverRegistry;
import org.windy.guildshelter.service.GuildService;
import org.windy.guildshelter.adapter.bungee.ProxyChannel;
import org.windy.guildshelter.persistence.Storage;
import org.windy.guildshelter.persistence.StorageFactory;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Bukkit 端入口（装配根）。
 *
 * <p>已接入：配置 → 持久层（SQLite/MySQL/FlatFile）→ WorldManager + 整地 → /gs 命令 → 保护监听器
 * → 公会插件 provider → 性能/社交/搬家/Schematic/PAPI/跨服 → UI 后端（原版 Inventory 兜底 + 模组联动桩）。
 * 待接入：GUI 入口命令（/gs gui，UI 后端与路由已预留，见 {@link #uiBackend()}/{@link #uiRouter()}）。
 */
public abstract class GuildShelterPlugin extends JavaPlugin {

    private static GuildShelterPlugin instance;

    /**
     * 载体接缝（PLAN_MODULES.md §4）：按载体分流的决策（整地/搬家/原生保护/群系采样/mod 搬运/UI auto）
     * 全收口到此。两个薄子类（bukkit / neoforge_26_2）各返回自己的实现。
     */
    protected abstract PlatformBindings createBindings();

    private Storage storage;
    private org.windy.guildshelter.service.AuditLog auditLog; // 领地审计（config audit.enabled）
    private WorldManager worldManager;
    private ClaimGuard claimGuard;
    private ManorLookup manorLookup;
    private InteractionPolicy interactionPolicy;
    private ManorEntityCensus entityCensus;
    private org.windy.guildshelter.adapter.bukkit.world.LazyRoadPaver lazyRoadPaver;
    private MergeRegistry mergeRegistry;
    private WorldCache worldCache;
    private SupervisorCache supervisorCache;
    private org.windy.guildshelter.adapter.bukkit.CityTrustCache cityTrustCache;
    private org.windy.guildshelter.adapter.bukkit.RoadPermitCache roadPermitCache;
    private YamlGuiLoader guiLoader;
    private UiActionRouter uiRouter;
    private UiBackend uiBackend;

    public static GuildShelterPlugin get() {
        return instance;
    }

    /** 供 NeoForge 端(混合端)的保护监听器在事件时取用共享判定;onEnable 前为 null。 */
    public static ClaimGuard protectionGuard() {
        return instance == null ? null : instance.claimGuard;
    }

    /** 供 NeoForge 端的 flag 后端取用庄园解析;onEnable 前/未启用为 null。 */
    public static ManorLookup manorLookup() {
        return instance == null ? null : instance.manorLookup;
    }

    /** 供 NeoForge 端(混合端)的 ChunkEvent.Load 原生监听取用惰性铺路决策;onEnable 前/未启用为 null。 */
    public static org.windy.guildshelter.adapter.bukkit.world.LazyRoadPaver lazyRoadPaver() {
        return instance == null ? null : instance.lazyRoadPaver;
    }

    /** 供 NeoForge 端(混合端)的保护监听器取用"访客交互放宽"判定;onEnable 前/未启用为 null。 */
    public static InteractionPolicy interactionPolicy() {
        return instance == null ? null : instance.interactionPolicy;
    }

    /**
     * 庄园实体计数服务。caps 拦截用,亦作可复用 API 供未来"家园卡/评分"等按实体数量判断的功能取数。
     * onEnable 前/保护未启用为 null。
     */
    public static ManorEntityCensus entityCensus() {
        return instance == null ? null : instance.entityCensus;
    }

    /** 合并缓存注册表（provider 解散公会时需清理缓存）。 */
    public static MergeRegistry mergeRegistry() {
        return instance == null ? null : instance.mergeRegistry;
    }

    /** 主城信任缓存（命令授权、provider 解散/退会时清理）。onEnable 前为 null。 */
    public static org.windy.guildshelter.adapter.bukkit.CityTrustCache cityTrustCache() {
        return instance == null ? null : instance.cityTrustCache;
    }

    /** 限时路权缓存（命令授予/撤销）。onEnable 前为 null。 */
    public static org.windy.guildshelter.adapter.bukkit.RoadPermitCache roadPermitCache() {
        return instance == null ? null : instance.roadPermitCache;
    }

    /** YAML GUI 加载器（从 gui.yml 读菜单定义）。 */
    public static YamlGuiLoader guiLoader() {
        return instance == null ? null : instance.guiLoader;
    }

    /**
     * UI 动作路由表（预留）。未来 {@code /gs gui} 命令在此注册各菜单的 onBuild/onAction，
     * 无需改动 UI 后端。onEnable 前为 null。
     */
    public static UiActionRouter uiRouter() {
        return instance == null ? null : instance.uiRouter;
    }

    /**
     * 当前 UI 渲染后端（原版 Inventory 兜底 / 模组联动，按 config {@code ui.backend} 选）。
     * 未来打开菜单走 {@code uiBackend().open(viewer, view)}。onEnable 前为 null。
     */
    public static UiBackend uiBackend() {
        return instance == null ? null : instance.uiBackend;
    }

    /** 给新分配庄园的玩家发欢迎消息。 */
    public static void sendWelcome(Player player, String guildName, int slot) {
        if (instance == null) return;
        player.sendMessage(Messages.get("success.welcome", guildName, slot));
    }

    @Override
    public void onEnable() {
        instance = this;
        final PlatformBindings bindings = createBindings(); // 载体接缝（整地/搬家/保护/采样/UI 分流）
        // 启动横幅在 onEnable 末尾打印（彼时存储/保护/宿主等信息齐全），见文件尾。

        getDataFolder().mkdirs();
        saveDefaultConfig();
        // 加载语言文件
        Messages.load(getConfig().getString("language", "zh_CN"), getDataFolder());

        // 等级系统独立配置 levels.yml（首启释放默认文件，并从旧 config.yml 迁移已有等级配置）。
        org.bukkit.configuration.file.FileConfiguration levelsCfg = loadLevelsConfig();
        org.bukkit.configuration.file.FileConfiguration serverCfg = loadServerConfig();
        GuildShelterConfig config = GuildShelterConfig.from(getConfig(), levelsCfg, serverCfg);

        // 存储后端按 config 选(sqlite/mysql/flatfile);领域只认端口仓库。
        this.storage = StorageFactory.create(config.storage(), getDataFolder().toPath(), config.layout());
        GuildRepository guilds = storage.guilds();
        ManorRepository manors = storage.manors();
        getLogger().info("存储后端: " + config.storage().type());

        // 代理跨服通道（PluginMessage）
        ProxyChannel proxyChannel = ProxyChannel.create(config.crossServer(), this);
        if (proxyChannel.isAvailable()) {
            getLogger().info("跨服模式已启用（PluginMessage，服务器名: " + config.serverName() + "）");
        }

        this.worldManager = new WorldManager(config.levels(), config.oceanReseed(), config.iris(), getLogger());
        this.worldManager.setBiomeSampler(bindings.biomeSampler()); // 混合端注入群系采样，纯 Bukkit 为 null
        this.worldManager.setPlugin(this); // 异步建 Iris 世界用其调度器
        // 落点落脚台：home/spawn 等落点地表是水/岩浆时铺一格(默认玻璃)防落水；none/空=关闭。
        String padId = getConfig().getString("safe-landing.water-pad-block", "minecraft:glass");
        org.bukkit.Material landingPad = null;
        if (padId != null && !padId.isBlank() && !padId.equalsIgnoreCase("none")) {
            landingPad = org.bukkit.Material.matchMaterial(padId);
            if (landingPad == null || !landingPad.isBlock()) {
                getLogger().warning("[GuildShelter] safe-landing.water-pad-block 无效方块: " + padId + "，回退玻璃。");
                landingPad = org.bukkit.Material.GLASS;
            }
        }
        this.worldManager.setWaterLandingPad(landingPad);
        // 整地按载体分流（接缝）
        String roadBlock = getConfig().getString("road-surface-block", "minecraft:dirt_path");
        String bridgeBlock = getConfig().getString("road-bridge-block", "auto");
        String bridgeRail = getConfig().getString("road-bridge-rail-block", "auto");
        var wall = config.cityWall();
        // 原始整地器（未套 Iris 预生成装饰器）：惰性铺路用它——区块已生成在内存里，直接写方块不该再 pregen。
        TerrainPreparer rawTerrain = bindings.terrain(this, roadBlock, bridgeBlock, bridgeRail,
                wall.enabled(), wall.block(), wall.height());
        // 惰性铺路节流是 Iris 内部调度参数，保持代码默认值，不暴露给服主配置。
        // Iris 世界：套一层异步预生成装饰器——整地/虚空平台前先用 Iris 多线程预生成器把区域生成好，
        // 避免在主线程同步 getChunk/loadChunk 触发 Iris 重型生成（建会后卡顿的根因）。非 Iris 原样返回。
        // 注意：路网【不再】走预生成提前铺满（那会强制生成整片世界、违背 Iris lazy-gen），改由惰性铺路按需铺。
        TerrainPreparer terrain = org.windy.guildshelter.adapter.bukkit.world.IrisPregenTerrainPreparer.wrap(
                this, rawTerrain, config.iris().enabled());
        getLogger().info("整地：" + (bindings.isHybrid() ? "NeoForge 原生端（混合端）。" : "Bukkit 高度图端。"));

        GuildService service = new GuildService(guilds, manors, worldManager, terrain,
                config.layout(), config.levels(), config.terrainPrep(), getLogger());

        // 领地审计日志（借鉴 HuskTowns town logs）：异步写关键领地操作，/gs log 查看。config 关则空操作。
        if (getConfig().getBoolean("audit.enabled", true)) {
            this.auditLog = org.windy.guildshelter.service.AuditLog.enabled(storage.audit(), getLogger());
            service.setAuditLog(this.auditLog);
            // 保留期清理：启动先清一次，再每天清一次（异步线程跑，不堵主线程）。
            int retentionDays = Math.max(1, getConfig().getInt("audit.retention-days", 30));
            long retainMs = retentionDays * 24L * 60 * 60 * 1000;
            getServer().getScheduler().runTaskTimerAsynchronously(this,
                    () -> this.auditLog.purgeOld(System.currentTimeMillis() - retainMs),
                    20L * 10, 20L * 60 * 60 * 24); // 启动后 10s，之后每 24h
            getLogger().info("[GuildShelter] 领地审计日志已启用（保留 " + retentionDays + " 天）。");
        } else {
            this.auditLog = org.windy.guildshelter.service.AuditLog.disabled();
        }

        // 对外领域事件出口（PLAN_API.md Phase 1b）：把核心内部的建会/解锁/升级动作转成 Bukkit 事件给附属监听。
        // 成员入会/退会/解散已走 MembershipChangeListener 的事件，不在此重复。
        service.setDomainEventSink(new org.windy.guildshelter.domain.port.DomainEventSink() {
            @Override public void onGuildCreated(org.windy.guildshelter.domain.model.GuildId g) {
                fireGuildEvent(new org.windy.guildshelter.api.event.GuildCampCreatedEvent(toRef(g)));
            }
            @Override public void onChunkUnlocked(org.windy.guildshelter.domain.model.GuildId g, int slot,
                                                  int cx, int cz, java.util.UUID player) {
                fireGuildEvent(new org.windy.guildshelter.api.event.ChunkUnlockedEvent(toRef(g), slot, cx, cz, player));
            }
            @Override public void onManorUpgraded(org.windy.guildshelter.domain.model.GuildId g, int slot,
                                                  int oldLevel, int newLevel, java.util.UUID owner) {
                fireGuildEvent(new org.windy.guildshelter.api.event.ManorUpgradedEvent(toRef(g), slot, oldLevel, newLevel, owner));
            }
            @Override public void onGuildUpgraded(org.windy.guildshelter.domain.model.GuildId g, int newLevel) {
                fireGuildEvent(new org.windy.guildshelter.api.event.GuildUpgradedEvent(toRef(g), newLevel));
            }
        });

        // 庄园升级回调：升到对应等级时由控制台执行对应命令（levels.yml: manor.upgrade-commands）。
        ManorUpgradeCommandHook upgradeHook = ManorUpgradeCommandHook.fromConfig(this, levelsCfg);
        if (upgradeHook != null) {
            service.setUpgradeHook(upgradeHook);
            getLogger().info("庄园升级命令回调已启用（levels.yml: manor.upgrade-commands）。");
        }

        // 搬家系统
        var moveConfig = config.move();
        if (moveConfig.enabled()) {
            ManorMover mover = bindings.manorMover(this);
            getLogger().info("搬家系统: " + (bindings.isHybrid()
                    ? "NeoForge 原生端（chunk 级复制）。" : "Bukkit/WorldEdit（clipboard 复制）。"));
            ModDataMoverRegistry modDataMovers = new ModDataMoverRegistry();
            bindings.registerModDataMovers(modDataMovers, this); // 混合端注册 RS2 等 mod 数据搬运

            EconomyPort moveEconomy = VaultEconomy.tryCreate(getLogger());
            java.nio.file.Path worldContainer = getServer().getWorldContainer().toPath();
            service.setManorMover(mover, moveEconomy, moveConfig.cost(), moveConfig.cooldownDays(),
                    modDataMovers, worldContainer);
        }

        GuildWorldRegistry registry = new GuildWorldRegistry();
        // 惰性铺路决策器（Iris 世界随区块自然生成顺手铺路，零强制生成；用【未套预生成】的 rawTerrain）。
        this.lazyRoadPaver = new org.windy.guildshelter.adapter.bukkit.world.LazyRoadPaver(
                registry, rawTerrain, worldManager::lazilyGenerated,
                config.terrainPrep() != org.windy.guildshelter.domain.model.TerrainPrepMode.NONE);
        // 注册区块生成钩子：混合端走原生 ChunkEvent.Load，纯 Bukkit 走 ChunkLoadEvent。
        if (!bindings.registerLazyRoadPaver(this)) {
            getServer().getPluginManager().registerEvents(
                    new org.windy.guildshelter.adapter.bukkit.world.LazyRoadPaveListener(this.lazyRoadPaver), this);
        }
        getLogger().info("惰性铺路已启用（Iris 世界随区块生成按需铺路，不预生成）。");
        // 统一配额解析器（优化量 + 机器；等级基础 + 管理员增量 + 玩家自调 cap）；注入 census 供上限拦截。
        this.entityCensus = new ManorEntityCensus(registry, config.performance().quotas(), config.cityLimits());

        // 合并缓存
        this.mergeRegistry = new MergeRegistry(manors);
        for (GuildWorld gw : guilds.findAll()) {
            List<Integer> slots = manors.findAll(gw.guild()).stream()
                    .map(org.windy.guildshelter.domain.model.Manor::slot).toList();
            mergeRegistry.load(gw.guild(), slots);
        }

        this.worldCache = new WorldCache(this.mergeRegistry);
        this.worldCache.setManorRepository(manors);
        this.supervisorCache = new SupervisorCache();
        GuildMemberCache memberCache = new GuildMemberCache(manors, guilds.findAll());

        // 宿主公会 provider（会长角色 + 人数上限），早于 ClaimGuard 选好以便注入主城权限判定。
        org.windy.guildshelter.domain.port.GuildProvider guildProvider;
        String guildSource; // 启动横幅用
        if (getServer().getPluginManager().getPlugin("PlayerGuild") != null) {
            guildProvider = new org.windy.guildshelter.adapter.provider.PlayerGuildProvider();
            guildSource = "PlayerGuild（会长角色 + 人数上限）";
            getLogger().info("宿主能力来源: " + guildSource + "。");
        } else if (getServer().getPluginManager().getPlugin("LegendaryGuildRemapped") != null) {
            guildProvider = new org.windy.guildshelter.adapter.provider.LegendaryGuildProvider();
            guildSource = "LegendaryGuild（会长 owner + 人数上限）";
            getLogger().info("宿主能力来源: " + guildSource + "。");
        } else {
            guildProvider = org.windy.guildshelter.domain.port.GuildProvider.NONE;
            guildSource = "无（仅 /gs admin 手动管理）";
        }
        service.setGuildProvider(guildProvider);
        this.worldManager.setGuildProvider(guildProvider);

        // 主城信任缓存（会长额外信任的会内成员可建主城）；启动全量加载。
        this.cityTrustCache = new org.windy.guildshelter.adapter.bukkit.CityTrustCache(
                storage.cityTrust(), guilds.findAll());

        // 主城 flag 缓存（会长/副会长设的主城 flag，热路径 resolveFlag 用）；启动全量加载。
        final org.windy.guildshelter.adapter.bukkit.CityFlagCache cityFlagCache =
                new org.windy.guildshelter.adapter.bukkit.CityFlagCache(storage.cityFlags(), guilds.findAll());
        // 主城子地块缓存（会长把已解锁主城地委托给成员开店）；启动全量加载。config 关则不建（命令/守卫不接）。
        final boolean cityPlotsEnabled = getConfig().getBoolean("city-plots.enabled", true);
        final org.windy.guildshelter.adapter.bukkit.CityPlotCache cityPlotCache = cityPlotsEnabled
                ? new org.windy.guildshelter.adapter.bukkit.CityPlotCache(storage.cityPlots(), guilds.findAll())
                : null;

        // 第三方建造决策注册中心（PLAN_API.md Phase 4）：API 注册 / ClaimGuard 询问 共用同一实例。
        final org.windy.guildshelter.adapter.bukkit.BuildCheckRegistry buildCheckRegistry =
                new org.windy.guildshelter.adapter.bukkit.BuildCheckRegistry();

        // 对外只读 API（PLAN_API.md 第一期）：注册到 Bukkit ServicesManager，供附属插件（如农场加速）消费。
        // 走与 ClaimGuard 同一套热路径缓存，零额外查询；只回 DTO，不暴露内部模型。
        org.windy.guildshelter.api.GuildShelterAPI api = new org.windy.guildshelter.adapter.bukkit.api.GuildShelterApiImpl(
                registry, this.worldCache, cityFlagCache, memberCache, guilds, manors, service, buildCheckRegistry);
        getServer().getServicesManager().register(org.windy.guildshelter.api.GuildShelterAPI.class,
                api, this, org.bukkit.plugin.ServicePriority.Normal);
        getLogger().info("[GuildShelter] 对外 API 已注册（ServicesManager: GuildShelterAPI）。");
        // 附属插件禁用时自动注销其 BuildCheckProvider（即便它没主动调 unregister），避免悬挂引用。
        getServer().getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onPluginDisable(org.bukkit.event.server.PluginDisableEvent e) {
                buildCheckRegistry.unregister(e.getPlugin());
            }
        }, this);

        // 限时路权缓存（管理员授予的临时路上建造权，惰性过期）；启动全量加载。
        this.roadPermitCache = new org.windy.guildshelter.adapter.bukkit.RoadPermitCache(storage.roadPermit());

        // 主城悬浮字后端：检测到 DecentHolograms 才构造实现类（否则 NOOP，绝不触发 DH 类加载）。
        final org.windy.guildshelter.adapter.bukkit.holo.HologramBackend holoBackend =
                (config.holograms().enabled() && getServer().getPluginManager().getPlugin("DecentHolograms") != null)
                        ? new org.windy.guildshelter.adapter.bukkit.holo.DecentHologramsBackend(getLogger())
                        : org.windy.guildshelter.adapter.bukkit.holo.HologramBackend.NOOP;
        final org.windy.guildshelter.domain.port.CityHologramStore holoStore = storage.cityHolograms();
        if (holoBackend.available()) {
            getLogger().info("主城悬浮字: 已联动 DecentHolograms（上限 " + config.holograms().maxPerGuild() + " 个/公会）");
        }

        // 成员变更回调扇出：成员缓存 + 主城信任缓存（离会自动撤信任、解散清空）。
        final GuildMemberCache mc = memberCache;
        final org.windy.guildshelter.adapter.bukkit.CityTrustCache ctc = this.cityTrustCache;
        final org.windy.guildshelter.adapter.bukkit.CityFlagCache cfc = cityFlagCache;
        final org.windy.guildshelter.adapter.bukkit.CityPlotCache cpc = cityPlotCache; // 可能为 null（功能关）
        service.setMembershipListener(new org.windy.guildshelter.domain.port.MembershipChangeListener() {
            @Override public void onMemberAssigned(org.windy.guildshelter.domain.model.GuildId g, java.util.UUID p) {
                mc.onMemberAssigned(g, p); ctc.onMemberAssigned(g, p); cfc.onMemberAssigned(g, p);
                if (cpc != null) cpc.onMemberAssigned(g, p);
                fireGuildEvent(new org.windy.guildshelter.api.event.ManorAssignedEvent(toRef(g), p));
            }
            @Override public void onMemberReleased(org.windy.guildshelter.domain.model.GuildId g, java.util.UUID p) {
                mc.onMemberReleased(g, p); ctc.onMemberReleased(g, p); cfc.onMemberReleased(g, p);
                if (cpc != null) cpc.onMemberReleased(g, p); // 退会成员名下子地块自动转未指派
                fireGuildEvent(new org.windy.guildshelter.api.event.ManorReleasedEvent(toRef(g), p));
            }
            @Override public void onGuildDissolved(org.windy.guildshelter.domain.model.GuildId g) {
                mc.onGuildDissolved(g); ctc.onGuildDissolved(g); cfc.onGuildDissolved(g);
                if (cpc != null) cpc.onGuildDissolved(g);
                // 解散：删该会全部主城悬浮字（后端 + 归属表）。
                for (var rec : holoStore.list(g)) {
                    holoBackend.remove(rec.name());
                }
                holoStore.clear(g);
                fireGuildEvent(new org.windy.guildshelter.api.event.GuildDissolvedEvent(toRef(g)));
            }
        });

        // 惰性世界(Iris)延迟铺路：建会时不强制生成主城区块，玩家首次进入该世界后再补铺主城路/围墙。
        var deferredCityPrep = new org.windy.guildshelter.adapter.bukkit.world.DeferredCityPrepListener(
                this, service, registry);
        service.setDeferredPrep(deferredCityPrep);
        getServer().getPluginManager().registerEvents(deferredCityPrep, this);

        GsCommand command = new GsCommand(worldManager, guilds, manors, service, registry,
                config.levels(), entityCensus, this.mergeRegistry, proxyChannel, config.serverName(), getLogger(), this);
        command.setGuildProvider(guildProvider);
        command.setCampSpawnStore(storage.campSpawn()); // 营地成员/访客传送点
        command.setCityFlagCache(cityFlagCache); // 主城 flag（会长/副会长可设）
        command.setAuditLog(this.auditLog); // 领地审计（/gs log；关闭时为空操作实例）
        command.setCityPlots(cityPlotCache, cityPlotsEnabled,
                getConfig().getInt("city-plots.max-per-guild", 16)); // 主城子地块（/gs cityplot）
        command.setHolograms(holoBackend, holoStore, config.holograms().enabled(), config.holograms().maxPerGuild(),
                config.holograms().papiWhitelist());
        PluginCommand gs = getCommand("gs");
        if (gs != null) {
            gs.setExecutor(command);
            gs.setTabCompleter(command);
        }

        for (GuildWorld gw : guilds.findAll()) {
            registry.register(gw);
        }

        getServer().getPluginManager().registerEvents(
                new RegionTitleListener(registry, this.worldCache, config.levels(),
                        this.cityTrustCache, guildProvider), this);

        // 对外领域事件：进/出公会领地（PLAN_API.md Phase 1）。无条件注册，供附属 @EventHandler 监听。
        getServer().getPluginManager().registerEvents(
                new org.windy.guildshelter.adapter.bukkit.listener.TerritoryEventListener(registry), this);

        // 公会领地迎送词（借鉴 HuskTowns greeting/farewell）：进/出公会世界弹欢迎/告别。
        // 文案优先取公会自定义（存于 cityFlagCache 的 greeting/farewell 保留键），回退 config 默认模板。
        if (getConfig().getBoolean("greeting.enabled", true)) {
            var greetMode = "chat".equalsIgnoreCase(getConfig().getString("greeting.mode", "title"))
                    ? org.windy.guildshelter.adapter.bukkit.listener.TerritoryGreetingListener.Mode.CHAT
                    : org.windy.guildshelter.adapter.bukkit.listener.TerritoryGreetingListener.Mode.TITLE;
            getServer().getPluginManager().registerEvents(
                    new org.windy.guildshelter.adapter.bukkit.listener.TerritoryGreetingListener(
                            registry, cityFlagCache, true, greetMode,
                            getConfig().getInt("greeting.fade-in", 10),
                            getConfig().getInt("greeting.stay", 40),
                            getConfig().getInt("greeting.fade-out", 10),
                            getConfig().getString("greeting.default-greeting", "&a欢迎来到 &e%guild% &a的领地"),
                            getConfig().getString("greeting.default-farewell", "&7您已离开 &e%guild% &7的领地")),
                    this);
        }

        // =========================================================================
        // 领地保护：加入极致啰嗦的启动诊断日志
        // =========================================================================
        this.claimGuard = new ClaimGuard(registry, new PermissionRules(), this.worldCache, this.supervisorCache,
                memberCache, this.cityTrustCache, guildProvider, this.roadPermitCache,
                getConfig().getBoolean("road-allow-fake-players", false),
                new java.util.HashSet<>(getConfig().getStringList("main-city-blocked-blocks")),
                new java.util.HashSet<>(getConfig().getStringList("road-permit.blocked-blocks")));
        this.claimGuard.setCityPlotCache(cityPlotCache); // 主城子地块委托建造（null=功能关，主城仍只会长/受信可建）
        this.claimGuard.setBuildCheckRegistry(buildCheckRegistry); // 第三方建造决策参与（PLAN_API.md Phase 4）
        // 共享农场/偷菜等玩法已外移为附属 GuildShelterFarm（注册 BuildCheckProvider）；核心只留 members-farm flag 标记。
        ManorLookup lookup = new ManorLookup(registry, manors, this.worldCache, cityFlagCache);
        this.manorLookup = lookup;
        this.interactionPolicy = new InteractionPolicy(claimGuard, lookup, buildCheckRegistry);

        getLogger().info("========== [启动诊断] 开始装配保护模块 ==========");
        // 接缝：混合端注册原生 NeoForge 保护并返回 true → 跳过下面三个 Bukkit 监听；纯 Bukkit 返回 false。
        boolean nativeProtection = bindings.registerNativeProtection(this);
        if (nativeProtection) {
            getLogger().info("[启动诊断] 混合端：已注册原生保护，跳过 Bukkit ManorProtection/Entity/Flag 监听。");
        } else {
            getServer().getPluginManager().registerEvents(new ManorProtectionListener(claimGuard, interactionPolicy), this);
            getServer().getPluginManager().registerEvents(new ManorEntityListener(interactionPolicy), this);
            getServer().getPluginManager().registerEvents(new ManorFlagListener(lookup), this);
            getLogger().info("[启动诊断] 纯 Bukkit：已注册 ManorProtection/Entity/Flag 监听。");
        }
        getLogger().info("========== [启动诊断] 保护模块装配结束 ==========");

        getServer().getPluginManager().registerEvents(new ManorFireListener(lookup), this);
        getServer().getPluginManager().registerEvents(new ManorEnvListener(lookup), this);
        getServer().getPluginManager().registerEvents(new ManorPlayerListener(lookup), this);
        getServer().getPluginManager().registerEvents(new ManorCapListener(lookup, entityCensus), this);

        EconomyPort economy = VaultEconomy.tryCreate(getLogger());
        VisitCounter visitCounter = new VisitCounter(manors, getLogger());
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override public void run() { visitCounter.flush(); }
        }.runTaskTimer(this, 60L * 20, 60L * 20);

        ManorAccessListener accessListener = new ManorAccessListener(lookup, economy, visitCounter, this.worldCache);
        getServer().getPluginManager().registerEvents(accessListener, this);
        command.setAccessListener(accessListener);
        accessListener.setOpenPlots(command.openPlots());

        getServer().getPluginManager().registerEvents(new ManorCommandListener(lookup), this);
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onQuit(org.bukkit.event.player.PlayerQuitEvent event) {
                UUID id = event.getPlayer().getUniqueId();
                claimGuard.onPlayerQuit(id);
                supervisorCache.clearAll();
                worldCache.removePlayerRef(id);
            }
        }, this);
        new ManorBuffTask(lookup).runTaskTimer(this, 20L, 20L);
        getLogger().info("庄园 Flag 执行已启用（访问/增益走 Bukkit，氛围按载体分流）。");
        new ManorParticleTask(lookup, registry, guilds).runTaskTimer(this, 10L, 10L);
        getServer().getPluginManager().registerEvents(new GuildMotdListener(registry, guilds), this);

        // 接公会插件
        boolean autoCreateCamp = getConfig().getBoolean("guild-hooks.auto-create-camp", false);
        getLogger().info("[GuildShelter] 宿主公会事件自动创建营地世界: " + (autoCreateCamp ? "enabled" : "disabled"));
        record GuildHook(String pluginName, Supplier<Listener> factory) {}
        List<GuildHook> hooks = List.of(
                new GuildHook("PlayerGuild",
                        () -> new PlayerGuildListener(service, guilds, registry, getLogger(), this, autoCreateCamp)),
                new GuildHook("LegendaryGuildRemapped",
                        () -> new LegendaryGuildListener(service, guilds, registry, getLogger(), this, autoCreateCamp)));

        boolean hooked = false;
        for (GuildHook hook : hooks) {
            if (getServer().getPluginManager().getPlugin(hook.pluginName()) != null) {
                getServer().getPluginManager().registerEvents(hook.factory().get(), this);
                getLogger().info("已挂接 " + hook.pluginName() + " 事件。");
                hooked = true;
            }
        }


        if (!hooked) {
            getLogger().info("未检测到任何受支持的公会插件，仅 /gs admin 手动管理可用。");
        }

        // ===== UI 后端（预留：插件 ↔ 模组联动）=====
        // 数据模型（UiView/UiItem/UiIcon）平台中立；易变的 Bukkit Inventory API 全部隔离在
        // BukkitInventoryUi 一个文件内。模组侧自定义 Screen = ModChannelUi（当前为桩）。
        // 注：尚无命令打开菜单，GUI 入口待接——uiRouter()/uiBackend() 即接线点。
        this.uiRouter = new UiActionRouter();
        BukkitInventoryUi bukkitUi = new BukkitInventoryUi(this.uiRouter);
        ModChannelUi modUi = new ModChannelUi(getLogger());
        String uiBackendCfg = getConfig().getString("ui.backend", "auto").toLowerCase(java.util.Locale.ROOT);
        boolean useMod = switch (uiBackendCfg) {
            case "mod" -> true;
            case "vanilla" -> false;
            default -> bindings.isHybrid() && modUi.available(); // auto：模组就绪才用，否则原版兜底
        };
        this.uiBackend = useMod ? modUi : bukkitUi;
        getServer().getPluginManager().registerEvents(new VanillaGuiListener(bukkitUi), this);
        this.guiLoader = new YamlGuiLoader(getDataFolder(), getLogger());
        command.registerUiActions(this.uiRouter);
        getLogger().info("UI 后端: " + this.uiBackend.getClass().getSimpleName()
                + (this.uiBackend == modUi ? "（模组联动）" : "（原版 Inventory 兜底；模组联动 UI 预留中）"));

        if (getConfig().getBoolean("upkeep.enabled", false)) {
            double baseCost = getConfig().getDouble("upkeep.base-cost", 100);
            double perLevelCost = getConfig().getDouble("upkeep.per-level-cost", 50);
            long periodTicks = 20L * 60 * 20;
            new GuildUpkeepTask(guilds, registry, baseCost, perLevelCost, getLogger())
                    .runTaskTimer(this, periodTicks, periodTicks);
            getLogger().info("每日维护费已启用（基础 " + baseCost + "，每级 +" + perLevelCost + "）");
        }

        var perf = config.performance();

        // 掉落物上限：庄园等级表配了 drops，或开了主城限额，就开定时清理任务（同任务兼扫庄园+主城）。
        if (perf.quotas().isConfigured(org.windy.guildshelter.domain.rule.OptimizationLimit.DROPS)
                || config.cityLimits().enabled()) {
            long limitTicks = Math.max(1L, perf.limitCheckSeconds()) * 20L;
            new ManorLimitTask(registry, guilds, manors, entityCensus, perf.dropCleanMode(), getLogger())
                    .runTaskTimer(this, limitTicks, limitTicks);
            getLogger().info("掉落物限制: 庄园按等级 + 主城固定（" + (perf.dropCleanMode() ? "清理最旧+拦截" : "只拦截") + "）");
        }

        WorldOptimizer worldOptimizer = null;
        if (perf.optimizeEnabled()) {
            worldOptimizer = new WorldOptimizer(registry, guilds, perf.optimizeMode(),
                    perf.optimizeInactiveMinutes(), perf.keepSpawnLoaded(), getLogger());
            long optTicks = Math.max(1L, perf.optimizeCheckSeconds()) * 20L;
            worldOptimizer.runTaskTimer(this, optTicks, optTicks);
            getLogger().info("世界级优化: " + perf.optimizeMode() + "（" + perf.optimizeInactiveMinutes() + "分钟无玩家卸载）");
        }

        if (perf.statsEnabled()) {
            long statsTicks = Math.max(1L, perf.statsBroadcastSeconds()) * 20L;
            new PerformanceBroadcastTask(registry, guilds, manors, entityCensus,
                    perf.statsTopCount(), perf.weightTileTick(), perf.weightEntityTick(),
                    perf.weightDropTick(), perf.weightChunkTick(), getLogger())
                    .runTaskTimer(this, statsTicks, statsTicks);
            getLogger().info("性能排行广播: 每 " + perf.statsBroadcastSeconds() + " 秒, Top " + perf.statsTopCount());
        }

        ManorChunkManager chunkManager = null;
        if (perf.chunkUnloadEnabled()) {
            chunkManager = new ManorChunkManager(registry, guilds, manors,
                    perf.chunkUnloadInactiveMinutes(), perf.chunkUnloadKeepRoad(), getLogger());
            long chunkTicks = Math.max(1L, perf.chunkUnloadCheckSeconds()) * 20L;
            chunkManager.runTaskTimer(this, chunkTicks, chunkTicks);

            WorldOptimizer finalWorldOptimizer = worldOptimizer;
            ManorChunkManager finalChunkManager = chunkManager;
            getServer().getPluginManager().registerEvents(new Listener() {
                @EventHandler
                public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
                    finalChunkManager.onPlayerJoin(event.getPlayer().getUniqueId());
                    if (finalWorldOptimizer != null) {
                        for (GuildWorld gw : guilds.findAll()) {
                            finalWorldOptimizer.onPlayerEnter(gw.worldName());
                        }
                    }
                }
            }, this);
            getLogger().info("庄园区块卸载: " + perf.chunkUnloadInactiveMinutes() + " 分钟无上级在线卸载");
        }

        SchematicStore schematicStore = SchematicStores.autoDetect(getDataFolder().toPath(), this);
        if (schematicStore != null) {
            command.setSchematicStore(schematicStore);
            getLogger().info("Schematic 模板已启用（" + schematicStore.getClass().getSimpleName() + "）");
        } else {
            getLogger().info("未检测到 WorldEdit/FAWE，Schematic 模板未启用。");
        }

        // 虚空(空岛)整备策略：服主配了 void-island.schematic 就贴该模板，否则铺默认平台。需 terrain/schematicStore 在场。
        service.setVoidPrep(new org.windy.guildshelter.service.terrain.VoidWorldPrep(
                terrain, schematicStore,
                getConfig().getString("void-island.schematic", ""),
                getConfig().getString("void-island.platform-block", "minecraft:grass_block"),
                getConfig().getInt("void-island.platform-radius", 2)));

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new GuildShelterPapi(manors, guilds, config.levels()).register();
            getLogger().info("PlaceholderAPI 扩展已注册。");
        }

        XaeroIntegration xaero = new XaeroIntegration(registry, guilds, manors, this, getLogger());
        getServer().getPluginManager().registerEvents(xaero, this);

        // Xaero 世界地图圈地（服务端半，PLAN_XAERO.md Phase 1）：注册 guildshelter:map 通道，进世界下发地皮、
        // 收客户端 mod 的圈地点击并裁决。无客户端 mod 时静默（出站无监听端即丢、入站永不发生）。
        org.windy.guildshelter.adapter.bukkit.map.MapClaimChannel mapChannel =
                new org.windy.guildshelter.adapter.bukkit.map.MapClaimChannel(
                        registry, guilds, manors, service, this, getLogger());
        mapChannel.register();
        command.setMapChannel(mapChannel); // 命令解锁后刷新地图高亮

        // ===== 启动横幅（彩色 logo + 信息汇总，载体一眼可辨）=====
        String protectionDesc = bindings.isHybrid() ? "原生 NeoForge 事件" : "Bukkit 监听器";
        String schematicDesc = schematicStore != null
                ? schematicStore.getClass().getSimpleName() : "未启用（无 WorldEdit/FAWE）";
        // 运行时统计：公会数（= 已注册公会世界）+ 庄园总数。
        List<GuildWorld> allGuilds = guilds.findAll();
        int manorCount = 0;
        for (GuildWorld gw : allGuilds) {
            manorCount += manors.findAll(gw.guild()).size();
        }
        String statsDesc = allGuilds.size() + " 个公会 · " + manorCount + " 个庄园";
        getServer().getConsoleSender().sendMessage(Texts.startupBanner(
                bindings.isHybrid(),
                getDescription().getVersion(),
                bindings.carrierName(),
                config.storage().type(),
                protectionDesc,
                guildSource,
                schematicDesc,
                this.uiBackend.getClass().getSimpleName(),
                statsDesc));
    }

    /**
     * 加载等级系统配置 levels.yml：首启时释放默认文件，并把旧 config.yml 里的等级配置（如有）迁移过来，
     * 避免老服升级后等级被悄悄重置。返回可读的 FileConfiguration。
     */
    private org.bukkit.configuration.file.FileConfiguration loadLevelsConfig() {
        java.io.File file = new java.io.File(getDataFolder(), "levels.yml");
        boolean firstRun = !file.exists();
        if (firstRun) {
            saveResource("levels.yml", false);
        }
        org.bukkit.configuration.file.FileConfiguration lv =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        if (firstRun) {
            org.bukkit.configuration.file.FileConfiguration old = getConfig();
            boolean migrated = false;
            // 旧 member-plot.* → manor.*
            migrated |= copyInt(old, "member-plot.initial-chunks", lv, "manor.initial-chunks");
            migrated |= copyInt(old, "member-plot.max-chunks", lv, "manor.max-chunks");
            // 旧 guild.* → guild.*（键名不变）
            migrated |= copyInt(old, "guild.max-level", lv, "guild.max-level");
            // 旧 main-city.* → guild.main-city.*
            migrated |= copyInt(old, "main-city.initial-chunks", lv, "guild.main-city.initial-chunks");
            migrated |= copyInt(old, "main-city.max-chunks", lv, "guild.main-city.max-chunks");
            migrated |= copySection(old, "manor-upgrade-commands", lv, "manor.upgrade-commands");
            if (migrated) {
                try {
                    lv.save(file);
                    getLogger().info("已把旧 config.yml 的等级配置迁移到 levels.yml。");
                } catch (java.io.IOException e) {
                    getLogger().warning("迁移 levels.yml 失败: " + e.getMessage());
                }
            }
        }
        return lv;
    }

    /**
     * 加载本服标识 server.yml。该文件不作为 jar 内默认资源打包；首启按服务器当前工作目录名生成，
     * 服主需要跨服标识时只改这个小文件。
     */
    private org.bukkit.configuration.file.FileConfiguration loadServerConfig() {
        java.io.File file = new java.io.File(getDataFolder(), "server.yml");
        if (!file.exists()) {
            String defaultName = defaultServerName();
            java.util.List<String> lines = java.util.List.of(
                    "# GuildShelter 本服标识配置",
                    "# 默认值取服务器当前工作目录的文件夹名；跨服时可改成代理端配置的服务器名。",
                    "server-name: \"" + defaultName.replace("\\", "\\\\").replace("\"", "\\\"") + "\"",
                    "");
            try {
                java.nio.file.Files.write(file.toPath(), lines, java.nio.charset.StandardCharsets.UTF_8);
            } catch (java.io.IOException e) {
                getLogger().warning("生成 server.yml 失败: " + e.getMessage());
            }
        }
        return org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
    }

    private String defaultServerName() {
        java.nio.file.Path cwd = java.nio.file.Path.of("").toAbsolutePath().normalize();
        java.nio.file.Path fileName = cwd.getFileName();
        String name = fileName != null ? fileName.toString() : "";
        if (name == null || name.isBlank()) {
            java.io.File worldContainer = getServer().getWorldContainer();
            name = worldContainer != null ? worldContainer.getName() : "";
        }
        return (name == null || name.isBlank()) ? "server" : name;
    }

    /** 若源有该键则复制到目标。返回是否复制了。 */
    private static boolean copyInt(org.bukkit.configuration.file.FileConfiguration src, String srcKey,
                                   org.bukkit.configuration.file.FileConfiguration dst, String dstKey) {
        if (src.contains(srcKey)) {
            dst.set(dstKey, src.getInt(srcKey));
            return true;
        }
        return false;
    }

    /** 若源有该配置段则复制到目标。返回是否复制了。 */
    private static boolean copySection(org.bukkit.configuration.file.FileConfiguration src, String srcKey,
                                       org.bukkit.configuration.file.FileConfiguration dst, String dstKey) {
        org.bukkit.configuration.ConfigurationSection section = src.getConfigurationSection(srcKey);
        if (section == null) {
            return false;
        }
        dst.set(dstKey, section);
        return true;
    }

    /** GuildId → 对外 {@link org.windy.guildshelter.api.GuildRef}；worldName 用确定式 worldName(g)（解散后仍可得）。 */
    private org.windy.guildshelter.api.GuildRef toRef(org.windy.guildshelter.domain.model.GuildId g) {
        return new org.windy.guildshelter.api.GuildRef(g.value(), worldManager.worldName(g));
    }

    /** fire 一个对外领域事件（主线程）。供附属 @EventHandler 监听。 */
    private void fireGuildEvent(org.bukkit.event.Event event) {
        getServer().getPluginManager().callEvent(event);
    }

    @Override
    public void onDisable() {
        if (auditLog != null) {
            auditLog.shutdown(); // 先排空审计写队列，再关库
        }
        if (storage != null) {
            storage.close();
        }
        getLogger().info("GuildShelter 已禁用。");
        instance = null;
    }
}
