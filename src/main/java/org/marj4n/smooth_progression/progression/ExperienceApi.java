package org.marj4n.smooth_progression.progression;

import net.minecraft.server.network.ServerPlayerEntity;

public final class ExperienceApi {

    private ExperienceApi() {
    }

    public static void addExperience(
            ServerPlayerEntity player,
            long amount,
            XpSource source
    ) {

        if (player == null || amount <= 0L) {
            return;
        }

        if (source == null) {
            source = XpSource.CUSTOM;
        }

        LevelManager.addExperience(
                player,
                amount
        );
    }
}