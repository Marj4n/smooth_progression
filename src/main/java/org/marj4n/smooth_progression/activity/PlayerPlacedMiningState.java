package org.marj4n.smooth_progression.activity;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

public final class PlayerPlacedMiningState extends PersistentState {
    private static final String STATE_ID = "smoothprogression_player_placed_mining";
    private static final String NBT_POSITIONS = "Positions";

    // Primitive long collection avoids boxing every BlockPos.asLong() into Long.
    // Persistence format stays exactly the same (long[]), so existing worlds remain compatible.
    private final LongSet positions = new LongOpenHashSet();

    public PlayerPlacedMiningState() {}

    public static PlayerPlacedMiningState fromNbt(NbtCompound nbt) {
        PlayerPlacedMiningState state = new PlayerPlacedMiningState();
        state.positions.addAll(LongOpenHashSet.of(nbt.getLongArray(NBT_POSITIONS)));
        return state;
    }

    public static PlayerPlacedMiningState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                PlayerPlacedMiningState::fromNbt,
                PlayerPlacedMiningState::new,
                STATE_ID
        );
    }

    public void markPlaced(BlockPos pos) {
        if (positions.add(pos.asLong())) {
            markDirty();
        }
    }

    public boolean consumePlaced(BlockPos pos) {
        if (!positions.remove(pos.asLong())) {
            return false;
        }

        markDirty();
        return true;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putLongArray(NBT_POSITIONS, positions.toLongArray());
        return nbt;
    }
}
