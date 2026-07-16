package org.windy.guildshelter.horde;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.windy.guildshelter.api.GuildRef;
import org.windy.guildshelter.api.GuildShelterAPI;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

final class WeeklyHordeScheduler {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");

    private final JavaPlugin plugin;
    private final GuildShelterAPI api;
    private final HordeManager manager;
    private final Logger logger;
    private final boolean enabled;
    private final ZoneId zoneId;
    private final int checkIntervalSeconds;
    private final List<Slot> slots;
    private final Set<String> handledWindows = new HashSet<>();
    private final Deque<QueuedCamp> pendingCamps = new ArrayDeque<>();
    private int nextCampIndex;
    private BukkitTask task;

    private WeeklyHordeScheduler(JavaPlugin plugin, GuildShelterAPI api, HordeManager manager, boolean enabled,
                                 ZoneId zoneId, int checkIntervalSeconds, List<Slot> slots) {
        this.plugin = plugin;
        this.api = api;
        this.manager = manager;
        this.logger = plugin.getLogger();
        this.enabled = enabled;
        this.zoneId = zoneId;
        this.checkIntervalSeconds = checkIntervalSeconds;
        this.slots = slots;
    }

    static WeeklyHordeScheduler create(JavaPlugin plugin, GuildShelterAPI api, HordeManager manager, FileConfiguration config) {
        boolean enabled = config.getBoolean("weekly.enabled", false);
        ZoneId zoneId = parseZone(config.getString("weekly.timezone", ZoneId.systemDefault().getId()), plugin.getLogger());
        int checkIntervalSeconds = Math.max(30, config.getInt("weekly.check-interval-seconds", 60));
        List<Slot> slots = loadSlots(config, plugin.getLogger());
        return new WeeklyHordeScheduler(plugin, api, manager, enabled, zoneId, checkIntervalSeconds, slots);
    }

    void start() {
        if (!enabled) {
            logger.info("GuildShelter 尸潮周常排期未启用。");
            return;
        }
        if (slots.isEmpty()) {
            logger.warning("GuildShelter 尸潮周常排期已启用，但没有有效时间槽。");
            return;
        }
        long periodTicks = checkIntervalSeconds * 20L;
        this.task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, periodTicks);
        logger.info("GuildShelter 尸潮周常排期已启用，时间槽数量=" + slots.size()
                + "，时区=" + zoneId + "。");
    }

    void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        handledWindows.clear();
        pendingCamps.clear();
    }

    Optional<ScheduleEta> etaFor(GuildRef camp) {
        if (!enabled || slots.isEmpty() || camp == null) {
            return Optional.empty();
        }
        Optional<QueuedCamp> queued = pendingCamps.stream()
                .filter(candidate -> candidate.worldName().equals(camp.worldName()))
                .findFirst();
        if (queued.isPresent()) {
            return Optional.of(ScheduleEta.queued(camp));
        }
        List<GuildRef> camps = sortedCamps();
        if (camps.isEmpty()) {
            return Optional.empty();
        }
        int targetIndex = indexOf(camps, camp);
        if (targetIndex < 0) {
            return Optional.empty();
        }
        int currentIndex = Math.floorMod(nextCampIndex, camps.size());
        int windowsUntilCamp = Math.floorMod(targetIndex - currentIndex, camps.size());
        ZonedDateTime dueAt = futureSlotAt(ZonedDateTime.now(zoneId), windowsUntilCamp);
        return Optional.of(ScheduleEta.scheduled(camp, dueAt));
    }

    private void tick() {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        int currentWeekMinute = weekMinute(now.getDayOfWeek(), now.toLocalTime());
        List<GuildRef> camps = sortedCamps();
        if (slots.isEmpty()) {
            return;
        }
        if (!camps.isEmpty()) {
            enqueueDueWindow(now, currentWeekMinute, camps);
        }
        drainPendingQueue();
        if (handledWindows.size() > 4096) {
            handledWindows.clear();
        }
    }

    private List<GuildRef> sortedCamps() {
        return api.guildCamps().stream()
                .sorted(Comparator.comparing(GuildRef::id).thenComparing(GuildRef::worldName))
                .toList();
    }

    private static int indexOf(List<GuildRef> camps, GuildRef camp) {
        for (int i = 0; i < camps.size(); i++) {
            GuildRef candidate = camps.get(i);
            if (candidate.worldName().equals(camp.worldName())) {
                return i;
            }
        }
        return -1;
    }

    private ZonedDateTime futureSlotAt(ZonedDateTime now, int futureIndex) {
        int seen = 0;
        for (int weekOffset = 0; weekOffset < 520; weekOffset++) {
            for (Slot slot : slots) {
                long dayOffset = (long) weekOffset * 7 + slot.day().getValue() - now.getDayOfWeek().getValue();
                ZonedDateTime candidate = now.toLocalDate().plusDays(dayOffset).atTime(slot.time()).atZone(zoneId);
                if (!candidate.isAfter(now)) {
                    continue;
                }
                if (seen == futureIndex) {
                    return candidate;
                }
                seen++;
            }
        }
        return now.plus(Duration.ofDays(7L * (futureIndex + 1)));
    }

    private void enqueueDueWindow(ZonedDateTime now, int currentWeekMinute, List<GuildRef> camps) {
        for (Slot slot : slots) {
            if (slot.weekMinute() != currentWeekMinute) {
                continue;
            }
            String windowKey = slot.weekMinute() + ":" + now.getYear() + ":" + now.getDayOfYear();
            if (!handledWindows.add(windowKey)) {
                return;
            }
            GuildRef camp = camps.get(nextCampIndex % camps.size());
            nextCampIndex = (nextCampIndex + 1) % camps.size();
            pendingCamps.addLast(new QueuedCamp(windowKey, slot.label(), camp.id(), camp.worldName()));
            logger.info("尸潮周常已入队 [" + slot.label() + "] 营地="
                    + camp.id() + " 世界=" + camp.worldName() + "。");
            return;
        }
    }

    private void drainPendingQueue() {
        if (manager.hasActiveHorde()) {
            return;
        }
        while (!pendingCamps.isEmpty()) {
            QueuedCamp queued = pendingCamps.peekFirst();
            if (!isStillValidCamp(queued)) {
                pendingCamps.removeFirst();
                logger.warning("尸潮周常已丢弃无效队列项 [" + queued.slotLabel()
                        + "] 营地=" + queued.campId() + " 世界=" + queued.worldName()
                        + "：营地世界已失效。");
                continue;
            }
            if (manager.tryStartAtWorldName(queued.worldName())) {
                pendingCamps.removeFirst();
                logger.info("尸潮周常已启动 [" + queued.slotLabel() + "] 营地="
                        + queued.campId() + " 世界=" + queued.worldName() + "。");
            }
            return;
        }
    }

    private boolean isStillValidCamp(QueuedCamp queued) {
        org.bukkit.World world = plugin.getServer().getWorld(queued.worldName());
        if (world == null) {
            return false;
        }
        Optional<GuildRef> current = api.guildAt(world.getSpawnLocation());
        return current.isPresent() && queued.worldName().equals(current.get().worldName());
    }

    private static ZoneId parseZone(String raw, Logger logger) {
        try {
            return ZoneId.of(raw);
        } catch (Exception ex) {
            ZoneId fallback = ZoneId.systemDefault();
            logger.warning("weekly.timezone 配置无效：'" + raw + "'，改用 " + fallback + "。");
            return fallback;
        }
    }

    private static List<Slot> loadSlots(FileConfiguration config, Logger logger) {
        List<Slot> parsed = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (Map<?, ?> raw : config.getMapList("weekly.slots")) {
            Slot slot = parseSlot(raw, logger);
            if (slot == null) {
                continue;
            }
            if (!seen.add(slot.weekMinute())) {
                logger.warning("跳过重复尸潮周常时间槽 '" + slot.label() + "'：同一天同一时间已存在。");
                continue;
            }
            parsed.add(slot);
        }
        parsed.sort(Comparator.comparingInt(Slot::weekMinute));
        return List.copyOf(parsed);
    }

    private static Slot parseSlot(Map<?, ?> raw, Logger logger) {
        String label = string(raw.get("label"), "slot");
        DayOfWeek day = parseDay(string(raw.get("day"), ""));
        if (day == null) {
            logger.warning("跳过尸潮周常时间槽 '" + label + "'：day 无效。");
            return null;
        }
        LocalTime time;
        try {
            time = LocalTime.parse(string(raw.get("time"), ""), TIME_FORMAT);
        } catch (DateTimeParseException ex) {
            logger.warning("跳过尸潮周常时间槽 '" + label + "'：time 无效。");
            return null;
        }
        return new Slot(label, day, time, weekMinute(day, time));
    }

    private static DayOfWeek parseDay(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim();
        try {
            int numeric = Integer.parseInt(value);
            if (numeric >= 1 && numeric <= 7) {
                return DayOfWeek.of(numeric);
            }
        } catch (NumberFormatException ignored) {
            // Try enum names below.
        }
        try {
            return DayOfWeek.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static int weekMinute(DayOfWeek day, LocalTime time) {
        return (day.getValue() - 1) * 24 * 60 + time.getHour() * 60 + time.getMinute();
    }

    private static String string(Object value, String fallback) {
        return value == null ? fallback : value.toString().trim();
    }

    private record Slot(String label, DayOfWeek day, LocalTime time, int weekMinute) {}

    private record QueuedCamp(String windowKey, String slotLabel, String campId, String worldName) {}

    record ScheduleEta(GuildRef camp, ZonedDateTime dueAt, boolean queued) {
        static ScheduleEta queued(GuildRef camp) {
            return new ScheduleEta(camp, null, true);
        }

        static ScheduleEta scheduled(GuildRef camp, ZonedDateTime dueAt) {
            return new ScheduleEta(camp, dueAt, false);
        }
    }
}
