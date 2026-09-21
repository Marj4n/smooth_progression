package org.marj4n.smooth_progression.boss;

public final class BossGateManager {

    private BossGateManager() {
    }

    public static boolean canAccess(
            int playerLevel,
            int requiredLevel
    ) {

        return playerLevel >= requiredLevel;
    }
}