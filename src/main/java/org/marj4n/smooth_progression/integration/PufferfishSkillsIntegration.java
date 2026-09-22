package org.marj4n.smooth_progression.integration;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.SkillsAPI;

import java.util.Optional;

public final class PufferfishSkillsIntegration {

    public static final int POINTS_PER_LEVEL = 2;

    private static final Identifier SIMPLY_SKILLS_TREE =
            new Identifier("simplyskills", "tree");

    private static final Identifier LEVEL_UP_SOURCE =
            new Identifier("smoothprogression", "level_up");

    private PufferfishSkillsIntegration() {
    }

    // =========================================================
    // LEVEL-UP REWARD
    // =========================================================

    public static void onPlayerLevelUp(
            ServerPlayerEntity player
    ) {

        addSkillPoints(
                player,
                POINTS_PER_LEVEL
        );
    }

    // =========================================================
    // VALIDATE SKILL POINT REWARD
    // =========================================================

    public static boolean canAwardSkillPoints(
            ServerPlayerEntity player,
            int amount
    ) {

        if (player == null || amount < 0) {
            return false;
        }

        Optional<Category> optionalCategory =
                SkillsAPI.getCategory(SIMPLY_SKILLS_TREE);

        if (optionalCategory.isEmpty()) {
            return false;
        }

        Category category = optionalCategory.get();

        if (!category.isUnlocked(player)) {
            return false;
        }

        int currentPoints =
                category.getPoints(
                        player,
                        LEVEL_UP_SOURCE
                );

        return (long) currentPoints + amount
                <= Integer.MAX_VALUE;
    }

    // =========================================================
    // AWARD SKILL POINTS
    //
    // Returns true when the operation succeeds.
    // =========================================================

    public static boolean addSkillPoints(
            ServerPlayerEntity player,
            int amount
    ) {

        if (!canAwardSkillPoints(player, amount)) {
            return false;
        }

        if (amount == 0) {
            return true;
        }

        Category category =
                SkillsAPI.getCategory(SIMPLY_SKILLS_TREE)
                        .orElseThrow();

        category.addPoints(
                player,
                LEVEL_UP_SOURCE,
                amount
        );

        return true;
    }

    // =========================================================
    // GET POINTS FROM SMOOTH PROGRESSION
    // =========================================================

    public static int getLevelUpPoints(
            ServerPlayerEntity player
    ) {

        if (player == null) {
            return 0;
        }

        return SkillsAPI.getCategory(SIMPLY_SKILLS_TREE)
                .map(category ->
                        category.getPoints(
                                player,
                                LEVEL_UP_SOURCE
                        )
                )
                .orElse(0);
    }
}