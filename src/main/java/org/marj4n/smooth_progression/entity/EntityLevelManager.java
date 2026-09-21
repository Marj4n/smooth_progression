package org.marj4n.smooth_progression.entity;

public final class EntityLevelManager {

    private EntityLevelManager() {
    }

    public static int getEntityLevel(
            int playerLevel,
            int distanceFromSpawn
    ) {

        int level = playerLevel;

        if (distanceFromSpawn > 1000) {
            level += 5;
        }

        if (distanceFromSpawn > 3000) {
            level += 10;
        }

        return Math.max(1, level);
    }
}