package org.marj4n.smooth_progression.integration;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.Skill;
import net.puffish.skillsmod.api.SkillsAPI;

import org.marj4n.smooth_progression.progression.ProgressionManager;

import java.util.List;
import java.util.Optional;

public final class PufferfishSkillsIntegration {

    public static final int POINTS_PER_LEVEL = 2;

    // Keep the original source ID to preserve existing save data.
    private static final Identifier LEVEL_UP_SOURCE =
            new Identifier("smoothprogression", "level_up");

    private static final Identifier TREE_ID =
            new Identifier("simplyskills", "tree");

    private static final Identifier ASCENDANCY_ID =
            new Identifier("simplyskills", "ascendancy");

    private static final List<Identifier> CLASS_IDS = List.of(
            new Identifier("simplyskills", "berserker"),
            new Identifier("simplyskills", "cleric"),
            new Identifier("simplyskills", "crusader"),
            new Identifier("simplyskills", "necromancer"),
            new Identifier("simplyskills", "ranger"),
            new Identifier("simplyskills", "rogue"),
            new Identifier("simplyskills", "spellblade"),
            new Identifier("simplyskills", "wizard")
    );

    public enum Stage {
        TREE,
        CLASS,
        ASCENDANCY,
        WAITING_FOR_CLASS,
        WAITING_FOR_ASCENDANCY,
        INVALID_CLASS_SELECTION,
        MAX_LEVEL,
        UNAVAILABLE
    }

    public record Destination(
            Stage stage,
            Optional<Category> category
    ) {
    }

    private PufferfishSkillsIntegration() {
    }

    // =========================================================
    // CATEGORY LOOKUP
    // =========================================================

    public static Optional<Category> getGeneralTree() {
        return SkillsAPI.getCategory(TREE_ID);
    }

    public static Optional<Category> getAscendancy() {
        return SkillsAPI.getCategory(ASCENDANCY_ID);
    }

    private static List<Category> getUnlockedClasses(
            ServerPlayerEntity player
    ) {

        return CLASS_IDS.stream()
                .map(SkillsAPI::getCategory)
                .flatMap(Optional::stream)
                .filter(category -> category.isUnlocked(player))
                .toList();
    }

    public static Optional<Category> getSelectedClass(
            ServerPlayerEntity player
    ) {

        if (player == null) {
            return Optional.empty();
        }

        List<Category> classes = getUnlockedClasses(player);

        // Never guess if multiple classes are unlocked.
        if (classes.size() != 1) {
            return Optional.empty();
        }

        return Optional.of(classes.get(0));
    }

    public static boolean hasSelectedClass(
            ServerPlayerEntity player
    ) {
        return getSelectedClass(player).isPresent();
    }

    // =========================================================
    // SKILL STATE
    // =========================================================

    public static boolean hasAffordableSkill(
            Category category,
            ServerPlayerEntity player
    ) {

        if (category == null || player == null) {
            return false;
        }

        return category.streamSkills()
                .anyMatch(skill ->
                        skill.getState(player)
                                == Skill.State.AFFORDABLE
                );
    }

    public static boolean hasAvailableSkill(
            Category category,
            ServerPlayerEntity player
    ) {

        if (category == null || player == null) {
            return false;
        }

        return category.streamSkills()
                .anyMatch(skill -> {

                    Skill.State state = skill.getState(player);

                    return state == Skill.State.AVAILABLE
                            || state == Skill.State.AFFORDABLE;
                });
    }

    public static boolean hasPurchasedSkill(
            Category category,
            ServerPlayerEntity player
    ) {

        if (category == null || player == null) {
            return false;
        }

        return category.streamSkills()
                .anyMatch(skill ->
                        skill.getState(player)
                                == Skill.State.UNLOCKED
                );
    }

    // =========================================================
    // CATEGORY COMPLETION
    //
    // EXCLUDED is ignored.
    // LOCKED is not considered purchasable here.
    //
    // Requires at least one purchased skill to avoid
    // completing a category that was never started.
    // =========================================================

    public static boolean isCategoryComplete(
            Category category,
            ServerPlayerEntity player
    ) {

        if (category == null || player == null) {
            return false;
        }

        if (!category.isUnlocked(player)) {
            return false;
        }

        // Category must have been used at least once.
        if (category.getSpentPoints(player) <= 0) {
            return false;
        }

        // No more skills that can be purchased or obtained.
        return category.streamSkills()
                .noneMatch(skill -> {

                    Skill.State state = skill.getState(player);

                    return state == Skill.State.AVAILABLE
                            || state == Skill.State.AFFORDABLE;
                });
    }

    public static boolean isGeneralTreeComplete(
            ServerPlayerEntity player
    ) {

        if (player == null) {
            return false;
        }

        return getGeneralTree()
                .map(category -> category.isUnlocked(player)
                        && category.getSpentPoints(player) >= 42
                        && category.getPointsLeft(player) == 0)
                .orElse(false);
    }

    public static boolean isClassComplete(
            ServerPlayerEntity player
    ) {

        return getSelectedClass(player)
                .map(category ->
                        isCategoryComplete(category, player)
                )
                .orElse(false);
    }

    public static boolean isAscendancyComplete(
            ServerPlayerEntity player
    ) {

        if (player == null) {
            return false;
        }

        return getAscendancy()
                .map(category ->
                        isCategoryComplete(category, player)
                )
                .orElse(false);
    }

    // =========================================================
    // LEGACY DEBUG HELPERS
    // =========================================================

    public static int getGeneralTreeSpentPoints(
            ServerPlayerEntity player
    ) {

        if (player == null) {
            return 0;
        }

        return getGeneralTree()
                .map(category ->
                        category.getSpentPoints(player)
                )
                .orElse(0);
    }

    public static int getGeneralTreePointsLeft(
            ServerPlayerEntity player
    ) {

        if (player == null) {
            return 0;
        }

        return getGeneralTree()
                .map(category ->
                        category.getPointsLeft(player)
                )
                .orElse(0);
    }

    // =========================================================
    // UNIVERSAL STAGE RESOLVER
    // =========================================================

    public static Destination resolveDestination(
            ServerPlayerEntity player
    ) {

        if (player == null) {
            return new Destination(
                    Stage.UNAVAILABLE,
                    Optional.empty()
            );
        }

        Optional<Category> tree = getGeneralTree();

        if (tree.isEmpty() || !tree.get().isUnlocked(player)) {
            return new Destination(
                    Stage.UNAVAILABLE,
                    Optional.empty()
            );
        }

        // Stage 1: General Tree.
        if (!isGeneralTreeComplete(player)) {
            return new Destination(Stage.TREE, tree);
        }

        // Stage 2: Selected class.
        List<Category> unlockedClasses = getUnlockedClasses(player);

        if (unlockedClasses.isEmpty()) {
            return new Destination(
                    Stage.WAITING_FOR_CLASS,
                    Optional.empty()
            );
        }

        if (unlockedClasses.size() != 1) {
            return new Destination(
                    Stage.INVALID_CLASS_SELECTION,
                    Optional.empty()
            );
        }

        Category selectedClass = unlockedClasses.get(0);

        if (!isCategoryComplete(selectedClass, player)) {
            return new Destination(
                    Stage.CLASS,
                    Optional.of(selectedClass)
            );
        }

        // Stage 3: Ascendancy. Only unlock after the class is complete.
        Optional<Category> ascendancy = getAscendancy();

        if (ascendancy.isPresent() && !ascendancy.get().isUnlocked(player)) {
            ascendancy.get().unlock(player);
        }

        if (ascendancy.isEmpty()
                || !ascendancy.get().isUnlocked(player)) {

            return new Destination(
                    Stage.WAITING_FOR_ASCENDANCY,
                    Optional.empty()
            );
        }

        if (!isCategoryComplete(ascendancy.get(), player)) {
            return new Destination(
                    Stage.ASCENDANCY,
                    ascendancy
            );
        }

        // All three stages completed.
        return new Destination(
                Stage.MAX_LEVEL,
                Optional.empty()
        );
    }

    public static boolean isMaxLevel(
            ServerPlayerEntity player
    ) {

        return resolveDestination(player).stage()
                == Stage.MAX_LEVEL;
    }

    // =========================================================
    // POINT DESTINATION
    //
    // currentLevel retained for compatibility with
    // existing LevelManager callers.
    // =========================================================

    public static Optional<Category> getPointDestination(
            ServerPlayerEntity player,
            int currentLevel
    ) {

        return resolveDestination(player).category();
    }

    // =========================================================
    // POINT VALIDATION
    // =========================================================

    public static boolean canAwardSkillPoints(
            ServerPlayerEntity player,
            int currentLevel,
            int amount
    ) {

        if (player == null || amount < 0) {
            return false;
        }

        Optional<Category> optionalCategory =
                getPointDestination(player, currentLevel);

        if (optionalCategory.isEmpty()) {
            return false;
        }

        Category category = optionalCategory.get();

        int currentPoints = category.getPoints(
                player,
                LEVEL_UP_SOURCE
        );

        return (long) currentPoints + amount
                <= Integer.MAX_VALUE;
    }

    // =========================================================
    // AWARD POINTS
    // =========================================================

    public static boolean addSkillPoints(
            ServerPlayerEntity player,
            int currentLevel,
            int amount
    ) {

        if (!canAwardSkillPoints(
                player,
                currentLevel,
                amount
        )) {
            return false;
        }

        if (amount == 0) {
            return true;
        }

        Optional<Category> category =
                getPointDestination(player, currentLevel);

        if (category.isEmpty()) {
            return false;
        }

        category.get().addPoints(
                player,
                LEVEL_UP_SOURCE,
                amount
        );

        return true;
    }

    // =========================================================
    // LEGACY METHODS
    // =========================================================

    public static void onPlayerLevelUp(
            ServerPlayerEntity player
    ) {

        if (player == null) {
            return;
        }

        int level = ProgressionManager.get(player).getLevel();

        addSkillPoints(
                player,
                level,
                POINTS_PER_LEVEL
        );
    }

    public static boolean canAwardSkillPoints(
            ServerPlayerEntity player,
            int amount
    ) {

        if (player == null) {
            return false;
        }

        int level = ProgressionManager.get(player).getLevel();

        return canAwardSkillPoints(
                player,
                level,
                amount
        );
    }

    public static boolean addSkillPoints(
            ServerPlayerEntity player,
            int amount
    ) {

        if (player == null) {
            return false;
        }

        int level = ProgressionManager.get(player).getLevel();

        return addSkillPoints(
                player,
                level,
                amount
        );
    }

    // =========================================================
    // TOTAL POINTS AWARDED BY SMOOTH PROGRESSION
    // =========================================================

    public static int getLevelUpPoints(
            ServerPlayerEntity player
    ) {

        if (player == null) {
            return 0;
        }

        long total = SkillsAPI.streamCategories()
                .filter(category ->
                        category.getId()
                                .getNamespace()
                                .equals("simplyskills")
                )
                .mapToLong(category ->
                        category.getPoints(
                                player,
                                LEVEL_UP_SOURCE
                        )
                )
                .sum();

        return (int) Math.min(
                total,
                Integer.MAX_VALUE
        );
    }
}