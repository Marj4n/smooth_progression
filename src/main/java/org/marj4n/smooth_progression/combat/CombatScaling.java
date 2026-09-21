package org.marj4n.smooth_progression.combat;

public final class CombatScaling {

    private CombatScaling() {
    }

    public static float getPlayerDamageMultiplier(int level) {

        return 1.0F + (level - 1) * 0.01F;
    }

    public static float getPlayerHealthMultiplier(int level) {

        return 1.0F + (level - 1) * 0.02F;
    }
}