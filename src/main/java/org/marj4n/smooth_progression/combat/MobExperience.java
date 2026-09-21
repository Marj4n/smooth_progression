package org.marj4n.smooth_progression.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.marj4n.smooth_progression.progression.ExperienceApi;
import org.marj4n.smooth_progression.progression.XpSource;

public final class MobExperience {

    private MobExperience() {
    }

    public static void onMobKilled(
            ServerPlayerEntity player,
            LivingEntity entity
    ) {

        if (player == null || entity == null) {
            return;
        }

        int xp = calculateExperience(entity);

        if (xp <= 0) {
            return;
        }

        ExperienceApi.addExperience(
                player,
                xp,
                XpSource.MOB_KILL
        );
    }

    private static int calculateExperience(
            LivingEntity entity
    ) {

        float maxHealth =
                entity.getMaxHealth();

        int xp =
                Math.round(maxHealth / 2.0F);

        return Math.max(1, xp);
    }
}