package org.marj4n.smooth_progression;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.marj4n.smooth_progression.combat.MobExperience;
import org.marj4n.smooth_progression.command.ProgressionCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.marj4n.smooth_progression.integration.PufferfishSkillsIntegration;

public class SmoothProgression implements ModInitializer {

    public static final String MOD_ID = "smoothprogression";

    public static final Logger LOGGER =
            LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {

        LOGGER.info("Initializing Smooth Progression");

        ProgressionCommands.register();

        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> {

                    ServerPlayerEntity player =
                            handler.getPlayer();

                    PufferfishSkillsIntegration
                            .disableAutomaticKillExperience(player);
                }
        );

        ServerLivingEntityEvents.AFTER_DEATH.register(
                (entity, damageSource) -> {

                    LOGGER.info(
                            "SMOOTH DEBUG: {} died, attacker={}",
                            entity.getType().toString(),
                            damageSource.getAttacker()
                    );

                    if (!(damageSource.getAttacker()
                            instanceof PlayerEntity playerEntity)) {
                        return;
                    }

                    if (!(playerEntity instanceof ServerPlayerEntity player)) {
                        return;
                    }

                    LOGGER.info(
                            "SMOOTH DEBUG: awarding XP to {}",
                            player.getName().getString()
                    );

                    MobExperience.onMobKilled(
                            player,
                            entity
                    );
                }
        );
    }
}