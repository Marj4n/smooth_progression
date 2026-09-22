package org.marj4n.smooth_progression.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.criterion.ImpossibleCriterion;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.AdvancementToast;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import org.marj4n.smooth_progression.network.ProgressionSync;

public final class ClientProgression implements ClientModInitializer {

    private static volatile int level = 1;
    private static volatile long xp = 0;
    private static volatile boolean max = false;
    private static volatile boolean received = false;

    @Override
    public void onInitializeClient() {

        ClientPlayNetworking.registerGlobalReceiver(
                ProgressionSync.CHANNEL,
                (client, handler, buf, sender) -> {

                    int incomingLevel = buf.readVarInt();
                    long incomingXp = buf.readLong();
                    boolean incomingMax = buf.readBoolean();

                    client.execute(() -> {

                        if (received && incomingLevel > level) {
                            showLevelUpToast(client, incomingLevel);
                        }

                        level = incomingLevel;
                        xp = incomingXp;
                        max = incomingMax;
                        received = true;
                    });
                }
        );

        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> client.execute(
                        ClientProgression::reset
                )
        );
    }

    private static void showLevelUpToast(
            MinecraftClient client,
            int newLevel
    ) {

        Advancement advancement = Advancement.Builder.create()
                .display(
                        Items.EXPERIENCE_BOTTLE,
                        Text.literal("Level " + newLevel + " Reached!"),
                        Text.literal("Your adventure continues."),
                        null,
                        AdvancementFrame.CHALLENGE,
                        true,
                        false,
                        false
                )
                .criterion(
                        "level_up",
                        new ImpossibleCriterion.Conditions()
                )
                .build(
                        new Identifier(
                                "smooth_progression",
                                "level_up_toast"
                        )
                );

        client.getToastManager().add(
                new AdvancementToast(advancement)
        );
    }

    private static void reset() {
        level = 1;
        xp = 0;
        max = false;
        received = false;
    }

    public static boolean ready() {
        return received;
    }

    public static int level() {
        return level;
    }

    public static long xp() {
        return xp;
    }

    public static boolean max() {
        return max;
    }

    public static long required() {

        long l = Math.max(1, level);

        if (l > 1_358_187_913L) {
            return Long.MAX_VALUE;
        }

        return 100L + 5L * l * l;
    }

    public static float progress() {

        if (max) {
            return 1.0f;
        }

        return (float) Math.min(
                1.0d,
                (double) xp / (double) required()
        );
    }

    public static int safeInt(long value) {

        return (int) Math.min(
                Integer.MAX_VALUE,
                Math.max(0L, value)
        );
    }
}