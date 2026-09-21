package org.marj4n.smooth_progression.integration;

import net.minecraft.server.network.ServerPlayerEntity;

public final class SimplySkillsIntegration {

    private SimplySkillsIntegration() {
    }

    public static void onPlayerLevelUp(
            ServerPlayerEntity player
    ) {
        PufferfishSkillsIntegration.onPlayerLevelUp(player);
    }

    public static int getSkillPoints(
            ServerPlayerEntity player
    ) {
        return PufferfishSkillsIntegration.getLevelUpPoints(player);
    }
}