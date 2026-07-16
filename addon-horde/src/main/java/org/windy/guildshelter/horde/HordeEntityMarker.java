package org.windy.guildshelter.horde;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;

final class HordeEntityMarker {

    private static final String SCOREBOARD_TAG = "guildshelter_horde";

    private final NamespacedKey key;

    HordeEntityMarker(JavaPlugin plugin) {
        this.key = new NamespacedKey(plugin, "horde_entity");
    }

    void mark(Entity entity) {
        entity.addScoreboardTag(SCOREBOARD_TAG);
        entity.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
    }

    boolean isHordeEntity(Entity entity) {
        return entity != null
                && (entity.getScoreboardTags().contains(SCOREBOARD_TAG)
                || entity.getPersistentDataContainer().has(key, PersistentDataType.BYTE));
    }
}
