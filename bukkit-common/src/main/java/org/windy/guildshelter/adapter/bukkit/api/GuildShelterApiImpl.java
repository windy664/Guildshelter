package org.windy.guildshelter.adapter.bukkit.api;

import org.bukkit.Location;
import org.bukkit.World;
import org.windy.guildshelter.adapter.bukkit.CityFlagCache;
import org.windy.guildshelter.adapter.bukkit.GuildMemberCache;
import org.windy.guildshelter.adapter.bukkit.GuildWorldRegistry;
import org.windy.guildshelter.adapter.bukkit.WorldCache;
import org.windy.guildshelter.api.GuildRef;
import org.windy.guildshelter.api.GuildShelterMigrationRegion;
import org.windy.guildshelter.api.GuildShelterAPI;
import org.windy.guildshelter.api.MapClaimResult;
import org.windy.guildshelter.api.MapClaimStatus;
import org.windy.guildshelter.api.ManorRef;
import org.windy.guildshelter.api.RegionKind;
import org.windy.guildshelter.api.TerritoryMapChunk;
import org.windy.guildshelter.api.TerritoryMapKind;
import org.windy.guildshelter.api.TerritoryMapSnapshot;
import org.windy.guildshelter.domain.flag.Flag;
import org.windy.guildshelter.domain.layout.Classification;
import org.windy.guildshelter.domain.layout.LayoutCalculator;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.ChunkRegion;
import org.windy.guildshelter.domain.model.PlayerRef;
import org.windy.guildshelter.domain.port.GuildRepository;
import org.windy.guildshelter.domain.port.ManorRepository;
import org.windy.guildshelter.domain.port.WorldControl;
import org.windy.guildshelter.service.GuildService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link GuildShelterAPI} 的实现（bukkit-common）：把对外只读查询翻译成内部缓存/仓库调用。
 * 由 {@code GuildShelterPlugin} 在 onEnable 构造并注册到 Bukkit {@code ServicesManager}。
 *
 * <p>全部走已有的热路径缓存（{@link WorldCache} / {@link CityFlagCache} / {@link GuildMemberCache}），
 * 与 {@code ClaimGuard} 同一套数据，零额外查询；不暴露任何内部可变模型，只回 API DTO。
 */
public final class GuildShelterApiImpl implements GuildShelterAPI {

    private final GuildWorldRegistry registry;
    private final WorldCache cache;
    private final CityFlagCache cityFlags;
    private final GuildMemberCache memberCache;
    private final GuildRepository guilds;
    private final ManorRepository manors;
    private final GuildService service;
    private final WorldControl worlds;
    private final org.windy.guildshelter.adapter.bukkit.BuildCheckRegistry buildChecks;

    public GuildShelterApiImpl(GuildWorldRegistry registry, WorldCache cache, CityFlagCache cityFlags,
                               GuildMemberCache memberCache, GuildRepository guilds, ManorRepository manors,
                               GuildService service, WorldControl worlds,
                               org.windy.guildshelter.adapter.bukkit.BuildCheckRegistry buildChecks) {
        this.registry = registry;
        this.cache = cache;
        this.cityFlags = cityFlags;
        this.memberCache = memberCache;
        this.guilds = guilds;
        this.manors = manors;
        this.service = service;
        this.worlds = worlds;
        this.buildChecks = buildChecks;
    }

    /** 内部：解析 loc → 所属 GuildWorld（不在营地世界 → null）。 */
    private GuildWorld worldAt(Location loc) {
        World w = loc == null ? null : loc.getWorld();
        return w == null ? null : registry.get(w.getName());
    }

    /** 内部：loc 的局部分类（gw 为空时返回 null）。 */
    private Classification classify(GuildWorld gw, Location loc) {
        int lx = (loc.getBlockX() >> 4) - gw.originChunkX();
        int lz = (loc.getBlockZ() >> 4) - gw.originChunkZ();
        return cache.layout(gw.layout()).classify(lx, lz);
    }

    @Override
    public Optional<GuildRef> guildAt(Location loc) {
        GuildWorld gw = worldAt(loc);
        return gw == null ? Optional.empty()
                : Optional.of(new GuildRef(gw.guild().value(), gw.worldName()));
    }

    @Override
    public RegionKind kindAt(Location loc) {
        GuildWorld gw = worldAt(loc);
        if (gw == null) {
            return RegionKind.WILDERNESS;
        }
        Classification c = classify(gw, loc);
        if (c.isMainCity()) return RegionKind.MAIN_CITY;
        if (c.isPlot()) return RegionKind.PLOT;
        return RegionKind.ROAD;
    }

    @Override
    public Optional<ManorRef> manorAt(Location loc) {
        GuildWorld gw = worldAt(loc);
        if (gw == null) {
            return Optional.empty();
        }
        Classification c = classify(gw, loc);
        if (!c.isPlot()) {
            return Optional.empty();
        }
        Manor m = cache.manorAt(gw, c.slot());
        if (m == null) {
            return Optional.empty();
        }
        return Optional.of(new ManorRef(new GuildRef(gw.guild().value(), gw.worldName()),
                m.slot(), m.owner().uuid()));
    }

    @Override
    public boolean isFarmZone(Location loc) {
        return booleanFlag(loc, Flag.MEMBERS_FARM.id());
    }

    @Override
    public boolean booleanFlag(Location loc, String flagId) {
        Optional<Flag> flagOpt = Flag.byId(flagId);
        if (flagOpt.isEmpty()) {
            return false;
        }
        Flag flag = flagOpt.get();
        GuildWorld gw = worldAt(loc);
        if (gw == null) {
            return flag.resolveBool(java.util.Map.of()); // 营地外 = flag 默认
        }
        Classification c = classify(gw, loc);
        if (c.isMainCity()) {
            return flag.resolveBool(cityFlags.flags(gw.guild())); // 主城 flag 存 CityFlagCache
        }
        if (c.isPlot()) {
            Manor m = cache.manorAt(gw, c.slot());
            return flag.resolveBool(m == null ? java.util.Map.of() : m.flags());
        }
        return flag.resolveBool(java.util.Map.of()); // 路 = 默认
    }

    @Override
    public int guildLevel(GuildRef guild) {
        if (guild == null) {
            return 0;
        }
        return guilds.find(new GuildId(guild.id())).map(GuildWorld::guildLevel).orElse(0);
    }

    @Override
    public int manorLevel(ManorRef manor) {
        if (manor == null) {
            return 0;
        }
        return manors.findBySlot(new GuildId(manor.guild().id()), manor.slot())
                .map(Manor::level).orElse(0);
    }

    @Override
    public boolean isMember(GuildRef guild, UUID player) {
        return guild != null && player != null
                && memberCache.isMember(new GuildId(guild.id()), player);
    }

    @Override
    public boolean isGuildAdmin(GuildRef guild, UUID player) {
        return guild != null && player != null
                && service.isGuildAdmin(PlayerRef.of(player), new GuildId(guild.id()));
    }

    @Override
    public List<GuildRef> guildCamps() {
        return guilds.findAll().stream()
                .map(gw -> new GuildRef(gw.guild().value(), gw.worldName()))
                .toList();
    }

    @Override
    public Optional<TerritoryMapSnapshot> campSnapshot(GuildRef camp) {
        if (camp == null) {
            return Optional.empty();
        }
        GuildWorld gw = guilds.find(new GuildId(camp.id())).orElse(null);
        return gw == null ? Optional.empty() : Optional.of(territorySnapshot(gw, null));
    }

    @Override
    public Optional<GuildShelterMigrationRegion> exportManor(UUID owner) {
        if (owner == null) {
            return Optional.empty();
        }
        Manor manor = manors.findByOwnerAnywhere(PlayerRef.of(owner)).orElse(null);
        if (manor == null) {
            return Optional.empty();
        }
        GuildWorld gw = guilds.find(manor.guild()).orElse(null);
        if (gw == null) {
            return Optional.empty();
        }
        gw = ensureMigrationWorld(gw);
        return Optional.of(regionOf(manor.owner().uuid(), gw, manor.slot(), manor.level(), "guildshelter:export"));
    }

    @Override
    public Optional<GuildShelterMigrationRegion> prepareManorImport(UUID owner, String targetGuild, int requiredSideChunks) {
        if (owner == null || targetGuild == null || targetGuild.isBlank()) {
            return Optional.empty();
        }
        PlayerRef player = PlayerRef.of(owner);
        if (manors.findByOwnerAnywhere(player).isPresent()) {
            return Optional.empty();
        }
        GuildId guild = new GuildId(targetGuild);
        GuildWorld gw = guilds.find(guild).orElse(null);
        if (gw == null) {
            return Optional.empty();
        }
        gw = ensureMigrationWorld(gw);
        int slot = manors.nextFreeSlot(guild);
        int capacity = service.memberCapacity(guild);
        if (capacity <= 0 || slot >= capacity) {
            return Optional.empty();
        }
        LayoutCalculator layout = cache.layout(gw.layout());
        ChunkRegion region = layout.plotRegion(slot).shift(gw.originChunkX(), gw.originChunkZ());
        int side = region.widthChunks();
        if (side < Math.max(1, requiredSideChunks)) {
            return Optional.empty();
        }
        return Optional.of(regionOf(owner, gw, slot, 1, "guildshelter:import"));
    }

    @Override
    public boolean completeManorImport(UUID owner, String targetGuild, int slot, int manorLevel) {
        if (owner == null || targetGuild == null || targetGuild.isBlank() || slot < 0) {
            return false;
        }
        PlayerRef player = PlayerRef.of(owner);
        if (manors.findByOwnerAnywhere(player).isPresent()) {
            return false;
        }
        GuildId guild = new GuildId(targetGuild);
        GuildWorld gw = guilds.find(guild).orElse(null);
        if (gw == null) {
            return false;
        }
        int capacity = service.memberCapacity(guild);
        if (capacity <= 0 || slot >= capacity || manors.findBySlot(guild, slot).isPresent()) {
            return false;
        }
        int level = Math.max(1, manorLevel);
        java.util.Set<Integer> unlocked = allPlotChunks(cache.layout(gw.layout()));
        Manor manor = new Manor(slot, guild, player, level,
                java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), java.util.Map.of(), unlocked);
        manors.save(manor);
        registry.register(gw);
        cache.invalidateManor(guild, slot);
        return true;
    }

    @Override
    public Optional<TerritoryMapSnapshot> mapSnapshot(org.bukkit.entity.Player viewer) {
        if (viewer == null) {
            return Optional.empty();
        }
        GuildWorld gw = registry.get(viewer.getWorld().getName());
        if (gw == null) {
            return Optional.empty();
        }
        return Optional.of(territorySnapshot(gw, viewer.getUniqueId()));
    }

    private TerritoryMapSnapshot territorySnapshot(GuildWorld gw, UUID viewer) {
        LayoutCalculator layout = cache.layout(gw.layout());
        PlayerRef ref = viewer == null ? null : PlayerRef.of(viewer);
        List<TerritoryMapChunk> chunks = new ArrayList<>();

        ChunkRegion city = layout.mainCityRegion();
        for (int dx = city.minChunkX(); dx <= city.maxChunkX(); dx++) {
            for (int dz = city.minChunkZ(); dz <= city.maxChunkZ(); dz++) {
                if (gw.isCityUnlocked(dx, dz)) {
                    chunks.add(new TerritoryMapChunk(gw.originChunkX() + dx, gw.originChunkZ() + dz,
                            TerritoryMapKind.CITY_UNLOCKED, "city"));
                }
            }
        }

        for (Manor manor : manors.findAll(gw.guild())) {
            ChunkRegion plot = layout.plotRegion(manor.slot());
            TerritoryMapKind kind = manor.owner().equals(ref)
                    ? TerritoryMapKind.OWN_UNLOCKED : TerritoryMapKind.OTHER_UNLOCKED;
            String label = "#" + manor.slot();
            for (int packed : manor.unlockedChunks()) {
                int wx = plot.minChunkX() + Manor.unpackDx(packed);
                int wz = plot.minChunkZ() + Manor.unpackDz(packed);
                chunks.add(new TerritoryMapChunk(gw.originChunkX() + wx, gw.originChunkZ() + wz, kind, label));
            }
        }

        return new TerritoryMapSnapshot(gw.worldName(), gw.originChunkX(), gw.originChunkZ(), chunks);
    }

    @Override
    public MapClaimResult tryClaimMapChunk(org.bukkit.entity.Player player, int worldChunkX, int worldChunkZ) {
        if (player == null) {
            return MapClaimResult.of(MapClaimStatus.INVALID_PLAYER);
        }
        GuildWorld gw = registry.get(player.getWorld().getName());
        if (gw == null) {
            return MapClaimResult.of(MapClaimStatus.NOT_IN_GUILD_WORLD);
        }

        LayoutCalculator layout = cache.layout(gw.layout());
        int lx = worldChunkX - gw.originChunkX();
        int lz = worldChunkZ - gw.originChunkZ();
        Classification c = layout.classify(lx, lz);
        PlayerRef ref = PlayerRef.of(player.getUniqueId());

        if (c.isPlot()) {
            return MapClaimResult.of(mapStatus(service.unlockChunk(gw.guild(), ref, worldChunkX, worldChunkZ)));
        }
        if (c.isMainCity()) {
            if (!service.isGuildAdmin(ref, gw.guild()) && !player.isOp()) {
                return MapClaimResult.of(MapClaimStatus.CITY_LEADER_ONLY);
            }
            GuildWorld updated = service.unlockCityChunk(gw.guild(), worldChunkX, worldChunkZ);
            if (updated != null) {
                registry.register(updated);
            }
            return MapClaimResult.of(mapStatus(service.lastCityUnlockResult()));
        }
        return MapClaimResult.of(MapClaimStatus.NOT_CLAIMABLE);
    }

    private static MapClaimStatus mapStatus(GuildService.UnlockResult result) {
        return switch (result) {
            case SUCCESS -> MapClaimStatus.SUCCESS;
            case NO_MANOR -> MapClaimStatus.NO_MANOR;
            case NOT_YOUR_PLOT -> MapClaimStatus.NOT_YOUR_PLOT;
            case ALREADY_UNLOCKED -> MapClaimStatus.ALREADY_UNLOCKED;
            case NO_QUOTA -> MapClaimStatus.NO_QUOTA;
            case NOT_ADJACENT -> MapClaimStatus.NOT_ADJACENT;
        };
    }

    private GuildShelterMigrationRegion regionOf(UUID owner, GuildWorld gw, int slot, int manorLevel, String source) {
        LayoutCalculator layout = cache.layout(gw.layout());
        ChunkRegion region = layout.plotRegion(slot).shift(gw.originChunkX(), gw.originChunkZ());
        return new GuildShelterMigrationRegion(
                owner,
                new GuildRef(gw.guild().value(), gw.worldName()),
                gw.worldName(),
                slot,
                region.minChunkX(),
                region.minChunkZ(),
                region.maxChunkX(),
                region.maxChunkZ(),
                region.widthChunks(),
                manorLevel,
                source);
    }

    private GuildWorld ensureMigrationWorld(GuildWorld gw) {
        GuildWorld ready = worlds.ensureWorld(gw);
        if (!ready.equals(gw)) {
            guilds.save(ready);
        }
        registry.register(ready);
        return ready;
    }

    private java.util.Set<Integer> allPlotChunks(LayoutCalculator layout) {
        int side = layout.config().plotChunks();
        java.util.Set<Integer> chunks = new java.util.HashSet<>();
        for (int dx = 0; dx < side; dx++) {
            for (int dz = 0; dz < side; dz++) {
                chunks.add(Manor.packOffset(dx, dz));
            }
        }
        return chunks;
    }

    @Override
    public void registerBuildCheck(org.bukkit.plugin.Plugin plugin,
                                   org.windy.guildshelter.api.BuildCheckProvider provider, int priority) {
        buildChecks.register(plugin, provider, priority);
    }

    @Override
    public void unregisterBuildChecks(org.bukkit.plugin.Plugin plugin) {
        buildChecks.unregister(plugin);
    }
}
