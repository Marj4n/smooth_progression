package org.marj4n.smooth_progression.progression;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ProgressionState extends PersistentState {

    private final Map<UUID, PlayerProgression> progressions =
            new HashMap<>();

    public PlayerProgression getOrCreate(
            ServerPlayerEntity player
    ) {

        return progressions.computeIfAbsent(
                player.getUuid(),
                uuid -> {
                    markDirty();
                    return new PlayerProgression();
                }
        );
    }

    public void markProgressionDirty() {
        markDirty();
    }

    public void remove(
            ServerPlayerEntity player
    ) {

        progressions.remove(player.getUuid());

        markDirty();
    }

    public static ProgressionState fromNbt(
            NbtCompound nbt
    ) {

        ProgressionState state =
                new ProgressionState();

        int count = nbt.getInt("Count");

        for (int i = 0; i < count; i++) {

            String uuidString =
                    nbt.getString("UUID_" + i);

            if (uuidString.isEmpty()) {
                continue;
            }

            try {

                UUID uuid =
                        UUID.fromString(uuidString);

                NbtCompound data =
                        nbt.getCompound(
                                "Progression_" + i
                        );

                PlayerProgression progression =
                        new PlayerProgression();

                progression.setLevel(
                        data.getInt("Level")
                );

                // Reads the old numeric Int tag as well.
                progression.setExperience(
                        data.getLong("Experience")
                );

                progression.setPowerLevel(
                        data.getInt("PowerLevel")
                );

                state.progressions.put(
                        uuid,
                        progression
                );

            } catch (IllegalArgumentException ignored) {
                // Ignore invalid UUID entries.
            }
        }

        return state;
    }

    @Override
    public NbtCompound writeNbt(
            NbtCompound nbt
    ) {

        nbt.putInt(
                "Count",
                progressions.size()
        );

        int index = 0;

        for (Map.Entry<UUID, PlayerProgression> entry :
                progressions.entrySet()) {

            UUID uuid = entry.getKey();

            PlayerProgression progression =
                    entry.getValue();

            nbt.putString(
                    "UUID_" + index,
                    uuid.toString()
            );

            NbtCompound data =
                    new NbtCompound();

            data.putInt(
                    "Level",
                    progression.getLevel()
            );

            data.putLong(
                    "Experience",
                    progression.getExperience()
            );

            data.putInt(
                    "PowerLevel",
                    progression.getPowerLevel()
            );

            nbt.put(
                    "Progression_" + index,
                    data
            );

            index++;
        }

        return nbt;
    }
}