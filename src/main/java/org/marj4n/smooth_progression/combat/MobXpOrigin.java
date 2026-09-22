package org.marj4n.smooth_progression.combat;

import net.minecraft.entity.Entity;

public final class MobXpOrigin {

    private static final String SPAWNER_TAG =
            "smoothprogression.spawner";

    private MobXpOrigin() {
    }

    // =========================================================
    // MARK SPAWNER MOB
    // =========================================================

    public static void markSpawner(Entity entity) {

        if (entity == null) {
            return;
        }

        entity.addCommandTag(SPAWNER_TAG);
    }

    // =========================================================
    // CHECK SPAWNER ORIGIN
    // =========================================================

    public static boolean isSpawner(Entity entity) {

        return entity != null
                && entity.getCommandTags().contains(
                SPAWNER_TAG
        );
    }
}