package org.windy.guildshelter.adapter.bukkit.api;

import org.bukkit.Location;
import org.bukkit.World;
import org.windy.guildshelter.adapter.bukkit.CityFlagCache;
import org.windy.guildshelter.adapter.bukkit.GuildMemberCache;
import org.windy.guildshelter.adapter.bukkit.GuildWorldRegistry;
import org.windy.guildshelter.adapter.bukkit.WorldCache;
import org.windy.guildshelter.api.GuildRef;
import org.windy.guildshelter.api.GuildShelterAPI;
import org.windy.guildshelter.api.ManorRef;
import org.windy.guildshelter.api.RegionKind;
import org.windy.guildshelter.domain.flag.Flag;
import org.windy.guildshelter.domain.layout.Classification;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;
import org.windy.guildshelter.domain.port.GuildRepository;
import org.windy.guildshelter.domain.port.ManorRepository;
import org.windy.guildshelter.service.GuildService;

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
    private final org.windy.guildshelter.adapter.bukkit.BuildCheckRegistry buildChecks;

    public GuildShelterApiImpl(GuildWorldRegistry registry, WorldCache cache, CityFlagCache cityFlags,
                               GuildMemberCache memberCache, GuildRepository guilds, ManorRepository manors,
                               GuildService service, org.windy.guildshelter.adapter.bukkit.BuildCheckRegistry buildChecks) {
        this.registry = registry;
        this.cache = cache;
        this.cityFlags = cityFlags;
        this.memberCache = memberCache;
        this.guilds = guilds;
        this.manors = manors;
        this.service = service;
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
    public void registerBuildCheck(org.bukkit.plugin.Plugin plugin,
                                   org.windy.guildshelter.api.BuildCheckProvider provider, int priority) {
        buildChecks.register(plugin, provider, priority);
    }

    @Override
    public void unregisterBuildChecks(org.bukkit.plugin.Plugin plugin) {
        buildChecks.unregister(plugin);
    }
}
