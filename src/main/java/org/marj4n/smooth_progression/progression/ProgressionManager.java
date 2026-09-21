package org.marj4n.smooth_progression.progression;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentStateManager;

public final class ProgressionManager {

    private static final String STATE_ID =
            "smooth_progression";

    private ProgressionManager() {
    }

    public static ProgressionState getState(
            ServerPlayerEntity player
    ) {

        PersistentStateManager manager =
                player.getServerWorld()
                        .getPersistentStateManager();

        return manager.getOrCreate(
                ProgressionState::fromNbt,
                ProgressionState::new,
                STATE_ID
        );
    }

    public static PlayerProgression get(
            ServerPlayerEntity player
    ) {

        ProgressionState state =
                getState(player);

        return state.getOrCreate(player);
    }

    public static void markDirty(
            ServerPlayerEntity player
    ) {

        ProgressionState state =
                getState(player);

        state.markProgressionDirty();
    }

    public static void remove(
            ServerPlayerEntity player
    ) {

        ProgressionState state =
                getState(player);

        state.remove(player);
    }
}