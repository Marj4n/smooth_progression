package org.marj4n.smooth_progression.progression;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import org.marj4n.smooth_progression.integration.PufferfishSkillsIntegration;

import java.math.BigInteger;

public final class LevelManager {

    // =========================================================
    // CONFIGURATION
    // =========================================================

    public static final int MAX_ADMIN_LEVEL_GAIN = 10_000;

    private static final BigInteger BIG_ZERO =
            BigInteger.ZERO;

    private static final BigInteger BIG_ONE =
            BigInteger.ONE;

    private static final BigInteger BIG_TWO =
            BigInteger.valueOf(2L);

    private static final BigInteger BIG_FIVE =
            BigInteger.valueOf(5L);

    private static final BigInteger BIG_SIX =
            BigInteger.valueOf(6L);

    private static final BigInteger BIG_HUNDRED =
            BigInteger.valueOf(100L);

    private LevelManager() {
    }

    // =========================================================
    // ADD EXPERIENCE
    // =========================================================

    public static void addExperience(
            ServerPlayerEntity player,
            long amount
    ) {

        if (player == null || amount <= 0L) {
            return;
        }

        ProgressionState state =
                ProgressionManager.getState(player);

        PlayerProgression progression =
                state.getOrCreate(player);

        long currentXp =
                progression.getExperience();

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
    // SET EXPERIENCE
    // =========================================================

    public static void setExperience(
            ServerPlayerEntity player,
            long amount
    ) {

        if (player == null || amount < 0L) {
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
    // APPLY EXPERIENCE
    // =========================================================

    private static void applyExperience(
            ServerPlayerEntity player,
            PlayerProgression progression,
            ProgressionState state,
            long newExperience
    ) {

        int startingLevel =
                progression.getLevel();

        BigInteger availableExperience =
                BigInteger.valueOf(newExperience);

        int targetLevel =
                findTargetLevel(
                        startingLevel,
                        availableExperience
                );

        long gainedLevels =
                (long) targetLevel - startingLevel;

        // Calculate skill points using long first.
        long pointsLong =
                gainedLevels
                        * PufferfishSkillsIntegration.POINTS_PER_LEVEL;

        // Never allow int overflow.
        if (pointsLong > Integer.MAX_VALUE) {

            sendError(
                    player,
                    "Skill point reward exceeds integer limit."
            );

            return;
        }

        int points = (int) pointsLong;

        // Validate before changing XP or level.
        if (gainedLevels > 0
                && !PufferfishSkillsIntegration
                .canAwardSkillPoints(player, points)) {

            sendError(
                    player,
                    "Unable to award skill points. "
                            + "Check SimplySkills Tree."
            );

            return;
        }

        // Total XP spent to reach targetLevel.
        BigInteger spentExperience =
                getExperienceBetweenLevels(
                        startingLevel,
                        targetLevel
                );

        BigInteger remainingExperience =
                availableExperience.subtract(
                        spentExperience
                );

        // The remaining XP cannot exceed the supplied
        // long value because spentExperience is nonnegative.
        long remainingXp =
                remainingExperience.longValueExact();

        // Award all skill points in one operation.
        if (gainedLevels > 0) {

            boolean awarded =
                    PufferfishSkillsIntegration.addSkillPoints(
                            player,
                            points
                    );

            if (!awarded) {

                sendError(
                        player,
                        "Skill point award failed. "
                                + "Level was not changed."
                );

                return;
            }
        }

        // Apply progression only after reward succeeds.
        progression.setLevel(targetLevel);

        progression.setExperience(remainingXp);

        state.markProgressionDirty();

        if (gainedLevels > 0) {

            player.sendMessage(
                    Text.literal(
                            "§aLevel Up! §f"
                                    + startingLevel
                                    + " §7→ §a"
                                    + targetLevel
                                    + "\n§eLevels gained: §f"
                                    + gainedLevels
                                    + "\n§eSkill points awarded: §f"
                                    + points
                    ),
                    false
            );
        }
    }

    // =========================================================
    // FIND TARGET LEVEL
    //
    // Binary search:
    // O(log Integer.MAX_VALUE)
    //
    // Does not loop once per level.
    // =========================================================

    private static int findTargetLevel(
            int startingLevel,
            BigInteger availableExperience
    ) {

        if (startingLevel >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        long low = startingLevel;

        long high = Integer.MAX_VALUE;

        while (low < high) {

            long middle =
                    low + (high - low + 1L) / 2L;

            BigInteger required =
                    getExperienceBetweenLevels(
                            startingLevel,
                            (int) middle
                    );

            if (required.compareTo(
                    availableExperience
            ) <= 0) {

                low = middle;

            } else {

                high = middle - 1L;
            }
        }

        return (int) low;
    }

    // =========================================================
    // CUMULATIVE EXPERIENCE
    //
    // XP(level) = 100 + 5 * level^2
    //
    // Sum from startLevel to endLevel - 1.
    //
    // Example:
    //
    // getExperienceBetweenLevels(1, 3)
    //
    // = XP(1) + XP(2)
    // = 105 + 120
    // = 225
    // =========================================================

    private static BigInteger getExperienceBetweenLevels(
            int startLevel,
            int endLevel
    ) {

        if (endLevel <= startLevel) {
            return BIG_ZERO;
        }

        BigInteger start =
                BigInteger.valueOf(startLevel);

        BigInteger end =
                BigInteger.valueOf(endLevel);

        BigInteger levelCount =
                end.subtract(start);

        BigInteger baseExperience =
                BIG_HUNDRED.multiply(levelCount);

        BigInteger squaredSum =
                sumOfSquares(
                        end.subtract(BIG_ONE)
                ).subtract(
                        sumOfSquares(
                                start.subtract(BIG_ONE)
                        )
                );

        return baseExperience.add(
                BIG_FIVE.multiply(squaredSum)
        );
    }

    // =========================================================
    // SUM OF SQUARES
    //
    // 1^2 + 2^2 + ... + n^2
    //
    // = n(n + 1)(2n + 1) / 6
    // =========================================================

    private static BigInteger sumOfSquares(
            BigInteger n
    ) {

        if (n.signum() <= 0) {
            return BIG_ZERO;
        }

        return n.multiply(
                n.add(BIG_ONE)
        ).multiply(
                n.multiply(BIG_TWO).add(BIG_ONE)
        ).divide(
                BIG_SIX
        );
    }

    // =========================================================
    // SET LEVEL
    // =========================================================

    /**
     * Increase level directly and award skill points.
     *
     * Returns the actual number of levels gained.
     *
     * Lowering the level is intentionally not supported.
     */
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

        int currentLevel =
                progression.getLevel();

        if (targetLevel <= currentLevel) {
            return 0;
        }

        long difference =
                (long) targetLevel - currentLevel;

        if (difference > MAX_ADMIN_LEVEL_GAIN) {
            return 0;
        }

        int gainedLevels =
                (int) difference;

        long pointsLong =
                difference
                        * PufferfishSkillsIntegration.POINTS_PER_LEVEL;

        if (pointsLong > Integer.MAX_VALUE) {

            sendError(
                    player,
                    "Skill point reward exceeds integer limit."
            );

            return 0;
        }

        int points = (int) pointsLong;

        // Do not change level if the reward cannot be given.
        if (!PufferfishSkillsIntegration
                .addSkillPoints(player, points)) {

            sendError(
                    player,
                    "Unable to award skill points. "
                            + "Level was not changed."
            );

            return 0;
        }

        progression.setLevel(targetLevel);

        progression.setExperience(0L);

        state.markProgressionDirty();

        player.sendMessage(
                Text.literal(
                        "§aLevel Up! §f"
                                + currentLevel
                                + " §7→ §a"
                                + targetLevel
                                + "\n§eSkill points awarded: §f"
                                + points
                ),
                false
        );

        return gainedLevels;
    }

    // =========================================================
    // ADD LEVEL
    // =========================================================

    public static int addLevel(
            ServerPlayerEntity player,
            int amount
    ) {

        if (player == null || amount <= 0) {
            return 0;
        }

        if (amount > MAX_ADMIN_LEVEL_GAIN) {
            return 0;
        }

        PlayerProgression progression =
                ProgressionManager.get(player);

        int currentLevel =
                progression.getLevel();

        if (amount > Integer.MAX_VALUE - currentLevel) {
            return 0;
        }

        return setLevel(
                player,
                currentLevel + amount
        );
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
                        "§cSmooth Progression: §f"
                                + message
                ),
                false
        );
    }
}