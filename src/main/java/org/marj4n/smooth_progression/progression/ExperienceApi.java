package org.marj4n.smooth_progression.progression;

import net.minecraft.server.network.ServerPlayerEntity;

public final class ExperienceApi {

    private ExperienceApi() {
    }

    public static void addExperience(
            ServerPlayerEntity player,
            int amount,
            XpSource source
    ) {

        if (player == null) {
            return;
        }

        if (amount <= 0) {
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