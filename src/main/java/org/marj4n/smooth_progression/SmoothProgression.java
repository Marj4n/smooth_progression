package org.marj4n.smooth_progression;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import org.marj4n.smooth_progression.combat.MobExperience;
import org.marj4n.smooth_progression.command.ProgressionCommands;
import org.marj4n.smooth_progression.network.ProgressionSync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmoothProgression implements ModInitializer {

    public static final String MOD_ID = "smoothprogression";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Smooth Progression");

        ProgressionCommands.register();
        ProgressionSync.register();

        // =====================================================
        // MOB EXPERIENCE
        // =====================================================

        ServerLivingEntityEvents.AFTER_DEATH.register(
                (entity, damageSource) -> {
                    MobExperience.onMobKilled(
                            entity,
                            damageSource
                    );
                }
        );
    }
}