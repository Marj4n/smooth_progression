package org.marj4n.smooth_progression.network;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import org.marj4n.smooth_progression.progression.PlayerProgression;
import org.marj4n.smooth_progression.progression.ProgressionManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ProgressionSync {
    public static final Identifier CHANNEL = new Identifier("smoothprogression", "ui_progression");
    private static final Map<UUID, Snapshot> LAST = new HashMap<>();
    private record Snapshot(int level, long xp, boolean max) {}
    private ProgressionSync() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 10 != 0) return;
            LAST.keySet().removeIf(id -> server.getPlayerManager().getPlayer(id) == null);
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (!ServerPlayNetworking.canSend(player, CHANNEL)) continue;
                PlayerProgression progression = ProgressionManager.get(player);
                boolean max = org.marj4n.smooth_progression.integration.PufferfishSkillsIntegration.isMaxLevel(player);
                Snapshot next = new Snapshot(progression.getLevel(), progression.getExperience(), max);
                if (next.equals(LAST.get(player.getUuid()))) continue;
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeVarInt(next.level());
                buf.writeLong(next.xp());
                buf.writeBoolean(next.max());
                ServerPlayNetworking.send(player, CHANNEL, buf);
                LAST.put(player.getUuid(), next);
            }
        });
    }
}
