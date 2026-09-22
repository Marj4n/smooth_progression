package org.marj4n.smooth_progression.integration;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
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

    // =========================================================
    // DEBUG SIMPLYSKILLS CATEGORIES
    // =========================================================

    public static void debugUnlockedCategories(
            ServerPlayerEntity player
    ) {

        if (player == null) {
            return;
        }

        player.sendMessage(
                Text.literal(
                        "§6=== SimplySkills Categories ==="
                ),
                false
        );

        SkillsAPI.streamCategories()
                .filter(category ->
                        category.getId()
                                .getNamespace()
                                .equals("simplyskills")
                )
                .forEach(category -> {

                    String categoryId =
                            category.getId().toString();

                    boolean unlocked =
                            category.isUnlocked(player);

                    int spent =
                            category.getSpentPoints(player);

                    int pointsLeft =
                            category.getPointsLeft(player);

                    int pointsTotal =
                            category.getPointsTotal(player);

                    String statusColor =
                            unlocked ? "§a" : "§c";

                    player.sendMessage(
                            Text.literal(
                                    "§e" + categoryId
                                            + "\n§7Unlocked: "
                                            + statusColor
                                            + unlocked
                                            + " §7| Spent: §f"
                                            + spent
                                            + " §7| Left: §f"
                                            + pointsLeft
                                            + " §7| Total: §f"
                                            + pointsTotal
                            ),
                            false
                    );
                });
    }
}