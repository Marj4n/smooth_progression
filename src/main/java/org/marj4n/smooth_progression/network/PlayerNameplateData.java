package org.marj4n.smooth_progression.network;

import net.minecraft.util.Identifier;
import net.minecraft.network.PacketByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Shared wire format; deliberately independent of client and server classes. */
public final class PlayerNameplateData {
    public static final Identifier CHANNEL = new Identifier("smoothprogression", "player_nameplates");
    public static final int UPDATE = 0;
    public static final int REMOVE = 1;
    public static final int MAX_BATCH_SIZE = 64;
    public static final int MAX_CLASS_ID_LENGTH = 64;

    public record Snapshot(UUID playerId, int level, String classId) {}

    /** A batch payload excludes its UPDATE operation prefix. */
    public static void writeSnapshots(PacketByteBuf buf, List<Snapshot> entries) {
        if (entries.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("Nameplate batch is too large");
        }
        buf.writeVarInt(entries.size());
        for (Snapshot entry : entries) {
            buf.writeUuid(entry.playerId());
            buf.writeVarInt(entry.level());
            buf.writeString(entry.classId(), MAX_CLASS_ID_LENGTH);
        }
    }

    public static List<Snapshot> readSnapshots(PacketByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("Invalid nameplate batch size: " + count);
        }
        List<Snapshot> entries = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            entries.add(new Snapshot(buf.readUuid(), buf.readVarInt(),
                    buf.readString(MAX_CLASS_ID_LENGTH)));
        }
        return entries;
    }

    private PlayerNameplateData() {}
}
