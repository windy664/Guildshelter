package org.windy.guildshelter.horde;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.windy.guildshelter.api.GuildShelterAPI;

final class HordeCombatListener implements Listener {

    private final GuildShelterAPI api;
    private final HordeManager.Settings settings;
    private final HordeEntityMarker marker;

    HordeCombatListener(GuildShelterAPI api, HordeManager.Settings settings, HordeEntityMarker marker) {
        this.api = api;
        this.settings = settings;
        this.marker = marker;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!settings.bypassPveFlags()) {
            return;
        }
        Entity attacker = resolveSource(event.getDamager());
        Entity victim = event.getEntity();
        boolean hordeAttacker = marker.isHordeEntity(attacker);
        boolean hordeVictim = marker.isHordeEntity(victim);
        if (!hordeAttacker && !hordeVictim) {
            return;
        }
        if (!(attacker instanceof LivingEntity) && !(victim instanceof LivingEntity)) {
            return;
        }
        if (victim instanceof Player player && !settings.bypassInvincibleFlag()
                && api.booleanFlag(player.getLocation(), "invincible")) {
            return;
        }
        if (hordeAttacker && victim instanceof Player) {
            event.setCancelled(false);
            return;
        }
        if (attacker instanceof Player && hordeVictim) {
            event.setCancelled(false);
        }
    }

    private Entity resolveSource(Entity damager) {
        if (damager instanceof Projectile projectile) {
            ProjectileSource source = projectile.getShooter();
            if (source instanceof Entity entity) {
                return entity;
            }
        }
        return damager;
    }
}
