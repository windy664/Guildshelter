package org.windy.guildshelter.farm;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 偷菜每日额度（内存,按自然日重置）。{@link FarmCheckProvider} 读"今天还能不能偷",
 * {@link FarmStealListener} 在真偷成时记一次。重启清零（偷菜是娱乐玩法,不需持久）。
 */
final class StealQuota {

    private record Day(LocalDate date, int count) {}

    private final Map<UUID, Day> byPlayer = new ConcurrentHashMap<>();
    private final int dailyLimit; // -1 = 不限

    StealQuota(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    /** 今天是否还能偷（未达上限）。 */
    boolean canSteal(UUID player) {
        if (dailyLimit < 0) {
            return true;
        }
        Day d = byPlayer.get(player);
        LocalDate today = LocalDate.now();
        if (d == null || !d.date().equals(today)) {
            return true; // 新的一天 / 从未偷
        }
        return d.count() < dailyLimit;
    }

    /** 记一次成功偷菜，返回今日累计次数。 */
    int record(UUID player) {
        LocalDate today = LocalDate.now();
        Day d = byPlayer.compute(player, (k, old) ->
                (old == null || !old.date().equals(today)) ? new Day(today, 1) : new Day(today, old.count() + 1));
        return d.count();
    }

    int dailyLimit() {
        return dailyLimit;
    }
}
