package org.marj4n.smooth_progression.progression;

public final class ExperienceManager {

    // =========================================================
    // XP CURVE CONFIGURATION
    // =========================================================

    private static final long BASE_EXPERIENCE = 100L;

    private static final long QUADRATIC_MULTIPLIER = 5L;

    private ExperienceManager() {
    }

    // =========================================================
    // REQUIRED EXPERIENCE
    //
    // Formula:
    //
    // XP(level) = 100 + 5 * level^2
    //
    // Examples:
    //
    // Level 1   -> 105 XP
    // Level 10  -> 600 XP
    // Level 50  -> 12,600 XP
    // Level 100 -> 50,100 XP
    //
    // This is XP required for ONE level-up.
    // Not cumulative XP.
    // =========================================================

    public static long getRequiredExperience(int level) {

        if (level < 1) {
            level = 1;
        }

        long currentLevel = level;

        // int * int is safe after conversion to long.
        // Maximum int squared fits inside long.
        long squared = currentLevel * currentLevel;

        // Prevent overflow during multiplication/addition.
        if (squared >
                (Long.MAX_VALUE - BASE_EXPERIENCE)
                        / QUADRATIC_MULTIPLIER) {

            return Long.MAX_VALUE;
        }

        return BASE_EXPERIENCE
                + QUADRATIC_MULTIPLIER * squared;
    }

    // =========================================================
    // LEVEL-UP CHECK
    // =========================================================

    public static boolean canLevelUp(
            PlayerProgression progression
    ) {

        if (progression == null) {
            return false;
        }

        int currentLevel = progression.getLevel();

        // Prevent integer overflow.
        // This is a technical storage limit,
        // not a gameplay cap at level 100.
        if (currentLevel >= Integer.MAX_VALUE) {
            return false;
        }

        long requiredExperience =
                getRequiredExperience(currentLevel);

        return progression.getExperience()
                >= requiredExperience;
    }
}