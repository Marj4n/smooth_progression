package org.marj4n.smooth_progression;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import org.marj4n.smooth_progression.combat.MobExperience;
import org.marj4n.smooth_progression.command.ProgressionCommands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmoothProgression implements ModInitializer {

    public static final String MOD_ID =
            "smoothprogression";

    public static final Logger LOGGER =
            LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {

        LOGGER.info("Initializing Smooth Progression");

        ProgressionCommands.register();

        ServerLivingEntityEvents.AFTER_DEATH.register(
                (entity, damageSource) -> {

                    if (!(damageSource.getAttacker()
                            instanceof PlayerEntity playerEntity)) {
                        return;
                    }

                    if (!(playerEntity instanceof ServerPlayerEntity player)) {
                        return;
                    }

                    MobExperience.onMobKilled(
                            player,
                            entity
                    );
                }
        );
    }
}