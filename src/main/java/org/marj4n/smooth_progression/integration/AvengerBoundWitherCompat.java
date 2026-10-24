package org.marj4n.smooth_progression.integration;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.WitherEntity;
import org.marj4n.smooth_progression.boss.BossProgression;

import java.util.UUID;

/**
 * Data-only compatibility with Smooth Classes: no Smooth Classes classes need
 * to be present at compile time. The persisted owner + summon tags survive
 * chunk unload, world relog and server restart.
 */
public final class AvengerBoundWitherCompat {
    private static final String SUMMON_TAG = "smooth_classes_avenger_summon";
    private static final String OWNER_PREFIX = "smooth_classes_avenger_owner_";

    private AvengerBoundWitherCompat() {}

    public static boolean isBoundWither(Entity entity) {
        if (!(entity instanceof WitherEntity)
                || !entity.getCommandTags().contains(SUMMON_TAG)) return false;
        for (String tag : entity.getCommandTags()) {
            if (!tag.startsWith(OWNER_PREFIX)) continue;
            try {
                UUID.fromString(tag.substring(OWNER_PREFIX.length()));
                return true;
            } catch (IllegalArgumentException ignored) {
                // Never bypass boss mechanics for a forged / broken owner tag.
            }
        }
        return false;
    }

    /** Remove an OLD saved boss HP modifier from an existing bound Wither.
     * Preserve its percentage health so a reload cannot fully heal or kill it.
     */
    public static boolean removeStaleBossHealth(Entity entity) {
        if (!isBoundWither(entity) || !(entity instanceof LivingEntity living)) return false;
        EntityAttributeInstance health = living.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (health == null || health.getModifier(BossProgression.HEALTH_MODIFIER_ID) == null) return false;
        float beforeMax = living.getMaxHealth();
        float fraction = beforeMax > 0F ? Math.max(0F, Math.min(1F, living.getHealth() / beforeMax)) : 1F;
        health.removeModifier(BossProgression.HEALTH_MODIFIER_ID);
        living.setHealth(Math.min(living.getMaxHealth(), Math.max(0.01F, living.getMaxHealth() * fraction)));
        return true;
    }
}
