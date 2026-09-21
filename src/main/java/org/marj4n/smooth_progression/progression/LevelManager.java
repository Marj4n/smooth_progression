package org.marj4n.smooth_progression.progression;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.marj4n.smooth_progression.integration.PufferfishSkillsIntegration;

public final class LevelManager {

    private LevelManager() {
    }

    public static void addExperience(
            ServerPlayerEntity player,
            int amount
    ) {

        if (amount <= 0) {
            return;
        }

        ProgressionState state =
                ProgressionManager.getState(player);

        PlayerProgression progression =
                state.getOrCreate(player);

        progression.setExperience(
                progression.getExperience() + amount
        );

        checkLevelUp(
                player,
                progression
        );

        state.markProgressionDirty();
    }

    private static void checkLevelUp(
            ServerPlayerEntity player,
            PlayerProgression progression
    ) {

        while (ExperienceManager.canLevelUp(progression)) {

            int required =
                    ExperienceManager.getRequiredExperience(
                            progression.getLevel()
                    );

            progression.setExperience(
                    progression.getExperience() - required
            );

            progression.setLevel(
                    progression.getLevel() + 1
            );

            onLevelUp(
                    player,
                    progression
            );
        }
    }

    private static void onLevelUp(
            ServerPlayerEntity player,
            PlayerProgression progression
    ) {

        PufferfishSkillsIntegration.onPlayerLevelUp(
                player
        );

        player.sendMessage(
                Text.literal(
                        "§aLevel Up! §fYou are now level "
                                + progression.getLevel()
                ),
                false
        );
    }
}