package org.windy.guildshelter.horde;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Difficulty;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
import org.windy.guildshelter.api.GuildRef;
import org.windy.guildshelter.api.GuildShelterAPI;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class HordeManager {

    private final JavaPlugin plugin;
    private final GuildShelterAPI api;
    private final Settings settings;
    private final HordeMessages messages;
    private final HordeEntityMarker marker;
    private final Map<String, HordeSession> sessions = new HashMap<>();

    HordeManager(JavaPlugin plugin, GuildShelterAPI api, FileConfiguration config, HordeMessages messages,
                 HordeEntityMarker marker) {
        this.plugin = plugin;
        this.api = api;
        this.settings = Settings.from(config);
        this.messages = messages;
        this.marker = marker;
    }

    Settings settings() {
        return settings;
    }

    synchronized String startAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return messages.get("error.not-in-camp");
        }
        Optional<GuildRef> guildOpt = api.guildAt(location);
        if (guildOpt.isEmpty()) {
            return messages.get("error.not-in-camp");
        }
        return start(guildOpt.get(), location.getWorld());
    }

    synchronized String startAtWorldName(String worldName) {
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return messages.get("error.world-not-loaded", "world", worldName);
        }
        Optional<GuildRef> guildOpt = api.guildAt(world.getSpawnLocation());
        if (guildOpt.isEmpty()) {
            return messages.get("error.world-not-camp", "world", worldName);
        }
        return start(guildOpt.get(), world);
    }

    private String start(GuildRef guild, World world) {
        if (sessions.containsKey(guild.worldName())) {
            return messages.get("error.already-running-camp", "guild", guild.id());
        }
        if (hasActiveHorde()) {
            return messages.get("error.already-running-global");
        }
        if (world.getDifficulty() == Difficulty.PEACEFUL && !settings.forceDifficultyEnabled()) {
            return messages.get("error.peaceful-disabled");
        }
        HordeSession session = new HordeSession(plugin, api, settings, messages, marker, guild, world, this::removeSession);
        sessions.put(guild.worldName(), session);
        session.start();
        return messages.get("command.started", "guild", guild.id());
    }

    synchronized boolean tryStartAtWorldName(String worldName) {
        World world = plugin.getServer().getWorld(worldName);
        if (world == null) {
            return false;
        }
        Optional<GuildRef> guildOpt = api.guildAt(world.getSpawnLocation());
        if (guildOpt.isEmpty()) {
            return false;
        }
        if (sessions.containsKey(guildOpt.get().worldName())) {
            return false;
        }
        if (hasActiveHorde()) {
            return false;
        }
        if (world.getDifficulty() == Difficulty.PEACEFUL && !settings.forceDifficultyEnabled()) {
            return false;
        }
        HordeSession session = new HordeSession(plugin, api, settings, messages, marker, guildOpt.get(), world,
                this::removeSession);
        sessions.put(guildOpt.get().worldName(), session);
        session.start();
        return true;
    }

    synchronized String stopAt(Location location, String reason) {
        if (location == null || location.getWorld() == null) {
            return messages.get("error.not-in-camp");
        }
        Optional<GuildRef> guildOpt = api.guildAt(location);
        if (guildOpt.isEmpty()) {
            return messages.get("error.not-in-camp");
        }
        return stopAtWorldName(guildOpt.get().worldName(), reason);
    }

    synchronized String stopAtWorldName(String worldName, String reason) {
        HordeSession session = sessions.remove(worldName);
        if (session != null) {
            session.stop(reason, false);
        }
        return session == null
                ? messages.get("command.not-running", "world", worldName)
                : messages.get("command.stopped", "world", worldName);
    }

    synchronized String statusAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return messages.get("error.not-in-camp");
        }
        Optional<GuildRef> guildOpt = api.guildAt(location);
        if (guildOpt.isEmpty()) {
            return messages.get("error.not-in-camp");
        }
        return statusAtWorldName(guildOpt.get().worldName());
    }

    synchronized String statusAtWorldName(String worldName) {
        HordeSession session = sessions.get(worldName);
        return session == null ? messages.get("command.not-running", "world", worldName) : session.statusLine();
    }

    synchronized void stopAll(String reason) {
        List<HordeSession> runningSessions = List.copyOf(sessions.values());
        runningSessions.forEach(session -> session.stop(reason, false));
        sessions.clear();
    }

    synchronized boolean hasActiveHorde() {
        return !sessions.isEmpty();
    }

    synchronized boolean hasActiveHorde(String worldName) {
        return sessions.containsKey(worldName);
    }

    private synchronized void removeSession(String worldName, HordeSession session) {
        HordeSession current = sessions.get(worldName);
        if (current == session) {
            sessions.remove(worldName);
        }
    }

    record Settings(
            int maxWaves,
            int baseMobsPerWave,
            int mobsPerGuildLevel,
            int waveGrowth,
            int waveDurationSeconds,
            int spawnIntervalSeconds,
            int spawnBatchSize,
            int spawnBatchPerGuildLevel,
            int maxAliveMobs,
            int intermissionSeconds,
            int retreatTimeoutSeconds,
            int spawnRadius,
            boolean preferNearPlayers,
            int nearPlayerCandidateCount,
            int rewardExp,
            int rewardExpPerGuildLevel,
            double mobHealthMultiplier,
            double mobHealthPerGuildLevel,
            double mobHealthPerWave,
            boolean forceDifficultyEnabled,
            Difficulty forceDifficulty,
            boolean restorePreviousDifficulty,
            boolean bypassPveFlags,
            boolean bypassInvincibleFlag,
            List<EntityType> mobTypes
    ) {
        static Settings from(FileConfiguration config) {
            List<EntityType> mobs = config.getStringList("mob-types").stream()
                    .map(s -> {
                        try {
                            return EntityType.valueOf(s.toUpperCase(Locale.ROOT));
                        } catch (IllegalArgumentException ex) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .filter(EntityType::isSpawnable)
                    .toList();
            if (mobs.isEmpty()) {
                mobs = List.of(EntityType.ZOMBIE, EntityType.HUSK, EntityType.SKELETON, EntityType.SPIDER);
            }
            return new Settings(
                    config.getInt("max-waves", 6),
                    config.getInt("base-mobs-per-wave", 16),
                    config.getInt("mobs-per-guild-level", 2),
                    config.getInt("wave-growth", 2),
                    config.getInt("wave-duration-seconds", 90),
                    config.getInt("spawn-interval-seconds", 3),
                    Math.max(1, config.getInt("spawn-batch-size", 4)),
                    Math.max(0, config.getInt("spawn-batch-per-guild-level", 0)),
                    config.getInt("max-alive-mobs", 40),
                    config.getInt("intermission-seconds", 12),
                    config.getInt("retreat-timeout-seconds", 45),
                    config.getInt("spawn-radius", 18),
                    config.getBoolean("spawn.prefer-near-players", true),
                    Math.max(1, config.getInt("spawn.near-player-candidate-count", 8)),
                    config.getInt("reward-exp", 80),
                    config.getInt("reward-exp-per-guild-level", 20),
                    Math.max(0.1, config.getDouble("mob-health.multiplier", 1.8)),
                    Math.max(0.0, config.getDouble("mob-health.per-guild-level", 0.08)),
                    Math.max(0.0, config.getDouble("mob-health.per-wave", 0.08)),
                    config.getBoolean("difficulty.force-during-horde", true),
                    parseDifficulty(config.getString("difficulty.forced-difficulty", "HARD")),
                    config.getBoolean("difficulty.restore-after-horde", true),
                    config.getBoolean("combat.bypass-pve-flags", true),
                    config.getBoolean("combat.bypass-invincible-flag", false),
                    List.copyOf(mobs)
            );
        }

        private static Difficulty parseDifficulty(String raw) {
            if (raw == null || raw.isBlank()) {
                return Difficulty.HARD;
            }
            try {
                Difficulty difficulty = Difficulty.valueOf(raw.trim().toUpperCase(Locale.ROOT));
                return difficulty == Difficulty.PEACEFUL ? Difficulty.HARD : difficulty;
            } catch (IllegalArgumentException ex) {
                return Difficulty.HARD;
            }
        }
    }
}
