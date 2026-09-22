package org.marj4n.smooth_progression.progression;

import net.minecraft.server.network.ServerPlayerEntity;
import org.marj4n.smooth_progression.network.ProgressionSync;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

public final class ProgressionManager {

    private static final String STATE_ID =
            "smooth_progression";

    private ProgressionManager() {
    }

    public static ProgressionState getState(
            ServerPlayerEntity player
    ) {

        PersistentStateManager manager =
                player.getServer()
                        .getWorld(World.OVERWORLD)
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

        ProgressionState state = getState(player);

        return state.getOrCreate(player);
    }

    public static void markDirty(
            ServerPlayerEntity player
    ) {

        getState(player).markProgressionDirty();
        ProgressionSync.request(player);
    }

    public static void remove(
            ServerPlayerEntity player
    ) {

        getState(player).remove(player);
        ProgressionSync.request(player);
    }
}