package org.marj4n.smooth_progression.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import org.marj4n.smooth_progression.progression.LevelManager;
import org.marj4n.smooth_progression.progression.ProgressionManager;

public final class ProgressionCommands {

    private ProgressionCommands() {
    }

    public static void register() {

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> {

                    dispatcher.register(
                            CommandManager.literal("sp")
                                    .then(
                                            CommandManager.literal("xp")
                                                    .then(
                                                            CommandManager.literal("add")
                                                                    .then(
                                                                            CommandManager.argument(
                                                                                            "amount",
                                                                                            IntegerArgumentType.integer(1)
                                                                                    )
                                                                                    .executes(context -> {

                                                                                        var player =
                                                                                                context.getSource()
                                                                                                        .getPlayerOrThrow();

                                                                                        int amount =
                                                                                                IntegerArgumentType.getInteger(
                                                                                                        context,
                                                                                                        "amount"
                                                                                                );

                                                                                        LevelManager.addExperience(
                                                                                                player,
                                                                                                amount
                                                                                        );

                                                                                        context.getSource()
                                                                                                .sendFeedback(
                                                                                                        () -> Text.literal(
                                                                                                                "Added "
                                                                                                                        + amount
                                                                                                                        + " XP."
                                                                                                        ),
                                                                                                        false
                                                                                                );

                                                                                        return 1;
                                                                                    })
                                                                    )
                                                    )
                                    )
                    );

                    dispatcher.register(
                            CommandManager.literal("sp")
                                    .then(
                                            CommandManager.literal("level")
                                                    .executes(context -> {

                                                        var player =
                                                                context.getSource()
                                                                        .getPlayerOrThrow();

                                                        var progression =
                                                                ProgressionManager.get(player);

                                                        context.getSource()
                                                                .sendFeedback(
                                                                        () -> Text.literal(
                                                                                "Level: "
                                                                                        + progression.getLevel()
                                                                                        + " | XP: "
                                                                                        + progression.getExperience()
                                                                        ),
                                                                        false
                                                                );

                                                        return 1;
                                                    })
                                    )
                    );
                }
        );
    }
}