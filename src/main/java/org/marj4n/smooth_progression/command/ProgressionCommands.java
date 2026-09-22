package org.marj4n.smooth_progression.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import org.marj4n.smooth_progression.progression.ExperienceManager;
import org.marj4n.smooth_progression.progression.LevelManager;
import org.marj4n.smooth_progression.progression.PlayerProgression;
import org.marj4n.smooth_progression.progression.ProgressionManager;

import org.marj4n.smooth_progression.integration.SimplySkillsIntegration;

public final class ProgressionCommands {

    private ProgressionCommands() {
    }

    // =========================================================
    // REGISTER
    // =========================================================

    public static void register() {

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> {

                    dispatcher.register(
                            CommandManager.literal("sp")

                                    .executes(context ->
                                            showHelp(context.getSource())
                                    )

                                    // LEVEL
                                    .then(
                                            CommandManager.literal("level")

                                                    .executes(context ->
                                                            showLevel(
                                                                    context.getSource()
                                                            )
                                                    )

                                                    // /sp level set <level>
                                                    .then(
                                                            CommandManager.literal("set")
                                                                    .requires(source ->
                                                                            source.hasPermissionLevel(2)
                                                                    )
                                                                    .then(
                                                                            CommandManager.argument(
                                                                                            "level",
                                                                                            IntegerArgumentType.integer(1)
                                                                                    )
                                                                                    .executes(context ->
                                                                                            setLevel(
                                                                                                    context.getSource(),
                                                                                                    IntegerArgumentType.getInteger(
                                                                                                            context,
                                                                                                            "level"
                                                                                                    )
                                                                                            )
                                                                                    )
                                                                    )
                                                    )

                                                    // /sp level add <amount>
                                                    .then(
                                                            CommandManager.literal("add")
                                                                    .requires(source ->
                                                                            source.hasPermissionLevel(2)
                                                                    )
                                                                    .then(
                                                                            CommandManager.argument(
                                                                                            "amount",
                                                                                            IntegerArgumentType.integer(1)
                                                                                    )
                                                                                    .executes(context ->
                                                                                            addLevel(
                                                                                                    context.getSource(),
                                                                                                    IntegerArgumentType.getInteger(
                                                                                                            context,
                                                                                                            "amount"
                                                                                                    )
                                                                                            )
                                                                                    )
                                                                    )
                                                    )

                                                    // /sp level reset
                                                    .then(
                                                            CommandManager.literal("reset")
                                                                    .requires(source ->
                                                                            source.hasPermissionLevel(2)
                                                                    )
                                                                    .executes(context ->
                                                                            resetLevel(
                                                                                    context.getSource()
                                                                            )
                                                                    )
                                                    )
                                    )

                                    // XP
                                    .then(
                                            CommandManager.literal("xp")

                                                    .executes(context ->
                                                            showLevel(
                                                                    context.getSource()
                                                            )
                                                    )

                                                    // /sp xp add <amount>
                                                    .then(
                                                            CommandManager.literal("add")
                                                                    .requires(source ->
                                                                            source.hasPermissionLevel(2)
                                                                    )
                                                                    .then(
                                                                            CommandManager.argument(
                                                                                            "amount",
                                                                                            LongArgumentType.longArg(1L)
                                                                                    )
                                                                                    .executes(context ->
                                                                                            addExperience(
                                                                                                    context.getSource(),
                                                                                                    LongArgumentType.getLong(
                                                                                                            context,
                                                                                                            "amount"
                                                                                                    )
                                                                                            )
                                                                                    )
                                                                    )
                                                    )

                                                    // /sp xp set <amount>
                                                    .then(
                                                            CommandManager.literal("set")
                                                                    .requires(source ->
                                                                            source.hasPermissionLevel(2)
                                                                    )
                                                                    .then(
                                                                            CommandManager.argument(
                                                                                            "amount",
                                                                                            LongArgumentType.longArg(0L)
                                                                                    )
                                                                                    .executes(context ->
                                                                                            setExperience(
                                                                                                    context.getSource(),
                                                                                                    LongArgumentType.getLong(
                                                                                                            context,
                                                                                                            "amount"
                                                                                                    )
                                                                                            )
                                                                                    )
                                                                    )
                                                    )
                                    )
                    );
                }
        );
    }

    // =========================================================
    // HELP
    // =========================================================

    private static int showHelp(
            ServerCommandSource source
    ) {

        source.sendFeedback(
                () -> Text.literal(
                        "§6=== Smooth Progression ===\n"
                                + "§e/sp level §7- Show progression\n"
                                + "§e/sp xp §7- Show progression"
                ),
                false
        );

        if (source.hasPermissionLevel(2)) {

            source.sendFeedback(
                    () -> Text.literal(
                            "§e/sp xp add <amount>\n"
                                    + "§e/sp xp set <amount>\n"
                                    + "§e/sp level set <level>\n"
                                    + "§e/sp level add <amount>\n"
                                    + "§e/sp level reset"
                    ),
                    false
            );
        }

        return 1;
    }

    // =========================================================
    // SHOW LEVEL
    // =========================================================

    private static int showLevel(
            ServerCommandSource source
    ) throws CommandSyntaxException {

        ServerPlayerEntity player =
                source.getPlayerOrThrow();

        PlayerProgression progression =
                ProgressionManager.get(player);

        long required =
                ExperienceManager.getRequiredExperience(
                        progression.getLevel()
                );

        String requiredText =
                required == Long.MAX_VALUE
                        ? "MAX"
                        : String.valueOf(required);

        source.sendFeedback(
                () -> Text.literal(
                        "§6=== Smooth Progression ===\n"
                                + "§eLevel: §f"
                                + progression.getLevel()
                                + "\n§eXP: §f"
                                + progression.getExperience()
                                + " §7/ §f"
                                + requiredText
                                + "\n§ePower Level: §f"
                                + progression.getPowerLevel()
                ),
                false
        );

        return 1;
    }

    // =========================================================
    // XP ADD
    // =========================================================

    private static int addExperience(
            ServerCommandSource source,
            long amount
    ) throws CommandSyntaxException {

        ServerPlayerEntity player =
                source.getPlayerOrThrow();

        LevelManager.addExperience(player, amount);

        PlayerProgression progression =
                ProgressionManager.get(player);

        source.sendFeedback(
                () -> Text.literal(
                        "§aAdded "
                                + amount
                                + " XP.\n§eCurrent Level: §f"
                                + progression.getLevel()
                                + "\n§eCurrent XP: §f"
                                + progression.getExperience()
                ),
                false
        );

        return 1;
    }

    // =========================================================
    // XP SET
    // =========================================================

    private static int setExperience(
            ServerCommandSource source,
            long amount
    ) throws CommandSyntaxException {

        ServerPlayerEntity player =
                source.getPlayerOrThrow();

        LevelManager.setExperience(player, amount);

        PlayerProgression progression =
                ProgressionManager.get(player);

        long required =
                ExperienceManager.getRequiredExperience(
                        progression.getLevel()
                );

        source.sendFeedback(
                () -> Text.literal(
                        "§aXP set and processed.\n"
                                + "§eLevel: §f"
                                + progression.getLevel()
                                + "\n§eXP: §f"
                                + progression.getExperience()
                                + " / "
                                + required
                ),
                false
        );

        return 1;
    }

    // =========================================================
    // LEVEL SET
    // =========================================================

    private static int setLevel(
            ServerCommandSource source,
            int level
    ) throws CommandSyntaxException {

        ServerPlayerEntity player =
                source.getPlayerOrThrow();

        int oldLevel =
                ProgressionManager.get(player).getLevel();

        if (level < oldLevel) {

            source.sendError(
                    Text.literal(
                            "Cannot lower level with /sp level set. "
                                    + "Use /sp level reset."
                    )
            );

            return 0;
        }

        if (level == oldLevel) {

            source.sendFeedback(
                    () -> Text.literal(
                            "§eAlready level " + oldLevel + "."
                    ),
                    false
            );

            return 1;
        }

        long difference =
                (long) level - oldLevel;

        if (difference > LevelManager.MAX_ADMIN_LEVEL_GAIN) {

            source.sendError(
                    Text.literal(
                            "Maximum level gain per command: "
                                    + LevelManager.MAX_ADMIN_LEVEL_GAIN
                    )
            );

            return 0;
        }

        int gained =
                LevelManager.setLevel(player, level);

        source.sendFeedback(
                () -> Text.literal(
                        "§aLevel set to "
                                + level
                                + ".\n§eLevels gained: §f"
                                + gained
                                + "\n§eSkill points awarded: §f"
                                + (gained * 2)
                ),
                false
        );

        return 1;
    }

    // =========================================================
    // LEVEL ADD
    // =========================================================

    private static int addLevel(
            ServerCommandSource source,
            int amount
    ) throws CommandSyntaxException {

        ServerPlayerEntity player =
                source.getPlayerOrThrow();

        if (amount > LevelManager.MAX_ADMIN_LEVEL_GAIN) {

            source.sendError(
                    Text.literal(
                            "Maximum level gain per command: "
                                    + LevelManager.MAX_ADMIN_LEVEL_GAIN
                    )
            );

            return 0;
        }

        int gained =
                LevelManager.addLevel(player, amount);

        if (gained <= 0) {

            source.sendError(
                    Text.literal(
                            "Could not add levels. "
                                    + "Level limit or invalid amount."
                    )
            );

            return 0;
        }

        PlayerProgression progression =
                ProgressionManager.get(player);

        source.sendFeedback(
                () -> Text.literal(
                        "§aAdded "
                                + gained
                                + " levels.\n"
                                + "§eCurrent Level: §f"
                                + progression.getLevel()
                                + "\n§eSkill points awarded: §f"
                                + (gained * 2)
                ),
                false
        );

        return 1;
    }

    // =========================================================
    // LEVEL RESET
    // =========================================================

    private static int resetLevel(
            ServerCommandSource source
    ) throws CommandSyntaxException {

        ServerPlayerEntity player =
                source.getPlayerOrThrow();

        PlayerProgression progression =
                ProgressionManager.get(player);

        // Reset SimplySkills first.
        SimplySkillsIntegration.resetSkillsAndPoints(player);

        // Reset Smooth Progression.
        progression.setLevel(1);
        progression.setExperience(0L);
        progression.setPowerLevel(0);

        ProgressionManager.markDirty(player);

        source.sendFeedback(
                () -> Text.literal(
                        "§aSmooth Progression reset.\n"
                                + "§eLevel: §f1\n"
                                + "§eXP: §f0\n"
                                + "§ePower Level: §f0\n"
                                + "§7SimplySkills skills and points reset attempted."
                ),
                false
        );

        return 1;
    }
}