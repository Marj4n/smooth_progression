package org.marj4n.smooth_progression.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import org.marj4n.smooth_progression.progression.ExperienceApi;
import org.marj4n.smooth_progression.progression.XpSource;

public final class MobExperience {

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
    // XP = Max Health / 2
    // =========================================================

    public static long calculateExperience(
            LivingEntity entity
    ) {

        if (entity == null) {
            return 0L;
        }

        double maxHealth =
                entity.getMaxHealth();

        if (!Double.isFinite(maxHealth)
                || maxHealth <= 0.0D) {

            return 0L;
        }

        double calculated =
                maxHealth / 2.0D;

        if (calculated >= Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }

        return Math.max(
                1L,
                Math.round(calculated)
        );
    }
}