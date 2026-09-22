package org.marj4n.smooth_progression.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import org.marj4n.smooth_progression.progression.ExperienceApi;
import org.marj4n.smooth_progression.progression.XpSource;

import java.util.UUID;

public final class MobExperience {

    private static final String LEVEL_PREFIX =
            "smoothprogression.level.";

    // Must match MobLevelScaling.HEALTH_MODIFIER_ID.
    private static final UUID HEALTH_MODIFIER_ID =
            UUID.fromString(
                    "f6a2791b-857e-48c2-9ab9-df4e22b5f8d0"
            );

    private static final double XP_PER_LEVEL = 0.01D;
    private static final double MAX_XP_MULTIPLIER = 3.0D;

    private MobExperience() {
    }

    // =========================================================
    // MOB KILL
    // =========================================================

    public static void onMobKilled(
            LivingEntity victim,
            DamageSource damageSource
    ) {

        if (victim == null || damageSource == null) {
            return;
        }

        // PvP XP is disabled.
        if (victim instanceof PlayerEntity) {
            return;
        }

        ServerPlayerEntity player =
                KillOwnerResolver.resolve(damageSource);

        if (player == null) {
            return;
        }

        long experience =
                calculateExperience(victim);

        if (experience <= 0L) {
            return;
        }

        // Spawner mobs receive 50% XP.
        if (MobXpOrigin.isSpawner(victim)) {

            experience = Math.max(
                    1L,
                    experience / 2L
            );
        }

        ExperienceApi.addExperience(
                player,
                experience,
                XpSource.MOB_KILL
        );
    }

    // =========================================================
    // EXPERIENCE CALCULATION
    //
    // XP = Base Max Health / 2 * Level Multiplier
    //
    // Non-scaled mobs retain their original XP behavior.
    // =========================================================

    public static long calculateExperience(
            LivingEntity entity
    ) {

        if (entity == null) {
            return 0L;
        }

        double maxHealth = entity.getMaxHealth();

        if (!Double.isFinite(maxHealth)
                || maxHealth <= 0.0D) {

            return 0L;
        }

        int level = getMobLevel(entity);

        // Only mobs with an assigned progression level
        // receive the level-based reward.
        if (level > 0) {

            maxHealth = getHealthBeforeScaling(
                    entity,
                    maxHealth
            );
        }

        double calculated = maxHealth / 2.0D;

        if (level > 0) {

            double multiplier = Math.min(
                    MAX_XP_MULTIPLIER,
                    1.0D + XP_PER_LEVEL * (level - 1)
            );

            calculated *= multiplier;
        }

        if (!Double.isFinite(calculated)
                || calculated >= Long.MAX_VALUE) {

            return Long.MAX_VALUE;
        }

        return Math.max(
                1L,
                Math.round(calculated)
        );
    }

    // =========================================================
    // READ MOB LEVEL
    // =========================================================

    private static int getMobLevel(
            LivingEntity entity
    ) {

        for (String tag : entity.getCommandTags()) {

            if (!tag.startsWith(LEVEL_PREFIX)) {
                continue;
            }

            try {

                int level = Integer.parseInt(
                        tag.substring(
                                LEVEL_PREFIX.length()
                        )
                );

                if (level >= 1) {
                    return level;
                }

            } catch (NumberFormatException ignored) {
                // Ignore invalid level tags.
            }
        }

        return 0;
    }

    // =========================================================
    // REMOVE OUR HP SCALING FROM XP CALCULATION
    //
    // Attribute modifier uses MULTIPLY_TOTAL:
    //
    // scaledHealth = previousHealth * (1 + modifierValue)
    //
    // This reverses only our modifier.
    // =========================================================

    private static double getHealthBeforeScaling(
            LivingEntity entity,
            double maxHealth
    ) {

        EntityAttributeInstance attribute =
                entity.getAttributeInstance(
                        EntityAttributes.GENERIC_MAX_HEALTH
                );

        if (attribute == null) {
            return maxHealth;
        }

        EntityAttributeModifier modifier =
                attribute.getModifier(
                        HEALTH_MODIFIER_ID
                );

        if (modifier == null) {
            return maxHealth;
        }

        double multiplier =
                1.0D + modifier.getValue();

        if (!Double.isFinite(multiplier)
                || multiplier <= 0.0D) {

            return maxHealth;
        }

        double baseHealth =
                maxHealth / multiplier;

        if (!Double.isFinite(baseHealth)
                || baseHealth <= 0.0D) {

            return maxHealth;
        }

        return baseHealth;
    }
}