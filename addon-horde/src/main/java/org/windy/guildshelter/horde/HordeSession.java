package org.windy.guildshelter.horde;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.windy.guildshelter.api.GuildRef;
import org.windy.guildshelter.api.GuildShelterAPI;
import org.windy.guildshelter.api.RegionKind;
import org.windy.guildshelter.api.TerritoryMapChunk;
import org.windy.guildshelter.api.TerritoryMapSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

final class HordeSession {

    private final JavaPlugin plugin;
    private final GuildShelterAPI api;
    private final HordeManager.Settings settings;
    private final HordeMessages messages;
    private final HordeEntityMarker marker;
    private final GuildRef guild;
    private final World world;
    private final BiConsumer<String, HordeSession> endCallback;
    private final BossBar bossBar;
    private final Set<UUID> mobs = new HashSet<>();
    private final List<ChunkPos> roadSpawnChunks;
    private final Random random = ThreadLocalRandom.current();
    private final int guildLevel;
    private final Location center;
    private final Difficulty originalDifficulty;
    private boolean difficultyChanged;

    private BukkitTask task;
    private boolean running;
    private boolean intermission;
    private int wave = 1;
    private int spawnedThisWave;
    private int aliveSeconds;
    private int spawnCooldownSeconds;
    private int intermissionSecondsLeft;
    private int emptySeconds;
    private int targetThisWave;

    HordeSession(JavaPlugin plugin, GuildShelterAPI api, HordeManager.Settings settings, HordeMessages messages,
                 HordeEntityMarker marker, GuildRef guild, World world,
                 BiConsumer<String, HordeSession> endCallback) {
        this.plugin = plugin;
        this.api = api;
        this.settings = settings;
        this.messages = messages;
        this.marker = marker;
        this.guild = guild;
        this.world = world;
        this.endCallback = endCallback;
        this.guildLevel = Math.max(0, api.guildLevel(guild));
        this.center = world.getSpawnLocation().clone().add(0.5, 0.0, 0.5);
        this.roadSpawnChunks = buildRoadSpawnChunks();
        this.bossBar = Bukkit.createBossBar(messages.get("boss.preparing"), BarColor.RED, BarStyle.SEGMENTED_12);
        this.originalDifficulty = world.getDifficulty();
    }

    void start() {
        if (running) {
            return;
        }
        running = true;
        bossBar.setVisible(true);
        applyForcedDifficulty();
        syncPlayers();
        beginWave();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        broadcast(messages.get("session.started"));
    }

    void stop(String reason, boolean success) {
        if (!running) {
            return;
        }
        running = false;
        if (task != null) {
            task.cancel();
            task = null;
        }
        mobs.forEach(uuid -> {
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) {
                entity.remove();
            }
        });
        mobs.clear();
        bossBar.removeAll();
        bossBar.setVisible(false);
        if (success) {
            rewardPlayers();
        }
        restoreDifficulty();
        broadcast(reason);
        endCallback.accept(world.getName(), this);
    }

    String statusLine() {
        int alive = aliveMobs();
        if (intermission) {
            return messages.get("status.intermission",
                    "wave", wave,
                    "max_waves", settings.maxWaves(),
                    "seconds", intermissionSecondsLeft);
        }
        return messages.get("status.running",
                "wave", wave,
                "max_waves", settings.maxWaves(),
                "spawned", spawnedThisWave,
                "target", targetThisWave,
                "alive", alive);
    }

    private void tick() {
        if (!running) {
            return;
        }
        if (Bukkit.getWorld(world.getName()) == null) {
            stop(messages.get("session.world-unloaded"), false);
            return;
        }
        if (world.getDifficulty() == Difficulty.PEACEFUL) {
            if (settings.forceDifficultyEnabled()) {
                applyForcedDifficulty();
            } else {
                stop(messages.get("session.world-peaceful"), false);
                return;
            }
        }
        syncPlayers();
        cleanupDeadMobs();
        if (world.getPlayers().isEmpty()) {
            emptySeconds++;
            updateBar();
            if (emptySeconds >= settings.retreatTimeoutSeconds()) {
                stop(messages.get("session.no-defenders"), false);
            }
            return;
        }
        emptySeconds = 0;

        if (intermission) {
            intermissionSecondsLeft--;
            updateBar();
            if (intermissionSecondsLeft <= 0) {
                beginWave();
            }
            return;
        }

        aliveSeconds++;
        spawnCooldownSeconds--;
        if (spawnedThisWave < targetThisWave
                && mobs.size() < settings.maxAliveMobs()
                && spawnCooldownSeconds <= 0) {
            spawnBatch();
            spawnCooldownSeconds = settings.spawnIntervalSeconds();
        }
        updateBar();
        if (spawnedThisWave >= targetThisWave && mobs.isEmpty()) {
            if (wave >= settings.maxWaves()) {
                stop(messages.get("session.success"), true);
            } else {
                beginIntermission();
            }
            return;
        }
        if (aliveSeconds >= settings.waveDurationSeconds() && wave < settings.maxWaves()) {
            beginIntermission();
        }
    }

    private void beginWave() {
        intermission = false;
        aliveSeconds = 0;
        spawnedThisWave = 0;
        spawnCooldownSeconds = 0;
        targetThisWave = settings.baseMobsPerWave()
                + guildLevel * settings.mobsPerGuildLevel()
                + (wave - 1) * settings.waveGrowth();
        broadcast(messages.get("session.wave-start", "wave", wave, "max_waves", settings.maxWaves()));
        updateBar();
    }

    private void beginIntermission() {
        intermission = true;
        intermissionSecondsLeft = settings.intermissionSeconds();
        wave++;
        broadcast(messages.get("session.wave-end", "wave", wave - 1));
        updateBar();
    }

    private void spawnBatch() {
        int batchSize = settings.spawnBatchSize() + guildLevel * settings.spawnBatchPerGuildLevel();
        int remainingThisWave = targetThisWave - spawnedThisWave;
        int remainingAliveCap = settings.maxAliveMobs() - mobs.size();
        int amount = Math.min(Math.min(batchSize, remainingThisWave), remainingAliveCap);
        for (int i = 0; i < amount && running; i++) {
            spawnMob();
        }
    }

    private void spawnMob() {
        EntityType type = randomMobType();
        if (type == null || !type.isSpawnable()) {
            return;
        }
        Location spawn = chooseSpawnLocation();
        Entity entity;
        try {
            entity = world.spawnEntity(spawn, type);
        } catch (IllegalStateException ex) {
            stop(messages.get("session.no-spawn"), false);
            return;
        }
        marker.mark(entity);
        mobs.add(entity.getUniqueId());
        spawnedThisWave++;
        if (entity instanceof LivingEntity living) {
            living.setRemoveWhenFarAway(false);
            living.setCustomName("尸潮");
            living.setCustomNameVisible(false);
            applyMobHealth(living);
        }
    }

    private void applyMobHealth(LivingEntity living) {
        AttributeInstance maxHealth = living.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        double multiplier = settings.mobHealthMultiplier()
                + guildLevel * settings.mobHealthPerGuildLevel()
                + (wave - 1) * settings.mobHealthPerWave();
        double health = Math.max(1.0, maxHealth.getBaseValue() * multiplier);
        maxHealth.setBaseValue(health);
        living.setHealth(Math.min(health, maxHealth.getValue()));
    }

    private Location chooseSpawnLocation() {
        if (!roadSpawnChunks.isEmpty()) {
            ChunkPos chunk = chooseRoadSpawnChunk();
            return roadSpawnLocation(chunk.chunkX(), chunk.chunkZ());
        }
        return randomSpawnLocation();
    }

    private ChunkPos chooseRoadSpawnChunk() {
        List<Player> players = world.getPlayers();
        if (!settings.preferNearPlayers() || players.isEmpty()
                || roadSpawnChunks.size() <= settings.nearPlayerCandidateCount()) {
            return roadSpawnChunks.get(random.nextInt(roadSpawnChunks.size()));
        }
        int limit = Math.min(settings.nearPlayerCandidateCount(), roadSpawnChunks.size());
        List<ChunkPos> nearby = roadSpawnChunks.stream()
                .sorted(Comparator.comparingDouble(chunk -> nearestPlayerDistanceSquared(chunk, players)))
                .limit(limit)
                .toList();
        return nearby.get(random.nextInt(nearby.size()));
    }

    private double nearestPlayerDistanceSquared(ChunkPos chunk, List<Player> players) {
        double x = (chunk.chunkX() << 4) + 8.0;
        double z = (chunk.chunkZ() << 4) + 8.0;
        double best = Double.MAX_VALUE;
        for (Player player : players) {
            Location location = player.getLocation();
            double dx = location.getX() - x;
            double dz = location.getZ() - z;
            best = Math.min(best, dx * dx + dz * dz);
        }
        return best;
    }

    private Location randomSpawnLocation() {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = settings.spawnRadius() * (0.6 + random.nextDouble() * 0.4);
        double x = center.getX() + Math.cos(angle) * distance;
        double z = center.getZ() + Math.sin(angle) * distance;
        int y = Math.max(world.getMinHeight() + 1, world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1);
        return new Location(world, x, y, z);
    }

    private List<ChunkPos> buildRoadSpawnChunks() {
        Optional<TerritoryMapSnapshot> snapshot = api.campSnapshot(guild);
        if (snapshot.isEmpty()) {
            return List.of();
        }
        List<ChunkPos> candidates = new ArrayList<>();
        for (TerritoryMapChunk chunk : snapshot.get().chunks()) {
            addRoadCandidate(candidates, chunk.chunkX() + 1, chunk.chunkZ());
            addRoadCandidate(candidates, chunk.chunkX() - 1, chunk.chunkZ());
            addRoadCandidate(candidates, chunk.chunkX(), chunk.chunkZ() + 1);
            addRoadCandidate(candidates, chunk.chunkX(), chunk.chunkZ() - 1);
        }
        return List.copyOf(candidates);
    }

    private void addRoadCandidate(List<ChunkPos> candidates, int chunkX, int chunkZ) {
        if (isRoadChunk(chunkX, chunkZ)) {
            candidates.add(new ChunkPos(chunkX, chunkZ));
        }
    }

    private boolean isRoadChunk(int chunkX, int chunkZ) {
        Location probe = new Location(world, (chunkX << 4) + 8.0, center.getY(), (chunkZ << 4) + 8.0);
        return api.kindAt(probe) == RegionKind.ROAD;
    }

    private Location roadSpawnLocation(int chunkX, int chunkZ) {
        double x = (chunkX << 4) + 8.0;
        double z = (chunkZ << 4) + 8.0;
        int y = Math.max(world.getMinHeight() + 1,
                world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1);
        return new Location(world, x, y, z);
    }

    private EntityType randomMobType() {
        List<EntityType> mobs = settings.mobTypes();
        if (mobs.isEmpty()) {
            return EntityType.ZOMBIE;
        }
        return mobs.get(random.nextInt(mobs.size()));
    }

    private int aliveMobs() {
        cleanupDeadMobs();
        return mobs.size();
    }

    private void cleanupDeadMobs() {
        mobs.removeIf(uuid -> {
            Entity entity = Bukkit.getEntity(uuid);
            return entity == null || entity.isDead() || !entity.isValid();
        });
    }

    private void syncPlayers() {
        bossBar.removeAll();
        for (Player player : world.getPlayers()) {
            bossBar.addPlayer(player);
        }
    }

    private void updateBar() {
        double progress;
        String title;
        if (intermission) {
            progress = intermissionSecondsLeft <= 0 ? 0.0 : Math.max(0.0,
                    intermissionSecondsLeft / (double) Math.max(1, settings.intermissionSeconds()));
            title = messages.get("boss.intermission", "wave", wave - 1);
        } else {
            int alive = Math.max(0, mobs.size());
            progress = settings.waveDurationSeconds() <= 0 ? 1.0 : Math.max(0.0,
                    1.0 - (aliveSeconds / (double) settings.waveDurationSeconds()));
            title = messages.get("boss.wave",
                    "wave", wave,
                    "max_waves", settings.maxWaves(),
                    "spawned", spawnedThisWave,
                    "target", targetThisWave,
                    "alive", alive);
        }
        bossBar.setProgress(Math.min(1.0, Math.max(0.0, progress)));
        bossBar.setTitle(title);
    }

    private void broadcast(String message) {
        for (Player player : world.getPlayers()) {
            player.sendMessage(message);
        }
    }

    private void rewardPlayers() {
        int exp = settings.rewardExp() + guildLevel * settings.rewardExpPerGuildLevel();
        for (Player player : world.getPlayers()) {
            player.giveExp(exp);
            player.sendMessage(messages.get("session.reward", "exp", exp));
        }
    }

    private void applyForcedDifficulty() {
        if (!settings.forceDifficultyEnabled()) {
            return;
        }
        Difficulty target = settings.forceDifficulty();
        if (world.getDifficulty() == target) {
            return;
        }
        world.setDifficulty(target);
        difficultyChanged = true;
        broadcast(messages.get("session.difficulty-forced", "difficulty", displayDifficulty(target)));
    }

    private void restoreDifficulty() {
        if (!difficultyChanged || !settings.restorePreviousDifficulty()) {
            return;
        }
        World currentWorld = Bukkit.getWorld(world.getName());
        if (currentWorld == null || currentWorld.getDifficulty() != settings.forceDifficulty()) {
            return;
        }
        currentWorld.setDifficulty(originalDifficulty);
        broadcast(messages.get("session.difficulty-restored", "difficulty", displayDifficulty(originalDifficulty)));
    }

    private String displayDifficulty(Difficulty difficulty) {
        return switch (difficulty) {
            case PEACEFUL -> "和平";
            case EASY -> "简单";
            case NORMAL -> "普通";
            case HARD -> "困难";
        };
    }

    private record ChunkPos(int chunkX, int chunkZ) {}
}
