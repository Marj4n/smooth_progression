package org.marj4n.smooth_progression.progression;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.puffish.skillsmod.api.Category;

import org.marj4n.smooth_progression.integration.PufferfishSkillsIntegration;
import org.marj4n.smooth_progression.integration.PufferfishSkillsIntegration.Destination;
import org.marj4n.smooth_progression.integration.PufferfishSkillsIntegration.Stage;

public final class LevelManager {

    public static final int MAX_ADMIN_LEVEL_GAIN = 10_000;

    // Safety limit for processing one XP event.
    // Remaining XP stays stored for the next event.
    private static final int MAX_LEVELS_PER_EVENT = 10_000;

    private static final Set<UUID> REMINDED = new HashSet<>();

    private LevelManager() {
    }

    // =========================================================
    // ADD XP
    // =========================================================

    public static void addExperience(
            ServerPlayerEntity player,
            long amount
    ) {

        if (player == null || amount <= 0L) {
            return;
        }

        // MAX means no more XP, points or level-up messages.
        if (PufferfishSkillsIntegration.isMaxLevel(player)) {
            return;
        }

        ProgressionState state =
                ProgressionManager.getState(player);

        PlayerProgression progression =
                state.getOrCreate(player);

        long currentXp = progression.getExperience();

        long newXp =
                amount > Long.MAX_VALUE - currentXp
                        ? Long.MAX_VALUE
                        : currentXp + amount;

        applyExperience(
                player,
                progression,
                state,
                newXp
        );
    }

    // =========================================================
    // SET XP
    // =========================================================

    public static void setExperience(
            ServerPlayerEntity player,
            long amount
    ) {

        if (player == null || amount < 0L) {
            return;
        }

        if (PufferfishSkillsIntegration.isMaxLevel(player)) {
            return;
        }

        ProgressionState state =
                ProgressionManager.getState(player);

        PlayerProgression progression =
                state.getOrCreate(player);

        applyExperience(
                player,
                progression,
                state,
                amount
        );
    }

    // =========================================================
    // APPLY XP
    // =========================================================

    private static void applyExperience(
            ServerPlayerEntity player,
            PlayerProgression progression,
            ProgressionState state,
            long newExperience
    ) {

        progression.setExperience(newExperience);
        ProgressionManager.markDirty(player);

        for (int iteration = 0;
             iteration < MAX_LEVELS_PER_EVENT;
             iteration++) {

            Destination destination =
                    PufferfishSkillsIntegration
                            .resolveDestination(player);

            // =================================================
            // MAX LEVEL
            // =================================================

            if (destination.stage() == Stage.MAX_LEVEL) {

                // Clear leftover XP once progression is complete.
                progression.setExperience(0L);
                ProgressionManager.markDirty(player);

                // Intentionally silent.
                return;
            }

            // =================================================
            // MISSING / INVALID STAGE
            // =================================================

            if (destination.category().isEmpty()) {

                sendStageMessage(
                        player,
                        destination.stage()
                );

                // XP remains pending.
                return;
            }

            Category category =
                    destination.category().get();

            // =================================================
            // SPEND AVAILABLE POINTS FIRST
            // =================================================

            if (PufferfishSkillsIntegration.hasAffordableSkill(
                    category,
                    player
            )) {

                remindSkillTree(player);

                // XP remains pending.
                return;
            }

            REMINDED.remove(player.getUuid());
            int currentLevel = progression.getLevel();

            if (currentLevel >= Integer.MAX_VALUE) {
                return;
            }

            long requiredXp =
                    ExperienceManager.getRequiredExperience(
                            currentLevel
                    );

            // =================================================
            // NOT ENOUGH XP
            // =================================================

            if (progression.getExperience() < requiredXp) {
                return;
            }

            // =================================================
            // VALIDATE EXACTLY ONE LEVEL'S REWARD
            // =================================================

            int points =
                    PufferfishSkillsIntegration.POINTS_PER_LEVEL;

            if (!PufferfishSkillsIntegration.canAwardSkillPoints(
                    player,
                    currentLevel,
                    points
            )) {

                sendError(
                        player,
                        "Unable to award skill points to "
                                + category.getId()
                );

                return;
            }

            // =================================================
            // AWARD POINTS BEFORE CHANGING LEVEL
            // =================================================

            boolean awarded =
                    PufferfishSkillsIntegration.addSkillPoints(
                            player,
                            currentLevel,
                            points
                    );

            if (!awarded) {

                sendError(
                        player,
                        "Skill point reward failed."
                );

                return;
            }

            // =================================================
            // APPLY EXACTLY ONE LEVEL
            // =================================================

            progression.setExperience(
                    progression.getExperience() - requiredXp
            );

            progression.setLevel(currentLevel + 1);

            ProgressionManager.markDirty(player);

            // Client displays one toast from the final synced level. No chat spam.

            // Next loop rechecks:
            // - current category
            // - affordable skills
            // - class completion
            // - Ascendancy completion
            // - MAX LEVEL
        }
    }

    // Admin-only test operation: grant levels and points to the CURRENT stage.
    // Does not bypass the General Tree -> class -> Ascendancy order.
    // Use on a backup world; these are real persistent skill points.
    public static int debugAddLevels(ServerPlayerEntity player, int amount) {
        if (player == null || amount <= 0 || amount > MAX_ADMIN_LEVEL_GAIN) return 0;
        ProgressionState state = ProgressionManager.getState(player);
        PlayerProgression progression = state.getOrCreate(player);
        int gained = 0;
        for (int i = 0; i < amount; i++) {
            Destination destination = PufferfishSkillsIntegration.resolveDestination(player);
            if (destination.category().isEmpty() || progression.getLevel() >= Integer.MAX_VALUE) break;
            int currentLevel = progression.getLevel();
            if (!PufferfishSkillsIntegration.addSkillPoints(player, currentLevel,
                    PufferfishSkillsIntegration.POINTS_PER_LEVEL)) break;
            progression.setLevel(currentLevel + 1);
            ProgressionManager.markDirty(player);
            gained++;
        }
        return gained;
    }

    // =========================================================
    // ADMIN: SET LEVEL
    //
    // Does not bypass skill gates.
    // Does not award a batch to one category.
    // =========================================================

    public static int setLevel(
            ServerPlayerEntity player,
            int targetLevel
    ) {

        if (player == null) {
            return 0;
        }

        ProgressionState state =
                ProgressionManager.getState(player);

        PlayerProgression progression =
                state.getOrCreate(player);

        int startingLevel = progression.getLevel();

        if (targetLevel <= startingLevel) {
            return 0;
        }

        long requestedGain =
                (long) targetLevel - startingLevel;

        if (requestedGain > MAX_ADMIN_LEVEL_GAIN) {
            return 0;
        }

        int gained = 0;

        while (progression.getLevel() < targetLevel) {

            Destination destination =
                    PufferfishSkillsIntegration
                            .resolveDestination(player);

            if (destination.stage() == Stage.MAX_LEVEL) {
                break;
            }

            if (destination.category().isEmpty()) {

                sendStageMessage(
                        player,
                        destination.stage()
                );

                break;
            }

            Category category =
                    destination.category().get();

            if (PufferfishSkillsIntegration.hasAffordableSkill(
                    category,
                    player
            )) {

                remindSkillTree(player);

                break;
            }

            int currentLevel = progression.getLevel();

            if (currentLevel >= Integer.MAX_VALUE) {
                break;
            }

            boolean awarded =
                    PufferfishSkillsIntegration.addSkillPoints(
                            player,
                            currentLevel,
                            PufferfishSkillsIntegration.POINTS_PER_LEVEL
                    );

            if (!awarded) {

                sendError(
                        player,
                        "Could not award skill points."
                );

                break;
            }

            progression.setLevel(currentLevel + 1);

            // Preserve pending XP instead of silently deleting it.
            ProgressionManager.markDirty(player);

            gained++;

            // Client displays one toast from the final synced level.
        }

        return gained;
    }

    // =========================================================
    // ADMIN: ADD LEVEL
    // =========================================================

    public static int addLevel(
            ServerPlayerEntity player,
            int amount
    ) {

        if (player == null
                || amount <= 0
                || amount > MAX_ADMIN_LEVEL_GAIN) {

            return 0;
        }

        int currentLevel =
                ProgressionManager.get(player).getLevel();

        if (amount > Integer.MAX_VALUE - currentLevel) {
            return 0;
        }

        return setLevel(
                player,
                currentLevel + amount
        );
    }

    // =========================================================
    // STAGE MESSAGES
    // =========================================================

    private static void sendStageMessage(
            ServerPlayerEntity player,
            Stage stage
    ) {

        switch (stage) {

            case WAITING_FOR_CLASS -> sendError(
                    player,
                    "General Tree complete! Select a class "
                            + "to continue leveling."
            );

            case INVALID_CLASS_SELECTION -> sendError(
                    player,
                    "Multiple classes are unlocked. "
                            + "Exactly one class must be selected."
            );

            case WAITING_FOR_ASCENDANCY -> sendError(
                    player,
                    "Class complete! Unlock Ascendancy "
                            + "to continue leveling."
            );

            case UNAVAILABLE -> sendError(
                    player,
                    "Skill category is unavailable."
            );

            default -> {
                // No message for MAX_LEVEL.
            }
        }
    }

    // One reminder per locked period; rearmed after the gate clears.
    private static void remindSkillTree(ServerPlayerEntity player) {
        if (REMINDED.add(player.getUuid())) {
            player.sendMessage(Text.literal(
                    "Spend your available skill points in the Skill Tree to continue leveling."
            ), false);
        }
    }

    // =========================================================
    // ERROR MESSAGE
    // =========================================================

    private static void sendError(
            ServerPlayerEntity player,
            String message
    ) {

        player.sendMessage(
                Text.literal(
                        "\u00A7cSmooth Progression: \u00A7f"
                                + message
                ),
                false
        );
    }
}