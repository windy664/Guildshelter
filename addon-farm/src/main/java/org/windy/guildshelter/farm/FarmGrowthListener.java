package org.windy.guildshelter.farm;

import org.bukkit.Location;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.windy.guildshelter.api.GuildShelterAPI;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A2 生长加速：共享农场区作物生长 / 幼崽成长随公会等级概率性加速。纯只读 {@link GuildShelterAPI}。
 */
public final class FarmGrowthListener implements Listener {

    private final GuildShelterAPI api;
    private final double cropPerLevel;
    private final double cropMaxChance;
    private final boolean breedEnabled;
    private final double breedPerLevel;
    private final double breedMaxChance;
    private final int breedBumpTicks;

    public FarmGrowthListener(GuildShelterAPI api, double cropPerLevel, double cropMaxChance,
                              boolean breedEnabled, double breedPerLevel, double breedMaxChance, int breedBumpTicks) {
        this.api = api;
        this.cropPerLevel = cropPerLevel;
        this.cropMaxChance = cropMaxChance;
        this.breedEnabled = breedEnabled;
        this.breedPerLevel = breedPerLevel;
        this.breedMaxChance = breedMaxChance;
        this.breedBumpTicks = breedBumpTicks;
    }

    /** 命中概率 = min(maxChance, 公会等级 × perLevel)；非农场区/无公会/0 级 → 0。 */
    private double hitChance(Location loc, double perLevel, double maxChance) {
        if (!api.isFarmZone(loc)) {
            return 0;
        }
        int level = api.guildAt(loc).map(api::guildLevel).orElse(0);
        return level <= 0 ? 0 : Math.min(maxChance, level * perLevel);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGrow(BlockGrowEvent event) {
        double chance = hitChance(event.getBlock().getLocation(), cropPerLevel, cropMaxChance);
        if (chance <= 0 || ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }
        BlockData data = event.getNewState().getBlockData();
        if (data instanceof Ageable age && age.getAge() < age.getMaximumAge()) {
            age.setAge(age.getAge() + 1); // 多长一阶
            event.getNewState().setBlockData(age);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (!breedEnabled) {
            return;
        }
        Entity baby = event.getEntity();
        double chance = hitChance(baby.getLocation(), breedPerLevel, breedMaxChance);
        if (chance <= 0 || ThreadLocalRandom.current().nextDouble() >= chance) {
            return;
        }
        if (baby instanceof org.bukkit.entity.Ageable a && !a.isAdult()) {
            a.setAge(Math.min(0, a.getAge() + breedBumpTicks)); // 加速成年
        }
    }
}
