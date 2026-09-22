package org.marj4n.smooth_progression.integration;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.SkillsAPI;

import java.util.List;

public final class SimplySkillsIntegration {

    private static final Identifier TREE_ID =
            new Identifier("simplyskills", "tree");

    private SimplySkillsIntegration() {
    }

    // =========================================================
    // RESET ALL SIMPLY SKILLS PROGRESSION
    // =========================================================

    public static void resetSkillsAndPoints(
            ServerPlayerEntity player
    ) {

        if (player == null) {
            return;
        }

        List<Category> categories =
                SkillsAPI.streamCategories()
                        .filter(category ->
                                category.getId()
                                        .getNamespace()
                                        .equals("simplyskills")
                        )
                        .toList();

        // STEP 1:
        // Reset purchased skills first so their rewards
        // and skill-lock events are processed.
        for (Category category : categories) {

            category.resetSkills(player);
        }

        // STEP 2:
        // Erase category data, including XP and points.
        // This also triggers a category UI update.
        for (Category category : categories) {

            category.erase(player);
        }

        // STEP 3:
        // Explicitly lock evolution/class categories.
        for (Category category : categories) {

            if (category.getId().equals(TREE_ID)) {
                continue;
            }

            category.lock(player);
        }

        // STEP 4:
        // Restore the starting skill tree.
        SkillsAPI.getCategory(TREE_ID)
                .ifPresent(category ->
                        category.unlock(player)
                );
    }
}