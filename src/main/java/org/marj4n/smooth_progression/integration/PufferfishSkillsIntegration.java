package org.marj4n.smooth_progression.integration;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.SkillsAPI;
import net.puffish.skillsmod.experience.source.builtin.KillEntityExperienceSource;

import java.util.Optional;

public final class PufferfishSkillsIntegration {

    public static final int POINTS_PER_LEVEL = 2;

    private static final Identifier SIMPLY_SKILLS_TREE =
            new Identifier("simplyskills", "tree");

    private static final Identifier LEVEL_UP_SOURCE =
            new Identifier("smoothprogression", "level_up");

    private PufferfishSkillsIntegration() {
    }

    public static void onPlayerLevelUp(ServerPlayerEntity player) {
        addSkillPoints(player, POINTS_PER_LEVEL);
    }

    public static void addSkillPoints(
            ServerPlayerEntity player,
            int amount
    ) {

        if (amount <= 0) {
            return;
        }

        Optional<Category> category =
                SkillsAPI.getCategory(SIMPLY_SKILLS_TREE);

        if (category.isEmpty()) {
            return;
        }

        Category simplySkillsTree =
                category.get();

        if (!simplySkillsTree.isUnlocked(player)) {
            return;
        }

        simplySkillsTree.addPoints(
                player,
                LEVEL_UP_SOURCE,
                amount
        );
    }

    public static int getLevelUpPoints(
            ServerPlayerEntity player
    ) {

        Optional<Category> category =
                SkillsAPI.getCategory(SIMPLY_SKILLS_TREE);

        if (category.isEmpty()) {
            return 0;
        }

        return category.get().getPoints(
                player,
                LEVEL_UP_SOURCE
        );
    }

    public static void disableAutomaticKillExperience(
            ServerPlayerEntity player
    ) {

        SkillsAPI.updateExperienceSources(
                player,
                KillEntityExperienceSource.class,
                source -> 0
        );
    }
}